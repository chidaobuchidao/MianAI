package com.mianmiantong.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.common.Result;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.user.UserAiConfigRequest;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.exam.AnswerRecordMapper;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import com.mianmiantong.service.user.QuotaService;
import com.mianmiantong.service.user.UserAiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AnswerRecordMapper answerRecordMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final UserAiConfigService userAiConfigService;
    private final UserMapper userMapper;
    private final QuotaService quotaService;

    public UserController(AnswerRecordMapper answerRecordMapper,
                          InterviewSessionMapper interviewSessionMapper,
                          WrongQuestionMapper wrongQuestionMapper,
                          UserAiConfigService userAiConfigService,
                          UserMapper userMapper,
                          QuotaService quotaService) {
        this.answerRecordMapper = answerRecordMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.userAiConfigService = userAiConfigService;
        this.userMapper = userMapper;
        this.quotaService = quotaService;
    }

    /** 获取用户统计数据 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long userId = JwtAuthFilter.getCurrentUserId();

        Long practiceCount = answerRecordMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mianmiantong.entity.exam.AnswerRecord>()
                .eq(com.mianmiantong.entity.exam.AnswerRecord::getUserId, userId)
        );

        Long interviewCount = interviewSessionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mianmiantong.entity.interview.InterviewSession>()
                .eq(com.mianmiantong.entity.interview.InterviewSession::getUserId, userId)
                .eq(com.mianmiantong.entity.interview.InterviewSession::getStatus, 1)
        );

        Long wrongCount = wrongQuestionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mianmiantong.entity.wrongbook.WrongQuestion>()
                .eq(com.mianmiantong.entity.wrongbook.WrongQuestion::getUserId, userId)
        );

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("practiceCount", practiceCount);
        stats.put("interviewCount", interviewCount);
        stats.put("wrongCount", wrongCount);

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
        try {
            var resource = new ClassPathResource("provider-presets.json");
            var objectMapper = new ObjectMapper();
            List<Map<String, Object>> providers = objectMapper.readValue(
                    resource.getInputStream(), new TypeReference<>() {});
            return Result.ok(providers);
        } catch (Exception e) {
            log.error("读取 provider-presets.json 失败", e);
            return Result.ok(List.of());
        }
    }

    /** 获取用户配额：每日免费 AI 调用次数剩余 */
    @GetMapping("/quota")
    public Result<Map<String, Object>> quota() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        Map<String, Object> result = new LinkedHashMap<>();
        boolean hasApiKey = userAiConfigService.hasApiKey(userId);
        result.put("hasApiKey", hasApiKey);
        boolean isAdmin = JwtAuthFilter.isAdmin();
        boolean userKbEnabled = false;
        int dailyQuota = 10, quotaUsed = 0, quotaRemaining = 10;
        if (userId != null) {
            var user = userMapper.selectById(userId);
            if (user != null && !isAdmin && !hasApiKey) {
                var quotaInfo = quotaService.getQuotaInfo(userId);
                dailyQuota = quotaInfo.dailyQuota();
                quotaUsed = quotaInfo.quotaUsed();
                quotaRemaining = quotaInfo.quotaRemaining();
            } else {
                dailyQuota = user != null && user.getDailyQuota() != null ? user.getDailyQuota() : 10;
                quotaUsed = user != null && user.getQuotaUsed() != null ? user.getQuotaUsed() : 0;
                quotaRemaining = Math.max(0, dailyQuota - quotaUsed);
            }
            userKbEnabled = user != null && user.getKnowledgeBaseEnabled() != null && user.getKnowledgeBaseEnabled() == 1;
        }
        result.put("isAdmin", isAdmin);
        result.put("knowledgeBaseEnabled", hasApiKey || isAdmin || userKbEnabled);
        result.put("dailyQuota", dailyQuota);
        result.put("quotaUsed", quotaUsed);
        result.put("quotaRemaining", quotaRemaining);
        return Result.ok(result);
    }
}
