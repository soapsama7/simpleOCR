package com.audaque.demo.service.impl;

import com.audaque.demo.config.OcrProperties;
import com.audaque.demo.dto.OcrResponse;
import com.audaque.demo.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;

/**
 * OCR 服务实现 + 剪贴板监听
 * <p>
 * 所有异常向上传播，由 {@link com.audaque.demo.exception.GlobalExceptionHandler} 统一处理。
 * 本类不自行 catch 任何业务异常。
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService, ApplicationRunner {

    private final ChatClient chatClient;
    private final ChatClient.Builder chatClientBuilder;
    private final OcrProperties ocrProperties;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public OcrServiceImpl(ChatClient.Builder chatClientBuilder, OcrProperties ocrProperties) {
        this.chatClient = chatClientBuilder.build();
        this.chatClientBuilder = chatClientBuilder;
        this.ocrProperties = ocrProperties;
    }

    // ────────── OCR 识别 ──────────

    @Override
    public OcrResponse recognize(MultipartFile image) {
        long start = System.currentTimeMillis();

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

        Media media = new Media(
                MimeTypeUtils.parseMimeType(contentType),
                new ByteArrayResource(imageBytes));
        log.info("开始 OCR 识别，文件: {}, 大小: {} bytes, 类型: {}",
                image.getOriginalFilename(), imageBytes.length, contentType);

        String result = chatClient.prompt()
                .system(ocrProperties.getSystemPrompt())
                .user(u -> u.text(ocrProperties.getUserPrompt()).media(media))
                .call()
                .content();

        long elapsed = System.currentTimeMillis() - start;
        log.info("OCR 识别完成，耗时: {}ms, 结果长度: {}", elapsed, result != null ? result.length() : 0);

        return OcrResponse.success(result, model, elapsed);
    }

    // ────────── 健康检查 ──────────

    @Override
    public Map<String, Object> health() {
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
    }

    // ────────── 剪贴板监听 ──────────

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
                    Image current = getImageFromClipboard(clipboard);
                    if (current != null && current != lastImage) {
                        lastImage = current;
                        processClipboardImage(current);
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // 其他 RuntimeException（AWT、LLM 等）向上抛，终止线程并打印堆栈
        }
    }

    private void processClipboardImage(Image image) {
        byte[] imageBytes = imageToPngBytes(image);
        log.info("检测到剪贴板新图片，大小: {} bytes，开始 OCR...", imageBytes.length);

        OcrResponse response = ocrService().recognize(toMultipartFile(imageBytes));

        if (response.isSuccess()) {
            writeTextToClipboard(response.getText());
            log.info("OCR 完成，结果已写入剪贴板: {}",
                    response.getText().length() > 50
                            ? response.getText().substring(0, 50) + "..."
                            : response.getText());
        } else {
            log.error("OCR 识别失败: {}", response.getText());
        }
    }

    // ────────── 剪贴板工具方法 ──────────

    /** 从剪贴板读取图片，将受检异常转为 RuntimeException */
    private Image getImageFromClipboard(Clipboard clipboard) {
        try {
            return (Image) clipboard.getData(DataFlavor.imageFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new RuntimeException("读取剪贴板图片失败", e);
        }
    }

    private byte[] imageToPngBytes(Image image) {
        try {
            BufferedImage buffered = image instanceof BufferedImage bi
                    ? bi
                    : toBufferedImage(image);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("图片转换失败", e);
        }
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

    /** 自引用，用于剪贴板线程中调用 recognize() 走 AOP 代理 */
    private OcrService ocrService() {
        return this;
    }
}
