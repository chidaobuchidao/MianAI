package com.mianmiantong.service.ai.gateway;

import com.mianmiantong.service.user.QuotaService;

/**
 * 配额策略
 * 封装配额计算逻辑
 */
public class QuotaPolicy {

    private final QuotaService quotaService;

    public QuotaPolicy(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /**
     * 检查并消耗配额
     * @param userId 用户 ID
     * @param taskType 任务类型
     * @param resolvedModel 解析后的模型名称
     */
    public void consume(Long userId, AiTaskType taskType, String resolvedModel) {
        if (userId == null) return;
        quotaService.checkAndConsume(userId, resolvedModel);
    }
}
