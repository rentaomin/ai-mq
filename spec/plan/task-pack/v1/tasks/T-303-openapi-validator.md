## Execution Constraints (Mandatory)

This task is a **static OpenAPI structure validator**.

All rules below are **hard constraints**. Any violation invalidates the result.

### Forbidden (Strict)

- DO NOT include any code templates or example implementations
- DO NOT include inline code blocks (Java / YAML / pseudo-code)
- DO NOT generate test case code (unit or integration)
- DO NOT generate summary, explanation, or descriptive paragraphs
- DO NOT execute, simulate, or suggest:
    - compilation
    - test execution
    - validation commands
- DO NOT read unrelated files or directories
- DO NOT read:
    - architecture.md
    - dag.md
    - requirements.md
- DO NOT infer behavior outside explicitly listed rules

---

## Allowed Scope Only

Implement a **declarative OpenAPI validator** that performs **static validation** on generated OpenAPI (OAS 3.x) artifacts.

Validation is limited to:

1. YAML syntactic validity
2. OpenAPI document structural completeness
3. Schema definition presence and correctness
4. `$ref` reference resolution (local only)
5. Forbidden field exclusion
6. Type mapping consistency
7. Structural alignment with intermediate JSON Tree

No runtime API behavior is evaluated.

---

## File Access Rules (Hard Limit)

You may read **ONLY** the following inputs:

- `output/openapi/openapi.yaml`
- `output/openapi/schemas/*.yaml`
- `output/json/spec-tree.json`
- `OpenApiTypeMapper` (type rules only)

You MUST NOT read:
- any other YAML or Java files
- generated code outside OpenAPI scope
- configuration files not explicitly listed above

---

## Validation Rules (Declarative)

All validation must be **rule-based and deterministic**.

1. YAML syntax
    - Detect parsing errors only

2. OpenAPI root structure
    - Required fields must exist:
        - openapi
        - info
        - components
    - paths may be empty but must exist

3. Schema definitions
    - Each schema must declare an explicit type
    - Object schemas must define properties

4. `$ref` resolution
    - Only local file references are validated
    - Target schema must exist
    - Circular references are detected and reported

5. Forbidden fields
    - `groupId`
    - `occurrenceCount`
      These fields MUST NOT appear in any schema

6. Type mapping
    - Schema types must match `OpenApiTypeMapper` rules
    - Comparison is mapping-based, not string-literal

7. Structural alignment
    - Schema nesting must align with JSON Tree hierarchy
    - Order is not validated
    - Presence and shape only

---

## Output Rules (Strict)

Output MUST be **structured only**.

Produce:
- A machine-readable validation result object

Each validation issue MUST include:
- filePath
- schemaPath or fieldPath
- ruleId
- severity (ERROR | WARNING)

DO NOT:
- generate prose explanations
- generate formatted reports
- include remediation guidance

---

## Exit Code Policy

- SUCCESS (0): no ERROR-level issues
- VALIDATION_ERROR (1): one or more ERROR issues

---

## Completion Checklist (Only Output)

At completion, output **ONLY** the following checklist with true/false marks:

- [ ] YAML syntax validated
- [ ] OpenAPI root structure validated
- [ ] Schema definitions validated
- [ ] $ref references validated
- [ ] Forbidden fields excluded
- [ ] Type mapping consistency validated
- [ ] Structural alignment with JSON Tree validated
- [ ] Structured validation result produced
- [ ] No test code generated
- [ ] No compilation or execution attempted
- [ ] No unrelated files read