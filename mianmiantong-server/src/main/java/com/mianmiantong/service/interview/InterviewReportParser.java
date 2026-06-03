package com.mianmiantong.service.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 面试报告解析器
 * 负责从 AI 响应中提取和解析 JSON 报告
 */
@Slf4j
@Component
class InterviewReportParser {

    private final ObjectMapper objectMapper;

    InterviewReportParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 从 AI 响应中提取 JSON 报告
     * 优先查找 [面试结束] 或 [笔试结束] 标记后的 JSON
     */
    Map<String, Object> parseReport(String aiResponse, String marker) {
        try {
            String jsonStr = extractJson(aiResponse, marker);
            if (jsonStr == null && aiResponse.contains("{")) {
                jsonStr = aiResponse.substring(aiResponse.indexOf("{"), aiResponse.lastIndexOf("}") + 1);
            }
            if (jsonStr == null) return Map.of();

            @SuppressWarnings("unchecked")
            Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);
            return report;
        } catch (Exception e) {
            log.warn("解析报告JSON失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 从文本中提取标记后的 JSON（支持嵌套括号）
     */
    String extractJson(String text, String marker) {
        if (text == null || marker == null) return null;
        int markerIdx = text.indexOf(marker);
        if (markerIdx < 0) return null;

        String afterMarker = text.substring(markerIdx + marker.length());
        int jsonStart = afterMarker.indexOf("{");
        if (jsonStart < 0) return null;

        // 平衡括号匹配
        int depth = 0;
        int end = -1;
        for (int i = jsonStart; i < afterMarker.length(); i++) {
            char c = afterMarker.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { end = i + 1; break; }
            }
        }

        if (end > jsonStart) {
            return afterMarker.substring(jsonStart, end);
        }
        return null;
    }

    /**
     * 构建默认的失败报告
     */
    Map<String, Object> buildFallbackReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("score", 1);
        report.put("feedback", "面试已结束，未能生成有效评估报告");
        report.put("dimensions", java.util.List.of());
        report.put("suggestion", "建议重新进行面试");
        return report;
    }

    /**
     * 从报告中提取分数
     */
    int extractScore(Map<String, Object> report) {
        Object score = report.get("score");
        if (score instanceof Number) return ((Number) score).intValue();
        return 0;
    }
}
