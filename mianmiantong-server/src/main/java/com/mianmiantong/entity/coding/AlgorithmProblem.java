package com.mianmiantong.entity.coding;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("algorithm_problem")
public class AlgorithmProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String difficulty;

    /** JSON: {"java":"...","python":"...","javascript":"..."} */
    @TableField("starter_code")
    private String starterCode;

    /** JSON: [{"input":"stdin","expected":"stdout"}] */
    @TableField("test_cases")
    private String testCases;

    /** JSON: {"java":"...","python":"...","javascript":"..."} */
    @TableField("solution_code")
    private String solutionCode;

    private String category;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
