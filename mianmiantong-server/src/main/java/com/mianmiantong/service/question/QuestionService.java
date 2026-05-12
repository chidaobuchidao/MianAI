package com.mianmiantong.service.question;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mianmiantong.dto.question.PageQuery;
import com.mianmiantong.entity.question.Question;
import com.mianmiantong.entity.question.QuestionCategory;
import com.mianmiantong.mapper.question.QuestionCategoryMapper;
import com.mianmiantong.mapper.question.QuestionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper categoryMapper;

    public QuestionService(QuestionMapper questionMapper, QuestionCategoryMapper categoryMapper) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
    }

    /** 分页查询题库 */
    public Page<Question> page(Long categoryId, Integer difficulty, Integer type, PageQuery pageQuery) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Question::getCategoryId, categoryId);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (type != null) {
            wrapper.eq(Question::getType, type);
        }
        wrapper.orderByDesc(Question::getCreateTime);

        Page<Question> result = questionMapper.selectPage(
            new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper
        );

        List<QuestionCategory> categories = categoryMapper.selectList(null);
        result.getRecords().forEach(q -> {
            categories.stream()
                .filter(c -> c.getId().equals(q.getCategoryId()))
                .findFirst()
                .ifPresent(c -> q.setCategoryName(c.getName()));
        });

        return result;
    }

    /** 随机抽题 */
    public List<Question> random(Long categoryId, Integer difficulty, int size) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Question::getCategoryId, categoryId);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        wrapper.last("ORDER BY RAND() LIMIT " + size);

        List<Question> questions = questionMapper.selectList(wrapper);

        List<QuestionCategory> categories = categoryMapper.selectList(null);
        questions.forEach(q -> {
            categories.stream()
                .filter(c -> c.getId().equals(q.getCategoryId()))
                .findFirst()
                .ifPresent(c -> q.setCategoryName(c.getName()));
        });

        return questions;
    }

    /** 获取题目详情 */
    public Question getById(Long id) {
        Question q = questionMapper.selectById(id);
        if (q != null) {
            QuestionCategory cat = categoryMapper.selectById(q.getCategoryId());
            if (cat != null) q.setCategoryName(cat.getName());
        }
        return q;
    }

    /** 获取所有分类 */
    public List<QuestionCategory> getCategories() {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<QuestionCategory>().orderByAsc(QuestionCategory::getSortOrder)
        );
    }
}
