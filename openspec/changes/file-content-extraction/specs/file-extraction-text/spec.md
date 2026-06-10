## ADDED Requirements

### Requirement: Line range extraction
TextExtractor MUST support `kind: "lineRange"` extracting a contiguous line range.

#### Scenario: Extract lines 10-20
- **WHEN** client requests `{"kind":"lineRange","from":10,"to":20}`
- **THEN** response `data` is `{"lines":["line10","line11",...],"count":11}`

#### Scenario: Line range out of bounds
- **WHEN** `to` exceeds file line count
- **THEN** system caps at actual line count; response `truncated: true`

### Requirement: Keyword line filter
TextExtractor MUST support `kind: "keywordLines"` returning only lines containing keywords.

#### Scenario: Lines containing "ERROR"
- **WHEN** client requests `{"kind":"keywordLines","keywords":["ERROR"],"caseSensitive":false}`
- **THEN** response `data` is `{"matches":[{"lineNumber":42,"line":"..."}],"matchedCount":N}`

### Requirement: Regular expression extraction
TextExtractor MUST support `kind: "regex"` extracting all matches of a regex pattern.

#### Scenario: Extract IP addresses
- **WHEN** client requests `{"kind":"regex","pattern":"\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b","group":0}`
- **THEN** response `data` is `{"matches":[{"text":"192.168.1.1","lineNumber":7,"start":12,"end":23}]}`

#### Scenario: Catastrophic regex rejected
- **WHEN** pattern is `(a+)+$` and applied to a 1MB string
- **THEN** system applies a 30s timeout and returns 504 if exceeded (no ReDoS)

### Requirement: Section by heading (markdown)
For .md files, TextExtractor MUST support `kind: "section"` extracting content under a specific markdown heading.

#### Scenario: Extract content under "## 安装"
- **WHEN** client requests `{"kind":"section","heading":"## 安装"}`
- **THEN** response `data` is `{"heading":"## 安装","content":"...","nextHeading":"## 使用"}`

### Requirement: Code block extraction (markdown)
For .md files, TextExtractor MUST support `kind: "codeBlocks"` extracting all fenced code blocks.

#### Scenario: Extract all code blocks with language
- **WHEN** client requests `{"kind":"codeBlocks"}`
- **THEN** response `data` is `{"blocks":[{"language":"bash","content":"...","startLine":10}]}`

### Requirement: Encoding handling
TextExtractor MUST auto-detect UTF-8 / GBK / UTF-16 encoded text files.

#### Scenario: GBK encoded file
- **WHEN** uploaded .txt is GBK encoded
- **THEN** response text is correctly decoded Chinese characters (not garbled)
