package com.mianmiantong.controller.user;

import com.mianmiantong.common.Result;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.user.UserAiConfigRequest;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.service.user.AiProviderPresetService;
import com.mianmiantong.service.user.UserAiConfigService;
import com.mianmiantong.service.user.UserProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserAiConfigService userAiConfigService;
    private final UserProfileService userProfileService;
    private final AiProviderPresetService aiProviderPresetService;

    public UserController(UserAiConfigService userAiConfigService,
                          UserProfileService userProfileService,
                          AiProviderPresetService aiProviderPresetService) {
        this.userAiConfigService = userAiConfigService;
        this.userProfileService = userProfileService;
        this.aiProviderPresetService = aiProviderPresetService;
    }

    /** 获取用户统计数据 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        UserProfileService.ActivityStats activity =
            userProfileService.activityStats(JwtAuthFilter.getCurrentUserId());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("practiceCount", activity.practiceCount());
        stats.put("interviewCount", activity.interviewCount());
        stats.put("wrongCount", activity.wrongCount());
        return Result.ok(stats);
    }

    /** 获取用户的 AI 配置 */
    @GetMapping("/ai-config")
    public Result<UserAiConfig> getAiConfig() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        return Result.ok(userAiConfigService.getByUserId(userId));
    }

    /** 保存用户的 AI 配置 */
    @PutMapping("/ai-config")
    public Result<?> saveAiConfig(@RequestBody UserAiConfigRequest req) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        String preferredModel = req.getPreferredModel() != null ? req.getPreferredModel() : req.getModel();
        userAiConfigService.save(userId, req.getProvider(), req.getApiKey(), req.getModel(),
                preferredModel, req.getCustomEndpoint());
        return Result.ok(null);
    }

    /** 获取系统支持的 AI 提供者列表 */
    @GetMapping("/ai-providers")
    public Result<List<Map<String, Object>>> getAiProviders() {
        return Result.ok(aiProviderPresetService.listPresets());
    }

    /** 获取用户配额：每日免费 AI 调用次数剩余 */
    @GetMapping("/quota")
    public Result<Map<String, Object>> quota() {
        UserProfileService.QuotaView quota = userProfileService.quotaView(
            JwtAuthFilter.getCurrentUserId(), JwtAuthFilter.isAdmin());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasApiKey", quota.hasApiKey());
        result.put("isAdmin", quota.isAdmin());
        result.put("knowledgeBaseEnabled", quota.knowledgeBaseEnabled());
        result.put("dailyQuota", quota.dailyQuota());
        result.put("quotaUsed", quota.quotaUsed());
        result.put("quotaRemaining", quota.quotaRemaining());
        return Result.ok(result);
    }
}
