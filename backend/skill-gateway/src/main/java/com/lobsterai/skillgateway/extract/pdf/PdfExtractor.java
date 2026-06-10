package com.lobsterai.skillgateway.extract.pdf;

import com.lobsterai.skillgateway.dto.ExtractResponse;
import com.lobsterai.skillgateway.exception.ExtractException;
import com.lobsterai.skillgateway.extract.FileExtractor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PDF 提取器 —— 骨架。
 *
 * <p>真实实现需要 Apache PDFBox（pom 需新增依赖，见 tasks 1.1）。
 * 关键 API：
 * <ul>
 *   <li>PDDocument.load(file.getInputStream())</li>
 *   <li>PDFTextStripper.getText(doc) 抽文本</li>
 *   <li>PDDocument.getDocumentCatalog() 拿 metadata / outline / acroForm</li>
 *   <li>StandardDecryptionMaterial 解密</li>
 * </ul>
 */
@Component
public class PdfExtractor implements FileExtractor {

    @Override
    public String supportedType() {
        return "pdf";
    }

    @Override
    public Set<String> supportedKinds() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "pageRange", "keyword", "fullText", "metadata")));
    }

    @Override
    public ExtractResponse extract(MultipartFile file, Map<String, Object> operation) {
        String kind = (String) operation.get("kind");
        // JDK 1.8: 经典 switch
        switch (kind) {
            case "pageRange": return doPageRange(file, operation);
            case "keyword":   return doKeyword(file, operation);
            case "fullText":  return doFullText(file, operation);
            case "metadata":  return doMetadata(file, operation);
            default:
                throw new ExtractException("unsupported_kind",
                        "kind=" + kind + " not in " + supportedKinds());
        }
    }

    private ExtractResponse doPageRange(MultipartFile file, Map<String, Object> op) {
        // TODO: PDDocument.load + startPage/endPage setText + getText
        throw new ExtractException("not_implemented", "PdfExtractor.pageRange is a stub");
    }

    private ExtractResponse doKeyword(MultipartFile file, Map<String, Object> op) {
        // TODO: 遍历每页文本, 找 keyword + 前后 contextChars
        throw new ExtractException("not_implemented", "PdfExtractor.keyword is a stub");
    }

    private ExtractResponse doFullText(MultipartFile file, Map<String, Object> op) {
        // TODO: PDFTextStripper, maxPages 截断
        throw new ExtractException("not_implemented", "PdfExtractor.fullText is a stub");
    }

    private ExtractResponse doMetadata(MultipartFile file, Map<String, Object> op) {
        // TODO: PDDocumentInformation -> {title, author, createdAt, pageCount}
        throw new ExtractException("not_implemented", "PdfExtractor.metadata is a stub");
    }
}
