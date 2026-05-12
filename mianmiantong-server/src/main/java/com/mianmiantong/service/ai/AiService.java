package com.mianmiantong.service.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI服务接口 - 通过 @ConditionalOnProperty 实现多模型切换
 * 使用方式类似 #if 宏:
 * - @ConditionalOnProperty(name = "ai.provider", havingValue = "qwen")  → QwenAiService
 * - @ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek") → DeepSeekAiService
 */
public interface AiService {

    /**
     * 对话调用（使用系统默认 API Key）
     */
    String chat(String systemPrompt, List<Map<String, String>> messages);

    /**
     * 对话调用（使用用户自定义 API Key，为 null 时回退到系统默认）
     */
    String chat(String systemPrompt, List<Map<String, String>> messages, String userApiKey);

    /**
     * 流式对话 - 每收到一个 token 回调 onToken.accept(content)
     * 阻塞直到流结束，异常直接抛出
     */
    void streamChat(String systemPrompt, List<Map<String, String>> messages,
                    String userApiKey, Consumer<String> onToken);
}
