## ADDED Requirements

### Requirement: Range extraction
ExcelExtractor MUST support `kind: "range"` extracting a rectangular cell range using A1 notation.

#### Scenario: Extract B2:D10 from Sheet1
- **WHEN** client requests `{"kind":"range","sheet":"Sheet1","range":"B2:D10"}`
- **THEN** response `data` contains `cells` as a 2D array of size 9x3

#### Scenario: Range with merged cells
- **WHEN** range overlaps merged cells
- **THEN** all cells in the returned array carry the merged cell's value at the top-left anchor; other positions are `null`

#### Scenario: Invalid A1 notation
- **WHEN** range string is `"B2:D"`
- **THEN** system returns 400 with `{"error":"invalid_range","value":"B2:D"}`

#### Scenario: Sheet does not exist
- **WHEN** requested `sheet` name is not in workbook
- **THEN** system returns 400 with `{"error":"sheet_not_found","available":["..."]}`

### Requirement: Column extraction
ExcelExtractor MUST support `kind: "column"` extracting an entire column.

#### Scenario: Extract column B as a list
- **WHEN** client requests `{"kind":"column","sheet":"Sheet1","column":"B"}`
- **THEN** response `data` is `{"values":["v1","v2",...],"rowCount":N,"emptyCount":K}`

### Requirement: Row extraction
ExcelExtractor MUST support `kind: "row"` extracting a single row.

#### Scenario: Extract row 5
- **WHEN** client requests `{"kind":"row","sheet":"Sheet1","row":5}`
- **THEN** response `data` is `{"values":["v1","v2",...],"colCount":N}`

### Requirement: Aggregate operations
ExcelExtractor MUST support `kind: "aggregate"` computing sum/avg/min/max/count over a numeric column.

#### Scenario: Sum of column B
- **WHEN** client requests `{"kind":"aggregate","sheet":"Sheet1","column":"B","function":"sum"}`
- **THEN** response `data` is `{"function":"sum","value":1234.5,"nonEmptyCount":K}`

#### Scenario: Aggregate on non-numeric column
- **WHEN** column contains text values and `function` is `sum`
- **THEN** response `data.value` is `null` and `skippedCount` > 0

### Requirement: Filter rows by predicate
ExcelExtractor MUST support `kind: "filter"` returning rows where a column matches a condition.

#### Scenario: Filter rows where column C equals "完成"
- **WHEN** client requests `{"kind":"filter","sheet":"Sheet1","where":{"column":"C","op":"eq","value":"完成"}}`
- **THEN** response `data` is `{"rows":[[...]],"matchedCount":N}`

#### Scenario: Supported operators
- **WHEN** `op` is one of `eq, ne, gt, lt, gte, lte, contains, startsWith, regex`
- **THEN** system applies the filter; otherwise returns 400 `unsupported_op`

### Requirement: Numeric cell handling
ExcelExtractor MUST return numeric cells as JSON numbers, not strings.

#### Scenario: Numeric cell with formula
- **WHEN** cell has formula `=A1+B1` and result is 5
- **THEN** returned value is `5` (number), not `"5"` (string) and not `"=A1+B1"`
