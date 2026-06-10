## ADDED Requirements

### Requirement: Paragraph extraction
WordExtractor MUST support `kind: "paragraphs"` returning all paragraphs as a structured list.

#### Scenario: Extract all paragraphs
- **WHEN** client requests `{"kind":"paragraphs"}`
- **THEN** response `data` is `{"paragraphs":[{"index":0,"text":"...","style":"Heading1"},...],"totalCount":N}`

#### Scenario: Filter by heading style
- **WHEN** client adds `{"styleFilter":"Heading[12]"}`
- **THEN** response contains only paragraphs whose style matches the regex

### Requirement: Section extraction by heading
WordExtractor MUST support `kind: "section"` extracting all content under a specific heading.

#### Scenario: Extract content under "第三章"
- **WHEN** client requests `{"kind":"section","heading":"第三章","matchLevel":"exact"}`
- **THEN** response `data` is `{"heading":"第三章","content":"...","paragraphCount":N,"endHeading":"第四章"}`

#### Scenario: Heading not found
- **WHEN** requested heading does not exist
- **THEN** system returns 400 with `{"error":"heading_not_found","availableHeadings":[...]}`

### Requirement: Table extraction
WordExtractor MUST support `kind: "table"` extracting all tables in the document.

#### Scenario: Extract all tables
- **WHEN** client requests `{"kind":"table","index":0}` (or omit for all)
- **THEN** response `data` is `{"tables":[{"index":0,"rows":[[...]],"rowCount":N,"colCount":M}]}`

#### Scenario: Table index out of range
- **WHEN** `index` exceeds actual table count
- **THEN** system returns 400 with `{"error":"table_index_out_of_range","actualCount":N}`

### Requirement: Keyword filter
WordExtractor MUST support `kind: "keyword"` extracting paragraphs containing certain keywords.

#### Scenario: Extract paragraphs mentioning "风险"
- **WHEN** client requests `{"kind":"keyword","keywords":["风险","隐患"],"matchMode":"contains"}`
- **THEN** response `data` is `{"matches":[{"paragraphIndex":3,"text":"...","matchedKeyword":"风险"}],...}`

### Requirement: Preserve document order
When mixing paragraphs and tables (e.g., in section extraction), WordExtractor MUST output them in their original document order.

#### Scenario: Section contains both paragraphs and tables
- **WHEN** section between two headings has 3 paragraphs and 1 table in order P1, P2, T1, P3
- **THEN** response `data.content` is the rendered sequence in that order
