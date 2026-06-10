## ADDED Requirements

### Requirement: Unified extract endpoint
The system SHALL expose `POST /api/file/extract` that accepts a multipart upload (field `file`) and a JSON body (field `operation` or query param `operation`) describing the extraction operation.

#### Scenario: Submit xlsx with range operation
- **WHEN** client POSTs an .xlsx file with `{"type":"excel","operation":{"kind":"range","sheet":"Sheet1","range":"B2:D10"}}`
- **THEN** system returns 200 with `{"kind":"range","cells":[[...],[...]],"rowCount":9,"colCount":3}`

#### Scenario: Unsupported kind for type
- **WHEN** client submits an excel operation with `kind: "pivot"` (not in the allowed enum)
- **THEN** system returns 400 with `{"error":"unsupported_kind","allowed":["range","column","row","aggregate","filter"]}`

#### Scenario: Empty file
- **WHEN** client submits an empty multipart file
- **THEN** system returns 400 with `{"error":"empty_file"}`

#### Scenario: File exceeds size limit
- **WHEN** client submits a file larger than 100MB
- **THEN** system returns 413 with `{"error":"file_too_large","maxBytes":104857600}`

#### Scenario: Operation times out
- **WHEN** extractor runs longer than 30s
- **THEN** system returns 504 with `{"error":"timeout","timeoutSeconds":30}`

### Requirement: Type-based extractor routing
The system MUST route requests to the correct extractor based on `type` field: `excel`, `word`, `text`, `pdf`, `image`.

#### Scenario: Excel type routes to ExcelExtractor
- **WHEN** `type: "excel"` and file extension is .xlsx or .xls
- **THEN** ExcelExtractor handles parsing

#### Scenario: Type/extension mismatch
- **WHEN** `type: "excel"` but file extension is .pdf
- **THEN** system returns 400 with `{"error":"type_extension_mismatch","type":"excel","actualExt":"pdf"}`

### Requirement: Operation structure
The operation JSON MUST contain a `kind` field (string) and an optional `params` object whose shape depends on `kind` and `type`. The system MUST validate this structure before invoking the extractor.

#### Scenario: Missing kind field
- **WHEN** client submits `{"type":"excel","operation":{}}`
- **THEN** system returns 400 with `{"error":"missing_field","field":"operation.kind"}`

### Requirement: Response shape
The extract endpoint MUST return JSON with at least these top-level fields: `kind` (echo of operation kind), `text` (string, human-readable), and a `data` field whose shape depends on kind.

#### Scenario: Successful range extraction
- **WHEN** ExcelExtractor handles `range` kind
- **THEN** response `data` is `{"cells":[[...]],"rowCount":N,"colCount":M,"sheet":"..."}`

### Requirement: Error contract
All extractor errors MUST be reported as a structured JSON with at least `error` (string code) and `message` (human description) fields, never as 500 with a stack trace.

#### Scenario: Corrupt xlsx file
- **WHEN** ExcelExtractor cannot parse the uploaded file
- **THEN** system returns 400 with `{"error":"parse_failed","message":"..."}`

### Requirement: Concurrent call safety
The system MUST handle up to 10 concurrent extract calls without resource exhaustion (no shared mutable state across requests).

#### Scenario: 10 simultaneous extractions
- **WHEN** 10 clients call /api/file/extract simultaneously with different files
- **THEN** all 10 return independently without deadlock or OOM
