package com.mianmiantong.service.ai.gateway;

/**
 * AI 任务类型，用于配额计算和成本控制
 */
public enum AiTaskType {
    /** 快速模型，成本低 */
    FLASH(1),
    /** 专业模型，成本高 */
    PRO(2);

    private final int costUnits;

    AiTaskType(int costUnits) {
        this.costUnits = costUnits;
    }

    public int getCostUnits() {
        return costUnits;
    }
}
