package com.mianmiantong.service.ai.gateway;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供者注册表
 * 管理所有可用的 AI 提供者适配器
 */
public class ProviderRegistry {

    private final Map<String, ProviderAdapter> adapters;
    private final String defaultProvider;
    private final Map<String, ProviderAdapter> customAdapters = new ConcurrentHashMap<>();

    public ProviderRegistry(Map<String, ProviderAdapter> adapters, String defaultProvider) {
        this.adapters = adapters != null ? adapters : Collections.emptyMap();
        this.defaultProvider = defaultProvider;
    }

    /**
     * 按名称获取适配器
     */
    public ProviderAdapter getAdapter(String name) {
        return adapters.get(name);
    }

    /**
     * 获取或创建自定义端点的适配器
     */
    public ProviderAdapter getOrCreateCustomAdapter(String endpoint) {
        return customAdapters.computeIfAbsent(endpoint, ep -> {
            ProviderConfig config = new ProviderConfig("custom", ep, "", Collections.emptyList());
            return new OpenAiCompatibleAdapter(config);
        });
    }

    /**
     * 获取默认适配器
     */
    public ProviderAdapter getDefault() {
        ProviderAdapter adapter = adapters.get(defaultProvider);
        if (adapter == null && !adapters.isEmpty()) {
            adapter = adapters.values().iterator().next();
        }
        return adapter;
    }

    /**
     * 获取所有可用的提供者名称
     */
    public java.util.Set<String> getAvailableProviders() {
        return adapters.keySet();
    }
}
