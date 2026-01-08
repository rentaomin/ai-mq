# T-310 End-to-End Verification

This task defines the **End-to-End Verification Gate**.

The task verifies that the system, as a whole, satisfies
**completeness, determinism, atomicity, and auditability**
across a full execution cycle.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task performs **gate-level end-to-end verification**.

It is NOT:
- an implementation example
- a test case generator
- a test framework tutorial
- a code template
- a performance benchmark
- a CI/CD configuration task
- a compiler, runner, or executor

---

## 2. Execution Constraints (Hard Rules)

### 2.1 Forbidden Actions (Absolute)

The following actions are strictly prohibited:

- DO NOT include any code templates or example implementations
- DO NOT include inline code blocks of any kind
- DO NOT include code comments or annotation templates
- DO NOT generate test case code (unit, integration, E2E, golden)
- DO NOT generate summary or explanatory paragraphs
- DO NOT generate remediation or recovery suggestions
- DO NOT simulate, execute, or suggest:
    - compilation
    - test execution
    - build or validation commands
- DO NOT read or parse:
    - Java source code
    - XML beans
    - OpenAPI YAML
- DO NOT read or reference:
    - architecture.md
    - dag.md
    - requirements.md
    - experience.md
- DO NOT scan directories or infer repository context
- DO NOT introduce verification dimensions not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following artifacts:

- Audit log output (from T-308)
- Output manifest (from T-307)
- Consistency validation result (from T-304)
- Message validation result (from T-306)
- CLI invocation metadata (from T-309)
- Explicitly provided test fixture identifiers (names only, no file parsing)

Explicitly forbidden:
- reading source artifacts
- reading generated files directly
- reading test resources or fixtures
- reading CI configuration
- reading performance metrics

---

## 4. Verification Scope (Gate-Level Only)

### 4.1 Completeness Verification

Verify that a successful execution produced:

- Output manifest
- Audit log
- All expected artifact categories declared in the manifest

No content inspection is permitted.

---

### 4.2 Determinism Verification

Verify determinism by checking:

- Identical input identifiers produce identical:
    - output manifest structure
    - audit log structure (excluding timestamps)

No hash recomputation or file comparison is allowed.

---

### 4.3 Atomicity Verification

Verify atomic behavior by checking audit and manifest records:

- On failure:
    - no committed output state is recorded
- On success:
    - exactly one committed transaction is recorded

No filesystem inspection is allowed.

---

### 4.4 Audit Integrity Verification

Verify that audit records include, at minimum:

- tool start event
- tool completion or failure event
- validation results
- transaction outcome

Audit content semantics are not evaluated.

---

### 4.5 CLI Integration Verification

Verify that:

- a valid command was resolved
- exactly one command was executed
- exit code propagated correctly

No command re-execution is permitted.

---

## 5. Explicitly Out of Scope

The following MUST NOT be implemented:

- File-by-file comparison
- Golden file management
- Performance or load measurement
- Memory or resource profiling
- Error message content validation
- Cross-run timing analysis
- Environment or OS compatibility checks

---

## 6. Output Rules (Strict)

Exactly one output artifact MUST be produced:

- `e2e-verification-result.json`
    - machine-readable only
    - deterministic ordering
    - no narrative text

No Markdown reports or human-readable summaries are allowed.

---

## 7. Exit Code Policy

Exit codes are fixed and non-extensible:

- 0   → E2E_VERIFICATION_PASS
- 81  → E2E_COMPLETENESS_FAILED
- 82  → E2E_DETERMINISM_FAILED
- 83  → E2E_ATOMICITY_FAILED
- 84  → E2E_AUDIT_INTEGRITY_FAILED
- 85  → E2E_CLI_INTEGRATION_FAILED

No other exit codes are allowed.

---

## 8. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] Audit log read successfully
- [ ] Output manifest read successfully
- [ ] Consistency validation result evaluated
- [ ] Message validation result evaluated
- [ ] Completeness verified
- [ ] Determinism verified
- [ ] Atomicity verified
- [ ] Audit integrity verified
- [ ] CLI integration verified
- [ ] e2e-verification-result.json generated
- [ ] Deterministic verification outcome guaranteed
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
