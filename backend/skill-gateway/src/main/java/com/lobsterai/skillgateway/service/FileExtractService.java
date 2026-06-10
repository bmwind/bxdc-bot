package com.lobsterai.skillgateway.service;

import com.lobsterai.skillgateway.dto.ExtractRequest;
import com.lobsterai.skillgateway.dto.ExtractResponse;
import com.lobsterai.skillgateway.exception.ExtractException;
import com.lobsterai.skillgateway.extract.FileExtractRegistry;
import com.lobsterai.skillgateway.extract.FileExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 文件提取服务 —— 统一入口：
 *   1. 前置校验（empty_file, file_too_large, mime）
 *   2. 按 type 路由到对应 extractor
 *   3. 校验 kind 在 extractor.supportedKinds() 范围内
 *   4. 30s 超时（ExecutorService.submit + Future.get）
 *   5. type 与文件后缀匹配检查
 */
@Service
public class FileExtractService {

    private final FileExtractRegistry registry;
    private final ExecutorService executor;

    @Value("${app.file.extract.timeout-seconds:30}")
    private long timeoutSeconds;

    @Value("${app.file.extract.max-size-mb:100}")
    private long maxSizeMb;

    public FileExtractService(FileExtractRegistry registry) {
        this.registry = registry;
        this.executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, "file-extract-worker");
                    t.setDaemon(true);
                    return t;
                });
    }

    public ExtractResponse extract(MultipartFile file, ExtractRequest request) {
        // 1) 前置校验
        validateFile(file);
        if (request == null || request.getType() == null || request.getType().trim().isEmpty()) {
            throw new ExtractException("missing_field", "type is required");
        }
        if (request.getOperation() == null || request.getOperation().isEmpty()) {
            throw new ExtractException("missing_field", "operation is required");
        }

        // 2) 按 type 查找 extractor
        FileExtractor extractor = registry.get(request.getType());

        // 3) 校验 kind
        Object kindObj = request.getOperation().get("kind");
        if (kindObj == null || !(kindObj instanceof String) || ((String) kindObj).trim().isEmpty()) {
            throw new ExtractException("missing_field", "operation.kind is required");
        }
        String kind = (String) kindObj;
        Set<String> allowed = extractor.supportedKinds();
        if (!allowed.contains(kind)) {
            throw new ExtractException(
                    "unsupported_kind",
                    "type=" + request.getType() + " kind=" + kind +
                            " not in allowed=" + allowed);
        }

        // 4) type 与文件后缀匹配
        validateTypeExtension(request.getType(), file.getOriginalFilename());

        // 5) 提交到线程池 + 30s 超时
        Future<ExtractResponse> future = executor.submit(
                () -> extractor.extract(file, request.getOperation()));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ExtractException("timeout",
                    "extractor exceeded " + timeoutSeconds + "s");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ExtractException) {
                throw (ExtractException) e.getCause();
            }
            throw new ExtractException("parse_failed", e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtractException("interrupted", "extractor interrupted", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExtractException("empty_file", "uploaded file is empty");
        }
        long maxBytes = maxSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new ExtractException("file_too_large",
                    "file size " + file.getSize() + " bytes exceeds " + maxBytes);
        }
    }

    private void validateTypeExtension(String type, String filename) {
        if (filename == null) return;
        String lower = filename.toLowerCase();
        boolean ok;
        if ("excel".equals(type)) {
            ok = lower.endsWith(".xls") || lower.endsWith(".xlsx");
        } else if ("word".equals(type)) {
            ok = lower.endsWith(".docx");
        } else if ("text".equals(type)) {
            ok = lower.endsWith(".txt") || lower.endsWith(".md")
                    || lower.endsWith(".csv") || lower.endsWith(".json")
                    || lower.endsWith(".xml") || lower.endsWith(".yaml")
                    || lower.endsWith(".yml") || lower.endsWith(".log");
        } else if ("pdf".equals(type)) {
            ok = lower.endsWith(".pdf");
        } else if ("image".equals(type)) {
            ok = lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".bmp")
                    || lower.endsWith(".webp") || lower.endsWith(".gif");
        } else {
            ok = true;
        }
        if (!ok) {
            String ext = lower.contains(".")
                    ? lower.substring(lower.lastIndexOf('.') + 1) : "";
            throw new ExtractException("type_extension_mismatch",
                    "type=" + type + " actualExt=" + ext);
        }
    }
}
