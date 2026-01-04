# Project AI Usage Rules (CLAUDE.md)

This repository contains a highly structured, engineering-driven system
focused on specification-based development and automation.

Claude must follow the rules below when assisting in this project.

---

## 1. Primary Project Context

This project is centered around:
- Parsing MQ message specification Excel files
- Generating XML bean definitions, Java bean models, and OpenAPI (OAS 3.x) YAML files
- Enforcing strict consistency across Excel specs, XML, Java, and API definitions
- Automating a previously manual, error-prone, template-based development workflow
- Supporting auditability, traceability, and spec-vs-message validation

The domain is **technical and implementation-oriented**, not marketing or product ideation.

---

## 2. Requirements Documentation Rule (Very Important)

When the task involves:
- Creating
- Refining
- Reviewing
- Formalizing
- Or deriving

**any form of requirements or specifications**

Claude MUST prefer using the `requirements-doc-writer` agent.

If a slash command such as `/write-requirements` exists, it should be used instead of ad-hoc responses.

---

## 3. Engineering-First Behavior Rules

Claude must:
- Avoid inventing requirements not supported by user input or example artifacts
- Explicitly mark assumptions, unknowns, and risks
- Treat Excel specs, XML files, YAML files, and sample code as authoritative references
- Prioritize precision, determinism, and testability over verbosity

Claude must NOT:
- Use vague or marketing-style language
- Collapse complex rules into generic summaries
- Silently “fix” ambiguous or inconsistent specifications

---

## 4. File-Driven Reasoning

When example files are present in the repository:
- Claude should read and reason from them
- Constraints and conventions inferred from examples must be reflected in outputs
- Missing or conflicting examples must be explicitly noted

---

## 5. Output Expectations

All major outputs should:
- Be structured
- Be suitable for direct engineering execution
- Serve as a single source of truth for product, engineering, and QA
- Support long-term maintenance and audit review

---

## 5.1 Spec-to-Intermediate JSON Requirement (Mandatory)

When tasks involve parsing MQ message spec Excel files (request/response/shared header):
Claude must treat the "Intermediate JSON Tree" as a first-class deliverable and ensure the requirements capture it explicitly.

The intermediate JSON tree MUST:
- Preserve correct nesting / hierarchy according to Seg lvl and object/group relationships
- Represent arrays vs objects based on occurrenceCount (e.g., 0..N / 1..N as arrays; 1..1 as object)
- Preserve field order exactly as defined in the Excel spec (order is authoritative)
- Retain source metadata for auditability (e.g., sheet name, row index, original field name, length, datatype, default/hardcode rules, occurrenceCount)
- Exclude `groupId` and `occurrenceCount` from Java bean and OpenAPI YAML outputs, but keep them in XML bean generation and in intermediate JSON metadata when needed

Output expectations for the intermediate JSON tree:
- It must be serialized into a dedicated file (e.g., `parser/schema/spec-tree.json` or equivalent)
- The requirements must define the schema of this JSON tree and how downstream generators (XML/Java/YAML) consume it
- Any transformation rules (e.g., field renaming to camelCase, special-character handling) must be traceable back to the source Excel entries


---

## 6. Language

- Requirements documents: Chinese (unless explicitly requested otherwise)
- Technical identifiers, schemas, and code: English

---

## 7. When in Doubt

If information is missing or unclear:
- Do not guess
- Surface the uncertainty clearly
- Ask targeted, high-impact clarification questions
