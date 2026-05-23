package com.mianmiantong.service.paper;

import com.mianmiantong.service.ai.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiReduceService {

    private final AiService aiService;

    public AiReduceService(AiService aiService) {
        this.aiService = aiService;
    }

    // === 12 AI template patterns (from AI_paper ai_reducer.py:16-28) ===
    private static final List<Pattern> AI_PATTERNS = List.of(
        Pattern.compile("首先.*其次.*最后"),
        Pattern.compile("综上所述"),
        Pattern.compile("值得注意的是"),
        Pattern.compile("不可否认"),
        Pattern.compile("毋庸置疑"),
        Pattern.compile("总而言之"),
        Pattern.compile("由此可见"),
        Pattern.compile("显而易见"),
        Pattern.compile("众所周知"),
        Pattern.compile("不言而喻"),
        Pattern.compile("此外.*同时.*另外")
    );

    private static final List<String> FLOW_CONNECTORS = List.of(
        "因此", "然而", "同时", "此外", "另外", "由此可见", "综上", "进一步说", "相较之下", "具体而言"
    );

    private static final List<String> AI_WORDS = List.of(
        "综上所述", "不可否认", "毋庸置疑", "显而易见", "众所周知", "值得注意的是"
    );

    /** 扫描 AI 写作痕迹（本地规则，不调 AI） */
    public AiScanResult scanAiFeatures(String text) {
        AiScanResult result = new AiScanResult();
        if (text == null || text.isBlank()) return result;

        // 1. 模板表达检测
        for (Pattern pattern : AI_PATTERNS) {
            if (pattern.matcher(text).find()) {
                String patStr = pattern.pattern();
                result.getFeatures().add("发现高频模板表达：" + patStr.substring(0, Math.min(20, patStr.length())));
                result.setScore(result.getScore() + 5);
            }
        }

        // 2. 句长方差分析
        String[] sentences = text.split("[。！？?!]");
        List<String> validSentences = Arrays.stream(sentences)
            .map(String::trim).filter(s -> s.length() > 5).collect(Collectors.toList());
        if (validSentences.size() > 5) {
            double avgLen = validSentences.stream().mapToInt(String::length).average().orElse(0);
            double variance = validSentences.stream()
                .mapToDouble(s -> Math.pow(s.length() - avgLen, 2))
                .average().orElse(0);
            if (variance < 100) {
                result.getFeatures().add("句子长度分布过于均匀（方差=" + String.format("%.0f", variance) + "），存在模板化生成倾向");
                result.setScore(result.getScore() + 10);
            }
        }

        // 3. 段落长度均匀度
        String[] paragraphs = text.split("\n{2,}");
        List<String> validParas = Arrays.stream(paragraphs)
            .map(String::trim).filter(p -> !p.isEmpty()).collect(Collectors.toList());
        if (validParas.size() > 3) {
            double avgLen2 = validParas.stream().mapToInt(String::length).average().orElse(0);
            boolean allUniform = validParas.stream().allMatch(
                p -> Math.abs(p.length() - avgLen2) < avgLen2 * 0.3
            );
            if (allUniform) {
                result.getFeatures().add("段落长度过于整齐，存在统一模板痕迹");
                result.setScore(result.getScore() + 8);
            }
        }

        // 4. 连接词密度
        long connectorCount = FLOW_CONNECTORS.stream().mapToLong(c -> countOccurrences(text, c)).sum();
        if (connectorCount > validSentences.size() * 0.4 && !validSentences.isEmpty()) {
            result.getFeatures().add("连接词使用偏密集，共出现 " + connectorCount + " 次");
            result.setScore(result.getScore() + 8);
        }

        // 5. AI高频词标记
        for (String s : validSentences) {
            for (String word : AI_WORDS) {
                if (s.contains(word) && result.getSentencesFlagged().size() < 10) {
                    result.getSentencesFlagged().add(s.length() > 60 ? s.substring(0, 60) + "..." : s);
                    break;
                }
            }
        }

        result.setScore(Math.min(100, result.getScore()));
        result.setRiskLevel(result.getScore() >= 30 ? "高风险" : result.getScore() >= 15 ? "中风险" : "低风险");
        return result;
    }

    private long countOccurrences(String text, String word) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) { count++; idx += word.length(); }
        return count;
    }

    /** SSE 流式降AI改写 */
    public SseEmitter rewrite(String text, String mode) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Map<String, String> modeLabels = Map.of("light", "轻度去痕", "deep", "深度重构", "academic", "学术拟合");
        String modeLabel = modeLabels.getOrDefault(mode != null ? mode : "light", "轻度去痕");
        String safeMode = mode != null ? mode : "light";
        String safeText = text != null ? text : "";

        String systemPrompt = getSystemPrompt("prompts/ai_reduce_transform.txt");
        String userPrompt = renderPrompt("prompts/ai_reduce_transform.txt", Map.of(
            "text", safeText, "mode", safeMode, "mode_label", modeLabel
        ));

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", userPrompt)
        );

        emitter.onTimeout(() -> {
            safeSend(emitter, "error", "去AI改写超时");
            emitter.complete();
        });

        CompletableFuture.runAsync(() -> {
            try {
                aiService.streamChat(systemPrompt, messages, null, null, token -> {
                    safeSend(emitter, "token", token);
                });

                emitter.send(SseEmitter.event().name("finish")
                    .data("{\"phase\":\"ai_reduce\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI reduce SSE stream failed", e);
                safeSend(emitter, "error", e.getMessage() != null ? e.getMessage() : "去AI改写失败");
                emitter.complete();
            }
        });

        return emitter;
    }

    // === Prompt loading (matches PolishService pattern) ===

    private String getSystemPrompt(String path) {
        try {
            String content = new String(
                new ClassPathResource(path).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
            String[] parts = content.split("---SYSTEM---", 2);
            return parts[0].trim();
        } catch (IOException e) {
            log.error("Failed to load prompt: {}", path, e);
            return "你是一位资深学术写作编辑。";
        }
    }

    private String renderPrompt(String path, Map<String, String> vars) {
        try {
            String content = new String(
                new ClassPathResource(path).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
            String[] parts = content.split("---SYSTEM---", 2);
            String template = parts.length > 1 ? parts[1].trim() : content;
            for (Map.Entry<String, String> e : vars.entrySet()) {
                template = template.replace("{" + e.getKey() + "}", e.getValue());
            }
            return template;
        } catch (IOException e) {
            log.error("Failed to load prompt: {}", path, e);
            return vars.getOrDefault("text", "");
        }
    }

    private void safeSend(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ignored) {
            // SSE connection already closed by client
        }
    }

    // === Inner classes ===

    @lombok.Data
    public static class AiScanResult {
        private int score;
        private String riskLevel = "低风险";
        private List<String> features = new ArrayList<>();
        private List<String> sentencesFlagged = new ArrayList<>();
    }
}
