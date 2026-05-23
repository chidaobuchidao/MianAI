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
public class PlagiarismReduceService {

    private final AiService aiService;

    public PlagiarismReduceService(AiService aiService) {
        this.aiService = aiService;
    }

    /** 本地检测文本内部重复风险 */
    public RepetitionResult detectRepetitive(String text) {
        RepetitionResult r = new RepetitionResult();
        if (text == null || text.isBlank()) return r;

        Pattern wordPattern = Pattern.compile("[\\u4e00-\\u9fff]{4,}");
        Map<String, Integer> seen = new LinkedHashMap<>();
        java.util.regex.Matcher m = wordPattern.matcher(text);
        while (m.find()) {
            String word = m.group();
            seen.put(word, seen.getOrDefault(word, 0) + 1);
        }
        List<Map.Entry<String, Integer>> repeated = seen.entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .sorted((a, b) -> {
                int cmp = b.getValue().compareTo(a.getValue());
                return cmp != 0 ? cmp : Integer.compare(b.getKey().length(), a.getKey().length());
            })
            .limit(20)
            .collect(Collectors.toList());
        r.setRepeatedPhrases(repeated);

        String[] sentences = text.split("[。！？?!]");
        r.setLongSentences(Arrays.stream(sentences)
            .map(String::trim)
            .filter(s -> s.length() > 100)
            .map(s -> s.length() > 80 ? s.substring(0, 80) + "..." : s)
            .collect(Collectors.toList()));

        List<String> topWords = repeated.stream().map(Map.Entry::getKey).limit(5).collect(Collectors.toList());
        String[] paragraphs = text.split("\n");
        r.setRiskParagraphs(Arrays.stream(paragraphs)
            .map(String::trim)
            .filter(p -> p.length() > 20)
            .filter(p -> topWords.stream().mapToLong(w -> countIn(p, w)).sum() > 3)
            .map(p -> p.length() > 100 ? p.substring(0, 100) + "..." : p)
            .collect(Collectors.toList()));

        return r;
    }

    /** 词汇相似度 */
    public SimilarityResult compareSimilarity(String text1, String text2) {
        Pattern tokenPattern = Pattern.compile("[\\u4e00-\\u9fff]+|[A-Za-z]+");
        Set<String> words1 = tokenPattern.matcher(text1 != null ? text1 : "").results()
            .map(java.util.regex.MatchResult::group).collect(Collectors.toSet());
        Set<String> words2 = tokenPattern.matcher(text2 != null ? text2 : "").results()
            .map(java.util.regex.MatchResult::group).collect(Collectors.toSet());

        SimilarityResult r = new SimilarityResult();
        if (words1.isEmpty() || words2.isEmpty()) {
            r.setSimilarity(0.0);
            r.setUniqueIn1(words1.size());
            r.setUniqueIn2(words2.size());
            return r;
        }

        Set<String> common = new HashSet<>(words1);
        common.retainAll(words2);
        double similarity = (double) common.size() / Math.max(words1.size(), words2.size()) * 100;
        r.setSimilarity(Math.round(similarity * 10) / 10.0);
        List<String> commonList = new ArrayList<>(common);
        r.setCommonWords(commonList.subList(0, Math.min(20, commonList.size())));
        r.setUniqueIn1(words1.size() - common.size());
        r.setUniqueIn2(words2.size() - common.size());
        return r;
    }

