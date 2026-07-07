package com.audaque.demo.service.impl;

import com.audaque.demo.config.OcrProperties;
import com.audaque.demo.dto.OcrResponse;
import com.audaque.demo.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * OCR 服务实现 — 基于 Spring AI 调用 OpenAI 兼容大模型进行图片文字识别
 * <p>
 * 异常全部向上传播，由 {@link com.audaque.demo.exception.GlobalExceptionHandler} 统一处理。
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final ChatClient chatClient;
    private final OcrProperties ocrProperties;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public OcrServiceImpl(ChatClient.Builder chatClientBuilder, OcrProperties ocrProperties) {
        this.chatClient = chatClientBuilder.build();
        this.ocrProperties = ocrProperties;
    }

    @Override
    public OcrResponse recognize(MultipartFile image) {
        long start = System.currentTimeMillis();

        // 读取图片字节和 MIME 类型
        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("读取上传图片失败", e);
        }

        String contentType = image.getContentType();
        if (contentType == null) {
            contentType = "image/png";
        }

        // 构造多模态请求：图片 + OCR 提示词
        Media media = new Media(
                MimeTypeUtils.parseMimeType(contentType),
                new ByteArrayResource(imageBytes));
        log.info("开始 OCR 识别，文件: {}, 大小: {} bytes, 类型: {}",
                image.getOriginalFilename(), imageBytes.length, contentType);

        String result = chatClient.prompt()
                .user(u -> u.text(ocrProperties.getDefaultPrompt()).media(media))
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - start;
        log.info("OCR 识别完成，耗时: {}ms, 结果长度: {}", elapsed, result != null ? result.length() : 0);

        return OcrResponse.success(result, model, elapsed);
    }
}
