package com.mianmiantong.service.ai.gateway;

/**
 * AI 响应包装
 */
public record AiResponse(
    String content,
    String model,
    int promptTokens,
    int completionTokens
) {
}
