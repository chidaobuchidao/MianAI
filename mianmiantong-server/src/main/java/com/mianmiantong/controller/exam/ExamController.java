package com.mianmiantong.controller.exam;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.exam.ExamSubmitRequest;
import com.mianmiantong.service.exam.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /** 获取试卷列表 */
    @GetMapping
    public Result<?> list() {
        return Result.ok(examService.getExams());
    }

    /** 开始考试 */
    @PostMapping("/{examId}/start")
    public Result<?> start(@PathVariable Long examId) {
        return Result.ok(examService.startExam(examId));
    }

    /** 提交考试 */
    @PostMapping("/{examId}/submit")
    public Result<?> submit(@PathVariable Long examId, @Valid @RequestBody ExamSubmitRequest request) {
        return Result.ok(examService.submit(examId, request));
    }
}
