## Execution Constraints (Mandatory)

This task is a **static structure validator**, not an execution or compilation task.

You MUST comply with all constraints below. Violations invalidate the result.

### Forbidden Actions (Strict)

- DO NOT include any code templates or example implementations
- DO NOT include test case code (unit or integration)
- DO NOT include inline code snippets (Java, regex, pseudo-code)
- DO NOT generate summary paragraphs or explanations
- DO NOT execute, simulate, or suggest:
    - compilation
    - test execution
    - build commands
- DO NOT read unrelated files or scan directories
- DO NOT read architecture.md, dag.md, requirements.md
- DO NOT infer behavior beyond explicitly listed rules

---

## Allowed Scope Only

Implement a **Java Bean structural validator** that performs static analysis based on parsed source structure.

Validation is limited to:

1. Class and field naming conventions
2. Presence / absence of forbidden fields
3. Structural alignment with intermediate JSON Tree
4. Type declaration consistency using JavaTypeMapper
5. Nested class presence and hierarchy

No runtime behavior is evaluated.

---

## File Access Rules (Hard Limit)

You may read ONLY the following inputs:

- Java source files explicitly provided as validation targets
- `spec-tree.json` (structure reference only)
- `JavaTypeMapper` (type mapping rules only)
- `naming-conventions.yaml` (pattern definitions only)

Do NOT read:
- other Java files
- generated code outside the target set
- configuration not explicitly listed above

---

## Validation Rules (Declarative)

Validation must be rule-driven and deterministic.

- Naming rules are applied via configuration, not hardcoded logic
- Forbidden fields:
    - `groupId`
    - `occurrenceCount`
      must not appear in any Java Bean
- Nested class structure must mirror JSON Tree hierarchy
- Type declarations must match mapper results
- Enum helper method existence is validated by name only (no body inspection)

---

## Output Rules (Strict)

Output MUST be **structured only**, no narrative text.

Produce:

- A machine-readable validation result object
- Each validation issue includes:
    - filePath
    - fieldPath or classPath
    - ruleId
    - severity (ERROR / WARNING)

DO NOT:
- generate prose explanations
- generate formatted reports
- include remediation text

---

## Completion Checklist (Only Output)

At completion, output **ONLY** the following checklist with true/false marks:

- [ ] Naming conventions validated
- [ ] Forbidden fields excluded
- [ ] Type mapping consistency validated
- [ ] Nested structure validated
- [ ] Enum helper presence validated
- [ ] Validation result produced in structured form
- [ ] No unrelated files read
- [ ] No test code generated
- [ ] No compilation or execution attempted
