package com.mianmiantong.service.wrongbook;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.entity.question.Question;
import com.mianmiantong.entity.question.QuestionCategory;
import com.mianmiantong.entity.wrongbook.WrongQuestion;
import com.mianmiantong.mapper.question.QuestionCategoryMapper;
import com.mianmiantong.mapper.question.QuestionMapper;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper categoryMapper;

    public WrongQuestionService(WrongQuestionMapper wrongQuestionMapper,
                                QuestionMapper questionMapper,
                                QuestionCategoryMapper categoryMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
    }

    public List<Question> getWrongQuestions() {
        Long userId = JwtAuthFilter.getCurrentUserId();

        List<WrongQuestion> wqs = wrongQuestionMapper.selectList(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime)
        );

        List<Long> questionIds = wqs.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList());
        if (questionIds.isEmpty()) return List.of();

        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        List<QuestionCategory> categories = categoryMapper.selectList(null);

        questions.forEach(q -> {
            categories.stream()
                .filter(c -> c.getId().equals(q.getCategoryId()))
                .findFirst()
                .ifPresent(c -> q.setCategoryName(c.getName()));
        });

        return questions;
    }

    public void remove(Long questionId) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        wrongQuestionMapper.delete(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId)
        );
    }
}
