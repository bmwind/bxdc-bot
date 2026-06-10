package com.lobsterai.skillgateway.extract.excel;

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
 * Excel 提取器 —— 骨架。
 *
 * <p>真实实现需要用 Apache POI（pom 已加 poi-ooxml + poi-scratchpad）：
 * <ul>
 *   <li>range: XSSFWorkbook + CellRangeAddress</li>
 *   <li>column: 列遍历 + 空值计数</li>
 *   <li>row: 单行遍历</li>
 *   <li>aggregate: 数值单元格累加/平均/最大/最小</li>
 *   <li>filter: 按条件遍历行</li>
 * </ul>
 *
 * <p>A1 解析工具建议单建 {@code A1NotationParser} 工具类（spec 7.x 中提到）。
 */
@Component
public class ExcelExtractor implements FileExtractor {

    @Override
    public String supportedType() {
        return "excel";
    }

    @Override
    public Set<String> supportedKinds() {
        // JDK 1.8: 用 HashSet + Arrays.asList 替代 Set.of
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "range", "column", "row", "aggregate", "filter")));
    }

    @Override
    public ExtractResponse extract(MultipartFile file, Map<String, Object> operation) {
        String kind = (String) operation.get("kind");
        // JDK 1.8: 经典 switch
        switch (kind) {
            case "range":     return doRange(file, operation);
            case "column":    return doColumn(file, operation);
            case "row":       return doRow(file, operation);
            case "aggregate": return doAggregate(file, operation);
            case "filter":    return doFilter(file, operation);
            default:
                throw new ExtractException("unsupported_kind",
                        "kind=" + kind + " not in " + supportedKinds());
        }
    }

    // ====== 骨架方法：每个内部判 operation 必填字段后调 POI 解析 ======
    // 当前实现：抛 stub 错误，让 LLM 知道还没接

    private ExtractResponse doRange(MultipartFile file, Map<String, Object> op) {
        // TODO: POI 实现
        // 1) parse "range" 为 [startCol, startRow, endCol, endRow]
        // 2) open Workbook, getSheet(op.get("sheet"))
        // 3) 遍历 row[i] for i in [startRow..endRow], output 2D cells
        // 4) 处理 merged cells (sheet.getMergedRegions())
        throw new ExtractException("not_implemented", "ExcelExtractor.range is a stub");
    }

    private ExtractResponse doColumn(MultipartFile file, Map<String, Object> op) {
        // TODO: POI 实现
        throw new ExtractException("not_implemented", "ExcelExtractor.column is a stub");
    }

    private ExtractResponse doRow(MultipartFile file, Map<String, Object> op) {
        // TODO: POI 实现
        throw new ExtractException("not_implemented", "ExcelExtractor.row is a stub");
    }

    private ExtractResponse doAggregate(MultipartFile file, Map<String, Object> op) {
        // TODO: POI 实现
        // function 校验 (sum/avg/min/max/count)
        // 数值强转 (cell.getNumericCellValue()); 跳过非数值
        throw new ExtractException("not_implemented", "ExcelExtractor.aggregate is a stub");
    }

    private ExtractResponse doFilter(MultipartFile file, Map<String, Object> op) {
        // TODO: POI 实现
        // op.get("where") = {column, op, value}
        // op 校验 (eq/ne/gt/lt/gte/lte/contains/startsWith/regex)
        // limit (可选) 限制返回行数
        throw new ExtractException("not_implemented", "ExcelExtractor.filter is a stub");
    }
}
