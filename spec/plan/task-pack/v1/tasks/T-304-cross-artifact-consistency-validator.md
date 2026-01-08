# T-304 Consistency Validator

This task defines a **Minimal Cross-Artifact Consistency Gate (P0)**.

The task validates **structural consistency only** across artifacts using
**normalized validator outputs**.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task performs **static consistency validation**.

It is NOT:
- an implementation example
- a semantic or business-rule analyzer
- a fixer or transformer
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
    - raw XML
    - Java source code
    - OpenAPI YAML
- DO NOT read or reference:
    - architecture.md
    - dag.md
    - requirements.md
    - experience.md
- DO NOT scan directories or infer repository context
- DO NOT introduce validation rules not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following normalized inputs:

- XML validator output JSON (from T-301)
- Java validator output JSON (from T-302)
- OpenAPI validator output JSON (from T-303)

Optional, read-only configuration:
- type-mapping rules
- ignore-fields list
- strict-mode flag

Explicitly forbidden:
- spec-tree.json
- original artifact files
- any additional configuration sources

---

## 4. Validation Scope (P0 Only)

### P0-1 Field Presence Alignment

For each `fieldPath` in the union set of all artifacts:

- If missing in any artifact → `MISSING_FIELD`

Special handling:
- `groupId`
- `occurrenceCount`

These fields:
- are allowed ONLY in XML results
- MUST be ignored when validating Java and OpenAPI presence

---

### P0-2 Type Consistency (Canonical Mapping)

- Normalize artifact-specific types using declared type-mapping rules
- Compare canonical type identifiers only
- Any mismatch → `TYPE_MISMATCH`

Unknown type handling:
- strict-mode = true  → ERROR
- strict-mode = false → WARNING

No string-literal comparison is allowed.

---

### P0-3 Structure Consistency

For each `fieldPath`, compare the shape category:

- primitive
- object
- array

Any mismatch → `STRUCTURE_MISMATCH`

---

### P0-4 Required Flag Consistency (Minimal)

Validate required/optional alignment **only if metadata exists**:

- XML: `minOccurs == 1`
- Java: non-null constraint marker (if present in validator output)
- OpenAPI: presence in `required` list

If required metadata is missing in any artifact:
- record `REQUIRED_UNKNOWN` as WARNING
- escalate to ERROR only if `strict-mode = true`

---

## 5. Explicitly Out of Scope

The following MUST NOT be implemented:

- Enum value comparison
- Default value comparison
- Length or range constraints
- Ordering validation
- Deep semantic equivalence
- Fix or recommendation generation
- Golden output comparison
- Report beautification or formatting

---

## 6. Output Rules (Strict)

Exactly two outputs MUST be produced:

1) `consistency-report.json`
    - machine-readable only
    - deterministic ordering
    - no narrative text

2) `consistency-report.md`
    - minimal tabular form only
    - no summary sections
    - no explanatory paragraphs

Each issue entry MUST contain only:

- fieldPath
- category
- xml (present | missing | canonicalType)
- java (present | missing | canonicalType)
- openapi (present | missing | canonicalType)
- severity (ERROR | WARNING)

No additional fields are permitted.

---

## 7. Exit Code Policy

Exit codes are fixed and non-extensible:

- 0   → PASS (no ERROR-level issues)
- 44  → FAIL (one or more ERROR-level issues)

No other exit codes are allowed.

---

## 8. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] Validator JSON inputs read successfully
- [ ] Canonical fieldPath maps constructed
- [ ] MISSING_FIELD checks completed
- [ ] TYPE_MISMATCH checks completed
- [ ] STRUCTURE_MISMATCH checks completed
- [ ] Required flag alignment checked
- [ ] groupId / occurrenceCount ignored correctly
- [ ] consistency-report.json generated
- [ ] consistency-report.md generated
- [ ] Deterministic output guaranteed
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
