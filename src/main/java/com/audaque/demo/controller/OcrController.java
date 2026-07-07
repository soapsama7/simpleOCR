package com.audaque.demo.controller;

import com.audaque.demo.dto.OcrResponse;
import com.audaque.demo.service.OcrService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OCR REST API 控制器
 * <p>
 * 纯委托层，所有逻辑在 {@link OcrService} 中实现。
 * 异常由 {@link com.audaque.demo.exception.GlobalExceptionHandler} 统一处理。
 */
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/recognize")
    public OcrResponse recognize(@RequestParam("image") MultipartFile image) {
        return ocrService.recognize(image);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return ocrService.health();
    }
}
