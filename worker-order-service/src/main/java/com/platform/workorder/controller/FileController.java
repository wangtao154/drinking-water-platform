package com.platform.workorder.controller;

import com.platform.common.result.R;
import com.platform.common.result.ResultCode;
import com.platform.common.auth.UserContext;
import com.platform.workorder.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class FileController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail(ResultCode.PARAM_INVALID, "文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return R.fail(ResultCode.PARAM_INVALID, "图片大小不能超过5MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "file.jpg";
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            return R.fail(ResultCode.PARAM_INVALID, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }
        contentType = contentType.toLowerCase(Locale.ROOT);
        String ext = IMAGE_EXTENSIONS.get(contentType);
        if (ext == null) {
            return R.fail(ResultCode.PARAM_INVALID, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }
        try {
            if (!isValidImageContent(file, contentType)) {
                return R.fail(ResultCode.PARAM_INVALID, "文件内容不是有效图片");
            }
        } catch (Exception e) {
            log.warn("[FileController] 图片内容校验失败: {}", originalName, e);
            return R.fail(ResultCode.PARAM_INVALID, "文件内容不是有效图片");
        }

        // 生成对象名: work-orders/2026-07-13/uuid.ext
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = "work-orders/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            // 生成外部访问URL
            String url = minioConfig.getExternalEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;

            log.info("[FileController] 文件上传成功: {} -> {}", originalName, url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("objectName", objectName);
            result.put("originalName", originalName);
            return R.ok(result);

        } catch (Exception e) {
            log.error("[FileController] 文件上传失败", e);
            return R.fail(ResultCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }

    private boolean isValidImageContent(MultipartFile file, String contentType) throws Exception {
        byte[] header = new byte[12];
        int len;
        try (InputStream inputStream = file.getInputStream()) {
            len = inputStream.read(header);
        }
        if (len < 4) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> len >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "image/gif" -> len >= 6
                    && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46
                    && header[3] == 0x38 && (header[4] == 0x37 || header[4] == 0x39) && header[5] == 0x61;
            case "image/webp" -> len >= 12
                    && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
            default -> false;
        };
    }
}
