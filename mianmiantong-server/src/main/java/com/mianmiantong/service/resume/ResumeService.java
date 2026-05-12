package com.mianmiantong.service.resume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.entity.resume.ResumeAnalysis;
import com.mianmiantong.mapper.resume.ResumeAnalysisMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.document.DocumentAiService;
import com.mianmiantong.service.document.DocumentParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper analysisMapper;
    private final DocumentAiService documentAiService;

    public ResumeService(ResumeMapper resumeMapper,
                         ResumeAnalysisMapper analysisMapper,
                         DocumentAiService documentAiService) {
        this.resumeMapper = resumeMapper;
        this.analysisMapper = analysisMapper;
        this.documentAiService = documentAiService;
    }

    /**
     * 上传简历 — 保存记录后立即返回，文档解析异步执行
     */
    @Transactional
    public Map<String, Object> upload(MultipartFile file, String jobDescription, String position) {
        Long userId = JwtAuthFilter.getCurrentUserId();

        String fileName = file.getOriginalFilename();
        String fileType = getFileType(fileName);

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(fileName);
        resume.setFileType(fileType);
        resume.setFileSize(file.getSize());
        resume.setJobDescription(jobDescription);
        resume.setPosition(position);
        resume.setParseStatus(0);
        // 保存原始文件，用于后续模板保留导出
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            resume.setFileData(fileBytes);
        } catch (Exception e) {
            log.error("读取文件失败: fileName={}", fileName, e);
            resume.setParseStatus(-1);
            resumeMapper.insert(resume);
            Map<String, Object> result = new HashMap<>();
            result.put("resumeId", resume.getId());
            result.put("parseStatus", resume.getParseStatus());
            result.put("fileName", resume.getFileName());
            return result;
        }
        resumeMapper.insert(resume);

        // 异步提交文档解析，不阻塞上传响应
        final Long resumeId = resume.getId();
        CompletableFuture.runAsync(() -> {
            try {
                String taskId = documentAiService.submitParse(
                        new ByteArrayInputStream(fileBytes), fileName);
                Resume r = resumeMapper.selectById(resumeId);
                if (r != null) {
                    r.setDocTaskId(taskId);
                    resumeMapper.updateById(r);
                }
                log.info("简历解析任务提交成功: resumeId={}, taskId={}", resumeId, taskId);
            } catch (Exception e) {
                log.error("文档解析提交失败: resumeId={}, fileName={}", resumeId, fileName, e);
                Resume r = resumeMapper.selectById(resumeId);
                if (r != null) {
                    r.setParseStatus(-1);
                    resumeMapper.updateById(r);
                }
            }
        });

        log.info("简历上传完成: resumeId={}, parseStatus=0(异步解析中), fileName={}",
                resume.getId(), fileName);

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("parseStatus", 0);
        result.put("fileName", resume.getFileName());
        return result;
    }

    /**
     * 轮询解析状态
     */
    @Transactional
    public Map<String, Object> getStatus(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) throw new IllegalArgumentException("简历不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("parseStatus", resume.getParseStatus());

        if (resume.getParseStatus() == 0 && resume.getDocTaskId() != null) {
            DocumentParseResult parseResult = documentAiService.getResult(resume.getDocTaskId());
            if ("SUCCESS".equals(parseResult.getStatus())) {
                resume.setParseStatus(1);
                resume.setParsedText(parseResult.getParsedText());
                resumeMapper.updateById(resume);
            } else if ("FAIL".equals(parseResult.getStatus())) {
                resume.setParseStatus(-1);
                resumeMapper.updateById(resume);
            }
        }

        result.put("parseStatus", resume.getParseStatus());
        result.put("statusText", statusText(resume.getParseStatus()));
        if (resume.getParseStatus() == 1) {
            result.put("parsedText", resume.getParsedText());
        }

        return result;
    }

    public Resume getById(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) throw new IllegalArgumentException("简历不存在");
        return resume;
    }

    public ResumeAnalysis getAnalysis(Long resumeId) {
        return analysisMapper.selectOne(
            new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
        );
    }

    public List<Resume> getHistory() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        return resumeMapper.selectList(
            new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getCreateTime)
                .last("LIMIT 20")
                .select(info -> !"fileData".equals(info.getColumn()))
        );
    }

    public void delete(Long resumeId) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new IllegalArgumentException("简历不存在");
        }
        resumeMapper.deleteById(resumeId);
        analysisMapper.delete(
            new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
        );
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "pdf";
        if (name.endsWith(".doc")) return "doc";
        if (name.endsWith(".docx")) return "docx";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".png")) return "png";
        return "unknown";
    }

    private String statusText(int status) {
        return switch (status) {
            case 0 -> "解析中...";
            case 1 -> "解析完成";
            case -1 -> "解析失败";
            default -> "未知";
        };
    }
}
