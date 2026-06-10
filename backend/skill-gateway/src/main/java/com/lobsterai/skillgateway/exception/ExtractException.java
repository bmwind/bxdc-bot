package com.lobsterai.skillgateway.exception;

/**
 * 文件提取业务异常 —— 携带 errorCode，便于 controller 转成结构化 4xx 响应。
 *
 * <p>典型 errorCode 列表（与 spec 错误契约保持一致）：
 * <ul>
 *   <li>missing_field         必填字段缺失</li>
 *   <li>unsupported_kind      type 不支持该 kind</li>
 *   <li>unsupported_type      type 字段值不合法</li>
 *   <li>type_extension_mismatch  type 与文件后缀不符</li>
 *   <li>empty_file            空文件</li>
 *   <li>file_too_large        超过 max-size-mb 限制</li>
 *   <li>parse_failed          解析失败（如 xlsx 损坏）</li>
 *   <li>invalid_range         A1 表示法非法</li>
 *   <li>sheet_not_found       sheet 名不存在</li>
 *   <li>heading_not_found     Word / md 标题不存在</li>
 *   <li>encrypted_pdf         PDF 加密</li>
 *   <li>no_text_layer         PDF 扫描版</li>
 *   <li>timeout               30s 超时</li>
 * </ul>
 */
public class ExtractException extends RuntimeException {

    private final String errorCode;

    public ExtractException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ExtractException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
