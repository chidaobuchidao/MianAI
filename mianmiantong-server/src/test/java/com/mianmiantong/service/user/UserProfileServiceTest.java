package com.mianmiantong.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.mapper.exam.AnswerRecordMapper;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService")
class UserProfileServiceTest {

    private static final long USER_ID = 5L;

    @Mock private AnswerRecordMapper answerRecordMapper;
    @Mock private InterviewSessionMapper interviewSessionMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserAiConfigService userAiConfigService;
    @Mock private QuotaService quotaService;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(answerRecordMapper, interviewSessionMapper,
            wrongQuestionMapper, userMapper, userAiConfigService, quotaService);
    }

    private static User user(Integer dailyQuota, Integer quotaUsed, Integer kbEnabled) {
        User u = new User();
        u.setId(USER_ID);
        u.setDailyQuota(dailyQuota);
        u.setQuotaUsed(quotaUsed);
        u.setKnowledgeBaseEnabled(kbEnabled);
        return u;
    }

    @Nested
    @DisplayName("activityStats")
    class ActivityStats {

        @Test
        @DisplayName("汇总答题、已完成面试与错题三个计数")
        void aggregatesThreeCounts() {
            when(answerRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(12L);
            when(interviewSessionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
            when(wrongQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);

            UserProfileService.ActivityStats stats = service.activityStats(USER_ID);

            assertThat(stats.practiceCount()).isEqualTo(12L);
            assertThat(stats.interviewCount()).isEqualTo(3L);
            assertThat(stats.wrongCount()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("quotaView")
    class QuotaView {

        @Test
        @DisplayName("普通用户走 QuotaService 的每日额度")
        void meteredUserReadsQuotaService() {
            when(userAiConfigService.hasApiKey(USER_ID)).thenReturn(false);
            when(userMapper.selectById(USER_ID)).thenReturn(user(20, 5, 0));
            when(quotaService.getQuotaInfo(USER_ID))
                .thenReturn(new QuotaService.QuotaInfo(false, 20, 5, 15));

            UserProfileService.QuotaView view = service.quotaView(USER_ID, false);

            assertThat(view.dailyQuota()).isEqualTo(20);
            assertThat(view.quotaUsed()).isEqualTo(5);
            assertThat(view.quotaRemaining()).isEqualTo(15);
            assertThat(view.knowledgeBaseEnabled()).isFalse();
        }

        @Test
        @DisplayName("自带 API Key 的用户不查 QuotaService，并开放知识库")
        void userWithOwnKeyBypassesQuotaService() {
            when(userAiConfigService.hasApiKey(USER_ID)).thenReturn(true);
            when(userMapper.selectById(USER_ID)).thenReturn(user(30, 8, 0));

            UserProfileService.QuotaView view = service.quotaView(USER_ID, false);

            assertThat(view.hasApiKey()).isTrue();
            assertThat(view.knowledgeBaseEnabled()).isTrue();
            assertThat(view.quotaRemaining()).isEqualTo(22);
            verify(quotaService, never()).getQuotaInfo(any());
        }

        @Test
        @DisplayName("管理员不受每日额度约束，并开放知识库")
        void adminBypassesQuotaService() {
            when(userAiConfigService.hasApiKey(USER_ID)).thenReturn(false);
            when(userMapper.selectById(USER_ID)).thenReturn(user(15, 15, 0));

            UserProfileService.QuotaView view = service.quotaView(USER_ID, true);

            assertThat(view.isAdmin()).isTrue();
            assertThat(view.knowledgeBaseEnabled()).isTrue();
            assertThat(view.quotaRemaining()).isZero();
            verify(quotaService, never()).getQuotaInfo(any());
        }

        @Test
        @DisplayName("被单独授权的用户即便无 Key 也开放知识库")
        void explicitlyGrantedUserGetsKnowledgeBase() {
            when(userAiConfigService.hasApiKey(USER_ID)).thenReturn(false);
            when(userMapper.selectById(USER_ID)).thenReturn(user(10, 0, 1));
            when(quotaService.getQuotaInfo(USER_ID))
                .thenReturn(new QuotaService.QuotaInfo(false, 10, 0, 10));

            assertThat(service.quotaView(USER_ID, false).knowledgeBaseEnabled()).isTrue();
        }

        @Test
        @DisplayName("配额字段为 null 时回落到默认值，不抛 NPE")
        void nullQuotaColumnsFallBackToDefaults() {
            when(userAiConfigService.hasApiKey(USER_ID)).thenReturn(true);
            when(userMapper.selectById(USER_ID)).thenReturn(user(null, null, null));

            UserProfileService.QuotaView view = service.quotaView(USER_ID, false);

            assertThat(view.dailyQuota()).isEqualTo(UserProfileService.DEFAULT_DAILY_QUOTA);
            assertThat(view.quotaUsed()).isZero();
            assertThat(view.quotaRemaining()).isEqualTo(UserProfileService.DEFAULT_DAILY_QUOTA);
        }

        @Test
        @DisplayName("已用超过额度时剩余量为 0 而非负数")
        void remainingNeverGoesNegative() {
            when(userAiConfigService.hasApiKey(USER_ID)).thenReturn(true);
            when(userMapper.selectById(USER_ID)).thenReturn(user(10, 14, 0));

            assertThat(service.quotaView(USER_ID, false).quotaRemaining()).isZero();
        }

        @Test
        @DisplayName("未登录时返回默认额度且不查库")
        void anonymousGetsDefaults() {
            when(userAiConfigService.hasApiKey(null)).thenReturn(false);

            UserProfileService.QuotaView view = service.quotaView(null, false);

            assertThat(view.dailyQuota()).isEqualTo(UserProfileService.DEFAULT_DAILY_QUOTA);
            assertThat(view.knowledgeBaseEnabled()).isFalse();
            verify(userMapper, never()).selectById(any());
        }
    }
}
