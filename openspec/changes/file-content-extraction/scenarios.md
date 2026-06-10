# 文件内容提取 — 场景全景与实现评估

> 本文档是 `openspec/changes/file-content-extraction/` 的**补充**，
> 用于评估 spec 之外的扩展场景可行性。
>
> 时间预估基于：1 名熟悉 Spring Boot 2.7 / POI / PDFBox / DdsUtil 的中级 Java 工程师。
> "实现复杂度"分级：⭐（简单） / ⭐⭐（中等） / ⭐⭐⭐（复杂）。

---

## 0. 评估维度说明

每条场景给出：

- **场景描述**：用户实际诉求
- **kind 标识**：spec 内的 `operation.kind` 名字（如已定义则用现有）
- **可实现性**：✅ 能 / ⚠️ 部分能 / ❌ 难（需评审）
- **实现思路**：核心技术方案与库
- **复杂度**：⭐ / ⭐⭐ / ⭐⭐⭐
- **预估工时**：开发 + 单测 + 联调（人天）
- **新增依赖**：需要新增的第三方包（按 AGENTS.md §7.1 需评审）
- **依赖真实 DdsUtil**：是否需要等真实 DdsUtil 上线

---

## 1. 通用层（适用于所有格式）

| # | 场景 | kind | 可行 | 思路 | 复杂度 | 工时 | 新增依赖 | 备注 |
|---|---|---|---|---|---|---|---|---|
| 1 | 取某行到某行 | `lineRange` / `range` | ✅ | 数组切片 | ⭐ | 已实现 | 无 | — |
| 2 | 取某章节 | `section` | ✅ | 按标题定位 | ⭐ | 已实现 | 无 | — |
| 3 | 关键词筛选 | `keyword` | ✅ | 遍历匹配 | ⭐ | 已实现 | 无 | — |
| 4 | 正则提取 | `regex` | ✅ | `Pattern` + `Future.get(30s)` | ⭐ | 已实现 | 无 | 防 ReDoS |
| 5 | 全文 | `fullText` | ✅ | 顺序拼接 | ⭐ | 已实现 | 无 | — |
| 6 | 段落列表 | `paragraphs` | ✅ | 遍历段落 | ⭐ | 已实现 | 无 | — |
| 7 | 文件信息（大小/MIME/页数/字数） | `meta` | ✅ | `MultipartFile` 元数据 | ⭐ | 0.1d | 无 | 简单包装 |
| 8 | 多 operation 链式 | `pipeline` | ⚠️ | spec 串多个 kind | ⭐⭐ | 1.5d | 无 | 需设计 DSL |
| 9 | 输出格式（json/csv/markdown） | `outputFormat` | ✅ | 简单 switch | ⭐ | 0.3d | 无 | 加 query 参数 |
| 10 | 异步任务（>30s 的 extract） | `asyncSubmit` | ⚠️ | 复用 AGENTS.md §4 异步系统 | ⭐⭐ | 1d | 无 | 任务管理 |

---

## 2. Excel (.xls / .xlsx)

### 2.1 已实现（spec 内）

| # | 场景 | kind | 库 | 工时 |
|---|---|---|---|---|
| 1 | 取 B2:D10 | `range` | POI `XSSFWorkbook` | 已实现 |
| 2 | 取整列 B | `column` | POI | 已实现 |
| 3 | 取第 5 行 | `row` | POI | 已实现 |
| 4 | 聚合 (sum/avg/min/max/count) | `aggregate` | POI | 已实现 |
| 5 | 条件筛 (eq/ne/gt/lt/contains/regex) | `filter` | POI | 已实现 |
| 6 | 选 sheet | `range.sheet` | POI | 已实现 |
| 7 | A1 解析 | — | regex | 已实现 |
| 8 | 合并单元格处理 | — | POI `CellRangeAddress` | 已实现 |

