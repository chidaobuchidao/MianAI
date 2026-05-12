package com.mianmiantong.controller.interview;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.interview.InterviewAnswerRequest;
import com.mianmiantong.dto.interview.InterviewStartRequest;
import com.mianmiantong.service.interview.InterviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /** 开始面试 */
    @PostMapping("/start")
    public Result<?> start(@Valid @RequestBody InterviewStartRequest request) {
        return Result.ok(interviewService.start(request));
    }

    /** 回答问题 */
    @PostMapping("/{sessionId}/answer")
    public Result<?> answer(@PathVariable Long sessionId,
                            @Valid @RequestBody InterviewAnswerRequest request) {
        return Result.ok(interviewService.answer(sessionId, request.getAnswer()));
    }

    /** 回答问题 - 流式SSE */
    @PostMapping("/{sessionId}/answer/stream")
    public SseEmitter answerStream(@PathVariable Long sessionId,
                                   @Valid @RequestBody InterviewAnswerRequest request) {
        return interviewService.answerStream(sessionId, request.getAnswer());
    }

    /** 手动结束面试 */
    @PostMapping("/{sessionId}/end")
    public Result<?> end(@PathVariable Long sessionId) {
        return Result.ok(interviewService.end(sessionId));
    }

    /** 面试历史列表 */
    @GetMapping("/list")
    public Result<?> history() {
        return Result.ok(interviewService.getHistory());
    }

    /** 面试详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(interviewService.getDetail(id));
    }
}
