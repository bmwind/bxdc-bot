## ADDED Requirements

### Requirement: OCR text extraction
ImageExtractor MUST call `DdsUtil.getOcrText(InputStream)` to extract text from uploaded image.

#### Scenario: PNG file OCR
- **WHEN** client uploads a .png file with `type: "image"` and `kind: "ocr"`
- **THEN** response `data` is `{"text":"...","confidence":0.0-1.0}`

#### Scenario: Supported image formats
- **WHEN** file extension is .png, .jpg, .jpeg, .bmp, .webp
- **THEN** system accepts; otherwise returns 400 `unsupported_image_format`

### Requirement: OCR with keyword filter
ImageExtractor MUST support `kind: "keyword"` extracting only lines containing certain keywords.

#### Scenario: OCR result filtered by keyword
- **WHEN** client requests OCR + filter `{"keywords":["金额","日期"]}`
- **THEN** response `data` is `{"matches":[{"text":"...","confidence":0.95}]}`

### Requirement: Multi-image batch
ImageExtractor MUST accept multiple image files in one request and return results in order.

#### Scenario: Batch OCR of 3 images
- **WHEN** client uploads 3 image files with same operation
- **THEN** response `data.results` is an array of 3 entries, one per file in input order

### Requirement: OCR timeout
ImageExtractor MUST enforce a 30s timeout per image (DdsUtil is sync/blocking).

#### Scenario: Large image exceeds timeout
- **WHEN** OCR takes longer than 30s
- **THEN** system returns 504 with `{"error":"ocr_timeout","imageIndex":N}`

### Requirement: Image-only file type
ImageExtractor MUST reject non-image MIME types with 400 even if extension matches.

#### Scenario: PDF renamed to .png
- **WHEN** file MIME type is application/pdf but extension is .png
- **THEN** system returns 400 with `{"error":"mime_mismatch","mime":"application/pdf"}`
