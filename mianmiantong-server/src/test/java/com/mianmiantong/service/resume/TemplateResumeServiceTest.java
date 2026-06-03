package com.mianmiantong.service.resume;

import com.mianmiantong.entity.resume.ResumeTemplate;
import com.mianmiantong.mapper.resume.ResumeTemplateMapper;
import com.mianmiantong.service.ai.gateway.AiGateway;
import com.mianmiantong.service.ai.gateway.AiRequest;
import com.mianmiantong.service.ai.gateway.AiResponse;
import com.mianmiantong.service.document.WordExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateResumeService Smoke Test")
class TemplateResumeServiceTest {

    @Mock
    private ResumeTemplateMapper templateMapper;

    @Mock
    private AiGateway aiGateway;

    @Mock
    private WordExportService wordExportService;

    private TemplateResumeService service;

    @BeforeEach
    void setUp() {
        service = new TemplateResumeService(templateMapper, aiGateway, wordExportService);
    }

    @Test
    @DisplayName("generateFromResume calls AI and returns bytes")
    void generateFromResume_callsAiAndReturnsBytes() {
        // Arrange
        ResumeTemplate template = new ResumeTemplate();
        template.setId(1L);
        template.setName("测试模板");
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(aiGateway.chat(any(AiRequest.class), isNull()))
                .thenReturn(new AiResponse("# 张三\n- 联系方式\n\n## 技能特长\n- Java: 熟练", "deepseek-v4-flash", 0, 0));
        when(wordExportService.exportMarkdown(anyString(), anyString()))
                .thenReturn(new byte[]{1, 2, 3});

        // Act
        byte[] result = service.generateFromResume(1L, 1L, "Java开发工程师", "后端开发");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("listTemplates returns active templates")
    void listTemplates_returnsActiveTemplates() {
        // Arrange
        ResumeTemplate template = new ResumeTemplate();
        template.setId(1L);
        template.setName("模板1");
        template.setIsActive(1);
        when(templateMapper.selectList(any())).thenReturn(List.of(template));

        // Act
        List<ResumeTemplate> result = service.listTemplates();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("模板1");
    }
}
