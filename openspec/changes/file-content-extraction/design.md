## Context

`AGENTS.md` §4 异步任务系统已就绪，但**只解决了"等结果"的问题**。文件上传后，目前 `useFileUpload.parseFileContent` 把整篇内容塞进 LLM context（任务 8 完成），LLM 自己识别用户提问中的"取列/取行/汇总"等指令并改写 prompt——这对小文件勉强可用，对大文件（>1MB Excel、多页 PDF）既慢又贵。

OpenSpec 任务追踪文档 `openspec/file-upload-tasks(1).md` 已经列了**任务 9 文本解析**和**任务 11 POI 引入**两个相关任务（任务 11 标"已加依赖、待落地"）。本 change 是它们的合并/扩展。

**当前状态**：
- 已有：`DdsUtil` 占位 OCR（任务 9 完成）
- 已有：POI 依赖（任务 11 完成依赖添加）
- 已有：前端 `useFileUpload` 状态机、agent-core 文件解析（任务 3/4/7/8 完成）
- 缺失：结构化、按 operation 路由的提取能力

## Goals / Non-Goals

**Goals:**
- 一个**统一** REST 端点 `POST /api/file/extract` 接受任意上述格式 + 一段声明式 operation JSON
- 6 种 extractor（excel/word/text/pdf/image）各自实现 N 种常见 `kind` 操作
- LLM 通过 agent-core 的 `file_extract` 工具调用它（**LLM 决定用哪种 kind**）
- 用户在前端也能直接调用（hover 文件时显示"提取数据"菜单——可选）
- 单次调用超时保护，避免大文件卡死（默认 30s）
- 操作失败的局部原因以结构化错误返回（不是 500）

**Non-Goals:**
- **不**做复杂的公式计算引擎（Excel 公式只解析值，不重算）
- **不**支持 Excel 图表 / 透视表 / VBA
- **不**做 PDF 表格结构还原（只取文本流）
- **不**支持 doc（只 docx，doc 走 OCR 或 fallback）
- **不**做流式 streaming（小文件一次性返回即可）
- **不**做结果缓存（每次按需解析）

## Decisions

### 决策 1：operation 用 JSON 而非 query string

operation 嵌套结构（kind + params），且未来要扩展更多 kind，**JSON 更可维护**。请求体格式：
```json
{
  "type": "excel",
  "operation": { "kind": "range", "sheet": "Sheet1", "range": "B2:D10" }
}
```
**替代方案**：query string。否决——嵌套结构 query 表达不清晰，特殊字符（`:`、`!`）需要转义。

### 决策 2：extractor 由 service 内 `switch (type)` 路由，不用 Spring `@Component` 多态

如果做成 5 个 `@Component Extractor`，需要 `Map<String, Extractor>` 自动注入——配置复杂。直接 `switch (type)` 简单可读，性能也更好。
**替代方案**：策略模式 + Spring 自动注入。否决——YAGNI。

### 决策 3：PDFBox 新增 pom 依赖（AGENTS.md §7.1 例外）

PDF 解析 JDK 8 没内置方案。iText 受 AGPL 限制。Tabula 只做表格抽取。**PDFBox 是 Apache 2.0 协议，最稳定**。评估：
- 体积：约 6MB 传递依赖
- license：Apache 2.0
- 维护：Apache 顶级项目，长期支持

结论：接受新增 `org.apache.pdfbox:pdfbox:3.0.x`。

### 决策 4：operation 的 kind 设计为有限枚举而非自由 DSL

每种文件类型下，`kind` 是一个**有限字符串枚举**（如 excel 有 `range`、`column`、`row`、`aggregate`、`filter`）。**不允许**用户传任意 JS/Python 表达式。
**理由**：安全 + 可控 + LLM 容易枚举。LLM 调用时只需要 `kind` 名字 + 参数值。

### 决策 5：列/行引用用 A1 表示法（不是 0-based 索引）

`B2:D10` 直观、Excel 用户熟悉。**内部** 0-based 数组 + `Sheet` 解析器处理 A1。
**替代方案**：直接 `{"startRow":1, "endRow":9, "startCol":1, "endCol":3}`。否决——LLM 输出 A1 更准、用户更懂。

### 决策 6：图片 OCR 复用 DdsUtil，不引入新方案

任务 9 已完成 DdsUtil 占位实现。ImageExtractor 直接调 `DdsUtil.getOcrText(InputStream)`。

## Risks / Trade-offs

- **[Risk] PDFBox 6MB 传递依赖 + 字体文件可能让 fat jar 变大**
  → Mitigation: 接受。POI 已经带 xmlbeans、commons-collections4 等类似体积的依赖，团队已习惯。

- **[Risk] POI 解析 docx 大文件（>50MB）可能 OOM**
  → Mitigation: 提取接口全局 30s 超时 + 输入文件大小前置校验（>100MB 直接拒绝并返回 413）。

- **[Risk] LLM 调用 `file_extract` 时传错 kind 字符串**
  → Mitigation: extractor 在 `switch` 中落到 `default` 时返回 400 + 列出该类型所有支持的 kind。

- **[Risk] 用户的"取某列"指令在 A1 表示法下有歧义（"B 列" vs "B2:B100"）**
  → Mitigation: spec 明确：列范围 = 整列（无行号），单元格范围 = 矩形。

- **[Risk] WordExtractor 提取表格可能与段落混合，顺序错乱**
  → Mitigation: 段落和表格按文档流顺序输出，每个表格用结构化 JSON（含 row/cell 数组），段落用 markdown。

- **[Risk] 并发量大时 POI 解析 CPU 密集**
  → Mitigation: 单文件 30s 超时 + 现阶段不上限并发（量小），必要时后续加信号量。

## Migration Plan

1. 加 pom 依赖（PDFBox）
2. 写 6 个 extractor + 1 个 controller + 1 个 service
3. 写前端 `extractFile` 方法（用 `apiUrl('/file/extract')`）
4. 写 agent-core `file_extract` 工具描述，让 LLM 在用户提到"取列/取行/汇总"时自动调用
5. 端到端测试：6 种格式各一个示例 operation
6. 不涉及 schema 变更、灰度发布
7. 失败回滚：直接 `git revert`（无 schema 迁移）

## Open Questions

- agent-core 的 `file_extract` 工具是否需要支持"先传文件 ID 再传 operation"两段式调用，还是单次 `multipart file + operation`？倾向单次（简单），但要看 LLM tool calling 是否能一次塞下。
- 是否需要 LLM 在多步操作中保持"当前 sheet / 当前 region"上下文？现版本不做，复杂场景 LLM 自己把 operation 写完整。
