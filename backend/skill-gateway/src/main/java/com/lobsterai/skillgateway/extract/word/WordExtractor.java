package com.lobsterai.skillgateway.extract.word;

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
 * Word (.docx) 提取器 —— 骨架。
 *
 * <p>真实实现需要 Apache POI（XWPFDocument）。
 *
 * <p>注意：section 提取要按文档流顺序混合段落和表格，不能只取段落。
 */
@Component
public class WordExtractor implements FileExtractor {

    @Override
    public String supportedType() {
        return "word";
    }

    @Override
    public Set<String> supportedKinds() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "paragraphs", "section", "table", "keyword")));
    }

    @Override
    public ExtractResponse extract(MultipartFile file, Map<String, Object> operation) {
        String kind = (String) operation.get("kind");
        // JDK 1.8: 经典 switch
        switch (kind) {
            case "paragraphs": return doParagraphs(file, operation);
            case "section":    return doSection(file, operation);
            case "table":      return doTable(file, operation);
            case "keyword":    return doKeyword(file, operation);
            default:
                throw new ExtractException("unsupported_kind",
                        "kind=" + kind + " not in " + supportedKinds());
        }
    }

    private ExtractResponse doParagraphs(MultipartFile file, Map<String, Object> op) {
        // TODO: XWPFDocument.getParagraphs() + styleFilter
        throw new ExtractException("not_implemented", "WordExtractor.paragraphs is a stub");
    }

    private ExtractResponse doSection(MultipartFile file, Map<String, Object> op) {
        // TODO: 找 heading, 收集到下一个同级/更高级 heading
        // 段落 + 表格按文档流顺序输出
        throw new ExtractException("not_implemented", "WordExtractor.section is a stub");
    }

    private ExtractResponse doTable(MultipartFile file, Map<String, Object> op) {
        // TODO: document.getTables() + index 过滤
        throw new ExtractException("not_implemented", "WordExtractor.table is a stub");
    }

    private ExtractResponse doKeyword(MultipartFile file, Map<String, Object> op) {
        // TODO: 遍历段落，正则/contains 匹配
        throw new ExtractException("not_implemented", "WordExtractor.keyword is a stub");
    }
}
