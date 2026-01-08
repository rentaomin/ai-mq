# T-305 Offset Calculator

This task defines a **Static Offset Calculation Gate**.

The task computes deterministic byte offsets for message fields based on
a normalized structural specification.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task performs **static, structural offset calculation**.

It is NOT:
- a runtime message processor
- a payload parser
- a semantic or business-rule evaluator
- a fixer or transformer
- a test generator
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
- DO NOT inspect or parse actual message payloads
- DO NOT infer dynamic field lengths at runtime
- DO NOT read or reference:
    - architecture.md
    - dag.md
    - requirements.md
    - experience.md
- DO NOT scan directories or infer repository context
- DO NOT introduce calculation rules not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following input:

- `spec-tree.json`

Explicitly forbidden:
- raw message data
- XML / Java / OpenAPI artifacts
- validator outputs
- any additional configuration sources

---

## 4. Offset Calculation Rules (Strict)

### 4.1 General Rules

- Offset calculation MUST start at `0`
- Field offset = previous field `offset + length`
- Calculation MUST be deterministic
- Traversal order MUST be stable and derived solely from `spec-tree.json`

---

### 4.2 Nested Objects

- Parent object nodes do NOT occupy bytes
- Child fields are laid out sequentially
- Nesting depth MUST be tracked

---

### 4.3 Arrays

- Use `occurrenceCount.max` to determine repetition count
- If `occurrenceCount.max` is undefined, treat as `1`
- If `occurrenceCount == 0`, skip the field entirely
- Array expansion MUST be deterministic and finite

---

### 4.4 Transitory Fields

The following fields:

- `groupId`
- `occurrenceCount`

MUST:
- participate in offset calculation
- be included in offset entries

They MUST NOT be filtered or treated specially in this task.

---

## 5. Error Handling (Deterministic)

The following conditions MUST produce ERROR-level issues:

- Missing field length
- Negative field length
- `occurrenceCount < 0`

Internal arithmetic MUST use a type that prevents overflow.

No error recovery or best-effort behavior is allowed.

---

## 6. Output Rules (Strict)

Exactly one output artifact MUST be produced:

### `offset-table.json`

The output MUST be:
- machine-readable only
- deterministic in ordering
- free of narrative text

---

### 6.1 OffsetEntry Requirements

Each offset entry MUST contain exactly:

- fieldPath (unique, deterministic)
- offset
- length
- endOffset
- nestingLevel

---

### 6.2 OffsetTable Requirements

The offset table MUST contain exactly:

- messageType
- totalLength
- ordered list of offset entries

No additional fields are permitted.

---

## 7. Explicitly Out of Scope

The following MUST NOT be implemented:

- Runtime message comparison
- Encoding concerns (EBCDIC, ASCII, UTF variants)
- Business rules (BLANK handling, enum mapping)
- Error tolerance for malformed specification
- Report formatting or visualization

---

## 8. Exit Code Policy

Exit codes are fixed and non-extensible:

- 0  → PASS (offset calculation completed without ERROR)
- 45 → FAIL (one or more ERROR-level issues)

No other exit codes are allowed.

---

## 9. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] spec-tree.json read successfully
- [ ] Deterministic traversal order established
- [ ] Offsets calculated starting from zero
- [ ] Nested object layout handled correctly
- [ ] Array expansion handled deterministically
- [ ] groupId / occurrenceCount included correctly
- [ ] ERROR conditions detected as specified
- [ ] offset-table.json generated
- [ ] Deterministic output guaranteed
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
