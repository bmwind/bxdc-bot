package com.lobsterai.skillgateway.dto;

import java.util.Map;

/**
 * 文件提取响应 —— 统一结构，便于 LLM 解析。
 *
 * <pre>
 * {
 *   "kind":   "range",                     // echo operation.kind
 *   "text":   "B2:D10 范围内的 9x3 表格",   // 给人/LLM 看的自然语言摘要
 *   "data":   { "cells":[[...]], ... }     // 结构化数据，因 kind 而异
 * }
 * </pre>
 *
 * <p>错误响应也用同一层（{@code error} + {@code message} 字段），
 * 不抛 500 堆栈，便于 LLM 自我修正后重试。</p>
 */
public class ExtractResponse {

    private String kind;
    private String text;
    private Map<String, Object> data;

    public ExtractResponse() {}

    public ExtractResponse(String kind, String text, Map<String, Object> data) {
        this.kind = kind;
        this.text = text;
        this.data = data;
    }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
