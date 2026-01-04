---
description: Generate an implementation task pack (DAG + parallel/serial tasks) from the approved architecture document into a specified directory
---

You must use the agent: mq-tool-delivery-planner.

## Arguments Convention
$ARGUMENTS supports:
- One argument: <ARCH_PATH>
- Two arguments: <ARCH_PATH> <TASK_DIR>

If <TASK_DIR> is not provided, default to: `spec/plan/task-pack`

If <ARCH_PATH> is not provided, attempt auto-detect in this order:
1) `spec/design/architecture.md`

If none exist, stop and print an error listing the expected paths.

## Task Directory Rules
- All outputs MUST be written under <TASK_DIR>.
- Create directories if needed.
- Do not write task outputs elsewhere.

## Task
Using ONLY the architecture document:
1) Build a minimal-dependency task DAG with maximum parallelism.
2) Split tasks into serial vs parallel groups.
3) Ensure every task is implementation-agent executable (unambiguous inputs/outputs/AC/deps).
4) Include gates (Gate 0/1/2/3) and map tasks to these gates.
5) Include deterministic build/audit/atomic-output requirements as acceptance criteria where relevant.

## Output (Write Files)
Under <TASK_DIR>, write:
- README.md
- dag.md (Mermaid graph + table)
- serial.md
- parallel.md
- tasks/ (one Markdown file per task: T-xxx-<short-name>.md)

## Hard Constraints
- Minimize task size and dependencies.
- No "umbrella tasks" without concrete artifacts.
- If you encounter ambiguity, create an explicit ADR/Spike task instead of guessing.
