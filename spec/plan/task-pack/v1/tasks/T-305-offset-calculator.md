### Implementation Constraints (Hard Rules)

- Offset calculation is purely static and structural.
- Use ONLY data from spec-tree.json.
- Do NOT inspect or parse actual message payloads.
- Do NOT infer dynamic field lengths at runtime.

### Offset Calculation Rules (Strict)

1. Offset always starts at 0.
2. Field offset = previous field (offset + length).
3. For nested objects:
    - Children are laid out sequentially.
    - Parent node itself does NOT occupy bytes.
4. For arrays:
    - Use occurrenceCount.max (or 1 if undefined).
    - Expand offsets deterministically.
    - occurrenceCount = 0 → skip field entirely.

5. Transitory fields (groupId / occurrenceCount):
    - Participate in offset calculation.
    - Are included in OffsetEntry.
    - MUST NOT be filtered here.

### Output Guarantees

Each OffsetEntry MUST include:
- fieldPath (unique, deterministic)
- offset
- length
- endOffset
- nestingLevel

OffsetTable MUST include:
- messageType
- totalLength
- ordered list of entries (stable order)

### Explicitly Out of Scope

- Runtime message comparison
- Encoding concerns (EBCDIC/ASCII)
- Business rules (BLANK, enum mapping)
- Error tolerance for malformed spec-tree

### Error Handling

- Missing length → ERROR
- Negative length → ERROR
- occurrenceCount < 0 → ERROR
- Use long internally to prevent overflow

### Test Strategy (Minimal)

Implement ONLY:
- flat fields
- single nested object
- array of primitives
- array of objects
- empty message

No payload-based tests.
