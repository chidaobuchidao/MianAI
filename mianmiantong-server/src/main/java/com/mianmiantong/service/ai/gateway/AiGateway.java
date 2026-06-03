package com.mianmiantong.service.ai.gateway;

import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.service.user.UserAiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 网关门面
 * 消费者使用此服务与 AI 交互
 */
@Slf4j
@Service
public class AiGateway {

    private final ProviderRegistry registry;
    private final KeyResolver keyResolver;
    private final ModelResolver modelResolver;
    private final QuotaPolicy quotaPolicy;
    private final UserAiConfigService userAiConfigService;

    public AiGateway(ProviderRegistry registry, KeyResolver keyResolver,
                     ModelResolver modelResolver, QuotaPolicy quotaPolicy,
                     UserAiConfigService userAiConfigService) {
        this.registry = registry;
        this.keyResolver = keyResolver;
        this.modelResolver = modelResolver;
        this.quotaPolicy = quotaPolicy;
        this.userAiConfigService = userAiConfigService;
    }

    /**
     * 同步聊天
     * @param request AI 请求
     * @param userId 用户 ID（null 表示匿名）
     * @return AI 响应
     */
    public AiResponse chat(AiRequest request, Long userId) {
        ProviderAdapter adapter = resolveAdapter(userId);
        String apiKey = keyResolver.resolve(userId);
        String model = modelResolver.resolve(userId, request.model(), null);

        // 创建带有解析后模型的请求
        AiRequest resolvedRequest = new AiRequest(
                request.systemPrompt(),
                request.messages(),
                model,
                request.taskType()
        );

        // 检查配额
        quotaPolicy.consume(userId, request.taskType(), model);

        log.info("AI chat: provider={}, model={}, userId={}", adapter.name(), model, userId);
        return adapter.chat(resolvedRequest, apiKey);
    }

    /**
     * 流式聊天
     * @param request AI 请求
     * @param userId 用户 ID（null 表示匿名）
     * @param handler 流式处理器
     */
    public void streamChat(AiRequest request, Long userId, AiStreamHandler handler) {
        ProviderAdapter adapter = resolveAdapter(userId);
        String apiKey = keyResolver.resolve(userId);
        String model = modelResolver.resolve(userId, request.model(), null);

        // 创建带有解析后模型的请求
        AiRequest resolvedRequest = new AiRequest(
                request.systemPrompt(),
                request.messages(),
                model,
                request.taskType()
        );

        // 检查配额
        quotaPolicy.consume(userId, request.taskType(), model);

        log.info("AI stream: provider={}, model={}, userId={}", adapter.name(), model, userId);
        adapter.streamChat(resolvedRequest, apiKey, handler);
    }

    private ProviderAdapter resolveAdapter(Long userId) {
        // 根据用户配置选择提供者
        if (userId != null) {
            UserAiConfig config = userAiConfigService.getByUserId(userId);
            if (config != null && config.getProvider() != null) {
                String provider = config.getProvider();
                // 自定义端点
                if ("custom".equals(provider) && config.getCustomEndpoint() != null
                        && !config.getCustomEndpoint().isBlank()) {
                    return registry.getOrCreateCustomAdapter(config.getCustomEndpoint());
                }
                // 预设提供者
                ProviderAdapter adapter = registry.getAdapter(provider);
                if (adapter != null) return adapter;
            }
        }
        // 回退到默认提供者
        ProviderAdapter adapter = registry.getDefault();
        if (adapter == null) {
            throw new RuntimeException("没有可用的 AI 提供者");
        }
        return adapter;
    }
}
