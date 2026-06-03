package com.mianmiantong.service.resume;

import com.mianmiantong.mapper.resume.ResumeAnalysisMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.ai.gateway.AiGateway;
import com.mianmiantong.service.document.DocumentAiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeAnalysisService Smoke Test")
class ResumeAnalysisServiceTest {

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private ResumeAnalysisMapper analysisMapper;

    @Mock
    private AiGateway aiGateway;

    @Mock
    private DocumentAiService documentAiService;

    @Test
    @DisplayName("service can be instantiated with mocked dependencies")
    void canInstantiate() {
        ResumeAnalysisService service = new ResumeAnalysisService(
                resumeMapper, analysisMapper, aiGateway, documentAiService);
        assertThat(service).isNotNull();
    }
}
