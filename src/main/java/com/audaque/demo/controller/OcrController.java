package com.audaque.demo.controller;

import com.audaque.demo.dto.OcrResponse;
import com.audaque.demo.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;

/**
 * OCR 控制器 — 统一入口
 * <p>
 * 提供 HTTP API（POST /recognize、GET /health），并在启动时开启后台剪贴板监听。
 * 两种入口均调用同一个 {@link OcrService}，保证业务逻辑一致。
 * <p>
 * 异常处理说明：
 * <ul>
 *   <li>HTTP 请求异常 → {@code @RestControllerAdvice} 统一拦截 → {@link OcrResponse#error(String)}</li>
 *   <li>剪贴板监听线程异常 → 线程内部 catch 后通过 {@link OcrResponse#error(String)} 构建响应并记录日志
 *       （后台线程无 HTTP 上下文，无法被 {@code @RestControllerAdvice} 捕获，但错误格式保持一致）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/ocr")
public class OcrController implements ApplicationRunner {

    private final OcrService ocrService;
    private final org.springframework.ai.chat.client.ChatClient.Builder chatClientBuilder;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public OcrController(OcrService ocrService,
                         org.springframework.ai.chat.client.ChatClient.Builder chatClientBuilder) {
        this.ocrService = ocrService;
        this.chatClientBuilder = chatClientBuilder;
    }

    // ────────── HTTP API ──────────

    /**
     * OCR 图片识别（HTTP 入口）
     *
     * @param image 上传的图片文件
     * @return 识别结果
     */
    @PostMapping("/recognize")
    public OcrResponse recognize(@RequestParam("image") MultipartFile image) {
        return ocrService.recognize(image);
    }

    /**
     * 健康检查 — 真实检测 LLM 连通性
     *
     * @return 应用状态 + LLM 是否可达
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            String testResult = chatClientBuilder.build()
                    .prompt()
                    .user("ping")
                    .call()
                    .content();
            return Map.of(
                    "status", "UP",
                    "service", "simpleOCR",
                    "model", model,
                    "llm", testResult != null ? "reachable" : "no response"
            );
        } catch (Exception e) {
            log.warn("LLM 连通性检测失败: {}", e.getMessage());
            return Map.of(
                    "status", "DOWN",
                    "service", "simpleOCR",
                    "model", model,
                    "llm", "unreachable",
                    "error", e.getMessage()
            );
        }
    }

    // ────────── 剪贴板监听（后台线程） ──────────

    @Override
    public void run(ApplicationArguments args) {
        Thread monitor = new Thread(this::monitorClipboard, "clipboard-monitor");
        monitor.setDaemon(true);
        monitor.start();
        log.info("剪贴板监听已启动，每隔 1 秒检测新截图...");
    }

    private void monitorClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Image lastImage = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                    Image current = (Image) clipboard.getData(DataFlavor.imageFlavor);
                    if (current != null && current != lastImage) {
                        lastImage = current;
                        processClipboardImage(current);
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 后台线程异常：无法走 @RestControllerAdvice，使用统一 error() 工厂记录
                log.error("剪贴板监听异常: {}", OcrResponse.error(e.getMessage()).getText());
            }
        }
    }

    private void processClipboardImage(Image image) {
        try {
            byte[] imageBytes = imageToPngBytes(image);
            log.info("检测到剪贴板新图片，大小: {} bytes，开始 OCR...", imageBytes.length);

            OcrResponse response = ocrService.recognize(toMultipartFile(imageBytes));

            if (response.isSuccess()) {
                writeTextToClipboard(response.getText());
                log.info("OCR 完成，结果已写入剪贴板: {}",
                        response.getText().length() > 50
                                ? response.getText().substring(0, 50) + "..."
                                : response.getText());
            } else {
                log.error("OCR 识别失败: {}", response.getText());
            }
        } catch (Exception e) {
            log.error("处理剪贴板图片失败: {}", OcrResponse.error(e.getMessage()).getText());
        }
    }

    // ────────── 剪贴板工具方法 ──────────

    private byte[] imageToPngBytes(Image image) throws IOException {
        BufferedImage buffered = image instanceof BufferedImage bi
                ? bi
                : toBufferedImage(image);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", baos);
        return baos.toByteArray();
    }

    private BufferedImage toBufferedImage(Image image) {
        BufferedImage bi = new BufferedImage(
                image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bi;
    }

    private void writeTextToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private MultipartFile toMultipartFile(byte[] bytes) {
        return new MultipartFile() {
            @Override public String getName() { return "image"; }
            @Override public String getOriginalFilename() { return "screenshot.png"; }
            @Override public String getContentType() { return "image/png"; }
            @Override public boolean isEmpty() { return bytes.length == 0; }
            @Override public long getSize() { return bytes.length; }
            @Override public byte[] getBytes() { return bytes; }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
            @Override public void transferTo(File dest) throws IOException {
                java.nio.file.Files.write(dest.toPath(), bytes);
            }
        };
    }
}