### 2.2 第一档：纯 Java 即可（推荐实现）

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| 9 | 排序 + Top N | `sort+limit` | `Collections.sort` + `subList` | ⭐ | 0.5d | 无 |
| 10 | 命名区域 | `namedRange` | `XSSFWorkbook.getName(name)` | ⭐ | 0.3d | 无 |
| 11 | 跨列拼接（concat 表达式） | `concat` | Java 模板 `"{A}-{B}-{C}"` | ⭐ | 0.5d | 无 |
| 12 | 去重统计 | `distinct` | `Set` + size | ⭐ | 0.2d | 无 |
| 13 | 多 sheet 合并（union） | `merge` | 遍历多 sheet 按列追加 | ⭐⭐ | 1d | 无 |
| 14 | 跨 sheet 聚合 | `crossSheet` | 遍历所有 sheet 后聚合 | ⭐⭐ | 1d | 无 |
| 15 | 公式表达（仅查 cell formula 文本） | `formula` | `Cell.getCellFormula()` | ⭐ | 0.3d | 无 |
| 16 | 数值精度控制 | `aggregate.round` | `BigDecimal.setScale` | ⭐ | 0.2d | 无 |
| 17 | 空值处理（`emptyAs` 字段） | `column.emptyAs` | `Optional` 包装 | ⭐ | 0.2d | 无 |
| 18 | 条件格式 / 数据校验信息 | `dataValidation` | POI `DataValidationConstraint` | ⭐ | 0.3d | 无 |
| 19 | 提取批注 | `comments` | POI `Sheet.getCellComments()` | ⭐ | 0.3d | 无 |
| 20 | 提取超链接 | `hyperlinks` | POI `Cell.getHyperlink()` | ⭐ | 0.3d | 无 |
| 21 | 工作簿结构（sheet 列表 + 尺寸） | `workbookMeta` | POI metadata | ⭐ | 0.2d | 无 |
| 22 | 工作簿级搜索（跨 sheet 找字符串） | `globalSearch` | 遍历全 sheet | ⭐ | 0.3d | 无 |

### 2.3 第二档：能做但需工程量

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| 23 | VLOOKUP / HLOOKUP 模拟 | `lookup` | Java 实现查找逻辑 | ⭐⭐ | 1d | 无 |
| 24 | 条件格式化（formatting 颜色/字体） | `cellStyle` | POI `CellStyle` API | ⭐⭐ | 1d | 无 |
| 25 | 冻结窗格 / 打印区域 / 分页符 | `pageSetup` | POI `Sheet.getPrintSetup()` | ⭐ | 0.5d | 无 |
| 26 | 加密 xlsx 读取（需密码） | `decrypt` | POI `EncryptionInfo` | ⭐⭐ | 1.5d | 无（POI 自带） |
| 27 | 流式读取（不加载整个 workbook） | `streaming` | POI `XSSFReader` (SAX) | ⭐⭐ | 1.5d | 无 |
| 28 | 写入修改后导出 | `writeBack` | 需新加 `outFile` 参数 | ⭐⭐ | 2d | 无 |
| 29 | CSV 独立 type 路由 | `type: csv` | 加 `CsvExtractor` 走 POI `CSVParser` | ⭐ | 0.5d | 无 |
| 30 | xls (BIFF8 旧格式) | — | POI `HSSFWorkbook`（已支持） | ⭐ | 0.3d | 无 |

### 2.4 第三档：需要评审 / 第三方

| # | 场景 | 真实方案 | 是否可做 | 工时 | 备注 |
|---|---|---|---|---|---|
| 31 | 透视表 / 数据透视图 | Apache POI 读写不计算 | ⚠️ | 5d+ | 需自己实现聚合 |
| 32 | 图表数据（chart） | POI `XDDFChart` API 复杂 | ⚠️ | 5d+ | 输出 XML 给前端 ECharts |
| 33 | 公式重算 | Apache ORO / 数学引擎 | ❌ | — | 用值，不重算 |
| 34 | 跨工作簿 join | 文件调度系统 | ❌ | — | 超出单接口 |
| 35 | VBA 宏 | Apache POI 不支持完整宏 | ❌ | — | 只读不执行 |

---

## 3. Word (.docx)

### 3.1 已实现（spec 内）

| # | 场景 | kind | 库 |
|---|---|---|---|
| 1 | 所有段落 | `paragraphs` | POI `XWPFDocument` |
| 2 | 按样式筛 | `paragraphs.styleFilter` | POI |
| 3 | 按标题取章节 | `section` | POI 遍历 |
| 4 | 表格 | `table` | POI |
| 5 | 关键词 | `keyword` | 段落匹配 |
| 6 | 文档顺序 | `section` | 段落+表格按流序 |

