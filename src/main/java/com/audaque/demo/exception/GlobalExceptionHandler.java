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

import java.io.UncheckedIOException;

/**
 * 全局异常处理（HTTP 请求链路）
 * <p>
 * 注意：{@code @RestControllerAdvice} 仅拦截 Controller 方法抛出的异常。
 * 后台线程（如 ClipboardMonitorService）的异常无法被此处理器捕获，
 * 需在线程内部使用 {@link OcrResponse#error(String)} 统一构建错误响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 图片参数缺失（未上传或参数名错误） */
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public OcrResponse handleMissingParam(Exception e) {
        log.warn("请求参数缺失: {}", e.getMessage());
        return OcrResponse.error("请上传图片文件（参数名: image）");
    }

    /** 文件上传异常（格式不支持等） */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public OcrResponse handleMultipartException(MultipartException e) {
        log.warn("文件上传异常: {}", e.getMessage());
        return OcrResponse.error("文件上传失败: " + e.getMessage());
    }

    /** 图片读取失败（IO 异常） */
    @ExceptionHandler(UncheckedIOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public OcrResponse handleIOException(UncheckedIOException e) {
        log.error("图片读取失败", e);
        String detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        return OcrResponse.error("图片读取失败: " + detail);
    }

    /** LLM 调用失败 / 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public OcrResponse handleGeneral(Exception e) {
        log.error("服务内部异常", e);
        return OcrResponse.error("服务内部错误: " + e.getMessage());
    }
}
