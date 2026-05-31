package com.mianmiantong.dto.paper;

/**
 * 论文导出接口的结构化错误响应。
 */
public record ExportErrorResponse(String error, String detail, String hint) {

    public static ExportErrorResponse of(String error, String detail) {
        return new ExportErrorResponse(error, detail, null);
    }

    public static ExportErrorResponse of(String error, String detail, String hint) {
        return new ExportErrorResponse(error, detail, hint);
    }
}
