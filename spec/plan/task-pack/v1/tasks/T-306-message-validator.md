# T-306 Message Validator

This task defines a **Static Message Structure Validation Gate**.

The task validates the **structural correctness of an MQ message**
using a precomputed offset table and normalized specification metadata.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task performs **static, offset-based message validation**.

It is NOT:
- a message encoder or decoder
- a runtime message processor
- a business-rule validator
- a fixer or auto-corrector
- a test generator
- a report formatter
- a compiler, runner, or executor

---

## 2. Execution Constraints (Hard Rules)

### 2.1 Forbidden Actions (Absolute)

The following actions are strictly prohibited:

- DO NOT include any code templates or example implementations
- DO NOT include inline code blocks of any kind
- DO NOT include code comments or annotation templates
- DO NOT generate test case code (unit, integration, or E2E)
- DO NOT generate summary or explanatory paragraphs
- DO NOT generate remediation or fix suggestions
- DO NOT simulate, execute, or suggest:
    - compilation
    - test execution
    - validation commands
- DO NOT read or parse:
    - XML bean files
    - Java source code
    - OpenAPI YAML
- DO NOT read or reference:
    - architecture.md
    - dag.md
    - requirements.md
    - experience.md
- DO NOT scan directories or infer repository context
- DO NOT inspect encoding conversion logic (EBCDIC / ASCII)
- DO NOT introduce validation rules not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following inputs:

- `output/offset/offset-table.json` (from T-305)
- `output/json/spec-tree.json`
- `input/message.bin` OR `input/message.txt`
- optional validation configuration (strict / lenient flags only)

Explicitly forbidden:
- any generated source code
- any validator outputs other than offset-table
- any additional configuration files

---

## 4. Validation Scope (Gate-Level Only)

### 4.1 Message Length Validation

- Actual message length MUST equal `OffsetTable.totalLength`
- Any mismatch → `LENGTH_MISMATCH` (ERROR)

---

### 4.2 Field Boundary Validation

For each offset entry:

- Extract byte range `[offset, offset + length)`
- If range exceeds message length → `BOUNDARY_ERROR` (ERROR)

No recovery or fallback behavior is allowed.

---

### 4.3 Data Type Format Validation (Structural)

Validate field content format using **datatype metadata only**.

Supported categories:

- Numeric (N): digits only
- Alphanumeric (AN): letters, digits, space
- Alpha (A): letters only
- Signed (S): optional +/- followed by digits
- Any (X): no restriction

Invalid format → `TYPE_MISMATCH` (ERROR or WARNING per configuration)

---

### 4.4 Required Field Validation

If a field is marked as required in specification metadata:

- Field content MUST NOT be entirely blank
- Field content MUST NOT be entirely default-filled

Violation → `REQUIRED_MISSING` (ERROR)

---

### 4.5 Hardcode Value Validation

If a hardcode value is defined for a field:

- Actual field content MUST exactly match the hardcode value

Mismatch → `HARDCODE_MISMATCH` (ERROR)

---

## 5. Explicitly Out of Scope

The following MUST NOT be implemented:

- Message encoding or decoding
- Character set transformation
- Business rule validation
- Cross-field dependency validation
- Message repair or normalization
- Golden report comparison
- Report formatting or visualization

---

## 6. Output Rules (Strict)

Exactly two outputs MUST be produced:

1) `message-validation-report.json`
    - machine-readable only
    - deterministic ordering
    - no narrative text

2) `message-validation-report.txt`
    - plain structured text
    - no summary sections
    - no explanatory paragraphs

---

### 6.1 Validation Issue Fields

Each validation issue MUST contain exactly:

- fieldPath
- offset
- length
- category
- severity (ERROR | WARNING)
- expected
- actual

No additional fields are permitted.

---

## 7. Exit Code Policy

Exit codes are fixed and non-extensible:

- 0   → PASS (no ERROR-level issues)
- 51  → FAIL (LENGTH_MISMATCH)
- 52  → FAIL (TYPE_MISMATCH)
- 53  → FAIL (REQUIRED_MISSING)
- 54  → FAIL (HARDCODE_MISMATCH)
- 55  → FAIL (BOUNDARY_ERROR)
- 56  → FAIL (multiple ERROR categories)

No other exit codes are allowed.

---

## 8. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] offset-table.json read successfully
- [ ] spec-tree.json read successfully
- [ ] Message length validated
- [ ] Field boundaries validated
- [ ] Data type formats validated
- [ ] Required fields validated
- [ ] Hardcode values validated
- [ ] message-validation-report.json generated
- [ ] message-validation-report.txt generated
- [ ] Deterministic output guaranteed
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
