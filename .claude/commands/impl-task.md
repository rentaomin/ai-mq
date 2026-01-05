---
description: Implement exactly one task file (T-XXX-*.md) using mq-tool-impl-agent with minimal context
---

You must use the agent: mq-tool-impl-agent.

## Input
- $ARGUMENTS must be a task spec file path, e.g.:
  /spec/plan/task-pack/v1/tasks/T-104-seg-level-parser.md

## Task
1) Read ONLY the provided task spec file.
2) Read only minimal dependent source files needed to implement it (interfaces/models/errors/config).
3) Implement the task and required tests.
4) Do not implement any other tasks.
5) Report changed files and acceptance criteria mapping.

## Output
- Make code changes in the repository.
- Provide a concise summary + file list + acceptance checklist.
