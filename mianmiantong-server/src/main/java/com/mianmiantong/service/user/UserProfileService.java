package com.mianmiantong.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.entity.exam.AnswerRecord;
import com.mianmiantong.entity.interview.InterviewSession;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.entity.wrongbook.WrongQuestion;
import com.mianmiantong.mapper.exam.AnswerRecordMapper;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import org.springframework.stereotype.Service;

/**
 * 用户个人页的统计与配额视图。
 *
 * <p>把练习/面试/错题计数与配额可见性规则从 controller 收拢到这里，
 * controller 只负责取当前用户和包装响应。
 */
@Service
public class UserProfileService {

    /** 未在用户记录上显式设置时的每日免费调用次数。 */
    static final int DEFAULT_DAILY_QUOTA = 10;

    /** 已完成的面试会话状态值。 */
    private static final int SESSION_STATUS_COMPLETED = 1;

    /** 知识库开关的启用值。 */
    private static final int KB_ENABLED = 1;

    private final AnswerRecordMapper answerRecordMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final UserMapper userMapper;
    private final UserAiConfigService userAiConfigService;
    private final QuotaService quotaService;

    public UserProfileService(AnswerRecordMapper answerRecordMapper,
                              InterviewSessionMapper interviewSessionMapper,
                              WrongQuestionMapper wrongQuestionMapper,
                              UserMapper userMapper,
                              UserAiConfigService userAiConfigService,
                              QuotaService quotaService) {
        this.answerRecordMapper = answerRecordMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.userMapper = userMapper;
        this.userAiConfigService = userAiConfigService;
        this.quotaService = quotaService;
    }

    /** 个人页三个计数：已答题数、已完成面试数、错题数。 */
    public ActivityStats activityStats(Long userId) {
        long practiceCount = answerRecordMapper.selectCount(
            new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getUserId, userId));

        long interviewCount = interviewSessionMapper.selectCount(
            new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getStatus, SESSION_STATUS_COMPLETED));

        long wrongCount = wrongQuestionMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));

        return new ActivityStats(practiceCount, interviewCount, wrongCount);
    }

    /**
     * 配额与能力可见性。
     *
     * <p>自带 API Key 的用户和管理员不受每日免费额度约束，直接读用户记录上的数值；
     * 其余用户走 {@link QuotaService} 的每日刷新逻辑。知识库对自带 Key、管理员
     * 或被单独授权的用户开放。
     */
    public QuotaView quotaView(Long userId, boolean isAdmin) {
        boolean hasApiKey = userAiConfigService.hasApiKey(userId);

        if (userId == null) {
            return new QuotaView(hasApiKey, isAdmin, hasApiKey || isAdmin,
                DEFAULT_DAILY_QUOTA, 0, DEFAULT_DAILY_QUOTA);
        }

        User user = userMapper.selectById(userId);
        boolean meteredByService = user != null && !isAdmin && !hasApiKey;

        int dailyQuota;
        int quotaUsed;
        int quotaRemaining;
        if (meteredByService) {
            QuotaService.QuotaInfo info = quotaService.getQuotaInfo(userId);
            dailyQuota = info.dailyQuota();
            quotaUsed = info.quotaUsed();
            quotaRemaining = info.quotaRemaining();
        } else {
            dailyQuota = valueOrDefault(user == null ? null : user.getDailyQuota(), DEFAULT_DAILY_QUOTA);
            quotaUsed = valueOrDefault(user == null ? null : user.getQuotaUsed(), 0);
            quotaRemaining = Math.max(0, dailyQuota - quotaUsed);
        }

        boolean userKbEnabled = user != null
            && Integer.valueOf(KB_ENABLED).equals(user.getKnowledgeBaseEnabled());

        return new QuotaView(hasApiKey, isAdmin, hasApiKey || isAdmin || userKbEnabled,
            dailyQuota, quotaUsed, quotaRemaining);
    }

    private static int valueOrDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    /** 个人页活动计数。 */
    public record ActivityStats(long practiceCount, long interviewCount, long wrongCount) {}

    /** 配额与能力可见性视图。 */
    public record QuotaView(boolean hasApiKey, boolean isAdmin, boolean knowledgeBaseEnabled,
                            int dailyQuota, int quotaUsed, int quotaRemaining) {}
}
