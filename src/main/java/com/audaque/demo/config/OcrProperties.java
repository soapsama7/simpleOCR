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
    private String systemPrompt = "你是一个专业的OCR文字识别助手。你的任务是准确识别图片中的所有文字，只输出识别结果，不要添加任何解释、说明或额外内容。";

    /** 用户提示词 — 具体的识别指令（配合图片一起发送） */
    private String userPrompt = "请识别图片中的所有文字。";

    /** 最大上传文件大小，默认 10MB */
    private String maxFileSize = "10MB";
}
