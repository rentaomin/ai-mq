### Implementation Constraints (Hard Rules)

- This task implements a *Minimal Consistency Gate* across artifacts, not an exhaustive semantic verifier.
- Do NOT parse XML/Java/OpenAPI directly in this task.
    - Only consume the normalized outputs produced by T-301/T-302/T-303 JSON files.
- Do NOT implement "fix suggestions", "golden report stability", or large-format report rendering.
- Keep the algorithm deterministic and linear-time (O(N)) on field count.

### Minimal Consistency Scope (P0)

Implement only the following checks:

P0-1 Field presence alignment
- For each fieldPath in the union set across artifacts:
    - report MISSING_FIELD if any artifact lacks it
- Special rule:
    - groupId and occurrenceCount are allowed only in XML result
    - They MUST be ignored when checking Java/OpenAPI presence

P0-2 Type consistency (mapping-based, not strict string equality)
- Use `consistency.type-mapping-rules` to normalize types into a canonical type key, e.g.:
    - XML xs:string -> CANON=string
    - Java String -> CANON=string
    - OpenAPI string -> CANON=string
- Compare canonical types; if mismatch => TYPE_MISMATCH
- If a type is unknown (no mapping):
    - strict-mode=true: ERROR
    - strict-mode=false: WARNING

P0-3 Structure consistency (array vs object vs primitive)
- Compare the shape category per fieldPath:
    - XML: derive from validator output metadata (e.g., maxOccurs>1 => array)
    - Java: List<T> => array; class => object; scalar => primitive
    - OpenAPI: type=array/object/scalar
- Mismatch => STRUCTURE_MISMATCH

P0-4 Required flag consistency (minimal)
- Only check "required vs optional" alignment where available:
    - XML minOccurs==1 => required
    - Java presence of NotNull (or equivalent marker) => required (if provided by T-302 output)
    - OpenAPI required contains field => required
- If any artifact lacks required metadata:
    - do not fail; record WARNING "REQUIRED_UNKNOWN" (unless strict-mode requires ERROR)

### Explicitly Out of Scope (Deferred to a future task or later extension)

- Enum value set comparison
- Default value comparison (beyond presence)
- Length/range constraints comparison
- Report fix suggestions
- Golden file/report-format stability tests
- Deep nesting semantic equivalence beyond shape/type/required

### Input Contract Assumptions

- Each validator JSON file includes a normalized field list/tree where each node has at minimum:
    - fieldPath (string, unique)
    - type (artifact-specific string)
    - shape (primitive/object/array) OR enough metadata to derive it
    - required flag (optional)
      If any required input fields are missing, return VALIDATION_ERROR with a clear message.

### Output Rules

- Always produce both:
    - `{output-dir}/validation/consistency-report.json`
    - `{output-dir}/validation/consistency-report.md` (minimal table view)
- JSON report must be machine-readable and stable.
- Markdown report may be minimal: summary + table (category, fieldPath, xml/java/openapi).
  Do NOT implement elaborate formatting.

### Exit Code Rules (Keep Simple)

- 0: PASS (no ERROR)
- 44: FAIL (one or more ERROR issues)
  Do NOT implement category-specific codes (41~43) in this task to reduce branching.
  (If required later, can be added without breaking report schema.)

### Acceptance Criteria (Minimal Gate)

1) ✅ Reads all three validator JSON files successfully; missing file => FAIL with actionable message.
2) ✅ Builds canonical maps keyed by fieldPath for each artifact.
3) ✅ Detects and reports (at least) these categories:
    - MISSING_FIELD
    - TYPE_MISMATCH (mapping-based)
    - STRUCTURE_MISMATCH (array/object/primitive)
4) ✅ Correctly ignores `groupId` and `occurrenceCount` when checking Java/OpenAPI presence.
5) ✅ Respects configuration:
    - strict-mode true => unknown types are ERROR
    - strict-mode false => unknown types are WARNING
    - ignore-fields removes those paths from all checks
6) ✅ Produces both outputs:
    - consistency-report.json
    - consistency-report.md (summary + table)
7) ✅ Deterministic: same inputs => byte-identical JSON output (stable ordering of issues).
8) ✅ Exit code:
    - 0 when no ERROR issues
    - 44 when any ERROR exists

### Tests (Minimal, No Large Suites)

Unit tests only (no golden report formatting tests):

1) missing field detection
- xml has fieldA, java has fieldA, openapi missing fieldA => one ERROR category MISSING_FIELD

2) type mismatch detection (mapping-based)
- xml xs:string, java String, openapi integer => TYPE_MISMATCH ERROR

3) structure mismatch detection
- java List<T> but openapi scalar => STRUCTURE_MISMATCH ERROR

4) ignore-fields works
- fieldPath in ignore list => not reported even if mismatched

5) strict-mode behavior
- unknown type with strict-mode=true => ERROR
- unknown type with strict-mode=false => WARNING
