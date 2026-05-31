package com.mianmiantong.service.user;

import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.service.ai.AiModelSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 统一配额管理服务。
 * 每日配额制：普通用户默认 10 次/天，Pro 模型消耗 ×2。
 * 管理员和自备 API Key 的用户不限配额。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserMapper userMapper;
    private final UserAiConfigService userAiConfigService;

    /**
     * 检查并消耗配额。满足以下任一条件不消耗：管理员、自备 API Key。
     * @param model AI 模型名，含 "pro" 则消耗 2 步，否则 1 步
     * @throws QuotaExhaustedException 配额耗尽
     */
    public void checkAndConsume(Long userId, String model) {
        if (userId == null) return;
        if (JwtAuthFilter.isAdmin()) return;

        UserAiConfig config = userAiConfigService.getByUserId(userId);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) return;

        User user = userMapper.selectById(userId);
        if (user == null) throw new QuotaExhaustedException("用户不存在");

        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : 10;
        int used = user.getQuotaUsed() != null ? user.getQuotaUsed() : 0;

        // 跨天自动重置
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getQuotaDate())) {
            user.setQuotaUsed(0);
            user.setQuotaDate(today);
            userMapper.updateById(user);
            used = 0;
        }

        if (used >= daily) {
            throw new QuotaExhaustedException("今日免费次数已用完（" + daily + "次/天），请配置 AI API Key 后无限使用");
        }

        int steps = AiModelSelector.PRO.equals(AiModelSelector.normalize(model)) ? 2 : 1;
        userMapper.incrementQuota(userId, steps);
    }

    /**
     * 仅检查配额（不消耗），返回应使用的系统 API Key。
     * Admin/自备 Key 用户返回 null 表示"不限"；普通用户配额耗尽则抛异常。
     */
    public String checkQuota() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        if (userId == null) return null;
        if (JwtAuthFilter.isAdmin()) return null;

        UserAiConfig config = userAiConfigService.getByUserId(userId);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) return null;

        User user = userMapper.selectById(userId);
        if (user == null) throw new QuotaExhaustedException("用户不存在");

        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : 10;
        int used = user.getQuotaUsed() != null ? user.getQuotaUsed() : 0;

        LocalDate today = LocalDate.now();
        if (!today.equals(user.getQuotaDate())) {
            user.setQuotaUsed(0);
            user.setQuotaDate(today);
            userMapper.updateById(user);
            used = 0;
        }

        if (used >= daily) {
            throw new QuotaExhaustedException("今日免费次数已用完（" + daily + "次/天），请配置 AI API Key 后无限使用");
        }
        return null; // 不限制，可用系统 Key
    }

    /** 获取额度信息，供前端展示 */
    public QuotaInfo getQuotaInfo(Long userId) {
        if (userId == null) return new QuotaInfo(true, 0, 0, 0);

        if (JwtAuthFilter.isAdmin()) {
            return new QuotaInfo(true, 0, 0, -1); // -1 = unlimited
        }

        UserAiConfig config = userAiConfigService.getByUserId(userId);
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return new QuotaInfo(true, 0, 0, -1);
        }

        User user = userMapper.selectById(userId);
        if (user == null) return new QuotaInfo(false, 0, 0, 0);

        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : 10;
        int used = user.getQuotaUsed() != null ? user.getQuotaUsed() : 0;

        LocalDate today = LocalDate.now();
        if (!today.equals(user.getQuotaDate())) used = 0;

        int remaining = Math.max(0, daily - used);
        return new QuotaInfo(false, daily, used, remaining);
    }

    /** 配额耗尽异常 */
    public static class QuotaExhaustedException extends RuntimeException {
        public QuotaExhaustedException(String message) { super(message); }
    }

    /** 额度信息 DTO */
    public record QuotaInfo(boolean unlimited, int dailyQuota, int quotaUsed, int quotaRemaining) {}
}
