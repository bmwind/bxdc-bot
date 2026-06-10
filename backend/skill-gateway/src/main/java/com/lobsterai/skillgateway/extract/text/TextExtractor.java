package com.lobsterai.skillgateway.extract.text;

import com.lobsterai.skillgateway.dto.ExtractResponse;
import com.lobsterai.skillgateway.exception.ExtractException;
import com.lobsterai.skillgateway.extract.FileExtractor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文本类提取器 —— 骨架。覆盖 .txt / .md / .csv / .json / .xml / .yaml / .log。
 *
 * <p>真实实现用 JDK NIO + 正则即可。
 * .md 特有 section / codeBlocks 在 MarkdownBranchHelper 里分派（见后续 tasks）。
 */
@Component
public class TextExtractor implements FileExtractor {

    @Override
    public String supportedType() {
        return "text";
    }

    @Override
    public Set<String> supportedKinds() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "lineRange", "keywordLines", "regex", "section", "codeBlocks")));
    }

    @Override
    public ExtractResponse extract(MultipartFile file, Map<String, Object> operation) {
        String kind = (String) operation.get("kind");
        // JDK 1.8: 经典 switch
        switch (kind) {
            case "lineRange":    return doLineRange(file, operation);
            case "keywordLines": return doKeywordLines(file, operation);
            case "regex":        return doRegex(file, operation);
            case "section":      return doSection(file, operation);
            case "codeBlocks":   return doCodeBlocks(file, operation);
            default:
                throw new ExtractException("unsupported_kind",
                        "kind=" + kind + " not in " + supportedKinds());
        }
    }

    private ExtractResponse doLineRange(MultipartFile file, Map<String, Object> op) {
        // TODO: readAllLines, from/to slice, truncated flag
        throw new ExtractException("not_implemented", "TextExtractor.lineRange is a stub");
    }

    private ExtractResponse doKeywordLines(MultipartFile file, Map<String, Object> op) {
        // TODO: 遍历每行, 命中 keywords 任一则收集
        throw new ExtractException("not_implemented", "TextExtractor.keywordLines is a stub");
    }

    private ExtractResponse doRegex(MultipartFile file, Map<String, Object> op) {
        // TODO: Pattern.compile + matcher; 30s 超时由 Service 层 Future.get 控
        throw new ExtractException("not_implemented", "TextExtractor.regex is a stub");
    }

    private ExtractResponse doSection(MultipartFile file, Map<String, Object> op) {
        // TODO: 按 heading 切分, 收集到下一个同级/更高级 heading
        // .md 格式: ^#{1,6}\s+(.+)$
        throw new ExtractException("not_implemented", "TextExtractor.section is a stub");
    }

    private ExtractResponse doCodeBlocks(MultipartFile file, Map<String, Object> op) {
        // TODO: 解析 ```lang\n...\n``` 围栏
        throw new ExtractException("not_implemented", "TextExtractor.codeBlocks is a stub");
    }
}
