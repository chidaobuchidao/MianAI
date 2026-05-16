package com.mianmiantong.service.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI服务接口
 */
public interface AiService {

    String chat(String systemPrompt, List<Map<String, String>> messages);

    String chat(String systemPrompt, List<Map<String, String>> messages, String userApiKey);

    /** 非流式对话，可指定模型（为 null 时使用默认） */
    String chat(String systemPrompt, List<Map<String, String>> messages, String userApiKey, String model);

    /** 流式对话，使用默认模型 */
    default void streamChat(String systemPrompt, List<Map<String, String>> messages,
                    String userApiKey, Consumer<String> onToken) {
        streamChat(systemPrompt, messages, userApiKey, null, onToken);
    }

    /** 流式对话，可指定模型（为 null 时使用默认） */
    void streamChat(String systemPrompt, List<Map<String, String>> messages,
                    String userApiKey, String model, Consumer<String> onToken);
}
