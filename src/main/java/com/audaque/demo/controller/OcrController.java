package com.audaque.demo.controller;

import com.audaque.demo.dto.OcrResponse;
import com.audaque.demo.service.OcrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OCR REST API 控制器
 */
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * OCR 图片识别
     *
     * @param image 上传的图片文件
     * @return 识别结果
     */
    @PostMapping("/recognize")
    public OcrResponse recognize(@RequestParam("image") MultipartFile image) {
        return ocrService.recognize(image);
    }

    /**
     * 健康检查接口
     *
     * @return 应用运行状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "simpleOCR",
                "model", model
        );
    }
}