### 3.2 第一档：纯 Java 即可

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| 7 | 批注 | `comments` | POI `XWPFDocument.getComments()` | ⭐ | 0.3d | 无 |
| 8 | 页眉页脚 | `headerFooter` | POI `getHeaderFooterPolicy()` | ⭐ | 0.3d | 无 |
| 9 | 目录 (TOC) | `toc` | 遍历 SDT/段落 + 标题识别 | ⭐⭐ | 1d | 无 |
| 10 | 脚注尾注 | `footnotes` | POI `XWPFFootnotes` | ⭐ | 0.3d | 无 |
| 11 | 段落级图片提取 | `images` | POI `XWPFParagraph.getAllPictures()` | ⭐⭐ | 1d | 无 |
| 12 | 段落级超链接 | `hyperlinks` | POI `XWPFHyperlink` | ⭐ | 0.3d | 无 |
| 13 | 列表识别 | `lists` | POI `XWPFNumbering` | ⭐ | 0.5d | 无 |
| 14 | 表格单元格合并识别 | `table.mergedCells` | POI `getRowSpan/getColSpan` | ⭐ | 0.5d | 无 |
| 15 | 跨多表格拼接 | `unionTables` | Java 数组拼接 | ⭐ | 0.3d | 无 |
| 16 | 段落编号（含 list 编号） | `paragraphs.numbered` | POI `getNumPr()` | ⭐ | 0.3d | 无 |
| 17 | 文档元数据 | `meta` | POI `XWPFDocument.getProperties()` | ⭐ | 0.2d | 无 |
| 18 | 修订/批注是否接受状态 | `revisions` | POI `getRunElementChanges()` | ⭐⭐ | 1d | 无 |
| 19 | 文本框/形状 | `shapes` | POI `XWPFShape` | ⭐ | 0.5d | 无 |

### 3.3 第二档：能做但复杂

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 备注 |
|---|---|---|---|---|---|---|
| 20 | 修订痕迹（Track Changes 内容） | `trackChanges` | POI 增量遍历 | ⭐⭐ | 1.5d | 需熟悉 XWPF 增量模型 |
| 21 | 嵌入对象（OLE / Chart / Equation） | `embed` | POI `getEmbeddedParts()` | ⭐⭐⭐ | 3d+ | 涉及二进制解析 |
| 22 | 文档结构图（按样式层级） | `outline` | 遍历 `getStyles()` | ⭐⭐ | 1d | 类似大纲视图 |
| 23 | 旧 .doc 格式 | — | Apache Tika | ⭐ | 0.5d | **需新增依赖** Tika |
| 24 | 多文档对比 | `diff` | 需第三方 diff 库 | ⭐⭐ | 2d | java-diff-utils |
| 25 | 加密 docx | `decrypt` | POI + 用户密码 | ⭐⭐ | 1.5d | — |

### 3.4 第三档：超出范围

| # | 场景 | 原因 |
|---|---|---|
| 26 | 渲染预览 | 需 LibreOffice/Word 转 PDF |
| 27 | 协同编辑历史 | 需 CRDT |
| 28 | 公式编辑器内容 | MathML/OMML 复杂 |

---

## 4. 文本 (.txt / .md)

### 4.1 已实现（spec 内）

| # | 场景 | kind |
|---|---|---|
| 1 | 行范围 | `lineRange` |
| 2 | 关键词行 | `keywordLines` |
| 3 | 正则 | `regex` |
| 4 | md 标题章节 | `section` |
| 5 | md 代码块 | `codeBlocks` |
| 6 | 编码检测 | — |

