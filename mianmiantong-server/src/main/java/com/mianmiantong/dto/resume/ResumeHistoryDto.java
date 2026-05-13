package com.mianmiantong.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistoryDto {
    private Long id;
    private String fileName;
    private Integer parseStatus;
    private Integer overallScore;
    private Integer deepStatus;
    private String createTime;
}
