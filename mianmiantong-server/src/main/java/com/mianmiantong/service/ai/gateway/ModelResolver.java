package com.mianmiantong.service.ai.gateway;

import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.service.user.UserAiConfigService;

/**
 * 模型解析器
 * 优先级：请求指定 > 用户偏好 > 任务默认 > 提供者默认
 */
public class ModelResolver {

    private final UserAiConfigService userAiConfigService;

    public ModelResolver(UserAiConfigService userAiConfigService) {
        this.userAiConfigService = userAiConfigService;
    }

    /**
     * 解析模型
     * @param userId 用户 ID
     * @param requestModel 请求中指定的模型
     * @param defaultModel 提供者默认模型
     * @return 解析后的模型名称
     */
    public String resolve(Long userId, String requestModel, String defaultModel) {
        // 1. 请求中指定的模型
        if (requestModel != null && !requestModel.isBlank()) {
            return requestModel;
        }

        // 2. 用户偏好模型
        if (userId != null) {
            UserAiConfig config = userAiConfigService.getByUserId(userId);
            if (config != null && config.getModel() != null && !config.getModel().isBlank()) {
                return config.getModel();
            }
        }

        // 3. 提供者默认模型
        return defaultModel;
    }
}
