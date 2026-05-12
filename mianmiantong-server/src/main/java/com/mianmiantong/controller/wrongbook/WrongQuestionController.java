package com.mianmiantong.controller.wrongbook;

import com.mianmiantong.common.Result;
import com.mianmiantong.service.wrongbook.WrongQuestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    @GetMapping
    public Result<?> list() {
        return Result.ok(wrongQuestionService.getWrongQuestions());
    }

    @DeleteMapping("/{questionId}")
    public Result<?> remove(@PathVariable Long questionId) {
        wrongQuestionService.remove(questionId);
        return Result.ok();
    }
}
