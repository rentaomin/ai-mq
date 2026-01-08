# T-307 Atomic Output Manager

This task defines an **Atomic Output Control Gate**.

The task ensures that all generated artifacts are written in a **fully atomic manner**:
either all outputs are committed successfully, or no output state is changed.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task defines **output transaction control rules**.

It is NOT:
- an implementation example
- a code template
- a file generator
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
- DO NOT introduce output rules not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following inputs:

- Consistency validation result (from T-304)
- Message validation result (from T-306, if enabled)
- In-memory generator outputs (provided explicitly to this task)
- Output configuration (output directory, atomic mode flags)

Explicitly forbidden:
- reading generated files from disk
- reading validator internals
- reading any additional configuration sources

---

## 4. Atomic Output Rules (Gate-Level Only)

### 4.1 Transaction Semantics

- All output operations MUST be executed within a single transaction
- A transaction has exactly two terminal states:
    - COMMITTED
    - ROLLED_BACK
- Partial commit is forbidden

---

### 4.2 Commit Preconditions

A transaction may be committed ONLY if:

- Consistency validation result indicates PASS
- Message validation result indicates PASS (if enabled)
- Output directory is writable
- Sufficient disk space is available

Failure of any precondition MUST prevent commit.

---

### 4.3 Atomic Write Strategy

- All outputs MUST be written to a temporary location first
- No file may be written directly to the target output directory
- Commit MUST replace the target output atomically
- On failure, the original output state MUST remain unchanged

---

### 4.4 Rollback Rules

- On any failure before commit:
    - All temporary outputs MUST be removed
    - Target output directory MUST remain unchanged
- A committed transaction MUST NOT be rolled back

---

### 4.5 Output Manifest Rules

- A manifest MUST be generated as part of a successful commit
- The manifest MUST include:
    - transaction identifier
    - generation timestamp
    - list of output files
    - file size and hash for each output
- The manifest MUST be written atomically with other outputs

---

## 5. Explicitly Out of Scope

The following MUST NOT be implemented:

- Distributed transactions
- Database or remote filesystem handling
- Version control integration
- Runtime output repair
- Output diff or comparison
- Report visualization or formatting

---

## 6. Output Rules (Strict)

Exactly one logical output set MUST be produced on success:

- Atomic output directory (final state)
- Output manifest file

No additional outputs are permitted.

---

## 7. Exit Code Policy

Exit codes are fixed and non-extensible:

- 0   → COMMITTED (success)
- 61  → OUTPUT_NOT_WRITABLE
- 62  → INSUFFICIENT_DISK_SPACE
- 63  → CONSISTENCY_VALIDATION_FAILED
- 64  → MESSAGE_VALIDATION_FAILED
- 65  → ATOMIC_COMMIT_FAILED
- 66  → ROLLBACK_FAILED

No other exit codes are allowed.

---

## 8. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] Consistency validation result evaluated
- [ ] Message validation result evaluated (if enabled)
- [ ] Output directory preconditions checked
- [ ] Transaction created
- [ ] Outputs written to temporary location only
- [ ] Commit performed atomically
- [ ] Rollback handled correctly on failure
- [ ] Output manifest generated
- [ ] Deterministic output state guaranteed
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
