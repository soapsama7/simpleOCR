package com.audaque.demo.service;

import com.audaque.demo.dto.OcrResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OCR 服务接口
 */
public interface OcrService {

    /**
     * 识别图片中的文字
     *
     * @param image 图片文件（支持 PNG、JPG 等常见格式）
     * @return OCR 识别结果
     */
    OcrResponse recognize(MultipartFile image);

    /**
     * 健康检查 — 真实检测 LLM 连通性
     *
     * @return 应用状态 + LLM 是否可达
     */
    Map<String, Object> health();
}
