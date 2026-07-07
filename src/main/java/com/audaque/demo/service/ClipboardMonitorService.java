package com.audaque.demo.service;

import com.audaque.demo.dto.OcrResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * 剪贴板监听服务 — 应用启动后后台轮询系统剪贴板
 * <p>
 * 检测到新截图 → 调用 OCR 识别 → 文本写回剪贴板（用户直接 Ctrl+V 粘贴）
 */
@Slf4j
@Component
public class ClipboardMonitorService implements ApplicationRunner {

    private final OcrService ocrService;

    public ClipboardMonitorService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread monitor = new Thread(this::monitor, "clipboard-monitor");
        monitor.setDaemon(true);
        monitor.start();
        log.info("剪贴板监听已启动，每隔 1 秒检测新截图...");
    }

    private void monitor() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Image lastImage = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                    Image current = (Image) clipboard.getData(DataFlavor.imageFlavor);
                    if (current != null && current != lastImage) {
                        lastImage = current;
                        processImage(current);
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("剪贴板监听异常", e);
            }
        }
    }

    private void processImage(Image image) throws IOException {
        // AWT Image → BufferedImage → PNG byte[]
        BufferedImage buffered = toBufferedImage(image);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        log.info("检测到剪贴板新图片，大小: {} bytes，开始 OCR 识别...", imageBytes.length);

        // 调用 OCR 服务
        OcrResponse response = ocrService.recognize(toMultipartFile(imageBytes));

        if (response.isSuccess()) {
            writeToClipboard(response.getText());
            log.info("OCR 识别完成，结果已写入剪贴板: {}...",
                    response.getText().length() > 50
                            ? response.getText().substring(0, 50) + "..."
                            : response.getText());
        } else {
            log.error("OCR 识别失败: {}", response.getText());
        }
    }

    /** AWT Image → BufferedImage */
    private BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage bi) {
            return bi;
        }
        BufferedImage bi = new BufferedImage(
                image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bi;
    }

    /** 将识别文本写回系统剪贴板 */
    private void writeToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    /** byte[] → MultipartFile（供 OcrService 调用） */
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
