package com.mianmiantong.service.ai.gateway;

/**
 * 替代 Map<String,String> 的类型安全消息表示
 */
public record ChatMessage(String role, String content) {
}
