package com.lobsterai.skillgateway.extract;

import com.lobsterai.skillgateway.exception.ExtractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extractor 注册表 —— 把 6 个 FileExtractor 按 supportedType 收集起来，service 路由用。
 *
 * <p>Spring 启动时自动注入所有 FileExtractor bean，
 * 此处按 type 索引供 FileExtractService 查找。
 */
@Component
public class FileExtractRegistry {

    private final Map<String, FileExtractor> byType;

    @Autowired
    public FileExtractRegistry(List<FileExtractor> extractors) {
        this.byType = new HashMap<>();
        for (FileExtractor e : extractors) {
            byType.put(e.supportedType(), e);
        }
    }

    public FileExtractor get(String type) {
        FileExtractor e = byType.get(type);
        if (e == null) {
            throw new ExtractException(
                    "unsupported_type",
                    "type must be one of " + byType.keySet() + ", got: " + type);
        }
        return e;
    }

    public List<String> supportedTypes() {
        return byType.keySet().stream().sorted().collect(Collectors.toList());
    }
}
