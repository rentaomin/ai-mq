# T-309 CLI Entry Point

This task defines the **CLI Entry Point Gate**.

The task specifies **command-line interaction rules and orchestration boundaries**
for invoking the tool in a controlled, deterministic manner.

This specification is **strict and authoritative**.
Any behavior not explicitly allowed is forbidden.

---

## 1. Task Nature

This task defines **CLI interface behavior and routing rules**.

It is NOT:
- an implementation example
- a code template
- a command-line framework tutorial
- a test generator
- a documentation generator
- a compiler, runner, or executor

---

## 2. Execution Constraints (Hard Rules)

### 2.1 Forbidden Actions (Absolute)

The following actions are strictly prohibited:

- DO NOT include any code templates or example implementations
- DO NOT include inline code blocks of any kind
- DO NOT include code comments or annotation templates
- DO NOT generate test case code or test descriptions
- DO NOT generate summary or explanatory paragraphs
- DO NOT generate remediation or usage suggestions
- DO NOT simulate, execute, or suggest:
    - compilation
    - test execution
    - validation commands
- DO NOT read or parse:
    - XML
    - Java source files
    - OpenAPI files
- DO NOT read or reference:
    - architecture.md
    - dag.md
    - requirements.md
    - experience.md
- DO NOT scan directories or infer repository structure
- DO NOT introduce CLI options, commands, or flows not explicitly listed

Any violation invalidates the task result.

---

## 3. Allowed Inputs (Hard Limit)

You may read **ONLY** the following inputs:

- Raw command-line arguments (`args[]`)
- Application configuration file (explicitly provided path only)
- Environment variables explicitly mapped to configuration keys
- Version registry (read-only)
- Audit logger interface (from T-308)

Explicitly forbidden:
- reading generated artifacts
- reading validator outputs
- reading runtime logs
- reading any additional configuration sources

---

## 4. CLI Scope (Gate-Level Only)

### 4.1 Supported Commands

The CLI MUST support exactly the following commands:

- generate
- validate
- parse
- version
- help

No additional commands are permitted.

---

### 4.2 Global Option Handling

The CLI MUST support global option handling with the following rules:

- Command-line options override configuration file values
- Configuration file values override environment variables
- Unknown options MUST result in an error exit

---

### 4.3 Command Routing Rules

- Exactly one command MUST be resolved per invocation
- Invalid or missing commands MUST result in an error exit
- Sub-command execution MUST be delegated without inlining logic

---

### 4.4 Flow Orchestration Rules

The CLI MAY orchestrate the following high-level phases:

- input validation
- configuration loading
- audit initialization
- sub-command execution
- audit finalization

The CLI MUST NOT:
- implement business logic
- perform generation or validation internally
- bypass gate validations

---

### 4.5 Audit Integration Rules

- Audit logging MUST be initialized before command execution
- Audit finalization MUST occur exactly once per invocation
- Audit MUST record command name, input paths, output paths, and exit code

---

## 5. Explicitly Out of Scope

The following MUST NOT be implemented:

- Interactive REPL behavior
- GUI or Web interfaces
- Plugin or extension mechanisms
- Background or daemon modes
- Runtime progress visualization logic
- Output content inspection

---

## 6. Output Rules (Strict)

The CLI MUST produce **only terminal output** appropriate to the selected command.

No structured files are produced directly by this task.

The CLI MUST NOT:
- generate reports
- format validation results
- write output artifacts

---

## 7. Exit Code Policy

Exit codes MUST be deterministic and mapped strictly to outcomes.

The CLI MUST:
- propagate sub-command exit codes unchanged
- use dedicated exit codes for argument or configuration errors

No implicit exit code mapping is allowed.

---

## 8. Completion Checklist (Only Allowed Final Output)

The final response MUST consist of **this checklist only**:

- [ ] Command-line arguments parsed
- [ ] Global options merged with configuration
- [ ] Command resolved deterministically
- [ ] Invalid commands rejected correctly
- [ ] Sub-command routed correctly
- [ ] Audit initialized before execution
- [ ] Audit finalized after execution
- [ ] Exit code returned correctly
- [ ] No test code generated
- [ ] No unrelated files read
- [ ] No compilation or execution attempted
