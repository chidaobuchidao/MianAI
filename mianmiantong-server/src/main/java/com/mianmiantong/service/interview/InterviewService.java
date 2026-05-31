package com.mianmiantong.service.interview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.interview.InterviewStartRequest;
import com.mianmiantong.entity.interview.InterviewSession;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.ai.AiModelSelector;
import com.mianmiantong.service.ai.AiService;
import com.mianmiantong.service.coding.AlgorithmProblemService;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.service.user.QuotaService;
import com.mianmiantong.service.user.UserAiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class InterviewService {

    @Value("${DEEPSEEK_API_KEY:}")
    private String systemApiKey;

    private final InterviewSessionMapper sessionMapper;
    private final AiService aiService;
    private final UserAiConfigService userAiConfigService;
    private final ResumeMapper resumeMapper;
    private final AlgorithmProblemService algorithmProblemService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final QuotaService quotaService;

    @Lazy @Autowired
    private InterviewService self;

        private static final String SYSTEM_PROMPT = """
        你是一位经验丰富的技术面试官"面面"，正在面试一位%s岗位的候选人。

        ## 核心规则（必须严格遵守）

        ### 1. 一次只问一个问题
        - 每条回复只包含一个明确的技术问题，绝对不要在一段话里列出多个问题!!!
        - 等候选人回答后，根据回答质量决定下一个问题的方向和深度
        - 不要问"你了解XXX吗"这种是非题，要问"为什么""怎么做的""遇到什么坑"

        ### 2. 逐步深入，自然对话
        - 从候选人的上一个回答切入，先简短反馈（一句话），再追问更深一层
        - 不要跳跃式切换话题，每个新问题要跟前一个问题有逻辑关联
        - 难度曲线：基础概念 → 原理追问 → 场景应用 → 边界/优化
        - 每3-4轮给一次简短评价，保持对话节奏

        ### 3. 面试流程
        开场 → 候选人自我介绍 → 逐步深入技术问答（3-5轮）
        → 如果你判断候选人表现良好：先说一段简短邀请语，然后独占一行输出 [笔试邀请]
        → 如果表现一般或连续2问无法回答：直接进入结束阶段

        ### 4. 自主触发笔试邀请
        当3-5轮问答后你判断候选人基础扎实，**主动**输出：
        先是自然的邀请语（如"前面聊得不错，要不要试试写一段代码？不参加也不影响面试结果"）
        然后独占一行：[笔试邀请]

        候选人接受后，系统会自动生成编程题并在编辑器展示，候选人写完代码会提交给你审查。
        收到代码后，从正确性、代码风格、时间复杂度、边界处理、可读性五个维度评审。

        ### 5. 自主结束面试
        满足以下任一条件时，输出 [面试结束] + JSON：
        - 5-10轮有效对话后，各维度有清晰判断
        - 候选人连续2问无法回答或明确表示不会
        - 候选人拒绝笔试邀请
        - 笔试审查完成后

        [面试结束]{"score":6,"feedback":"总评语（50-100字）","dimensions":[{"name":"基础掌握","score":7,"comment":"简评"},{"name":"表达清晰","score":6,"comment":"简评"},{"name":"深度思考","score":5,"comment":"简评"},{"name":"实战经验","score":4,"comment":"简评"},{"name":"学习潜力","score":6,"comment":"简评"}],"suggestion":"具体提升建议"}

        JSON必须一整行，不要换行，不要markdown包裹。

        ### 6. 评分维度（1-10分）
        1. 基础掌握 — 核心概念是否准确
        2. 表达清晰 — 能否简洁说清复杂问题
        3. 深度思考 — 是理解原理还是死记硬背
        4. 实战经验 — 是否有实际场景体感
        5. 学习潜力 — 面对不会的问题如何应对

        ## 风格
        - 专业但不死板，偶尔带点技术幽默感
        - 每次回复50-200字
        - 你是面试官，不要做自我介绍，不要以候选人身份说话
        - 候选人说"不会"，换方向问，不要死磕
        """;

    /** 手动结束面试时的评估 prompt — 独立于面试官角色，要求 AI 切换为评估者 */
    private static final String EVALUATION_PROMPT = """
        你是一位资深技术面试评估专家。你的任务是阅读以下面试对话记录，对候选人进行综合评估。

        ## 评估要求
        阅读完整的面试对话后，输出以下JSON格式的评估报告（JSON必须一整行，不要换行，不要markdown）：

        {"score":7,"feedback":"总评语（50-150字，评价技术深度、表达能力、逻辑思维、实战经验，指出亮点和不足）","dimensions":[{"name":"基础掌握","score":7,"comment":"简评（15字内）"},{"name":"表达清晰","score":6,"comment":"简评"},{"name":"深度思考","score":5,"comment":"简评"},{"name":"实战经验","score":4,"comment":"简评"},{"name":"学习潜力","score":6,"comment":"简评"}],"suggestion":"具体提升建议（30-80字）"}

        ## 评分维度（1-10分）
        1. 基础掌握 — 核心概念是否准确、扎实
        2. 表达清晰 — 能否简洁说清复杂问题
        3. 深度思考 — 是理解原理还是死记硬背
        4. 实战经验 — 是否有实际场景体感
        5. 学习潜力 — 面对不会的问题如何思考、是否诚实

        ## 评分原则
        - 在校生/实习生默认偏低（基础掌握6-7，实战经验4-6），特别优秀可给高分
        - 不要因为候选人说"不会"就扣分，反而看其面对不知的态度
        - 整体评分反映综合水平，不取维度分平均值

        ## 对话记录
        %s

        ## 输出格式
        严格按以下格式输出（不要任何额外文字）：
        [面试结束]{"score":整,"feedback":"评语","dimensions":[...],"suggestion":"建议"}
        """;

    public InterviewService(InterviewSessionMapper sessionMapper, AiService aiService,
                            UserAiConfigService userAiConfigService, ResumeMapper resumeMapper,
                            AlgorithmProblemService algorithmProblemService,
                            UserMapper userMapper, QuotaService quotaService) {
        this.sessionMapper = sessionMapper;
        this.aiService = aiService;
        this.userAiConfigService = userAiConfigService;
        this.resumeMapper = resumeMapper;
        this.algorithmProblemService = algorithmProblemService;
        this.userMapper = userMapper;
        this.quotaService = quotaService;
        this.objectMapper = new ObjectMapper();
    }

    private final Map<String, String> promptCache = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 50;
            }
        });

    /** 获取当前用户可用的 API Key（不扣配额）。Admin/有Key用户不限，无Key用户需有剩余配额。 */
    private String getUserApiKey() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        if (userId == null) return null;

        // User has their own API key → unlimited
        UserAiConfig config = userAiConfigService.getByUserId(userId);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return config.getApiKey();
        }

        // Admin → system key, unlimited
        if (JwtAuthFilter.isAdmin()) {
            return resolveSystemApiKey();
        }

        // Regular user → check daily quota (don't consume)
        quotaService.checkQuota();
        return resolveSystemApiKey();
    }

    /** 消耗配额 */
    private void consumeQuota(String model) {
        quotaService.checkAndConsume(JwtAuthFilter.getCurrentUserId(), AiModelSelector.normalize(model));
    }

    /** System-level API key (from env/config, for admin use) */
    private String resolveSystemApiKey() {
        if (systemApiKey == null || systemApiKey.isBlank()) {
            throw new IllegalStateException("系统 API Key 未配置，请设置 DEEPSEEK_API_KEY 环境变量");
        }
        return systemApiKey;
    }

    /** 开始面试 — AI自主生成第一个问题（同时清理旧记录，保留最近5条） */
    @Transactional
    public Map<String, Object> start(InterviewStartRequest request) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        String position = request.getPosition();
        String selectedModel = AiModelSelector.normalize(request.getModel());

        cleanupOldSessions(userId);

        // 如果携带了简历，获取简历文本并注入 System Prompt
        String resumeContext = "";
        if (request.getResumeId() != null) {
            Resume resume = resumeMapper.selectById(request.getResumeId());
            if (resume != null && resume.getParseStatus() == 1) {
                resumeContext = String.format("""

                    ## 候选人简历背景

                    %s
                    """, resume.getParsedText());
            }
        }

        String systemPrompt = String.format(SYSTEM_PROMPT, position) + resumeContext;

        List<Map<String, String>> initMessages = List.of(
            Map.of("role", "user", "content", "面试现在开始。请你作为面试官，先让候选人做一个简短的自我介绍，然后问第一个技术问题。注意：你是面试官，候选人会回答你的问题。")
        );

        log.info("\n" + "=".repeat(80) + "\n" +
                 "【AI面试启动】岗位: {} | 用户ID: {}\n" +
                 "=".repeat(80),
                 position, userId);

        String firstQuestion = aiService.chat(systemPrompt, initMessages, getUserApiKey(), selectedModel);

        log.info("\n" + "-".repeat(60) + "\n" +
                 "【AI原始输出 - 第1问】\n{}\n" +
                 "-".repeat(60),
                 firstQuestion);

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPosition(position);
        session.setCurrentQuestionIndex(0);
        session.setModel(selectedModel);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "assistant", "content", firstQuestion, "time", LocalDateTime.now().toString()));
        session.setMessages(toJson(messages));
        session.setStatus(0);

        sessionMapper.insert(session);

        consumeQuota(selectedModel); // 面试成功启动后才消耗

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("question", firstQuestion);
        result.put("questionIndex", 1);
        return result;
    }

    /** 回答问题 — AI评估 + 决定继续还是结束 */
    @Transactional
    public Map<String, Object> answer(Long sessionId, String answer) {
        Long userId = JwtAuthFilter.getCurrentUserId();

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("面试会话不存在");
        }
        if (session.getStatus() == 1) {
            throw new IllegalArgumentException("面试已结束");
        }

        // 编程环节拦截：不经过面试AI，单独调用出题AI
        if (answer.contains("[进入编程环节]")) {
            return handleCodingRound(session, answer);
        }

        List<Map<String, Object>> messages = parseMessages(session.getMessages());
        String systemPrompt = promptCache.computeIfAbsent(session.getPosition(), pos -> String.format(SYSTEM_PROMPT, pos));

        messages.add(Map.of("role", "user", "content", answer, "time", LocalDateTime.now().toString()));
        int nextIndex = session.getCurrentQuestionIndex() + 1;

        List<Map<String, String>> aiMessages = toAiMessages(messages);

        String contextHint = nextIndex < 3
            ? "请根据我的回答继续深入提问。"
            : nextIndex < 5
                ? "请根据我的回答追问或换一个领域提问。如果你觉得已经对我的水平有了初步判断，也可以继续深入。"
                : "请根据我的回答质量判断：如果已经充分了解我的水平（无论好坏），可以结束面试并给出报告；如果还需要考察，请继续提问。";

        aiMessages.add(Map.of("role", "user", "content", contextHint));

        log.info("\n" + "=".repeat(60) + "\n" +
                 "【第{}轮对话】sessionId: {}\n" +
                 "用户回答: {}\n" +
                 "-".repeat(40) + "\n" +
                 "对话上下文(最近3轮):\n{}\n" +
                 "-".repeat(40),
                 nextIndex, sessionId, answer, summarizeContext(aiMessages));

        String aiResponse = aiService.chat(systemPrompt, aiMessages, getUserApiKey(), AiModelSelector.normalize(session.getModel()));

        boolean finished = aiResponse.contains("[面试结束]");

        log.info("\n" + "-".repeat(40) + "\n" +
                 "【AI原始输出 - 第{}轮】(结束={})\n{}\n" +
                 "-".repeat(40),
                 nextIndex, finished, aiResponse);

        messages.add(Map.of("role", "assistant", "content", aiResponse, "time", LocalDateTime.now().toString()));
        session.setCurrentQuestionIndex(nextIndex);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);

        if (finished) {
            try {
                String jsonStr = aiResponse.substring(aiResponse.indexOf("{"), aiResponse.lastIndexOf("}") + 1);
                @SuppressWarnings("unchecked")
                Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);

                boolean hasInterviewReport = session.getFeedback() != null
                    && !session.getFeedback().isEmpty();

                if (hasInterviewReport) {
                    session.setCodingScore(report.get("score") != null
                        ? ((Number) report.get("score")).intValue() : 0);
                    session.setCodingDimensions(toJson(report.get("dimensions")));
                    session.setCodingFeedback((String) report.get("feedback"));
                    session.setCodingSuggestion((String) report.get("suggestion"));
                } else {
                    session.setOverallScore(report.get("score") != null
                        ? ((Number) report.get("score")).intValue() : 0);
                    session.setDimensions(toJson(report.get("dimensions")));
                    session.setFeedback((String) report.get("feedback"));
                }
                session.setStatus(1);
                session.setFinishTime(LocalDateTime.now());

                result.put("finished", true);
                result.put("report", report);
                result.put("hasCodingRound", hasInterviewReport);

                log.info("\n" + "=".repeat(60) + "\n" +
                         "【面试报告解析成功】总分: {} | 维度数: {} | 建议: {}\n" +
                         "完整报告:\n{}\n" +
                         "=".repeat(60),
                         session.getOverallScore(),
                         report.get("dimensions") instanceof List ? ((List<?>) report.get("dimensions")).size() : 0,
                         report.get("suggestion"),
                         toJson(report));

                String closingMsg = aiResponse.replace("[面试结束]", "").replace(jsonStr, "").trim();
                if (!closingMsg.isEmpty()) {
                    result.put("closingMessage", closingMsg);
                }
            } catch (Exception e) {
                log.warn("解析AI报告JSON失败: {}", e.getMessage());
                session.setOverallScore(6);
                session.setFeedback("面试已完成，感谢参与。");
                session.setStatus(1);
                session.setFinishTime(LocalDateTime.now());
                result.put("finished", true);
                result.put("report", Map.of("score", 6, "feedback", "面试已完成", "dimensions", List.of()));
            }
        } else {
            result.put("finished", false);
            result.put("question", aiResponse);
            result.put("questionIndex", nextIndex + 1);
        }

        session.setMessages(toJson(messages));
        sessionMapper.updateById(session);
        return result;
    }

    /** 流式回答问题 — AI评估 + 实时推送token到前端（支持代码审查） */
    public SseEmitter answerStream(Long sessionId, String answer,
                                   String code, String codeLang, String codeFile) {
        Long userId = JwtAuthFilter.getCurrentUserId();

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return errorEmitter("面试会话不存在");
        }
        if (session.getStatus() == 1) {
            return errorEmitter("面试已结束");
        }

        // 编程环节拦截：先后台生成面试报告，再出题
        if (answer.contains("[进入编程环节]")) {
            saveInterviewReportAsync(session);
            return handleCodingRoundStream(session, answer);
        }

        List<Map<String, Object>> messages = parseMessages(session.getMessages());
        String systemPrompt = promptCache.computeIfAbsent(session.getPosition(), pos -> String.format(SYSTEM_PROMPT, pos));

        String fullAnswer = answer != null ? answer : "";
        boolean isCodeReview = code != null && !code.isEmpty();

        if (!isCodeReview) {
            messages.add(Map.of("role", "user", "content", fullAnswer, "time", LocalDateTime.now().toString()));
        }
        // For code review: handled separately below, user message added after review
        int nextIndex = session.getCurrentQuestionIndex() + 1;

        List<Map<String, String>> aiMessages = toAiMessages(messages);

        String contextHint;
        if (isCodeReview) {
            contextHint = ""; // Not used for code review path
        } else if (nextIndex < 3) {
            contextHint = "请根据我的回答继续深入提问。";
        } else if (nextIndex < 5) {
            contextHint = "请根据我的回答追问或换一个领域提问。如果你觉得已经对我的水平有了初步判断，也可以继续深入。";
        } else {
            contextHint = "请根据我的回答质量判断：如果已经充分了解我的水平（无论好坏），可以结束面试并给出报告；如果还需要考察，请继续提问。";
        }

        SseEmitter emitter = new SseEmitter(isCodeReview ? 180_000L : 120_000L);

        emitter.onTimeout(() -> {
            log.warn("SSE流超时: sessionId={}", sessionId);
            try {
                emitter.send(SseEmitter.event().name("error").data("AI响应超时，请重试"));
            } catch (Exception ignored) {}
            emitter.complete();
        });
        emitter.onError(ex -> {
            log.error("SSE流错误: sessionId={}", sessionId, ex);
        });

        String userApiKey = getUserApiKey();

        if (isCodeReview) {
            // ===== 代码审查：独立 AI 调用 =====
            String langLabel = codeLang != null ? codeLang : "java";
            String fileLabel = codeFile != null ? codeFile : "Solution";
            String codingAnswer = "请审查以下代码，从正确性、代码风格、时间复杂度、边界处理、可读性五个维度评分。\n\n```" + langLabel + "\n// " + fileLabel + "\n" + code + "\n```\n\n输出格式（仅输出以下JSON，不要其他文字）：\n[笔试结束]{\"score\":8,\"feedback\":\"总体评价\",\"dimensions\":[{\"name\":\"正确性\",\"score\":8,\"comment\":\"...\"},{\"name\":\"代码风格\",\"score\":8,\"comment\":\"...\"},{\"name\":\"时间复杂度\",\"score\":8,\"comment\":\"...\"},{\"name\":\"边界处理\",\"score\":8,\"comment\":\"...\"},{\"name\":\"可读性\",\"score\":8,\"comment\":\"...\"}],\"suggestion\":\"提升建议\"}";

            List<Map<String, String>> codingMessages = new ArrayList<>(aiMessages);
            codingMessages.add(Map.of("role", "user", "content", codingAnswer));

            log.info("\n" + "=".repeat(60) + "\n" +
                     "【代码审查 - 独立AI调用】sessionId: {}\n" +
                     "-".repeat(40),
                     sessionId);

            CompletableFuture.runAsync(() -> {
                StringBuilder codingResponse = new StringBuilder();
                try {
                    aiService.streamChat(systemPrompt, codingMessages, userApiKey,
                            AiModelSelector.normalize(session.getModel()), token -> {
                        codingResponse.append(token);
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (Exception e) {
                            throw new RuntimeException("SSE发送失败", e);
                        }
                    });

                    String codingText = codingResponse.toString();
                    log.info("\n" + "-".repeat(40) + "\n" +
                             "【代码审查 - AI输出】\n{}\n" +
                             "-".repeat(40), codingText);

                    // 解析 [笔试结束] JSON 并存储笔试报告
                    try {
                        int markerIdx = codingText.indexOf("[笔试结束]");
                        if (markerIdx >= 0) {
                            String afterMarker = codingText.substring(markerIdx + "[笔试结束]".length());
                            int jsonStart = afterMarker.indexOf("{");
                            int jsonEnd = afterMarker.lastIndexOf("}") + 1;
                            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                String jsonStr = afterMarker.substring(jsonStart, jsonEnd);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);
                                session.setCodingScore(report.get("score") != null
                                    ? ((Number) report.get("score")).intValue() : 0);
                                session.setCodingDimensions(toJson(report.get("dimensions")));
                                session.setCodingFeedback((String) report.get("feedback"));
                                session.setCodingSuggestion((String) report.get("suggestion"));
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析[笔试结束]JSON失败: {}", e.getMessage());
                    }

                    // Save coding review to messages
                    messages.add(Map.of("role", "user", "content",
                            answer != null ? answer : "请审查代码",
                            "time", LocalDateTime.now().toString()));
                    messages.add(Map.of("role", "assistant", "content", codingText,
                            "time", LocalDateTime.now().toString()));
                    session.setMessages(toJson(messages));
                    session.setStatus(1);
                    session.setFinishTime(LocalDateTime.now());

                    // 精准保存：用 LambdaUpdateWrapper 避免覆盖后台线程写好的面试报告
                    sessionMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InterviewSession>()
                            .eq(InterviewSession::getId, session.getId())
                            .set(InterviewSession::getMessages, session.getMessages())
                            .set(InterviewSession::getStatus, session.getStatus())
                            .set(InterviewSession::getFinishTime, session.getFinishTime())
                            .set(InterviewSession::getCodingScore, session.getCodingScore())
                            .set(InterviewSession::getCodingDimensions, session.getCodingDimensions())
                            .set(InterviewSession::getCodingFeedback, session.getCodingFeedback())
                            .set(InterviewSession::getCodingSuggestion, session.getCodingSuggestion()));

                    Map<String, Object> finishData = new LinkedHashMap<>();
                    finishData.put("finished", true);
                    finishData.put("hasCodingRound", true);
                    emitter.send(SseEmitter.event().name("finish")
                            .data(objectMapper.writeValueAsString(finishData)));
                    emitter.complete();

                } catch (Exception e) {
                    log.error("代码审查AI调用失败", e);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("代码审查失败"));
                    } catch (Exception ignored) {}
                    emitter.complete();
                }
            });

            return emitter;
        }

        // ===== 普通面试对话 =====
        aiMessages.add(Map.of("role", "user", "content", contextHint));

        log.info("\n" + "=".repeat(60) + "\n" +
                 "【第{}轮对话 - 流式】sessionId: {}\n" +
                 "用户回答: {}\n" +
                 "-".repeat(40) + "\n" +
                 "对话上下文(最近3轮):\n{}\n" +
                 "-".repeat(40),
                 nextIndex, sessionId, fullAnswer.length() > 200 ? fullAnswer.substring(0, 200) + "..." : fullAnswer, summarizeContext(aiMessages));

        CompletableFuture.runAsync(() -> {
            StringBuilder fullResponse = new StringBuilder();
            boolean[] interviewFinished = {false};
            boolean[] codingFinished = {false};

            try {
                aiService.streamChat(systemPrompt, aiMessages, userApiKey,
                    AiModelSelector.normalize(session.getModel()), token -> {
                    fullResponse.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (Exception e) {
                        throw new RuntimeException("SSE发送失败", e);
                    }
                    String content = fullResponse.toString();
                    if (content.contains("[面试结束]")) {
                        interviewFinished[0] = true;
                    }
                    if (content.contains("[笔试结束]")) {
                        codingFinished[0] = true;
                    }
                });

                String aiResponse = fullResponse.toString();
                messages.add(Map.of("role", "assistant", "content", aiResponse,
                        "time", LocalDateTime.now().toString()));

                boolean finished = interviewFinished[0] || codingFinished[0];
                log.info("\n" + "-".repeat(40) + "\n" +
                         "【AI流式输出完成 - 第{}轮】(面试结束={}, 笔试结束={})\n{}\n" +
                         "-".repeat(40),
                         nextIndex, interviewFinished[0], codingFinished[0], aiResponse);

                if (finished) {
                    try {
                        // 优先解析 [笔试结束] JSON，其次 [面试结束] JSON
                        String marker = codingFinished[0] ? "[笔试结束]" : "[面试结束]";
                        int markerIdx = aiResponse.indexOf(marker);
                        String afterMarker = aiResponse.substring(markerIdx + marker.length());
                        int jsonStart = afterMarker.indexOf("{");
                        int jsonEnd = afterMarker.lastIndexOf("}") + 1;
                        String jsonStr = null;
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            jsonStr = afterMarker.substring(jsonStart, jsonEnd);
                        }
                        if (jsonStr == null && aiResponse.contains("{")) {
                            jsonStr = aiResponse.substring(aiResponse.indexOf("{"), aiResponse.lastIndexOf("}") + 1);
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, Object> report = jsonStr != null
                            ? objectMapper.readValue(jsonStr, Map.class) : Map.of();

                        // 笔试结束 → 存为笔试报告；面试结束且有面试报告 → 也存为笔试报告
                        boolean saveAsCoding = codingFinished[0] || hasInterviewReport(session);

                        if (saveAsCoding) {
                            session.setCodingScore(report.get("score") != null
                                ? ((Number) report.get("score")).intValue() : 0);
                            session.setCodingDimensions(toJson(report.get("dimensions")));
                            session.setCodingFeedback((String) report.get("feedback"));
                            session.setCodingSuggestion((String) report.get("suggestion"));
                        } else {
                            session.setOverallScore(report.get("score") != null
                                ? ((Number) report.get("score")).intValue() : 0);
                            session.setDimensions(toJson(report.get("dimensions")));
                            session.setFeedback((String) report.get("feedback"));
                        }
                        session.setStatus(1);
                        session.setFinishTime(LocalDateTime.now());

                        Map<String, Object> finishData = new LinkedHashMap<>();
                        finishData.put("finished", true);
                        finishData.put("report", report);
                        finishData.put("hasCodingRound", saveAsCoding);

                        String closingMsg = aiResponse
                                .replace("[面试结束]", "").replace("[笔试结束]", "")
                                .replace(jsonStr != null ? jsonStr : "", "").trim();
                        if (!closingMsg.isEmpty()) {
                            finishData.put("message", closingMsg);
                        }

                        emitter.send(SseEmitter.event().name("finish")
                                .data(objectMapper.writeValueAsString(finishData)));

                        log.info("\n" + "=".repeat(60) + "\n" +
                                 "【报告解析成功】类型: {} | marker: {} | 总分: {}\n" +
                                 "=".repeat(60),
                                 saveAsCoding ? "笔试" : "面试", marker,
                                 saveAsCoding ? session.getCodingScore() : session.getOverallScore());

                    } catch (Exception e) {
                        log.warn("解析AI报告JSON失败: {}", e.getMessage());
                        if (hasInterviewReport(session)) {
                            session.setCodingScore(6);
                            session.setCodingFeedback("编程评估已完成");
                        } else {
                            session.setOverallScore(6);
                            session.setFeedback("面试已完成。");
                        }
                        session.setStatus(1);
                        session.setFinishTime(LocalDateTime.now());
                        emitter.send(SseEmitter.event().name("finish")
                                .data("{\"finished\":true,\"report\":{\"score\":6,\"feedback\":\"评估已完成\"}}"));
                    }
                } else {
                    Map<String, Object> finishData = new LinkedHashMap<>();
                    finishData.put("finished", false);
                    finishData.put("questionIndex", nextIndex + 1);
                    emitter.send(SseEmitter.event().name("finish")
                            .data(objectMapper.writeValueAsString(finishData)));
                }

                session.setMessages(toJson(messages));
                session.setCurrentQuestionIndex(nextIndex);
                self.saveAnswerData(session);

                emitter.complete();

            } catch (Exception e) {
                log.error("流式对话异常: sessionId={}", sessionId, e);
                try {
                    if (fullResponse.length() > 0) {
                        messages.add(Map.of("role", "assistant", "content",
                                fullResponse.toString(), "time", LocalDateTime.now().toString()));
                        session.setMessages(toJson(messages));
                        session.setCurrentQuestionIndex(nextIndex);
                        self.saveAnswerData(session);
                    }
                    emitter.send(SseEmitter.event().name("error")
                            .data("AI服务暂时不可用: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    /** 保存面试数据（独立事务，供异步回调使用） */
    @Transactional
    public void saveAnswerData(InterviewSession session) {
        sessionMapper.updateById(session);
    }

    /** 手动结束面试 — 将现有对话交给AI生成报告 */
    @Transactional
    public Map<String, Object> end(Long sessionId) {
        Long userId = JwtAuthFilter.getCurrentUserId();

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("面试会话不存在");
        }
        if (session.getStatus() == 1) {
            throw new IllegalArgumentException("面试已结束");
        }

        List<Map<String, Object>> messages = parseMessages(session.getMessages());

        // 构建纯文本对话记录（排除编程环节），供评估 prompt 使用
        StringBuilder transcript = new StringBuilder();
        boolean inCodingBlock = false;
        for (Map<String, Object> msg : messages) {
            String role = (String) msg.get("role");
            String content = (String) msg.get("content");
            if (content == null) continue;
            if (content.contains("[进入编程环节]")) { inCodingBlock = true; continue; }
            if (content.contains("[笔试结束]")) { inCodingBlock = false; continue; }
            if (inCodingBlock) continue;
            String label = "user".equals(role) ? "候选人" : "面试官";
            transcript.append("【").append(label).append("】").append(content).append("\n\n");
        }

        String systemPrompt = String.format(EVALUATION_PROMPT, transcript.toString());
        List<Map<String, String>> aiMessages = List.of(
            Map.of("role", "user", "content", "请对以上面试对话进行评估，输出[面试结束]+JSON。")
        );

        String aiResponse = aiService.chat(systemPrompt, aiMessages, getUserApiKey(), AiModelSelector.normalize(session.getModel()));

        log.info("\n" + "-".repeat(40) + "\n" +
                 "【AI最终报告 - 原始输出】\n{}\n" +
                 "-".repeat(40),
                 aiResponse);
        messages.add(Map.of("role", "assistant", "content", aiResponse, "time", LocalDateTime.now().toString()));
        session.setStatus(1);
        session.setFinishTime(LocalDateTime.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);

        try {
            // Try [面试结束] marker extraction first (balanced-brace), then fall back to raw {..}
            String jsonStr = extractJson(aiResponse, "[面试结束]");
            if (jsonStr == null && aiResponse.contains("{")) {
                jsonStr = aiResponse.substring(aiResponse.indexOf("{"), aiResponse.lastIndexOf("}") + 1);
            }
            if (jsonStr == null) throw new IllegalArgumentException("AI未输出有效JSON");

            @SuppressWarnings("unchecked")
            Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);

            session.setOverallScore(report.get("score") != null
                ? ((Number) report.get("score")).intValue() : 0);
            session.setDimensions(toJson(report.get("dimensions")));
            session.setFeedback((String) report.get("feedback"));
            result.put("finished", true);
            result.put("report", report);
        } catch (Exception e) {
            log.warn("解析面试报告JSON失败: {}", e.getMessage());
            session.setOverallScore(1);
            session.setFeedback("面试已结束，未能生成有效评估报告");
            session.setDimensions("[]");
            result.put("finished", true);
            result.put("report", Map.of("score", 1, "feedback", "面试已结束，未能生成有效评估", "dimensions", List.of()));
        }

        session.setMessages(toJson(messages));
        sessionMapper.updateById(session);
        return result;
    }

    /** 面试历史（仅最近5条） */
    public List<InterviewSession> getHistory() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        return sessionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreateTime)
                .last("LIMIT 5")
        );
    }

    private boolean hasInterviewReport(InterviewSession session) {
        return hasInterviewReport(session);
    }

    /** SSE 错误事件：避免 @ExceptionHandler 对 SSE 端点返回 Result 导致 406 */
    private SseEmitter errorEmitter(String message) {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}"));
        } catch (Exception ignored) {}
        emitter.complete();
        return emitter;
    }

    /** 进入笔试时，后台生成并存储面试报告（基于笔试前的对话） */
    private void saveInterviewReportAsync(InterviewSession session) {
        final Long sessionId = session.getId();
        final String sessionModel = AiModelSelector.normalize(session.getModel());
        final String messagesJson = session.getMessages();
        // 在主线程捕获 API key，异步线程无 JWT ThreadLocal 上下文
        final String apiKey = getUserApiKey();
        log.info("saveInterviewReportAsync启动: sessionId={}, apiKey={}", sessionId, apiKey != null ? "有" : "无");
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> messages = parseMessages(messagesJson);
                StringBuilder transcript = new StringBuilder();
                for (Map<String, Object> msg : messages) {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    if (content == null) continue;
                    if (content.contains("[进入编程环节]") || content.contains("[笔试邀请]")) continue;
                    if (content.contains("[编程题目]")) break;
                    String label = "user".equals(role) ? "候选人" : "面试官";
                    transcript.append("【").append(label).append("】").append(content).append("\n\n");
                }

                String systemPrompt = String.format(EVALUATION_PROMPT, transcript.toString());
                String aiResponse = aiService.chat(systemPrompt,
                    List.of(Map.of("role", "user", "content", "请评估以上面试对话")),
                    apiKey, sessionModel);

                String jsonStr = extractJson(aiResponse, "[面试结束]");
                if (jsonStr == null && aiResponse.contains("{")) {
                    jsonStr = aiResponse.substring(aiResponse.indexOf("{"), aiResponse.lastIndexOf("}") + 1);
                }
                if (jsonStr == null) {
                    log.warn("面试报告生成失败: AI未输出JSON, sessionId={}", sessionId);
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);

                // 用 LambdaUpdateWrapper 精准更新，不碰 messages 字段
                sessionMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InterviewSession>()
                        .eq(InterviewSession::getId, sessionId)
                        .set(InterviewSession::getOverallScore, report.get("score") != null
                            ? ((Number) report.get("score")).intValue() : 0)
                        .set(InterviewSession::getDimensions, toJson(report.get("dimensions")))
                        .set(InterviewSession::getFeedback, (String) report.get("feedback")));
                log.info("面试报告后台生成成功: sessionId={}, score={}", sessionId,
                    report.get("score"));
            } catch (Exception e) {
                log.warn("面试报告后台生成失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            }
        });
    }

    /** 清理旧会话：只保留最近5条，使用批量删除 */
    private void cleanupOldSessions(Long userId) {
        List<InterviewSession> all = sessionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewSession>()
                .select(InterviewSession::getId)
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreateTime)
        );
        if (all.size() <= 5) return;
        List<Long> ids = all.subList(5, all.size()).stream()
                .map(InterviewSession::getId).toList();
        sessionMapper.deleteBatchIds(ids);
        log.info("批量清理旧面试记录: userId={}, count={}", userId, ids.size());
    }

    /** 面试详情 */
    public InterviewSession getDetail(Long id) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        InterviewSession session = sessionMapper.selectById(id);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("面试记录不存在");
        }
        return session;
    }

    // ===== 编程环节专用 =====

    /** 从题库随机选题，生成 [编程题目] JSON（与原有格式完全兼容） */
    private Map<String, Object> handleCodingRound(InterviewSession session, String answer) {
        String codingProblem = algorithmProblemService.generateCodingProblem(session.getPosition());

        log.info("\n" + "=".repeat(60) + "\n" +
                 "【题库出题】sessionId: {} | 岗位: {}\n{}\n" +
                 "=".repeat(60),
                 session.getId(), session.getPosition(), codingProblem);

        List<Map<String, Object>> messages = parseMessages(session.getMessages());
        messages.add(Map.of("role", "user", "content", "[进入编程环节]", "time", LocalDateTime.now().toString()));
        messages.add(Map.of("role", "assistant", "content", codingProblem, "time", LocalDateTime.now().toString()));
        session.setMessages(toJson(messages));
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        // 精准更新，不覆盖后台线程刚写好的面试报告
        sessionMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InterviewSession>()
                .eq(InterviewSession::getId, session.getId())
                .set(InterviewSession::getMessages, session.getMessages())
                .set(InterviewSession::getCurrentQuestionIndex, session.getCurrentQuestionIndex()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("content", codingProblem);
        return result;
    }

    private SseEmitter handleCodingRoundStream(InterviewSession session, String answer) {
        SseEmitter emitter = new SseEmitter(15_000L);
        String codingProblem = algorithmProblemService.generateCodingProblem(session.getPosition());

        log.info("\n" + "=".repeat(60) + "\n" +
                 "【题库出题 - 流式】sessionId: {} | 岗位: {}\n{}\n" +
                 "=".repeat(60),
                 session.getId(), session.getPosition(), codingProblem);

        List<Map<String, Object>> messages = parseMessages(session.getMessages());
        messages.add(Map.of("role", "user", "content", "[进入编程环节]", "time", LocalDateTime.now().toString()));
        messages.add(Map.of("role", "assistant", "content", codingProblem, "time", LocalDateTime.now().toString()));
        session.setMessages(toJson(messages));
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        // 精准更新，不覆盖后台线程刚写好的面试报告
        sessionMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InterviewSession>()
                .eq(InterviewSession::getId, session.getId())
                .set(InterviewSession::getMessages, session.getMessages())
                .set(InterviewSession::getCurrentQuestionIndex, session.getCurrentQuestionIndex()));

        emitter.onTimeout(() -> {
            try { emitter.send(SseEmitter.event().name("error").data("出题超时")); } catch (Exception ignored) {}
            emitter.complete();
        });

        // 将完整 JSON 作为 token 推送，前端 composable 会累积后解析 [编程题目]
        new Thread(() -> {
            try {
                emitter.send(SseEmitter.event().name("token").data(codingProblem));
                emitter.send(SseEmitter.event().name("finish")
                    .data("{\"finished\":false,\"coding\":true}"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("编程题生成失败"));
                    emitter.complete();
                } catch (Exception ex) { /* ignore */ }
            }
        }).start();

        return emitter;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> toAiMessages(List<Map<String, Object>> messages) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            String role = (String) msg.get("role");
            String content = (String) msg.get("content");
            if ("user".equals(role) || "assistant".equals(role)) {
                result.add(Map.of("role", role, "content", content));
            }
        }
        return result;
    }

    private List<Map<String, Object>> parseMessages(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try { return objectMapper.readValue(json, List.class); }
        catch (JsonProcessingException e) {
            log.warn("解析messages JSON失败，会话历史可能丢失: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) {
            log.warn("序列化JSON失败: {}", e.getMessage());
            return "[]";
        }
    }

    /** Extract balanced JSON from text following a marker like [笔试结束] */
    private String extractJson(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx == -1) return null;
        int i = idx + marker.length();
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        if (i >= text.length() || text.charAt(i) != '{') return null;
        int depth = 0;
        int start = i;
        for (; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return null;
    }

    /** 摘要最近的对话上下文（用于日志） */
    private String summarizeContext(List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        int total = messages.size();
        int start = Math.max(0, total - 6);
        for (int i = start; i < total; i++) {
            Map<String, String> msg = messages.get(i);
            String role = "user".equals(msg.get("role")) ? "候选人" : "面试官";
            String content = msg.get("content");
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            sb.append(String.format("  [%s] %s%n", role, content));
        }
        return sb.toString();
    }
}
