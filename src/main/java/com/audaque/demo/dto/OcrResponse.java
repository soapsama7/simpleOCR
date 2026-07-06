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

    /** 识别出的文本内容 */
    private String text;

    /** 使用的模型名称 */
    private String model;

    /** 耗时（毫秒） */
    private long elapsedMs;
}
