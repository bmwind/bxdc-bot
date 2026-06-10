package com.lobsterai.skillgateway.dto;

import java.util.Map;

/**
 * 文件提取请求 —— 与前端 / agent-core 约定的 operation 形态。
 *
 * <p>采用松耦合结构：type + operation（嵌套 Map），不强制每种 type 一套 DTO。
 * 这样后续加 kind / 加 type 时不用改 DTO 类，只在 Extractor 接口里加判断即可。
 *
 * <p>校验分两层：
 * <ol>
 *   <li>框架级：@NotBlank 保证 type 必填（待加）</li>
 *   <li>业务级：FileExtractService.extract() 内 switch type 后强校验 operation.kind 必填</li>
 * </ol>
 */
public class ExtractRequest {

    /** 文件类型：excel / word / text / pdf / image */
    private String type;

    /**
     * operation 必须包含 kind 字段，其他字段因 type + kind 而异。
     * 例：
     *   {"kind":"range","sheet":"Sheet1","range":"B2:D10"}
     *   {"kind":"section","heading":"第三章"}
     *   {"kind":"keyword","keyword":"合同金额","contextChars":50}
     */
    private Map<String, Object> operation;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getOperation() { return operation; }
    public void setOperation(Map<String, Object> operation) { this.operation = operation; }
}
