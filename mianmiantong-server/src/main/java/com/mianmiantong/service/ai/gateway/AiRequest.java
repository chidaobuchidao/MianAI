package com.mianmiantong.service.ai.gateway;

import java.util.List;

/**
 * 不可变的 AI 请求对象
 */
public record AiRequest(
    String systemPrompt,
    List<ChatMessage> messages,
    String model,
    AiTaskType taskType
) {
}
