import { DynamicStructuredTool } from "@langchain/core/tools";
import { z } from "zod";

/**
 * file_extract tool —— 从已上传的文件中按"操作"提取结构化内容。
 *
 * 整体设计：
 *   - 一个 tool 名字（`file_extract`）覆盖所有文件类型和所有 kind
 *   - 由 LLM 根据用户问话自动选 type + kind + params
 *   - skill-gateway 收到 operation 后 switch 路由到对应 extractor
 *
 * 路由关系（type × kind 矩阵）：
 *
 *   | type   | kind 可选值                                                   |
 *   |--------|--------------------------------------------------------------|
 *   | excel  | range, column, row, aggregate, filter                       |
 *   | word   | paragraphs, section, table, keyword                         |
 *   | text   | lineRange, keywordLines, regex, section, codeBlocks         |
 *   | pdf    | pageRange, keyword, fullText, metadata                      |
 *   | image  | ocr, keyword                                                 |
 *
 * 选错 type / kind / params 时，gateway 会返回结构化 4xx 错误
 * （例如 `{"error":"unsupported_kind","type":"excel","allowed":[...]}`），
 * 本 tool 把错误原文转抛给 LLM，让 LLM 自我修正后重试。
 */

// ================= 子 schema：每种 kind 的参数形状 =================

const ExcelRangeOp = z.object({
  kind: z.literal("range"),
  sheet: z.string().optional().describe("Sheet name, defaults to first sheet"),
  range: z
    .string()
    .regex(/^[A-Z]+\d+:[A-Z]+\d+$/, "A1 notation, e.g. 'B2:D10'"),
});

const ExcelColumnOp = z.object({
  kind: z.literal("column"),
  sheet: z.string().optional(),
  column: z.string().regex(/^[A-Z]+$/, "Column letter, e.g. 'B'"),
});

const ExcelRowOp = z.object({
  kind: z.literal("row"),
  sheet: z.string().optional(),
  row: z.number().int().positive().describe("1-based row number"),
});

const ExcelAggregateOp = z.object({
  kind: z.literal("aggregate"),
  sheet: z.string().optional(),
  column: z.string().regex(/^[A-Z]+$/),
  function: z.enum(["sum", "avg", "min", "max", "count"]),
});

const ExcelFilterOp = z.object({
  kind: z.literal("filter"),
  sheet: z.string().optional(),
  where: z.object({
    column: z.string().regex(/^[A-Z]+$/),
    op: z.enum(["eq", "ne", "gt", "lt", "gte", "lte", "contains", "startsWith", "regex"]),
    value: z.union([z.string(), z.number(), z.boolean()]),
  }),
  limit: z.number().int().positive().max(1000).optional(),
});

const WordParagraphsOp = z.object({
  kind: z.literal("paragraphs"),
  styleFilter: z.string().optional().describe("Regex matching paragraph styles, e.g. '^Heading[12]$'"),
  limit: z.number().int().positive().max(1000).optional(),
});

const WordSectionOp = z.object({
  kind: z.literal("section"),
  heading: z.string().describe("Heading text to locate, e.g. '第三章'"),
  matchLevel: z.enum(["exact", "contains"]).default("exact"),
});

const WordTableOp = z.object({
  kind: z.literal("table"),
  index: z.number().int().nonnegative().optional().describe("0-based table index, omit for all"),
});

const WordKeywordOp = z.object({
  kind: z.literal("keyword"),
  keywords: z.array(z.string()).min(1),
  matchMode: z.enum(["contains", "regex"]).default("contains"),
});

const TextLineRangeOp = z.object({
  kind: z.literal("lineRange"),
  from: z.number().int().positive(),
  to: z.number().int().positive(),
});

const TextKeywordLinesOp = z.object({
  kind: z.literal("keywordLines"),
  keywords: z.array(z.string()).min(1),
  caseSensitive: z.boolean().default(false),
});

const TextRegexOp = z.object({
  kind: z.literal("regex"),
  pattern: z.string(),
  group: z.number().int().nonnegative().default(0),
});

const TextSectionOp = z.object({
  kind: z.literal("section"),
  heading: z.string().describe("Markdown heading including leading ##s, e.g. '## 安装'"),
});

const TextCodeBlocksOp = z.object({
  kind: z.literal("codeBlocks"),
  language: z.string().optional().describe("Filter to a language, e.g. 'bash'"),
});

const PdfPageRangeOp = z.object({
  kind: z.literal("pageRange"),
  from: z.number().int().positive(),
  to: z.number().int().positive(),
});

const PdfKeywordOp = z.object({
  kind: z.literal("keyword"),
  keyword: z.string(),
  contextChars: z.number().int().nonnegative().default(50),
});

