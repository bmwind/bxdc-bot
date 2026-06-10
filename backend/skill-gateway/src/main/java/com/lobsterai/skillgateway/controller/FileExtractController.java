package com.lobsterai.skillgateway.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lobsterai.skillgateway.dto.ExtractRequest;
import com.lobsterai.skillgateway.dto.ExtractResponse;
import com.lobsterai.skillgateway.exception.ExtractException;
import com.lobsterai.skillgateway.service.FileExtractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * 文件提取统一端点 —— 任务编号：file-content-extraction 任务 7。
 *
 * <p>前端 / agent-core LLM tool 通过
 * {@code POST /api/file/extract}（multipart/form-data, field=file + operation）调用，
 * 返回 {@link ExtractResponse}。</p>
 *
 * <p>operation 字段是个 JSON 字符串，spec 形态：
 * <pre>
 * {"type":"excel","operation":{"kind":"range","sheet":"Sheet1","range":"B2:D10"}}
 * </pre>
 *
 * <p>错误统一走 {@code @ExceptionHandler}，返回结构化 JSON（不暴露堆栈）：
 * <pre>
 * {"error":"unsupported_kind","message":"...","type":"excel","allowed":["range",...]}
 * </pre>
 */
@RestController
@RequestMapping("/api/file")
public class FileExtractController {

    private static final Logger log = LoggerFactory.getLogger(FileExtractController.class);

    private final FileExtractService fileExtractService;
    private final ObjectMapper objectMapper;

    public FileExtractController(FileExtractService fileExtractService, ObjectMapper objectMapper) {
        this.fileExtractService = fileExtractService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtractResponse> extract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("operation") String operationJson) {

        ExtractRequest req;
        try {
            req = objectMapper.readValue(operationJson, ExtractRequest.class);
        } catch (Exception e) {
            throw new ExtractException("invalid_operation_json",
                    "operation must be valid JSON: " + e.getMessage());
        }

        log.info("[extract] type={}, file={}, size={} bytes",
                req.getType(), file.getOriginalFilename(), file.getSize());
        ExtractResponse resp = fileExtractService.extract(file, req);
        log.info("[extract] done, kind={}", resp.getKind());
        return ResponseEntity.ok(resp);
    }

    /**
     * 业务异常统一转 4xx JSON（与 spec 错误契约一致）。
     */
    @ExceptionHandler(ExtractException.class)
    public ResponseEntity<Map<String, Object>> handleExtractException(ExtractException e) {
        log.error("[extract] failed: code={}, msg={}", e.getErrorCode(), e.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getErrorCode());
        body.put("message", e.getMessage());
        return ResponseEntity.status(statusFor(e.getErrorCode())).body(body);
    }

    /**
     * errorCode → HTTP 状态码的简单映射。
     * 设计：业务校验用 4xx，只有真正的服务器错误才 500。
     */
    private int statusFor(String code) {
        // JDK 1.8: 经典 switch
        if ("empty_file".equals(code) || "missing_field".equals(code)
                || "unsupported_kind".equals(code) || "unsupported_type".equals(code)
                || "type_extension_mismatch".equals(code) || "invalid_range".equals(code)
                || "invalid_operation_json".equals(code) || "sheet_not_found".equals(code)
                || "heading_not_found".equals(code) || "unsupported_op".equals(code)
                || "encrypted_pdf".equals(code) || "no_text_layer".equals(code)) {
            return 400;
        }
        switch (code) {
            case "file_too_large": return 413;
            case "timeout":       return 504;
            case "parse_failed":  return 422;
            default:              return 500;
        }
    }

    /**
     * （可选）调试用：列出所有支持的 type + kind。
     * GET /api/file/extract/capabilities
     */
    @GetMapping("/extract/capabilities")
    public ResponseEntity<List<String>> capabilities() {
        // JDK 1.8: 用 Arrays.asList 替代 List.of
        return ResponseEntity.ok(Arrays.asList(
                "excel: range, column, row, aggregate, filter",
                "word: paragraphs, section, table, keyword",
                "text: lineRange, keywordLines, regex, section, codeBlocks",
                "pdf: pageRange, keyword, fullText, metadata",
                "image: ocr, keyword"));
    }
}
