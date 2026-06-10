package com.lobsterai.skillgateway.extract.image;

import com.lobsterai.skillgateway.dto.ExtractResponse;
import com.lobsterai.skillgateway.exception.ExtractException;
import com.lobsterai.skillgateway.extract.FileExtractor;
import com.lobsterai.skillgateway.util.DdsUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 图片提取器 —— 骨架。直接调 DdsUtil.getOcrText()。
 *
 * <p>DdsUtil 已有占位实现（任务 9 完成），返回固定文字。
 * 真实 DdsUtil 上线后，调用方代码 0 改动。
 */
@Component
public class ImageExtractor implements FileExtractor {

    @Override
    public String supportedType() {
        return "image";
    }

    @Override
    public Set<String> supportedKinds() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "ocr", "keyword")));
    }

    @Override
    public ExtractResponse extract(MultipartFile file, Map<String, Object> operation) {
        String kind = (String) operation.get("kind");
        // JDK 1.8: 经典 switch
        switch (kind) {
            case "ocr":     return doOcr(file, operation);
            case "keyword": return doKeyword(file, operation);
            default:
                throw new ExtractException("unsupported_kind",
                        "kind=" + kind + " not in " + supportedKinds());
        }
    }

    private ExtractResponse doOcr(MultipartFile file, Map<String, Object> op) {
        try (InputStream in = file.getInputStream()) {
            String text = DdsUtil.getOcrText(in);
            // JDK 1.8: 显式 new HashMap 替代 Map.of
            Map<String, Object> data = new HashMap<>();
            data.put("text", text);
            data.put("confidence", null);  // 真实 DdsUtil 上线后填
            return new ExtractResponse("ocr", text, data);
        } catch (Exception e) {
            throw new ExtractException("parse_failed",
                    "OCR failed: " + e.getMessage(), e);
        }
    }

    private ExtractResponse doKeyword(MultipartFile file, Map<String, Object> op) {
        // 1) 调 doOcr 拿到 text
        // 2) 遍历 op.get("keywords") (List<String>), 收集命中行
        ExtractResponse ocrResult = doOcr(file, op);
        String text = ocrResult.getText();
        // TODO: keyword filter
        throw new ExtractException("not_implemented", "ImageExtractor.keyword is a stub");
    }
}