    /** 引用格式检查 */
    public CitationCheckResult checkCitations(String text) {
        CitationCheckResult r = new CitationCheckResult();
        if (text == null || text.isBlank()) return r;

        int refSectionIdx = text.indexOf("参考文献");
        if (refSectionIdx < 0) refSectionIdx = text.indexOf("引用文献");
        if (refSectionIdx < 0) refSectionIdx = text.indexOf("参考资料");

        String body = refSectionIdx >= 0 ? text.substring(0, refSectionIdx) : text;
        String refs = refSectionIdx >= 0 ? text.substring(refSectionIdx) : "";

        Pattern citePattern = Pattern.compile("\\[(\\d+)\\]");
        Set<Integer> bodyRefs = citePattern.matcher(body).results()
            .map(mr -> Integer.parseInt(mr.group(1))).collect(Collectors.toSet());

        Pattern refPattern = Pattern.compile("^\\s*\\[(\\d+)\\]", Pattern.MULTILINE);
        Set<Integer> refNums = refPattern.matcher(refs).results()
            .map(mr -> Integer.parseInt(mr.group(1))).collect(Collectors.toSet());

        r.setCitationCount(bodyRefs.size());
        r.setReferenceCount(refNums.size());
        r.setHasReferenceSection(refSectionIdx >= 0);

        List<String> issues = new ArrayList<>();
        if (body.length() > 300 && bodyRefs.isEmpty() && refSectionIdx < 0) {
            issues.add("正文暂未发现引用标记，查重时可能因引用缺失而被整体判重");
        }
        if (!bodyRefs.isEmpty() && refSectionIdx < 0) {
            issues.add("正文已有引用编号，但未发现参考文献列表");
        }
        if (refSectionIdx >= 0 && bodyRefs.isEmpty() && refNums.isEmpty()) {
            issues.add("存在参考文献区，但正文未见对应引用标记");
        }
        Set<Integer> missing = new HashSet<>(bodyRefs);
        missing.removeAll(refNums);
        if (!missing.isEmpty()) {
            issues.add("正文引用编号缺少对应参考文献条目：" +
                missing.stream().sorted().map(String::valueOf).collect(Collectors.joining("、")));
        }
        Set<Integer> unused = new HashSet<>(refNums);
        unused.removeAll(bodyRefs);
        if (!unused.isEmpty()) {
            issues.add("参考文献存在未在正文引用的编号：" +
                unused.stream().sorted().map(String::valueOf).collect(Collectors.joining("、")));
        }
        r.setIssues(issues);
        return r;
    }

    /** SSE 流式降重改写 */
    public SseEmitter reduce(String text, String sourceText, String mode) {
        SseEmitter emitter = new SseEmitter(120_000L);

        Map<String, String> modeLabels = Map.of("light", "轻度降重", "medium", "中度降重", "deep", "深度降重");
        String modeLabel = modeLabels.getOrDefault(mode != null ? mode : "medium", "中度降重");
        String safeMode = mode != null ? mode : "medium";
        String safeText = text != null ? text : "";
        String safeSource = sourceText != null ? sourceText : "";

        String systemPrompt = getSystemPrompt("prompts/plagiarism_transform.txt");
        String userPrompt = renderPrompt("prompts/plagiarism_transform.txt", Map.of(
            "text", safeText,
            "source_text", safeSource,
            "mode", safeMode,
            "mode_label", modeLabel
        ));

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", userPrompt)
        );

        emitter.onTimeout(() -> {
            safeSend(emitter, "error", "降重超时");
            emitter.complete();
        });

        CompletableFuture.runAsync(() -> {
            try {
                aiService.streamChat(systemPrompt, messages, null, null, token -> {
                    safeSend(emitter, "token", token);
                });

                emitter.send(SseEmitter.event().name("finish")
                    .data("{\"phase\":\"reduce\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Plagiarism reduce SSE stream failed", e);
                safeSend(emitter, "error", e.getMessage() != null ? e.getMessage() : "降重失败");
                emitter.complete();
            }
        });

        return emitter;
    }

    private void safeSend(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ignored) {
            // SSE connection already closed by client
        }
    }

    private long countIn(String text, String word) {
        int cnt = 0, idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) { cnt++; idx++; }
        return cnt;
    }

    /** 读取 prompt 文件的 system 部分（---SYSTEM--- 之前） */
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
            return "你是一位专业的学术改写编辑。";
        }
    }

    /** 读取 prompt 文件并替换模板变量 */
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

    // === Inner classes ===
    @lombok.Data
    public static class RepetitionResult {
        private List<Map.Entry<String, Integer>> repeatedPhrases = new ArrayList<>();
        private List<String> longSentences = new ArrayList<>();
        private List<String> riskParagraphs = new ArrayList<>();
    }

    @lombok.Data
    public static class SimilarityResult {
        private double similarity;
        private List<String> commonWords = new ArrayList<>();
        private int uniqueIn1;
        private int uniqueIn2;
    }

    @lombok.Data
    public static class CitationCheckResult {
        private int citationCount;
        private int referenceCount;
        private boolean hasReferenceSection;
        private List<String> issues = new ArrayList<>();
    }
}
