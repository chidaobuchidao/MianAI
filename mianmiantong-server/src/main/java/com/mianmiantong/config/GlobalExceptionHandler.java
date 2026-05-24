package com.mianmiantong.config;

import com.mianmiantong.common.Result;
import com.mianmiantong.service.user.QuotaService.QuotaExhaustedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QuotaExhaustedException.class)
    public Result<?> handleQuotaExhausted(QuotaExhaustedException ex) {
        return Result.fail(429, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);
        return Result.fail(400, fieldError.getDefaultMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.fail(400, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        return Result.fail(500, "服务器内部错误: " + ex.getMessage());
    }
}
