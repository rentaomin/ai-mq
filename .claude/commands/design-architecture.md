---
description: Generate an implementation-ready architecture design document from the approved requirements analysis document
---

You must use the agent: mq-tool-architect.

## Input Resolution Rules
- Primary input is `$ARGUMENTS`.
- If `$ARGUMENTS` is a file path, read that file as the approved requirements source.
- If `$ARGUMENTS` is empty, attempt to locate the requirements document using this priority order:
    1) `spec/requirement/需求分析文档.md`
    2) `spec/requirement/需求文档.md`
    3) `需求分析文档.md`
    4) `需求文档.md`
- If none exist, stop and output a short error message listing the expected paths.

## Mandatory Reference Files (If Present)
If these files exist in the repository, you MUST read them and incorporate constraints/conventions into the design.
If any are missing, list them under "Assumptions / Missing References" in the output.

- `.claude/commands/reference/create_app.xlsx`
- `.claude/commands/reference/ISM v2.0 FIX mapping.xlsx`
- `.claude/commands/reference/outbound-converter.xml`
- `.claude/commands/reference/inbound-converter.xml`
- `.claude/commands/reference/sample_code.md`

## Task
Using ONLY the approved requirements document (and the reference artifacts if present), produce a concrete,
engineering-deliverable Architecture & Technical Design document that is:
- unambiguous
- boundary-clear (what is in scope vs out of scope)
- directly implementable
- deterministic (same inputs => same outputs)
- audit-ready (traceable, reproducible)
- aligned to requirement IDs (FR/NFR/VR/AC)

The design MUST explicitly define:
1) module decomposition and responsibilities
2) Intermediate JSON Tree as single source of truth (schema, versioning, ordering guarantees, metadata)
3) parser algorithm for seg lvl nesting + array/object detection via occurrenceCount
4) generator designs (XML/Java/OAS) and their mapping rules
5) cross-artifact consistency validation (mismatch classification, reports, exit codes)
6) spec-vs-message fixed-length validator (offset strategy, field extraction, report format)
7) auditability and rollback/atomic output strategy
8) CLI/config design and precedence rules

Do NOT introduce new features that are not in the requirements. Mark unknowns as "To Be Confirmed".

## Output
Write the architecture document to:
- `spec/design/architecture.md` (create directories if needed)

The output must be Markdown and follow a consistent heading structure suitable for engineering review.
Include diagrams (Mermaid/ASCII) when they reduce ambiguity.
