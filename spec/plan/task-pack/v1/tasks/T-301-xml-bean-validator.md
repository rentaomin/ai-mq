### Implementation Constraints (Hard Rules)

- This validator is a *local XML structural validator*, NOT a cross-artifact validator.
- Do NOT implement full JSON Tree vs XML field-by-field comparison (handled by T-304).
- Do NOT generate large test suites.
- Focus on fail-fast structural correctness.

### Scope Clarification (Must Follow)

Implement ONLY the following checks:

1. XML well-formed validation
    - File exists
    - Non-empty
    - Parsable by standard XML parser (DOM or SAX)

2. Root-level contract validation
    - Root element exists
    - Required attributes present:
        - messageType
        - version

3. Node-level structural validation
    - Each field node must have:
        - name (non-empty, camelCase)
        - type (resolved XML type)
    - groupId and occurrenceCount:
        - MUST be allowed in XML
        - MUST NOT be validated against Java/OpenAPI here

4. XML type mapping validation
    - Validate declared XML type is resolvable via XmlTypeMapper
    - Do NOT compare against Java/YAML types

### Explicitly Out of Scope

- Order validation
- Cross-artifact field consistency
- JSON Tree full traversal
- Runtime message content validation

### Error Handling Rules

- Collect errors; do not throw on first failure
- Each error must include:
    - filePath
    - xpath or element name
    - errorCode
    - human-readable message

### Exit Code Policy

- SUCCESS (0): no ERROR-level issues
- VALIDATION_ERROR (4): one or more ERROR issues

### Test Strategy (Minimal)

Implement ONLY:
- valid XML → pass
- malformed XML → fail
- missing root attribute → fail
- unknown XML type → fail

No exhaustive test matrices.