### 4.2 第一档：纯 Java 即可

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| 7 | 字符数 / 词数 / 行数 | `stats` | 简单计数 | ⭐ | 0.2d | 无 |
| 8 | 大小写转换 | `transform` | `toLowerCase/toUpperCase` | ⭐ | 0.2d | 无 |
| 9 | 去重行 | `distinctLines` | `LinkedHashSet` | ⭐ | 0.2d | 无 |
| 10 | 按时间戳筛（通用 regex 子集） | `regex` | 用 `kind: regex` 配 `\\d{4}-\\d{2}-\\d{2}` | ⭐ | 已实现 | 无 |
| 11 | 排序行 | `sortLines` | `Collections.sort` | ⭐ | 0.2d | 无 |
| 12 | JSON path 查询 | `jsonPath` | Spring 自带 jackson + 手写 path | ⭐⭐ | 1d | 无 |
| 13 | 行前 N 字摘要 | `head` | 字符串切片 | ⭐ | 0.2d | 无 |
| 14 | 关键词频率统计 | `keywordFreq` | `Map<String,Integer>` | ⭐ | 0.3d | 无 |
| 15 | CSV 解析（按 type=csv 路由） | `type: csv` | POI `CSVParser` | ⭐ | 0.5d | 无 |
| 16 | XML 提取（XPath） | `type: xml` | JDK `javax.xml.xpath` | ⭐⭐ | 1d | 无 |

### 4.3 第二档

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 备注 |
|---|---|---|---|---|---|---|
| 17 | YAML 解析 | `type: yaml` | SnakeYAML | ⭐ | 0.5d | **需新增依赖** |
| 18 | 树形目录提取（.ini / .conf） | `keyValue` | 手写解析 | ⭐ | 0.5d | 无 |
| 19 | 代码语法高亮输出 | `highlight` | 需库 | ⭐⭐ | 2d | **需新增依赖** highlight.js java port |
| 20 | 大文件分块读（>100MB） | `stream` | `InputStream` + buffer | ⭐⭐ | 1d | 无 |

### 4.4 Markdown 专属场景（常见业务需求）

> 范围：`.md` 文件。`type: "text"` + 后缀 `.md` 路由到 markdown 解析分支。

#### 4.4.1 已实现（spec 内）

| # | 场景 | kind | 思路 |
|---|---|---|---|
| M1 | 按 `##` 标题取章节 | `section` | regex `^#{1,6}\s+(.+)$` |
| M2 | 提取所有围栏代码块 | `codeBlocks` | regex `` ```(\w+)?\n([\s\S]*?)``` `` |

