*\ MQ Message Spec Excel 自动化工具 - 任务包 v1

**创建日期**: 2026-01-04
**基于架构文档**: architecture.md v2.2
**任务包版本**: v1.0

---

## 1. 概述

本任务包将架构设计文档分解为可由自动化编码代理（coding agent）独立执行的任务单元。任务按照最小依赖原则拆分，支持最大化并行执行。

### 1.1 系统目标

基于 Excel 规范文件自动生成：
1. 中间 JSON Tree（单一数据源）
2. XML Bean 定义（outbound-converter.xml / inbound-converter.xml）
3. Java Bean 类（Request/Response POJO）
4. OpenAPI YAML 文件（api.yaml + schemas）
5. 跨制品一致性验证报告
6. 字段映射表（diff.md）

### 1.2 关键约束

- **确定性输出**: 相同输入 + 相同版本 = 相同输出（字节级别）
- **顺序保留**: Excel 字段顺序在所有制品中严格保持
- **可审计性**: 输入、版本、环境、输出完整可追溯
- **原子输出**: 任何生成步骤失败则全部回滚，无部分输出

---

## 2. Gate 定义

任务按 Gate 分组，每个 Gate 代表一个里程碑检查点：

| Gate | 名称 | 目标 | 验收条件 |
|------|------|------|---------|
| Gate 0 | 基础设施与契约 | ADR 决策锁定、核心接口定义 | 所有接口编译通过，配置加载正常 |
| Gate 1 | 最小垂直切片 MVP | Excel 解析 → JSON Tree → XML 生成（仅 Request） | 端到端生成 outbound-converter.xml |
| Gate 2 | 响应与 OpenAPI 切片 | Response 支持、Java Bean、OpenAPI 生成 | 所有制品生成且字段一致 |
| Gate 3 | 验证器与 CI 硬化 | 跨制品验证、报文验证、审计、回滚 | 完整验证通过，CI 集成 |

---

## 3. 任务 ID 编号规范

- **T-0xx**: Gate 0 任务（基础设施、契约）
- **T-1xx**: Gate 1 任务（Excel 解析、JSON Tree、XML 生成）
- **T-2xx**: Gate 2 任务（Response、Java Bean、OpenAPI）
- **T-3xx**: Gate 3 任务（验证器、审计、集成）

---

## 4. 文件结构

```
task-pack/v1/
├── README.md          # 本文件：总览
├── dag.md             # 任务 DAG 图与依赖表
├── serial.md          # 串行任务执行计划
├── parallel.md        # 并行任务执行计划
└── tasks/
    ├── T-001-project-setup.md
    ├── T-002-core-interfaces.md
    ├── T-003-config-loader.md
    ├── ...
    └── T-3xx-xxx.md
```

---

## 5. 任务执行规则

### 5.1 执行前置条件

1. 检查所有 `Dependencies` 中列出的任务是否已完成
2. 验证所需输入文件存在
3. 确认配置项可用

### 5.2 执行后验证

1. 检查 `Outputs` 中列出的文件是否已生成
2. 运行 `Acceptance Criteria` 中的测试用例
3. 如有失败，参考 `Risks / Edge Cases` 进行排查

### 5.3 失败处理

- 任务失败时记录详细错误信息
- 不继续执行依赖此任务的下游任务
- 生成失败报告供人工审查

---

## 6. 技术栈

| 类别 | 技术选型 |
|------|---------|
| 语言 | Java 11+ |
| 构建 | Maven |
| Excel 解析 | Apache POI |
| JSON 处理 | Gson |
| XML 处理 | JAXB / DOM |
| Java 代码生成 | JavaPoet |
| YAML 处理 | SnakeYAML |
| OpenAPI 验证 | Swagger Parser |
| Java 语法验证 | JavaParser |
| 单元测试 | JUnit 5 + Mockito |

---

## 7. 关键路径

```
T-001 → T-002 → T-003 → T-101 → T-102 → T-103 → T-104 → T-105
                                   ↓
                    ┌──────────────┴──────────────┐
                    ↓                              ↓
              T-201 (Response)               T-202 (Java Gen)
                    ↓                              ↓
              T-203 (Inbound XML)            T-204 (OpenAPI)
                    └──────────────┬──────────────┘
                                   ↓
                             T-301 (Validator)
                                   ↓
                    ┌──────────────┴──────────────┐
                    ↓                              ↓
              T-302 (Msg Validator)          T-303 (Audit)
                    └──────────────┬──────────────┘
                                   ↓
                             T-304 (Integration)
```

---

## 8. 快速开始

1. 阅读 `dag.md` 了解任务依赖关系
2. 查看 `serial.md` 确定必须串行执行的任务
3. 查看 `parallel.md` 识别可并行执行的任务组
4. 按 Gate 顺序执行 `tasks/` 目录下的任务

---

## 9. 相关文档

- 架构设计文档: `spec/design/architecture.md`
- 需求分析文档: `spec/requirements/requirements.md`
- 参考文件目录: `.claude/commands/reference/`

---

**文档结束**
