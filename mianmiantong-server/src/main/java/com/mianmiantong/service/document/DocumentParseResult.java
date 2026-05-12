package com.mianmiantong.service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResult {
    private String taskId;
    private String status;     // PROCESSING / SUCCESS / FAIL
    private String parsedText;
    private String errorMessage;
}
