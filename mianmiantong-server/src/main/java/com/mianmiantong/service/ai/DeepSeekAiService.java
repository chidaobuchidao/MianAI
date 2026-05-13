package com.mianmiantong.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * DeepSeek AI服务
 * 当 ai.provider=deepseek 时生效
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek")
public class DeepSeekAiService implements AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String endpoint;

    public DeepSeekAiService(@Value("${ai.deepseek.api-key}") String apiKey,
                             @Value("${ai.deepseek.model}") String model,
                             @Value("${ai.deepseek.endpoint}") String endpoint) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        // Spring @Value 可能因占位符解析失败返回空，兜底读系统属性
        this.apiKey = !apiKey.isBlank() ? apiKey : System.getProperty("DEEPSEEK_API_KEY", apiKey);
        this.model = model;
        this.endpoint = endpoint;
        log.info("DeepSeek config - endpoint: {}, model: {}, apiKey(len={}, prefix={})",
                endpoint, model, this.apiKey.length(),
                this.apiKey.isEmpty() ? "<EMPTY>" : this.apiKey.substring(0, Math.min(7, this.apiKey.length())));
    }

    @Override
    public String chat(String systemPrompt, List<Map<String, String>> messages) {
        return doChat(systemPrompt, messages, apiKey);
    }

    @Override
    public String chat(String systemPrompt, List<Map<String, String>> messages, String userApiKey) {
        String key = (userApiKey != null && !userApiKey.isBlank()) ? userApiKey : apiKey;
        return doChat(systemPrompt, messages, key);
    }

    private String doChat(String systemPrompt, List<Map<String, String>> messages, String key) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + key);

            List<Map<String, String>> allMessages = new ArrayList<>();
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
            allMessages.addAll(messages);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", allMessages);

            String json = objectMapper.writeValueAsString(body);
            log.debug("DeepSeek request: {}", json);

            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            String response = restTemplate.postForObject(endpoint, entity, String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = objectMapper.readValue(response, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            throw new RuntimeException("DeepSeek返回格式异常: " + response);

        } catch (Exception e) {
            log.error("DeepSeek API调用失败", e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(String systemPrompt, List<Map<String, String>> messages,
                           String userApiKey, String modelOverride, Consumer<String> onToken) {
        String key = (userApiKey != null && !userApiKey.isBlank()) ? userApiKey : apiKey;
        String mdl = (modelOverride != null && !modelOverride.isBlank()) ? modelOverride : model;
        doStreamChat(systemPrompt, messages, key, mdl, onToken);
    }

    private void doStreamChat(String systemPrompt, List<Map<String, String>> messages,
                              String key, String mdl, Consumer<String> onToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + key);

            List<Map<String, String>> allMessages = new ArrayList<>();
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
            allMessages.addAll(messages);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", mdl);
            body.put("messages", allMessages);
            body.put("stream", true);

            String json = objectMapper.writeValueAsString(body);
            log.debug("DeepSeek stream request: {}", json);

            restTemplate.execute(endpoint, HttpMethod.POST, request -> {
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                request.getHeaders().set("Authorization", "Bearer " + key);
                request.getBody().write(json.getBytes(StandardCharsets.UTF_8));
            }, response -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) break;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        if (choices == null || choices.isEmpty()) continue;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                        if (delta == null) continue;

                        String content = (String) delta.get("content");
                        if (content != null && !content.isEmpty()) {
                            onToken.accept(content);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("DeepSeek流式调用失败", e);
            throw new RuntimeException("AI流式调用失败: " + e.getMessage());
        }
    }
}
