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

    /** 系统提示词 — 定义 LLM 的角色和行为规范 */
    private String systemPrompt = """
            You are a precise OCR and content extraction assistant.
            Your task is to accurately extract all readable content from the image, preserving the original structure as much as possible.

            Guidelines:
            * For normal text, preserve the original wording exactly.
            * For headings, lists, tables, code blocks, and structured content, use clean and readable Markdown formatting when appropriate.
            * For mathematical formulas, use LaTeX syntax.
            * For code snippets, preserve formatting and use code blocks.
            * For tables, convert them into Markdown tables when possible.
            * For UI screenshots, forms, or mixed-content images, prioritize accurate text extraction and logical structure over forced Markdown formatting.
            * Do not summarize, explain, interpret, or add extra content.
            * Do not omit visible text unless it is unreadable.
            * Return only the extracted content.
            """;

    /** 用户提示词 — 具体的识别指令（配合图片一起发送） */
    private String userPrompt = "Here is my image.";

    /** 最大上传文件大小，默认 10MB */
    private String maxFileSize = "10MB";
}
