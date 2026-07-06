package com.audaque.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OCR 自定义配置属性
 * 对应 application.yml 中 ocr.* 配置段
 */
@Data
@Component
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {

    /** 默认 OCR 提示词，引导 LLM 进行纯文字提取 */
    private String defaultPrompt = "请识别图片中的文字，只输出识别结果，不要添加任何额外说明或解释。";

    /** 最大上传文件大小，默认 10MB */
    private String maxFileSize = "10MB";
}
