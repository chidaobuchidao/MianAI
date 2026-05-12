package com.mianmiantong.service.resume;

import com.mianmiantong.entity.resume.ResumeTemplate;
import com.mianmiantong.mapper.resume.ResumeTemplateMapper;
import com.mianmiantong.service.ai.AiService;
import com.mianmiantong.service.document.WordExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板简历生成服务 — AI 填充模板内容，WordExportService 渲染
 */
@Slf4j
@Service
public class TemplateResumeService {

    private final ResumeTemplateMapper templateMapper;
    private final AiService aiService;
    private final WordExportService wordExportService;

    public TemplateResumeService(ResumeTemplateMapper templateMapper,
                                 AiService aiService,
                                 WordExportService wordExportService) {
        this.templateMapper = templateMapper;
        this.aiService = aiService;
        this.wordExportService = wordExportService;
    }

    private static final String PROMPT = """
        你是资深技术面试官和职业顾问。请根据以下信息生成优化的简历内容。

        ## 用户背景
        %s

        ## 目标岗位
        %s

        ## 输出格式
        请生成完整简历，使用 Markdown 格式：
        # 姓名
        - 联系方式 | 邮箱

        ## 求职意向
        目标职位

        ## 技能特长
        - 技能1：描述
        - 技能2：描述

        ## 工作/项目经历
        ### 公司/项目名 | 时间段
        - 职责/成果1
        - 职责/成果2

        ## 教育背景
        ### 学校 | 学历 | 时间

        ## 自我评价
        2-3句话的专业总结
        """;

    /**
     * 从已有简历生成模板化内容
     */
    public byte[] generateFromResume(Long resumeId, Long templateId,
                                      String parsedText, String jobDescription) {
        ResumeTemplate tpl = templateMapper.selectById(templateId);
        if (tpl == null) throw new IllegalArgumentException("模板不存在");

        String prompt = String.format(PROMPT, parsedText, jobDescription);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", "请生成简历")
        );

        String aiResponse = aiService.chat(prompt, messages);
        log.info("AI模板简历生成完成: templateId={}, len={}", templateId, aiResponse.length());

        return wordExportService.exportMarkdown(aiResponse,
                tpl.getName() + "简历");
    }

    /** 获取所有模板 */
    public List<ResumeTemplate> listTemplates() {
        return templateMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeTemplate>()
                        .eq(ResumeTemplate::getIsActive, 1)
                        .orderByAsc(ResumeTemplate::getSortOrder)
        );
    }
}
