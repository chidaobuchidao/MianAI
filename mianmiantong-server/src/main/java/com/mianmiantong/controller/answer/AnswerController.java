package com.mianmiantong.controller.answer;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.answer.AnswerSubmitRequest;
import com.mianmiantong.service.answer.AnswerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping
    public Result<?> submit(@Valid @RequestBody AnswerSubmitRequest request) {
        return Result.ok(answerService.submit(request));
    }
}
