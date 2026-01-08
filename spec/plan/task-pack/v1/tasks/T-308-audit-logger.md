# T-308 Audit Logger

This task defines an **Audit Logging Gate**.

The task records **deterministic, structured audit information**
covering tool execution, inputs, outputs, validation results,
and transactional state, to ensure full traceability.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task performs **audit record aggregation and persistence**.

It is NOT:
- an implementation example
- a logging framework showcase
- a code template
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
- DO NOT generate test case code (unit, integration, E2E, or golden)
- DO NOT generate summary or explanatory paragraphs
- DO NOT generate remediation or recovery suggestions
- DO NOT simulate, execute, or suggest:
    - compilation
    - test execution
    - validation commands
- DO NOT read or parse:
    - XML beans
    - Java source code
    - OpenAPI YAML
- DO NOT read or reference:
    - architecture.md
    - dag.md
    - requirements.md
    - experience.md
- DO NOT scan directories or infer repository context
- DO NOT introduce audit rules not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following inputs:

- Output manifest (from T-307)
- Consistency validation result (from T-304)
- Message validation result (from T-306, optional)
- Audit configuration (output path, level, format flags)
- Runtime invocation parameters (provided explicitly)

Explicitly forbidden:
- reading generated source files
- reading validator internals
- reading any additional configuration sources

---

## 4. Audit Scope (Gate-Level Only)

### 4.1 Required Audit Categories

The audit log MUST record events in the following categories:

- Lifecycle  
  (tool start, tool completion, tool failure)

- Input  
  (input file path, hash, size, modification time)

- Process  
  (parse started/completed, generation started/completed)

- Validation  
  (consistency validation result, message validation result)

- Output  
  (transaction started, committed, rolled back, manifest generated)

---

### 4.2 Determinism Rules

- Audit records MUST be written in a stable, deterministic order
- Identical inputs and execution paths MUST produce
  identical audit log structure
- Timestamp format MUST be consistent across all records

---

### 4.3 Correlation Rules

- All audit records within a single run MUST share
  a single correlation identifier
- The correlation identifier MUST be propagated
  across all audit events

---

## 5. Explicitly Out of Scope

The following MUST NOT be implemented:

- Log rotation or archival
- Remote log shipping
- Real-time alerting
- Performance metrics collection
- Log encryption
- Stack trace recording
- Output diff or comparison

---

## 6. Output Rules (Strict)

Exactly the following outputs MUST be produced:

- `audit-log.json`
    - machine-readable only
    - deterministic ordering
    - no narrative text

- `audit-log.txt`
    - plain structured text
    - no summary sections
    - no explanatory paragraphs

No additional outputs are permitted.

---

## 7. Exit Code Policy

Exit codes are fixed and non-extensible:

- 0   → AUDIT_LOG_CREATED
- 71  → AUDIT_INPUT_MISSING
- 72  → AUDIT_WRITE_FAILED
- 73  → AUDIT_VALIDATION_FAILED

No other exit codes are allowed.

---

## 8. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] Output manifest read successfully
- [ ] Consistency validation result recorded
- [ ] Message validation result recorded (if provided)
- [ ] Lifecycle events recorded
- [ ] Input file metadata recorded
- [ ] Process events recorded
- [ ] Output transaction events recorded
- [ ] audit-log.json generated
- [ ] audit-log.txt generated
- [ ] Deterministic audit structure guaranteed
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
