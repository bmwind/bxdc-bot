## ADDED Requirements

### Requirement: Page range extraction
PdfExtractor MUST support `kind: "pageRange"` extracting text from specified pages.

#### Scenario: Extract pages 5-10
- **WHEN** client requests `{"kind":"pageRange","from":5,"to":10}`
- **THEN** response `data` is `{"pages":[{"pageNumber":5,"text":"..."},...],"pageCount":6}`

### Requirement: Keyword search across PDF
PdfExtractor MUST support `kind: "keyword"` finding all occurrences of a keyword with surrounding context.

#### Scenario: Find "合同金额" in PDF
- **WHEN** client requests `{"kind":"keyword","keyword":"合同金额","contextChars":50}`
- **THEN** response `data` is `{"matches":[{"pageNumber":3,"context":"...前文...合同金额...后文...","position":128}]}`

### Requirement: Full text extraction
PdfExtractor MUST support `kind: "fullText"` returning the entire document text in page order.

#### Scenario: Extract full text
- **WHEN** client requests `{"kind":"fullText"}`
- **THEN** response `data` is `{"pages":[{"pageNumber":1,"text":"..."},...],"totalPages":N}`

### Requirement: Metadata extraction
PdfExtractor MUST support `kind: "metadata"` returning PDF document metadata (title, author, creation date, page count).

#### Scenario: Extract metadata
- **WHEN** client requests `{"kind":"metadata"}`
- **THEN** response `data` is `{"title":"...","author":"...","createdAt":"...","pageCount":N}`

### Requirement: Scanned PDF handling
For scanned PDFs (no embedded text), PdfExtractor MUST return a structured error indicating OCR is required, NOT silently return empty text.

#### Scenario: PDF without text layer
- **WHEN** all pages return empty text from PDFBox
- **THEN** system returns 422 with `{"error":"no_text_layer","suggestion":"use image extractor after rasterization"}`

### Requirement: Encrypted PDF
PdfExtractor MUST reject password-encrypted PDFs with a clear error code (no silent failure).

#### Scenario: Encrypted PDF
- **WHEN** uploaded PDF requires a password
- **THEN** system returns 400 with `{"error":"encrypted_pdf","requiresPassword":true}`