const PdfFullTextOp = z.object({
  kind: z.literal("fullText"),
  maxPages: z.number().int().positive().optional(),
});

const PdfMetadataOp = z.object({
  kind: z.literal("metadata"),
});

const ImageOcrOp = z.object({
  kind: z.literal("ocr"),
  languages: z.array(z.string()).optional().describe("e.g. ['zh','en']"),
});

const ImageKeywordOp = z.object({
  kind: z.literal("keyword"),
  keywords: z.array(z.string()).min(1),
});

// ================= type × kind 联合（discriminated union） =================

// Excel
const ExcelOperation = z.discriminatedUnion("kind", [
  ExcelRangeOp,
  ExcelColumnOp,
  ExcelRowOp,
  ExcelAggregateOp,
  ExcelFilterOp,
]);

// Word
const WordOperation = z.discriminatedUnion("kind", [
  WordParagraphsOp,
  WordSectionOp,
  WordTableOp,
  WordKeywordOp,
]);

// Text（txt + md 共用）
const TextOperation = z.discriminatedUnion("kind", [
  TextLineRangeOp,
  TextKeywordLinesOp,
  TextRegexOp,
  TextSectionOp,
  TextCodeBlocksOp,
]);

// PDF
const PdfOperation = z.discriminatedUnion("kind", [
  PdfPageRangeOp,
  PdfKeywordOp,
  PdfFullTextOp,
  PdfMetadataOp,
]);

// Image
const ImageOperation = z.discriminatedUnion("kind", [ImageOcrOp, ImageKeywordOp]);

// ================= 顶层 schema：type 决定走哪个 operation schema =================

const fileExtractInputSchema = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("excel"),
    fileId: z.string().min(1).describe("File ID returned by upload"),
    operation: ExcelOperation,
  }),
  z.object({
    type: z.literal("word"),
    fileId: z.string().min(1),
    operation: WordOperation,
  }),
  z.object({
    type: z.literal("text"),
    fileId: z.string().min(1),
    operation: TextOperation,
  }),
  z.object({
    type: z.literal("pdf"),
    fileId: z.string().min(1),
    operation: PdfOperation,
  }),
  z.object({
    type: z.literal("image"),
    fileId: z.string().min(1),
    operation: ImageOperation,
  }),
]);

export type FileExtractInput = z.infer<typeof fileExtractInputSchema>;

// ================= Tool 类 =================

export class FileExtractTool extends DynamicStructuredTool {
  /**
   * @param gatewayUrl  skill-gateway base URL, e.g. "http://localhost:18080"
   * @param apiToken    bearer token (可选)
   * @param fileResolver 异步函数：把 fileId 转成 multipart 需要的文件流 / URL
   *                     （详见下：skill-gateway 期望的是 multipart 文件，
   *                       所以 agent-core 需要先通过 fileId 取到原文件字节再转发）
   */
  constructor(
    private readonly gatewayUrl: string,
    private readonly apiToken: string | undefined,
    private readonly fileResolver: (fileId: string) => Promise<{
      filename: string;
      contentType: string;
      bytes: Uint8Array;
    }>,
  ) {
    super({
      name: "file_extract",
      description:
        "Extract structured content from an already-uploaded file. " +
        "Call this when the user asks to read a specific range, row, column, " +
        "section, page, line range, keyword, regex match, table, OCR text, " +
        "or aggregate from a file. " +
        "The 'type' must match the file's actual type (excel|word|text|pdf|image). " +
        "The 'operation.kind' selects the specific extraction; each kind has " +
        "its own required fields. If the request is malformed the gateway " +
        "returns a structured 4xx error describing exactly which field or " +
        "value is wrong — pass that error back to the model and let it retry.",
      schema: fileExtractInputSchema,
      func: async (input: FileExtractInput): Promise<string> => {
        const { type, fileId, operation } = input;
        const file = await this.fileResolver(fileId);
        const form = new FormData();
        // Blob 在 Node 环境可以直接接受 Uint8Array
        form.append(
          "file",
          new Blob([file.bytes], { type: file.contentType }),
          file.filename,
        );
        form.append("operation", JSON.stringify({ type, operation }));

        const headers: Record<string, string> = {};
        if (this.apiToken) headers["Authorization"] = `Bearer ${this.apiToken}`;

        const res = await fetch(`${this.gatewayUrl}/api/file/extract`, {
          method: "POST",
          body: form,
          headers,
        });

        // 不论 2xx 还是 4xx，gateway 都返回结构化 JSON；原样转给 LLM
        const text = await res.text();
        if (!res.ok) {
          // 4xx 错误：让 LLM 看到错误详情以便自我修正
          return `ERROR ${res.status}: ${text}`;
        }
        return text;
      },
    });
  }
}
