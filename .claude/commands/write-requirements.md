---
description: Generate requirements.md from MQ message spec + examples using requirements-doc-writer agent
---

# Requirements Doc Generator

You must use the existing agent: requirements-doc-writer.

## Goal
Given the requirement description (from $ARGUMENTS) and the provided example files in this repo, produce an implementation-ready requirements document in Markdown and write it to `需求分析文档.md` at the project root (or `spec/requirement/需求分析文档.md` if that folder exists).

## Inputs
1) Requirement description:
- Use `$ARGUMENTS` as the primary input.
- If `$ARGUMENTS` looks like a file path (e.g., ends with .md/.txt), read that file and treat its content as the requirement description.

2) Example / reference files (read all if present):
- Message spec example excel: `.claude/commands/reference/create_app.xlsx`
- Shared header excel: `.claude/commands/reference v2.0 FIX mapping.xlsx`
- Request XML bean example: `.claude/commands/reference/outbound-converter.xml`
- Response XML bean example: `.claude/commands/reference/inbound-converter.xml`
- Template processing code example: `.claude/commands/reference/sample_code.md`

If any file is missing, note it explicitly in the output under "Assumptions / Not Provided".

## Required output behavior
- Produce a single primary requirements document following the requirements-doc-writer agent’s standard structure.
- Incorporate constraints and domain rules from the examples (XML bean patterns, naming rules, OAS 3.x YAML splitting conventions, MQ fixed-length constraints, groupId/occurrenceCount handling, diff.md and intermediate JSON tree requirements, audit traceability, and message-vs-spec comparison requirement).
- Do NOT invent details not implied by the requirement description or examples. Mark unknowns as "To Be Confirmed".

## Mandatory sections to include in 需求分析文档.md (Hard Requirements)
The generated `需求分析文档.md` MUST include explicit, well-defined sections for:

1) Non-Goals / Out of Scope
- Enumerate what is explicitly NOT being built in this project
- Include exclusions that reduce scope creep (e.g., not building MQ runtime, not redesigning enterprise security component, etc.)

2) Acceptance Criteria (Testable)
   Provide a checklist of acceptance criteria that can be objectively verified, including at minimum:
- Excel parsing correctness (hierarchy, arrays vs objects, ordering)
- Intermediate JSON tree file generation (location, schema, metadata, determinism)
- XML bean generation correctness (order/type/default/occurrence rules, inclusion of groupId/occurrenceCount only where required)
- Java bean generation correctness (camelCase renaming rules, type mapping, exclusions of groupId/occurrenceCount)
- OpenAPI YAML generation correctness (OAS 3.x compliance; split files for headers/request/response/errors; references consistent)
- Cross-artifact consistency checks (yaml/java/xml alignment: names, types, required/defaults)
- diff.md generation (table output: original vs renamed fields)
- Audit trail / traceability (who/when/what inputs; hash/versioning; reproducibility)
- Spec-vs-real-message validation capability (compare actual message payload against Excel spec field-by-field with actionable reports)

## Deliverables
Write the main requirements document to:
- `spec/requirement/需求分析文档.md` if `docs/` exists, otherwise `需求分析文档.md`.

Within the requirements, enumerate expected generated artifacts as outputs of the tool (even if they are not created by this command), including:
- `diff.md`
- intermediate JSON tree file (path must be specified)
- generated XML bean files
- generated Java bean source files
- generated OpenAPI YAML folder structure
- audit logs / trace artifacts
- spec-vs-message comparison reports

## Execution steps
1) Read the requirement description.
2) Read the example files and extract concrete conventions and constraints.
3) Generate `需求分析文档.md` (Chinese) with clear scope, functional/non-functional requirements, data/model mapping rules, validation rules, audit requirements, acceptance criteria, and test strategy.
4) Ensure the document is consistent, unambiguous, and engineering-deliverable.