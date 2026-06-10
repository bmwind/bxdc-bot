## 1. 后端依赖与 DTO

- [ ] 1.1 在 `backend/skill-gateway/pom.xml` 中新增 `org.apache.pdfbox:pdfbox:3.0.2`（Apache 2.0 协议，license 无问题）
- [ ] 1.2 创建 `com.lobsterai.skillgateway.extract.dto` 包
- [ ] 1.3 创建 `ExtractRequest` DTO，字段：`type`（String，必填）、`operation`（Map<String,Object>，必填）
- [ ] 1.4 创建 `ExtractResponse` DTO，字段：`kind`（String）、`text`（String）、`data`（Map<String,Object>）
- [ ] 1.5 创建 `ExtractException` 继承 RuntimeException，携带 `errorCode` 字段

## 2. Excel 提取器

- [ ] 2.1 在 `com.lobsterai.skillgateway.extract.excel` 包下创建 `ExcelExtractor` 类
- [ ] 2.2 实现 `range` kind（解析 A1 表示法，返回二维单元格数组，处理合并单元格）
- [ ] 2.3 实现 `column` kind（返回 values + emptyCount）
- [ ] 2.4 实现 `row` kind（返回 values + colCount）
- [ ] 2.5 实现 `aggregate` kind（sum/avg/min/max/count，带数值强制转换）
- [ ] 2.6 实现 `filter` kind（支持 eq/ne/gt/lt/gte/lte/contains/startsWith/regex）
- [ ] 2.7 添加 A1 表示法解析工具（校验正则 `^[A-Z]+\d+(:[A-Z]+\d+)?$`）

## 3. Word 提取器

- [ ] 3.1 在 `com.lobsterai.skillgateway.extract.word` 包下创建 `WordExtractor` 类
- [ ] 3.2 实现 `paragraphs` kind（带可选的 styleFilter 正则）
- [ ] 3.3 实现 `section` kind（按标题定位，提取到下一个同级或更高级标题为止）
- [ ] 3.4 实现 `table` kind（按索引提取表格）
- [ ] 3.5 实现 `keyword` kind（段落匹配，支持 contains/regex 模式）
- [ ] 3.6 提取 section 时保持段落与表格的文档原始顺序

## 4. 文本提取器（txt + md）

- [ ] 4.1 在 `com.lobsterai.skillgateway.extract.text` 包下创建 `TextExtractor` 类
- [ ] 4.2 实现 `lineRange` kind（from/to，越界时设置 truncated 标志）
- [ ] 4.3 实现 `keywordLines` kind（支持 case-sensitive 参数）
- [ ] 4.4 实现 `regex` kind 带 30s 超时（用 Future.get(timeout)）
- [ ] 4.5 为 .md 实现 `section` kind（解析 markdown 标题层级）
- [ ] 4.6 为 .md 实现 `codeBlocks` kind（提取带语言标识的围栏代码块）
- [ ] 4.7 添加编码自动检测（UTF-8 BOM 检查，GBK 兜底，可用 juniversalchardet 或简单启发式）

## 5. PDF 提取器

- [ ] 5.1 在 `com.lobsterai.skillgateway.extract.pdf` 包下创建 `PdfExtractor` 类
- [ ] 5.2 实现 `pageRange` kind（按页提取文本）
- [ ] 5.3 实现 `keyword` kind（带 contextChars 上下文搜索）
- [ ] 5.4 实现 `fullText` kind（按页顺序输出）
- [ ] 5.5 实现 `metadata` kind（PDDocumentInformation）
- [ ] 5.6 添加扫描版 PDF 检测（所有页面 PDFBox 抽不到文本时返回 422）
- [ ] 5.7 处理加密 PDF（返回 400 带 `encrypted_pdf` 错误码）

## 6. 图片提取器（复用 DdsUtil）

- [ ] 6.1 在 `com.lobsterai.skillgateway.extract.image` 包下创建 `ImageExtractor` 类
- [ ] 6.2 实现 `ocr` kind（调用 `DdsUtil.getOcrText(InputStream)`，返回 text + confidence）
- [ ] 6.3 实现 `keyword` kind（对 OCR 结果按关键词筛选）
- [ ] 6.4 添加单张图片 30s 超时（DdsUtil 是阻塞调用）
- [ ] 6.5 添加 MIME 类型校验（拒绝非图片 MIME）

## 7. Service 与 Controller

- [ ] 7.1 创建 `FileExtractService`，提供 `extract(MultipartFile, ExtractRequest)` 方法
- [ ] 7.2 实现 `switch (type)` 路由到对应的 extractor
- [ ] 7.3 添加 operation 校验（kind 字段存在、按 type 检查允许的 kind 集合）
- [ ] 7.4 添加 extractor 级别 30s 超时（Future.get 带 timeout）
- [ ] 7.5 添加 100MB 文件大小前置检查
- [ ] 7.6 创建 `FileExtractController`，端点 `POST /api/file/extract`（multipart）
- [ ] 7.7 添加 `@ExceptionHandler(ExtractException.class)`，返回结构化 4xx 响应
- [ ] 7.8 在 `application.properties` 中添加 `app.file.extract.timeout-seconds=30` 和 `app.file.extract.max-size-mb=100`

## 8. 前端集成

- [ ] 8.1 在 `useFileUpload.ts` 中添加 `extractFile(file, operation): Promise<ExtractResponse>` 方法
- [ ] 8.2 在 `frontend/src/types/fileUpload.ts` 中定义 `ExtractRequest` 和 `ExtractResponse` 类型
- [ ] 8.3 在 `frontend/vite.config.ts` 中确认 `/api` 代理已覆盖（无需新增规则）
- [ ] 8.4 在每个已上传文件 chip 上添加"提取数据"按钮（按文件类型条件渲染，可选）

## 9. agent-core 工具集成

- [ ] 9.1 在 agent-core 工具注册表中定义 `file_extract` 工具描述
- [ ] 9.2 工具 schema：接收 `fileId` + `operation`，返回结构化数据
- [ ] 9.3 实现工具 handler，POST 到 skill-gateway
- [ ] 9.4 更新 LLM system prompt，让模型在用户提到"取列/取行/汇总"等指令时知道调用 `file_extract`
- [ ] 9.5 为每个 extractor kind 添加工具描述示例

## 10. 测试

- [ ] 10.1 单元测试 `ExcelExtractor`（range/column/row/aggregate/filter），准备样例 xlsx
- [ ] 10.2 单元测试 `WordExtractor`（paragraphs/section/table/keyword），准备样例 docx
- [ ] 10.3 单元测试 `TextExtractor`（lineRange/regex/section/codeBlocks）
- [ ] 10.4 单元测试 `PdfExtractor`（pageRange/keyword/fullText/metadata）
- [ ] 10.5 单元测试 `ImageExtractor`（ocr/keyword/timeout）
- [ ] 10.6 集成测试：curl POST `/api/file/extract` 每种格式
- [ ] 10.7 错误路径测试：400 unsupported_kind、400 parse_failed、413 too_large、504 timeout

## 11. 文档与收尾

- [ ] 11.1 更新 `openspec/file-upload-tasks(1).md` 任务 9 引用新的 extract 端点
- [ ] 11.2 所有任务完成后归档 OpenSpec change：`npx openspec archive file-content-extraction -y`
- [ ] 11.3 本地 build + commit + push（按 AGENTS.md §1 部署规则）
