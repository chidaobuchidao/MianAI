package com.mianmiantong.service.answer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.answer.AnswerSubmitRequest;
import com.mianmiantong.entity.exam.*;
import com.mianmiantong.entity.question.Question;
import com.mianmiantong.entity.wrongbook.WrongQuestion;
import com.mianmiantong.mapper.exam.*;
import com.mianmiantong.mapper.question.QuestionMapper;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AnswerService {

    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    public AnswerService(AnswerRecordMapper answerRecordMapper,
                         QuestionMapper questionMapper,
                         WrongQuestionMapper wrongQuestionMapper) {
        this.answerRecordMapper = answerRecordMapper;
        this.questionMapper = questionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    @Transactional
    public Map<String, Object> submit(AnswerSubmitRequest request) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        Question question = questionMapper.selectById(request.getQuestionId());

        if (question == null) {
            throw new IllegalArgumentException("题目不存在");
        }

        boolean correct = judge(question, request.getUserAnswer());

        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(request.getQuestionId());
        record.setUserAnswer(request.getUserAnswer());
        record.setIsCorrect(correct ? 1 : 0);
        record.setScore(correct ? 10 : 0);
        record.setExamId(request.getExamId());
        answerRecordMapper.insert(record);

        if (!correct) {
            addToWrongBook(userId, request.getQuestionId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", correct);
        result.put("correctAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        return result;
    }

    private boolean judge(Question question, String userAnswer) {
        if (userAnswer == null) return false;

        return switch (question.getType()) {
            case 1, 2 ->
                question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            case 3 ->
                question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            case 4 ->
                question.getAnswer().trim().toLowerCase().contains(userAnswer.trim().toLowerCase());
            default -> false;
        };
    }

    private void addToWrongBook(Long userId, Long questionId) {
        WrongQuestion existing = wrongQuestionMapper.selectOne(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId)
        );

        if (existing != null) {
            wrongQuestionMapper.update(null,
                new LambdaUpdateWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getUserId, userId)
                    .eq(WrongQuestion::getQuestionId, questionId)
                    .set(WrongQuestion::getWrongCount, existing.getWrongCount() + 1)
                    .set(WrongQuestion::getLastWrongTime, LocalDateTime.now())
            );
        } else {
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setWrongCount(1);
            wq.setLastWrongTime(LocalDateTime.now());
            wrongQuestionMapper.insert(wq);
        }
    }
}
