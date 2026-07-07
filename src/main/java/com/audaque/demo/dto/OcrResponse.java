package com.audaque.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCR 识别结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {

    /** 识别是否成功 */
    private boolean success;

    /** 识别出的文本内容（成功时）或错误信息（失败时） */
    private String text;

    /** 使用的模型名称 */
    private String model;

    /** 耗时（毫秒） */
    private long elapsedMs;

    // ────────── 静态工厂方法 ──────────

    /** 构造成功响应 */
    public static OcrResponse success(String text, String model, long elapsedMs) {
        return new OcrResponse(true, text, model, elapsedMs);
    }

    /** 构造错误响应（model 填 N/A，elapsedMs 填 0） */
    public static OcrResponse error(String message) {
        return new OcrResponse(false, message, "N/A", 0);
    }
}
