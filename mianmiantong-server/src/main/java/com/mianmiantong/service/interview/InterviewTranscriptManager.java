package com.mianmiantong.service.interview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.service.ai.gateway.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 面试对话记录管理器
 * 负责消息的解析、序列化和转换
 */
@Slf4j
@Component
class InterviewTranscriptManager {

    private final ObjectMapper objectMapper;

    InterviewTranscriptManager() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析 JSON 字符串为消息列表
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> parseMessages(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            log.warn("解析messages JSON失败，会话历史可能丢失: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 将对象序列化为 JSON 字符串
     */
    String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "[]";
        }
    }

    /**
     * 将内部消息格式转换为 AI 消息格式
     */
    List<ChatMessage> toChatMessages(List<Map<String, Object>> messages) {
        List<ChatMessage> result = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            String role = (String) msg.get("role");
            String content = (String) msg.get("content");
            if ("user".equals(role) || "assistant".equals(role)) {
                result.add(new ChatMessage(role, content));
            }
        }
        return result;
    }

    /**
     * 构建对话记录文本（用于评估 prompt）
     */
    String buildTranscript(List<Map<String, Object>> messages) {
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
        return transcript.toString();
    }

    /**
     * 构建面试报告用的对话记录（排除编程环节）
     */
    String buildReportTranscript(List<Map<String, Object>> messages) {
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
        return transcript.toString();
    }

    /**
     * 摘要最近的对话上下文（用于日志）
     */
    String summarizeContext(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        int total = messages.size();
        int start = Math.max(0, total - 6);
        for (int i = start; i < total; i++) {
            ChatMessage msg = messages.get(i);
            String role = "user".equals(msg.role()) ? "候选人" : "面试官";
            String content = msg.content();
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            sb.append(String.format("  [%s] %s%n", role, content));
        }
        return sb.toString();
    }
}