#### 4.4.2 第一档：纯 Java 即可

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| M3 | 提取所有链接 | `mdLinks` | regex `\[([^\]]*)\]\(([^)]+)\)` | ⭐ | 0.2d | 无 |
| M4 | 提取所有图片引用 | `mdImages` | regex `!\[([^\]]*)\]\(([^)]+)\)` | ⭐ | 0.2d | 无 |
| M5 | 提取所有标题（含层级） | `mdHeadings` | 遍历 `^#{1,6}\s+(.+)$` 输出 `[{level,text,lineNumber}]` | ⭐ | 0.3d | 无 |
| M6 | 提取所有表格 | `mdTable` | 解析连续 `\|...\|` 行，输出 `[[row]]` 二维数组 | ⭐ | 0.5d | 无 |
| M7 | 提取所有引用块（`>`） | `mdBlockquotes` | 合并连续 `^>` 行 | ⭐ | 0.3d | 无 |
| M8 | 提取所有列表项 | `mdListItems` | `-` / `*` / `1.` / `- [ ]` 起始 | ⭐ | 0.3d | 无 |
| M9 | 提取所有任务项（task list） | `mdTasks` | 识别 `- [ ]` / `- [x]`，输出 `{text,done}` | ⭐ | 0.3d | 无 |
| M10 | 提取水平分割线 | `mdHorizontalRules` | 识别 `---` / `***` / `___` 整行 | ⭐ | 0.2d | 无 |
| M11 | 提取所有 HTML 标签（raw） | `mdHtml` | 抓 `<...>` 块 | ⭐ | 0.2d | 无 |
| M12 | 提取所有内联代码（` ` ） | `mdInlineCode` | regex 抓反引号内容 | ⭐ | 0.2d | 无 |
| M13 | 提取所有加粗 / 斜体 | `mdEmphasis` | regex 抓 `**...**` / `*...*` | ⭐ | 0.2d | 无 |
| M14 | 提取脚注（`[^1]`） | `mdFootnotes` | 抓脚注定义和引用 | ⭐⭐ | 0.5d | 无 |
| M15 | 提取所有 frontmatter（YAML/TOML） | `mdFrontmatter` | 解析开头的 `---` 块 | ⭐ | 0.3d | 无 |
| M16 | 提取所有定义列表 | `mdDefinitionLists` | 抓 `术语\n: 定义` 模式 | ⭐⭐ | 0.5d | 无 |
| M17 | 提取所有数学公式（`$...$` / `$$...$$`） | `mdMath` | regex 抓 `$..$` 块，输出 LaTeX | ⭐ | 0.3d | 无 |
| M18 | 提取目录（TOC） | `mdToc` | 解析 `# M5` 输出后组装成层级树 | ⭐ | 0.3d | 无 |
| M19 | 提取 mermaid / plantuml 代码块 | `mdDiagrams` | code blocks 过滤 language ∈ {mermaid,plantuml} | ⭐ | 0.2d | 无 |
| M20 | 按 H1 切分为多个子文档 | `mdSplitByH1` | 按顶级标题切分，返回数组 | ⭐ | 0.5d | 无 |
| M21 | 删除所有 markdown 格式（纯文本输出） | `mdPlainText` | 剥掉所有标记符 | ⭐ | 0.3d | 无 |
| M22 | 提取所有外链（http/https） | `mdExternalLinks` | 在 links 基础上 filter `^(http|https)://` | ⭐ | 0.2d | 无 |
| M23 | 提取所有内链（同文档 `#xxx`） | `mdInternalLinks` | 抓 `(#xxx)` | ⭐ | 0.2d | 无 |
| M24 | 按标签分组（`tag: xxx`） | `mdByTag` | 扫描全文找 tag 关键字，分段落归类 | ⭐⭐ | 0.5d | 无 |
| M25 | 提取所有 admonition（`> [!NOTE]`） | `mdAdmonitions` | 抓 `> [!XXX]` 块 | ⭐ | 0.3d | 无 |
| M26 | 提取所有 callout | `mdCallouts` | GitHub 风格 `> [!TIP]` 等 | ⭐ | 0.3d | 无 |
| M27 | 表格单元格对齐信息 | `mdTableAlign` | 解析 `\|:-\|:-:\|-\|` 标记 | ⭐ | 0.3d | 无 |
| M28 | 删除/保留指定章节 | `mdFilterSection` | 按 heading include/exclude 列表 | ⭐ | 0.5d | 无 |
| M29 | 拼接多个 md（合并 frontmatter 去重） | `mdMerge` | 多文件 markdown 合并 | ⭐⭐ | 1d | 无 |
| M30 | 转 HTML | `mdToHtml` | 手写微型 parser 或引入 commonmark-java | ⭐⭐ | 1d | **需新增依赖** commonmark |
| M31 | 转纯文本（保留段落换行） | `mdToText` | 剥标记 + 保留换行 | ⭐ | 0.3d | 无 |
| M32 | 检查 markdown 语法正确性 | `mdValidate` | 标题层级、列表缩进、围栏闭合 | ⭐⭐ | 1d | 无 |
| M33 | 提取所有 emoji 短代码（`:emoji:`） | `mdEmojis` | regex `:[a-z_]+:` | ⭐ | 0.2d | 无 |
| M34 | 提取所有 mention（`@user`） | `mdMentions` | regex `@\w+` | ⭐ | 0.2d | 无 |
| M35 | 提取所有 hashtag（`#tag`） | `mdHashtags` | regex `#\w+`（注意区分标题） | ⭐ | 0.2d | 无 |
| M36 | 提取所有删除线（`~~...~~`） | `mdStrikethrough` | regex `~~[^~]+~~` | ⭐ | 0.2d | 无 |
| M37 | 提取所有键盘按键（`<kbd>`） | `mdKbd` | regex `<kbd>[^<]+</kbd>` | ⭐ | 0.2d | 无 |
| M38 | 提取所有上标 / 下标 | `mdSubSuper` | regex `\^...\^\|~...~` | ⭐ | 0.2d | 无 |
| M39 | 提取所有 abbreviation（`*[ABBR]: full`） | `mdAbbreviations` | 解析 abbr 定义 | ⭐⭐ | 0.5d | 无 |
| M40 | 提取所有 cite 块（`> [!cite]`） | `mdCites` | 类比 admonition | ⭐ | 0.2d | 无 |
| M41 | 提取所有 prism 代码块（含语言高亮类） | `mdPrismBlocks` | 抓 ```language blocks | ⭐ | 0.2d | 无 |
| M42 | 提取所有 WikiLinks（`[[Page]]`） | `mdWikiLinks` | Obsidian/Logseq 风格 | ⭐ | 0.2d | 无 |
| M43 | 提取所有 callout 类型分布统计 | `mdCalloutStats` | 统计 `> [!TIP/WARN/...]` 各几个 | ⭐ | 0.3d | 无 |
| M44 | 提取所有时间戳（`@timestamp` 格式） | `mdTimestamps` | regex `\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}` | ⭐ | 0.2d | 无 |
| M45 | 提取所有 fenced div（`:::name ... :::`） | `mdFencedDivs` | 抓 Pandoc/MkDocs 风格围栏 div | ⭐ | 0.3d | 无 |
| M46 | 提取所有缩写（GitHub 风格） | `mdGithubAbbreviations` | 解析 `<abbr title="...">x</abbr>` HTML | ⭐ | 0.2d | 无 |
| M47 | 渲染 mermaid 为图片（PNG/SVG） | `mdRenderMermaid` | 调 mermaid-cli（需 Node） | ⭐⭐⭐ | — | **需评审** |
| M48 | 提取所有 highlight（`==text==`） | `mdHighlight` | regex `==[^=]+==` | ⭐ | 0.2d | 无 |
| M49 | 提取所有 subscript / superscript | `mdSubSuper2` | 同 M38，区分 sub/sup | ⭐ | 0.2d | 无 |
| M50 | 按 H2 分组（输出每组子内容） | `mdGroupByH2` | 类似 mdSplitByH1 但按 H2 | ⭐ | 0.3d | 无 |

#### 4.4.3 第二档

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 备注 |
|---|---|---|---|---|---|---|
| M51 | 转 PDF（保留样式） | `mdToPdf` | 需 wkhtmltopdf / chromium | ⭐⭐⭐ | — | **需评审** |
| M52 | 转 DOCX | `mdToDocx` | commonmark + POI | ⭐⭐⭐ | 3d+ | **需评审** |
| M53 | Lint 规则（标题层级、缩进、链接有效性） | `mdLint` | 完整 markdownlint 规则 | ⭐⭐⭐ | 5d+ | **需评审** |
| M54 | Mermaid 实时代码预览 | `mdLivePreview` | 需 mermaid 服务 | — | — | **超出范围** |
| M55 | 双向链接图谱 | `mdLinkGraph` | 构建 `[A] -> [B]` 关系图 | ⭐⭐ | 1.5d | 无 |

#### 4.4.4 第三档

| # | 场景 | 真实方案 | 备注 |
|---|---|---|---|
| M56 | 智能摘要（基于 LLM） | 调用 agent-core | ⚠️ 跨服务 |
| M57 | 自动生成目录（基于 LLM） | 调用 agent-core | ⚠️ 跨服务 |
| M58 | 内容合规检查（敏感词、隐私） | 需专用 NLP | ❌ |

---

## 5. PDF

### 5.1 已实现（spec 内）

| # | 场景 | kind |
|---|---|---|
| 1 | 页范围 | `pageRange` |
| 2 | 关键词 | `keyword` |
| 3 | 全文 | `fullText` |
| 4 | 元数据 | `metadata` |
| 5 | 扫描版检测 | 422 |
| 6 | 加密 PDF | 400 |

### 5.2 第一档：PDFBox 即可

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| 7 | 文档大纲/outline | `outline` | PDFBox `PDDocumentOutline` | ⭐⭐ | 1d | 无 |
| 8 | 链接提取 | `links` | PDFBox `PDPage.getAnnotations()` | ⭐ | 0.5d | 无 |
| 9 | 表单字段 | `formFields` | PDFBox `PDAcroForm` | ⭐⭐ | 1d | 无 |
| 10 | 图片提取 | `images` | PDFBox `PDResources.getXObject()` | ⭐⭐ | 1.5d | 无 |
| 11 | 注释 (annotations) | `annotations` | PDFBox `PDAnnotation` | ⭐ | 0.5d | 无 |
| 12 | 附件提取 | `attachments` | PDFBox `PDDocumentNameDictionary` | ⭐ | 0.5d | 无 |
| 13 | 字体列表 | `fonts` | PDFBox `PDFont` 遍历 | ⭐ | 0.3d | 无 |
| 14 | 页面尺寸 / 旋转 | `pageMeta` | PDFBox `PDPage` 属性 | ⭐ | 0.3d | 无 |
| 15 | 书签 | `bookmarks` | PDFBox `PDBookmark` | ⭐ | 0.3d | 无 |
| 16 | 数字签名验证 | `signatures` | PDFBox `PDSignature` | ⭐⭐ | 1.5d | 无 |
| 17 | 加密 PDF 解密（需密码） | `decrypt` | PDFBox `StandardDecryptionMaterial` | ⭐⭐ | 1.5d | 无 |
| 18 | XFA 表单（动态表单） | `xfa` | PDFBox `PDXFA` | ⭐⭐ | 1.5d | 无 |

### 5.3 第二档

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 备注 |
|---|---|---|---|---|---|---|
| 19 | 文本坐标（带 x,y） | `textWithCoords` | PDFBox `PDFTextStripper` 自定义 | ⭐⭐ | 1d | 简单但版面识别弱 |
| 20 | 单页双栏自动还原 | `reorder` | 按 x 坐标聚类 | ⭐⭐ | 2d | 启发式 |
| 21 | PDF 转 HTML | `toHtml` | PDFBox `PDFTextStripper` + 自定义 | ⭐⭐ | 2d | 简单版 |
| 22 | PDF 比对 | `diff` | 文本 diff | ⭐⭐ | 1.5d | 无 |

### 5.4 第三档：需评审

| # | 场景 | 真实方案 | 备注 |
|---|---|---|---|
| 23 | **表格结构还原** | Tabula / Camelot（Java）/ pdfplumber（Python） | ⚠️ 需新依赖或新服务 |
| 24 | **OCR 扫描版** | 调 ImageExtractor（已存在）+ 多页分割 | ⚠️ 需先有 OCR，且 PDFBox 不能直接给图 |
| 25 | 表单填写回写 | PDFBox `PDAcroForm` setValue | ⚠️ 1-2d 可做 |
| 26 | 转 Office（Word/Excel） | Aspose（付费）或 LibreOffice | ❌ 需评审 |
| 27 | 复杂版面分析（论文/财报） | pdfminer.six（Python ML） | ❌ 需新服务 |
| 28 | 数字签名签发 | BouncyCastle | ⚠️ 需新增依赖 |

---

## 6. 图片（OCR 后的二次处理）

### 6.1 已实现（spec 内）

| # | 场景 | kind |
|---|---|---|
| 1 | OCR 全文 | `ocr` |
| 2 | OCR 关键词筛 | `keyword` |
| 3 | 批量多图 | spec 明确数组 |
| 4 | OCR 30s 超时 | 504 |
| 5 | MIME 校验 | 400 |

### 6.2 第一档：纯 Java 即可

| # | 场景 | kind | 思路 | 复杂度 | 工时 | 新增依赖 |
|---|---|---|---|---|---|---|
| 6 | 多图批量 | — | spec 已支持 `data.results` 数组 | ⭐ | 0.3d | 无 |
| 7 | 图片基本信息 | `meta` | `ImageIO.read` 拿宽高/格式 | ⭐ | 0.2d | 无（Spring 已带） |
| 8 | EXIF 信息 | `exif` | metadata-extractor | ⭐ | 0.3d | **需新增依赖** metadata-extractor |
| 9 | 缩略图生成 | `thumbnail` | `ImageIO` resize | ⭐ | 0.3d | 无 |
| 10 | 颜色直方图 | `colorHist` | `BufferedImage` 像素遍历 | ⭐⭐ | 1d | 无 |
| 11 | 主色提取 | `mainColor` | K-means 简化版 | ⭐⭐ | 1d | 无 |

### 6.3 依赖 DdsUtil 能力

| # | 场景 | kind | 复杂度 | 工时 | 依赖真实 DdsUtil |
|---|---|---|---|---|---|
| 12 | OCR 坐标（每字位置） | `ocrWithCoords` | ⭐⭐ | 1.5d | **需要** |
| 13 | OCR 行级/段级分组 | `ocrLines` | ⭐⭐ | 1d | **需要** |
| 14 | 表格区域检测 | `tableRegion` | ⭐⭐⭐ | 3d+ | **需要** |
| 15 | 多语言识别（中英混合等） | `ocr.lang` | ⭐ | 0.5d | **需要** |
| 16 | 置信度阈值过滤 | `ocr.minConfidence` | ⭐ | 0.3d | **需要** |

### 6.4 第三档：需评审 / 第三方

| # | 场景 | 真实方案 | 备注 |
|---|---|---|---|
| 17 | 通用图片分类（发票/合同/照片） | TensorFlow Serving / ONNX | ❌ 需 ML 服务 |
| 18 | **手写体识别** | 专用模型 | ❌ 需 ML 服务 |
| 19 | 人脸识别 / 隐私检测 | 专用模型 | ❌ |
| 20 | 文档结构识别（页眉/正文/页脚） | LayoutLM 等 | ❌ 需 ML |

---

## 7. 跨类型综合

| # | 场景 | 思路 | 复杂度 | 工时 | 备注 |
|---|---|---|---|---|---|
| 1 | 一次请求返回多文件结果 | spec `data.results` 数组已设计 | ⭐ | 已实现 | — |
| 2 | 跨文件 join | 用同一 `correlationId` 串联 | ⭐⭐ | 2d | LLM 端做 |
| 3 | 文件类型自动识别（MIME + 魔数） | `Files.probeContentType` + 魔数 | ⭐ | 0.5d | 无 |
| 4 | zip 压缩包批量处理 | 解压后循环 | ⭐⭐ | 1.5d | 无 |
| 5 | 文件摘要缓存（避免重复解析） | MD5 缓存 key | ⭐⭐ | 1d | 无 |
| 6 | 用户配额（每天 N 次 extract） | 加 interceptor | ⭐ | 0.5d | 无 |
| 7 | 审计日志（谁/什么时候/什么文件） | 已存在 Audit 模式 | ⭐ | 0.3d | 复用 §4 |

---

## 8. 工时汇总

### 8.1 第一档（纯 Java，推荐实现）

| 类型 | 场景数 | 累计工时 |
|---|---|---|
| 通用 | 4 (7/9/10 已实现，新加 3) | 2d |
| Excel | 14 | 7.5d |
| Word | 13 | 8d |
| 文本 | 13 | 6d |
| PDF | 12 | 11.5d |
| 图片 | 6 | 3d |
| 跨类型 | 4 (3/5/6/7) | 2.3d |
| **合计** | **66** | **40.3d ≈ 8 周** |

### 8.2 第二档（需工程量）

| 类型 | 场景数 | 累计工时 |
|---|---|---|
| Excel | 5 | 7d |
| Word | 4 | 7d |
| 文本 | 4 | 4d |
| PDF | 4 | 6.5d |
| **合计** | **17** | **24.5d ≈ 5 周** |

### 8.3 第三档（需评审）

每个场景都要单独立项评审，**没法按工时估算**。如要推进，每个先做 1-2 天 PoC 验证可行性。

### 8.4 第四档（超出范围）

不计入工时。

---

## 9. 实施建议

**Phase 1（2 周）**：实现 spec 已定义的 32 个场景（已在 `tasks.md`）。
**Phase 2（4 周）**：补第一档高频需求（Excel 排序+TopN、Word 批注、PDF outline 等）。
**Phase 3（5 周）**：第二档（多 sheet 合并 / 加密 PDF / 流式读取等）。
**Phase 4（按需）**：评审第三方依赖后再决定是否做第三档（表格识别 / ML 分类等）。

---

## 10. 决策记录

| 决策 | 选择 | 理由 |
|---|---|---|
| 是否引入 PDFBox | ✅ | Apache 2.0，无 license 问题，唯一可靠的纯 Java PDF 库 |
| 是否引入 metadata-extractor | 待评审 | EXIF 用得少，可不引入，需要时再说 |
| 是否引入 Apache Tika | 待评审 | 旧 .doc 解析用，但 POI 已可处理 90%，可暂缓 |
| 是否引入 YAML / 高亮库 | ❌ | 用户量小，不值得增加供应链风险 |
| 是否引入 ML 模型 | ❌ | 跨过技术债务大槛，且需要 GPU/服务化部署 |

---

**版本**：1.0
**关联**：`openspec/changes/file-content-extraction/{proposal,design,tasks,specs/}.md`
