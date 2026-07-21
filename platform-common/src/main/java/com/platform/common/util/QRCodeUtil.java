package com.platform.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成工具
 * 用于设备二维码、滤芯二维码生成
 */
@Slf4j
public class QRCodeUtil {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    private static final String FORMAT = "PNG";

    private QRCodeUtil() {
    }

    /**
     * 生成二维码图片字节数组
     *
     * @param content 二维码内容
     * @return PNG 图片字节数组
     */
    public static byte[] generate(String content) {
        return generate(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 生成二维码图片字节数组
     *
     * @param content 二维码内容
     * @param width   宽度（像素）
     * @param height  高度（像素）
     * @return PNG 图片字节数组
     */
    public static byte[] generate(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, FORMAT, out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("[QRCode] 生成二维码失败: content={}", content, e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 生成二维码并写入输出流
     */
    public static void writeToStream(String content, OutputStream out) {
        writeToStream(content, DEFAULT_WIDTH, DEFAULT_HEIGHT, out);
    }

    public static void writeToStream(String content, int width, int height, OutputStream out) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            MatrixToImageWriter.writeToStream(bitMatrix, FORMAT, out);
        } catch (Exception e) {
            log.error("[QRCode] 写入输出流失败: content={}", content, e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 生成 Base64 编码的二维码图片（用于直接嵌入 HTML/前端展示）
     */
    public static String generateBase64(String content) {
        byte[] bytes = generate(content);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
