package com.mianmiantong.controller.user;

import com.mianmiantong.common.Result;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.user.UserAiConfigRequest;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.exam.AnswerRecordMapper;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import com.mianmiantong.service.user.UserAiConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AnswerRecordMapper answerRecordMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final UserAiConfigService userAiConfigService;

    public UserController(AnswerRecordMapper answerRecordMapper,
                          InterviewSessionMapper interviewSessionMapper,
                          WrongQuestionMapper wrongQuestionMapper,
                          UserAiConfigService userAiConfigService) {
        this.answerRecordMapper = answerRecordMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.userAiConfigService = userAiConfigService;
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

    /** 保存用户的 AI 配置（API Key） */
    @PutMapping("/ai-config")
    public Result<?> saveAiConfig(@RequestBody UserAiConfigRequest req) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        userAiConfigService.save(userId, req.getProvider(), req.getApiKey(), req.getModel());
        return Result.ok(null);
    }
}
