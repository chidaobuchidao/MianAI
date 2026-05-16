package com.mianmiantong.mapper.resume;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.dto.resume.ResumeHistoryDto;
import com.mianmiantong.entity.resume.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {

    @Select("SELECT r.id, r.file_name, r.position AS position, r.parse_status, " +
            "a.overall_score AS overallScore, a.deep_status AS deepStatus, r.create_time AS createTime " +
            "FROM resume r LEFT JOIN resume_analysis a ON a.resume_id = r.id " +
            "WHERE r.user_id = #{userId} ORDER BY r.create_time DESC LIMIT 5")
    List<ResumeHistoryDto> selectHistory(@Param("userId") Long userId);
}
