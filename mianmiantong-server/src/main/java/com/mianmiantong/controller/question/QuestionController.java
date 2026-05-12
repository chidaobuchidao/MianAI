package com.mianmiantong.controller.question;

import com.mianmiantong.common.Result;
import com.mianmiantong.dto.question.PageQuery;
import com.mianmiantong.service.question.QuestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /** 获取题库列表 */
    @GetMapping
    public Result<?> list(@RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) Integer difficulty,
                          @RequestParam(required = false) Integer type,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        PageQuery pq = new PageQuery();
        pq.setPage(page);
        pq.setSize(size);
        return Result.ok(questionService.page(categoryId, difficulty, type, pq));
    }

    /** 随机抽题 */
    @GetMapping("/random")
    public Result<?> random(@RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Integer difficulty,
                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(questionService.random(categoryId, difficulty, size));
    }

    /** 获取题目详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(questionService.getById(id));
    }

    /** 获取分类列表 */
    @GetMapping("/categories")
    public Result<?> categories() {
        return Result.ok(questionService.getCategories());
    }
}
