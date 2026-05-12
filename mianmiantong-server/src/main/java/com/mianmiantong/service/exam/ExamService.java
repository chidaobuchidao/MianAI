package com.mianmiantong.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.exam.ExamSubmitRequest;
import com.mianmiantong.entity.exam.*;
import com.mianmiantong.entity.question.Question;
import com.mianmiantong.entity.question.QuestionCategory;
import com.mianmiantong.entity.wrongbook.WrongQuestion;
import com.mianmiantong.mapper.exam.*;
import com.mianmiantong.mapper.question.*;
import com.mianmiantong.mapper.wrongbook.WrongQuestionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionCategoryMapper categoryMapper;

    public ExamService(ExamMapper examMapper, ExamQuestionMapper examQuestionMapper,
                       QuestionMapper questionMapper, AnswerRecordMapper answerRecordMapper,
                       WrongQuestionMapper wrongQuestionMapper, QuestionCategoryMapper categoryMapper) {
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.questionMapper = questionMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.categoryMapper = categoryMapper;
    }

    /** 获取可用试卷列表 */
    public List<Exam> getExams() {
        return examMapper.selectList(
            new LambdaQueryWrapper<Exam>().eq(Exam::getStatus, 1)
        );
    }

    /** 开始考试，返回题目列表（不含答案） */
    public Map<String, Object> startExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new IllegalArgumentException("试卷不存在");

        List<ExamQuestion> eqs = examQuestionMapper.selectList(
            new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSortOrder)
        );

        List<Long> questionIds = eqs.stream().map(ExamQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(questionIds);

        List<QuestionCategory> categories = categoryMapper.selectList(null);
        questions.forEach(q -> {
            categories.stream()
                .filter(c -> c.getId().equals(q.getCategoryId()))
                .findFirst()
                .ifPresent(c -> q.setCategoryName(c.getName()));
        });

        Map<Long, Integer> orderMap = eqs.stream()
            .collect(Collectors.toMap(ExamQuestion::getQuestionId, ExamQuestion::getSortOrder));
        questions.sort(Comparator.comparingInt(q -> orderMap.getOrDefault(q.getId(), 0)));

        Map<String, Object> result = new HashMap<>();
        result.put("exam", exam);
        result.put("questions", questions);
        return result;
    }

    /** 提交考试 */
    @Transactional
    public Map<String, Object> submit(Long examId, ExamSubmitRequest request) {
        Long userId = JwtAuthFilter.getCurrentUserId();

        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new IllegalArgumentException("试卷不存在");

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
            request.getAnswers().stream().map(ExamSubmitRequest.ExamAnswer::getQuestionId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Question::getId, q -> q));

        int totalScore = 0;
        int correctCount = 0;

        for (ExamSubmitRequest.ExamAnswer answer : request.getAnswers()) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) continue;

            boolean correct = judge(question, answer.getUserAnswer());

            AnswerRecord record = new AnswerRecord();
            record.setUserId(userId);
            record.setExamId(examId);
            record.setQuestionId(answer.getQuestionId());
            record.setUserAnswer(answer.getUserAnswer());
            record.setIsCorrect(correct ? 1 : 0);
            record.setScore(correct ? 10 : 0);
            answerRecordMapper.insert(record);

            if (correct) {
                correctCount++;
                totalScore += 10;
            } else {
                addToWrongBook(userId, answer.getQuestionId());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("correctCount", correctCount);
        result.put("totalCount", request.getAnswers().size());
        result.put("examTitle", exam.getTitle());
        return result;
    }

    private boolean judge(Question question, String userAnswer) {
        if (userAnswer == null) return false;
        return switch (question.getType()) {
            case 1, 2 -> question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            case 3 -> question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            case 4 -> question.getAnswer().trim().toLowerCase().contains(userAnswer.trim().toLowerCase());
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
