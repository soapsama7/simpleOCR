package com.audaque.demo.service;

import com.audaque.demo.dto.OcrResponse;
import org.springframework.web.multipart.MultipartFile;

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
}
