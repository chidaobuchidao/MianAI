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
import com.mianmiantong.service.ai.AiService;
import com.mianmiantong.service.coding.AlgorithmProblemService;
import com.mianmiantong.mapper.user.UserMapper;
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

    public InterviewService(InterviewSessionMapper sessionMapper, AiService aiService,
                            UserAiConfigService userAiConfigService, ResumeMapper resumeMapper,
                            AlgorithmProblemService algorithmProblemService,
                            UserMapper userMapper) {
        this.sessionMapper = sessionMapper;
        this.aiService = aiService;
        this.userAiConfigService = userAiConfigService;
        this.resumeMapper = resumeMapper;
        this.algorithmProblemService = algorithmProblemService;
        this.userMapper = userMapper;
        this.objectMapper = new ObjectMapper();
    }

    private final Map<Long, String> apiKeyCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> promptCache = new java.util.concurrent.ConcurrentHashMap<>();

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

        // Regular user without key → check daily quota (don't consume here)
        var user = userMapper.selectById(userId);
        if (user == null) throw new IllegalStateException("用户不存在");
        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : 10;
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getQuotaDate())) {
            user.setQuotaUsed(0);
            user.setQuotaDate(today);
            userMapper.updateById(user);
        }
        int used = user.getQuotaUsed() != null ? user.getQuotaUsed() : 0;
        if (used >= daily) {
            throw new IllegalStateException("今日免费次数已用完（" + daily + "次/天），请配置 AI API Key 后无限使用");
        }
        return resolveSystemApiKey();
    }

    /**
     * 消耗配额。仅普通用户且无个人Key时消耗。Admin/有Key用户不消耗。
     * @param model AI模型名，含"pro"则消耗翻倍
     */
    private void consumeQuota(String model) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        if (userId == null || JwtAuthFilter.isAdmin()) return;
        UserAiConfig config = userAiConfigService.getByUserId(userId);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) return;
        int steps = (model != null && model.toLowerCase().contains("pro")) ? 2 : 1;
        userMapper.incrementQuota(userId, steps);
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

        String firstQuestion = aiService.chat(systemPrompt, initMessages, getUserApiKey(), request.getModel());

        log.info("\n" + "-".repeat(60) + "\n" +
                 "【AI原始输出 - 第1问】\n{}\n" +
                 "-".repeat(60),
                 firstQuestion);

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPosition(position);
        session.setCurrentQuestionIndex(0);
        session.setModel(request.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "assistant", "content", firstQuestion, "time", LocalDateTime.now().toString()));
        session.setMessages(toJson(messages));
        session.setStatus(0);

        sessionMapper.insert(session);

        consumeQuota(request.getModel()); // 面试成功启动后才消耗

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

        String aiResponse = aiService.chat(systemPrompt, aiMessages, getUserApiKey(), session.getModel());

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

                session.setOverallScore(report.get("score") != null
                    ? ((Number) report.get("score")).intValue() : 0);
                session.setDimensions(toJson(report.get("dimensions")));
                session.setFeedback((String) report.get("feedback"));
                session.setStatus(1);
                session.setFinishTime(LocalDateTime.now());

                result.put("finished", true);
                result.put("report", report);

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
            throw new IllegalArgumentException("面试会话不存在");
        }
        if (session.getStatus() == 1) {
            throw new IllegalArgumentException("面试已结束");
        }

        // 编程环节拦截：用独立prompt出题，不走面试对话流
        if (answer.contains("[进入编程环节]")) {
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
                            session.getModel(), token -> {
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

                    // Save coding review to messages (frontend extracts [笔试结束] from here)
                    messages.add(Map.of("role", "user", "content",
                            answer != null ? answer : "请审查代码",
                            "time", LocalDateTime.now().toString()));
                    messages.add(Map.of("role", "assistant", "content", codingText,
                            "time", LocalDateTime.now().toString()));

                    session.setMessages(toJson(messages));
                    sessionMapper.updateById(session);

                    // Coding review done → send event; frontend will auto-trigger interview evaluation
                    emitter.send(SseEmitter.event().name("finish")
                            .data("{\"finished\":false,\"codingReviewDone\":true}"));
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
            boolean[] finished = {false};

            try {
                aiService.streamChat(systemPrompt, aiMessages, userApiKey,
                        session.getModel(), token -> {
                    fullResponse.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (Exception e) {
                        throw new RuntimeException("SSE发送失败", e);
                    }
                    if (fullResponse.toString().contains("[面试结束]")) {
                        finished[0] = true;
                    }
                });

                String aiResponse = fullResponse.toString();
                messages.add(Map.of("role", "assistant", "content", aiResponse,
                        "time", LocalDateTime.now().toString()));

                log.info("\n" + "-".repeat(40) + "\n" +
                         "【AI流式输出完成 - 第{}轮】(结束={})\n{}\n" +
                         "-".repeat(40),
                         nextIndex, finished[0], aiResponse);

                if (finished[0]) {
                    try {
                        String jsonStr = aiResponse.substring(aiResponse.indexOf("{"),
                                aiResponse.lastIndexOf("}") + 1);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);

                        session.setOverallScore(report.get("score") != null
                            ? ((Number) report.get("score")).intValue() : 0);
                        session.setDimensions(toJson(report.get("dimensions")));
                        session.setFeedback((String) report.get("feedback"));
                        session.setStatus(1);
                        session.setFinishTime(LocalDateTime.now());

                        Map<String, Object> finishData = new LinkedHashMap<>();
                        finishData.put("finished", true);
                        finishData.put("report", report);

                        String closingMsg = aiResponse.replace("[面试结束]", "")
                                .replace(jsonStr, "").trim();
                        if (!closingMsg.isEmpty()) {
                            finishData.put("message", closingMsg);
                        }

                        emitter.send(SseEmitter.event().name("finish")
                                .data(objectMapper.writeValueAsString(finishData)));

                        log.info("\n" + "=".repeat(60) + "\n" +
                                 "【面试报告解析成功】总分: {} | 建议: {}\n" +
                                 "=".repeat(60),
                                 session.getOverallScore(), report.get("suggestion"));

                    } catch (Exception e) {
                        log.warn("解析AI报告JSON失败: {}", e.getMessage());
                        session.setOverallScore(6);
                        session.setFeedback("面试已完成，感谢参与。");
                        session.setStatus(1);
                        session.setFinishTime(LocalDateTime.now());
                        emitter.send(SseEmitter.event().name("finish")
                                .data("{\"finished\":true,\"report\":{\"score\":6,\"feedback\":\"面试已完成\"}}"));
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
        String systemPrompt = promptCache.computeIfAbsent(session.getPosition(), pos -> String.format(SYSTEM_PROMPT, pos));

        // Exclude coding-round messages from interview evaluation context
        // The interview report should evaluate Q&A only, not the coding exercise
        List<Map<String, Object>> evalMessages = new ArrayList<>();
        boolean inCodingBlock = false;
        for (Map<String, Object> msg : messages) {
            String content = (String) msg.get("content");
            if (content == null) { evalMessages.add(msg); continue; }
            if (content.contains("[进入编程环节]")) { inCodingBlock = true; continue; }
            if (content.contains("[编程题目]")) { evalMessages.add(msg); continue; }
            if (content.contains("[笔试结束]")) { evalMessages.add(msg); inCodingBlock = false; continue; }
            if (inCodingBlock) continue;
            evalMessages.add(msg);
        }

        List<Map<String, String>> aiMessages = toAiMessages(evalMessages);

        String aiResponse = aiService.chat(systemPrompt, aiMessages, getUserApiKey(), session.getModel());

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

    /** 清理旧会话：只保留最近5条已完成的记录 */
    private void cleanupOldSessions(Long userId) {
        List<InterviewSession> all = sessionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getCreateTime)
        );
        if (all.size() <= 5) return;
        for (int i = 5; i < all.size(); i++) {
            sessionMapper.deleteById(all.get(i).getId());
            log.info("清理旧面试记录: sessionId={}", all.get(i).getId());
        }
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
        sessionMapper.updateById(session);

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
        sessionMapper.updateById(session);

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
