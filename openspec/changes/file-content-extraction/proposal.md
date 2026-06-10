## Why

当前上传链路只把整篇文件内容塞进 LLM context（任务 8 已完成）。但用户的真实诉求是**对上传文件做结构化、范围化的内容操作**：从 Excel 拿指定列/行、做聚合、统计；从 Word 抽取某个章节；从 PDF / 图片 OCR 后按关键词筛选；按规则（包含、排除、正则）筛选段落等。

现在没有这套能力，只能让 LLM "看完整篇内容" 然后人肉写 prompt，**对大文件（>1MB）天然不友好，且每次都得重新描述**。

需要在 skill-gateway 里建一个**统一的"文件结构化提取"能力**，对常见格式（Excel / Word / txt / md / pdf / image）都暴露同一套**位置化+筛选**接口，让前端 / agent-core / 用户 prompt 都能按"区域"和"规则"精确取数据，LLM 只拿到被裁剪后的内容。

## What Changes

- 新增**文件提取服务**（skill-gateway Java 端），按文件类型分发到不同的 extractor
- 引入 **Apache POI** 解析 xls/xlsx/doc/docx（pom 已加，任务 11 准备落地）
- 引入 **PDFBox** 解析 pdf（**新增 pom 依赖**——见 AGENTS.md §7.1 例外场景）
- 复用任务 9 的 **DdsUtil OCR** 解析图片
- 新增**统一 REST 接口** `POST /api/file/extract`（multipart file + JSON operation）
- operation 是一段声明式的 JSON：指定"取哪几列/行"、"取哪个章节"、"按规则筛选段落"等
- 返回结构化结果（text + 表格 cell + 命中片段）
- 前端 `useFileUpload` 增加 `extractFile` 入口（可选，agent-core 也能调）
- agent-core 的 LLM tool list 增加一个 `file_extract` 工具，让模型在用户 prompt 提到"取某列""某行"时自动调用

## Capabilities

### New Capabilities
- `file-extraction-api`: skill-gateway 上统一的 `POST /api/file/extract` 接口，支持声明式 operation
- `file-extraction-excel`: xls/xlsx 的列/行/范围/聚合提取
- `file-extraction-word`: docx 的章节/段落/表格提取
- `file-extraction-text`: txt / md 的行范围 / 关键词 / 正则提取
- `file-extraction-pdf`: pdf 的页范围 / 文本块 / 关键词提取
- `file-extraction-image`: png/jpg 经 OCR 后的文本提取与关键词筛选

### Modified Capabilities
（无。`file-upload-tasks(1).md` 是任务追踪文档，不是 spec；不在 openspec/specs/ 体系内。）

## Impact

**新增依赖**（pom.xml 需改动）：
- `org.apache.pdfbox:pdfbox:3.0.x`（pdf 解析）—— 唯一新增的第三方包，AGENTS.md §7.1 允许范围内（无现成 JDK 替代）
- POI 已加（`poi-ooxml`、`poi-scratchpad`），无需新加

**新增代码**：
- `backend/skill-gateway/src/main/java/.../extract/` 包
  - `FileExtractController.java`：统一 REST 端点
  - `FileExtractService.java`：operation 路由
  - `excel/ExcelExtractor.java`
  - `word/WordExtractor.java`
  - `text/TextExtractor.java`
  - `pdf/PdfExtractor.java`
  - `image/ImageExtractor.java`（包 DdsUtil）
  - `dto/ExtractRequest.java`、`ExtractResponse.java`、`ExtractOperation.java`（union-type）
  - `exception/ExtractException.java`

**修改代码**：
- `frontend/src/composables/useFileUpload.ts`：增加 `extractFile(file, operation)` 方法
- `backend/agent-core/src/.../tools/`：增加 `file-extract` 工具
- `backend/skill-gateway/src/main/java/.../controller/`：FileOcrController 的 `/ocr-image` 可保留作为简化接口，extract 走更结构化路径

**接口约定（operation JSON 形态）**：
```json
{
  "type": "excel",
  "operation": {
    "kind": "range",
    "sheet": "Sheet1",          // 可选，默认第一张
    "range": "B2:D10"            // A1 表示法
  }
}
```
多种 `kind` 覆盖多种场景（见 design.md）。

**OpenSpec 文档位置**：
- 6 个新 capability 各自一个 spec.md
- `openspec/changes/file-content-extraction/specs/` 下创建
