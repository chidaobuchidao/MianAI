package com.mianmiantong.service.interview;

import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.service.ai.gateway.AiGateway;
import com.mianmiantong.service.coding.AlgorithmProblemService;
import com.mianmiantong.service.user.QuotaService;
import com.mianmiantong.service.user.UserAiConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService Smoke Test")
class InterviewServiceTest {

    @Mock
    private InterviewSessionMapper sessionMapper;

    @Mock
    private AiGateway aiGateway;

    @Mock
    private UserAiConfigService userAiConfigService;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private AlgorithmProblemService algorithmProblemService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private QuotaService quotaService;

    @Mock
    private InterviewPromptBuilder promptBuilder;

    @Mock
    private InterviewTranscriptManager transcriptManager;

    @Mock
    private InterviewReportParser reportParser;

    @Test
    @DisplayName("service can be instantiated with mocked dependencies")
    void canInstantiate() {
        InterviewService service = new InterviewService(
                sessionMapper, aiGateway, userAiConfigService,
                resumeMapper, algorithmProblemService, userMapper, quotaService,
                promptBuilder, transcriptManager, reportParser);
        assertThat(service).isNotNull();
    }
}
