---
description: Incrementally generate missing task specs (T-xxx.md) in small batches to avoid token limits
---

You must use the agent: mq-tool-delivery-planner.

## Arguments
Usage:
/plan-tasks-batch <DAG_PATH> <TASK_DIR> <TASK_ID_1> [TASK_ID_2 ... TASK_ID_N]

Defaults:
- If <DAG_PATH> not provided: `spec/plan/task-pack/v1/dag.md`
- If <TASK_DIR> not provided: `spec/plan/task-pack/v1/tasks`

Hard limit:
- Generate at most 5 tasks per run. If more are provided, only process the first 5.

## Inputs
- Read ONLY the DAG file (Mermaid + dependency table) from <DAG_PATH>.
- Do NOT read architecture.md unless absolutely necessary.
- If a task file already exists in <TASK_DIR>, skip it.

## Task
For each provided Task ID:
1) Derive the task spec from the dependency table and project conventions in the DAG.
2) Produce a single Markdown file in <TASK_DIR> named:
   `T-XXX-<short-name>.md`
3) Ensure each task spec is implementation-agent executable:
    - Goal
    - In Scope / Out of Scope
    - Inputs
    - Outputs (paths)
    - Dependencies (Task IDs)
    - Implementation Notes
    - Acceptance Criteria (testable)
    - Tests
    - Risks / Edge Cases

## Output
Write only the requested task files. Do NOT regenerate dag.md/parallel.md/serial.md/README.md.
