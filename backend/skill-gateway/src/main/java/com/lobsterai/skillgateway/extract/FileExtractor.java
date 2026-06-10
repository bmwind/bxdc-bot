package com.lobsterai.skillgateway.extract;

import com.lobsterai.skillgateway.dto.ExtractResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件提取器统一接口 —— 每种文件类型一个实现。
 *
 * <p>设计原则：
 * <ul>
 *   <li>每个 extractor 只负责一种 type，便于单测和维护</li>
 *   <li>extract() 收到的是 multipart file + 已校验过 kind 存在的 operation Map</li>
 *   <li>实现类内部按 kind 做 switch，落到具体的解析逻辑</li>
 *   <li>业务校验失败抛 {@link com.lobsterai.skillgateway.exception.ExtractException}，
 *       controller 统一转 4xx JSON</li>
 * </ul>
 */
public interface FileExtractor {

    /**
     * @return 支持的 type，如 "excel" / "word" / "text" / "pdf" / "image"
     */
    String supportedType();

    /**
     * @return 该 type 下支持的 kind 列表（用于 unsupported_kind 错误时返回 allowed 字段）
     */
    java.util.Set<String> supportedKinds();

    /**
     * 实际提取。
     *
     * @param file      上传的文件（已通过 empty_file / file_too_large / mime 校验）
     * @param operation 必须包含 kind 字段；其他字段由实现类按 kind 解析
     * @return 结构化结果（kind + text + data）
     */
    ExtractResponse extract(MultipartFile file, Map<String, Object> operation);
}
