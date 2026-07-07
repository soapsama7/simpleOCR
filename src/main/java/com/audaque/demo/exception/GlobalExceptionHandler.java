package com.audaque.demo.exception;

import com.audaque.demo.dto.OcrResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 图片参数缺失 */
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public OcrResponse handleMissingParam(Exception e) {
        log.warn("请求参数缺失: {}", e.getMessage());
        return new OcrResponse(false, "请上传图片文件（参数名: image）", "N/A", 0);
    }

    /** 文件上传异常 */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public OcrResponse handleMultipartException(MultipartException e) {
        log.warn("文件上传异常: {}", e.getMessage());
        return new OcrResponse(false, "文件上传失败: " + e.getMessage(), "N/A", 0);
    }

    /** LLM 调用失败 / 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public OcrResponse handleGeneral(Exception e) {
        log.error("服务内部异常", e);
        return new OcrResponse(false, "服务内部错误: " + e.getMessage(), "N/A", 0);
    }
}
