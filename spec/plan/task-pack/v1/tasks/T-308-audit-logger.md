# T-308 审计日志记录器

## Goal

实现审计日志记录器 (AuditLogger)，记录工具运行过程中的所有关键操作、输入输出制品信息、版本信息、验证结果，确保生成过程的完全可追溯性和审计合规性。

---

## In Scope / Out of Scope

### In Scope

- 记录工具启动信息（版本、时间、配置）
- 记录输入文件信息（Excel 路径、SHA-256 哈希、修改时间）
- 记录输出制品信息（文件路径、SHA-256 哈希、大小）
- 记录验证结果摘要（一致性验证、报文验证）
- 记录运行时环境信息（Java 版本、操作系统、工作目录）
- 支持多种输出格式（JSON、人类可读文本）
- 生成审计报告摘要
- 支持日志级别配置（DEBUG、INFO、WARN、ERROR）
- 集成原子输出管理器（T-307）的事务信息
- 提供审计日志的确定性输出（相同操作产生相同日志结构）

### Out of Scope

- 日志轮转和归档策略（由运维层处理）
- 远程日志收集（如 ELK、Splunk 集成）
- 实时告警
- 性能指标采集（APM）
- 日志加密

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `OutputManifest` | T-307 | 原子输出管理器生成的输出清单 |
| `ConsistencyReport` | T-304 | 跨制品一致性验证报告 |
| `MessageValidationResult` | T-306 | 报文验证结果（可选） |
| `config/audit.yaml` | 配置 | 审计日志配置（输出路径、级别、格式） |
| 运行时参数 | CLI | 命令行参数、输入文件路径 |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| AuditLogger.java | `src/main/java/com/example/mqtool/audit/AuditLogger.java` | 审计日志记录器主类 |
| AuditRecord.java | `src/main/java/com/example/mqtool/audit/AuditRecord.java` | 审计记录数据模型 |
| AuditEvent.java | `src/main/java/com/example/mqtool/audit/AuditEvent.java` | 审计事件枚举 |
| AuditContext.java | `src/main/java/com/example/mqtool/audit/AuditContext.java` | 审计上下文（线程安全） |
| AuditReportGenerator.java | `src/main/java/com/example/mqtool/audit/AuditReportGenerator.java` | 审计报告生成器 |
| audit-log.json | `{output-dir}/audit/audit-log.json` | JSON 格式审计日志 |
| audit-log.txt | `{output-dir}/audit/audit-log.txt` | 人类可读审计日志 |
| audit-summary.md | `{output-dir}/audit/audit-summary.md` | 审计摘要报告（Markdown） |
| 单元测试 | `src/test/java/com/example/mqtool/audit/AuditLoggerTest.java` | 审计日志测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-307 | 原子输出管理器 | 需要 OutputManifest 记录输出制品信息 |
| T-304 | 跨制品一致性验证 | 需要一致性验证结果 |
| T-306 | 报文验证器 | 需要报文验证结果（可选） |
| T-003 | 配置加载器 | 读取审计日志配置 |
| T-006 | 版本注册器 | 获取工具版本信息 |

---

## Implementation Notes

### 审计事件类型

```java
public enum AuditEvent {
    // 生命周期事件
    TOOL_STARTED,           // 工具启动
    TOOL_COMPLETED,         // 工具正常完成
    TOOL_FAILED,            // 工具执行失败

    // 输入事件
    INPUT_FILE_LOADED,      // 输入文件加载
    INPUT_FILE_HASH_COMPUTED, // 输入文件哈希计算完成
    CONFIG_LOADED,          // 配置加载

    // 处理事件
    PARSE_STARTED,          // 解析开始
    PARSE_COMPLETED,        // 解析完成
    GENERATION_STARTED,     // 生成开始
    GENERATION_COMPLETED,   // 生成完成

    // 验证事件
    VALIDATION_STARTED,     // 验证开始
    VALIDATION_PASSED,      // 验证通过
    VALIDATION_FAILED,      // 验证失败

    // 输出事件
    TRANSACTION_STARTED,    // 事务开始
    TRANSACTION_COMMITTED,  // 事务提交
    TRANSACTION_ROLLED_BACK, // 事务回滚
    OUTPUT_FILE_WRITTEN,    // 输出文件写入
    MANIFEST_GENERATED      // 清单生成
}
```

### 审计记录数据结构

```java
public class AuditRecord {
    private String recordId;           // 记录 ID (UUID)
    private Instant timestamp;         // 时间戳 (ISO-8601)
    private AuditEvent event;          // 事件类型
    private String level;              // DEBUG / INFO / WARN / ERROR
    private String category;           // 分类: INPUT / PROCESS / VALIDATION / OUTPUT
    private String message;            // 人类可读消息
    private Map<String, Object> data;  // 结构化数据
    private String correlationId;      // 关联 ID（同一次运行的所有记录共享）
}
```

### 审计上下文（线程安全）

```java
public class AuditContext {
    private static final ThreadLocal<AuditContext> CURRENT = new ThreadLocal<>();

    private String runId;              // 运行 ID
    private String correlationId;      // 关联 ID
    private Instant startTime;         // 开始时间
    private String toolVersion;        // 工具版本
    private String inputFilePath;      // 输入文件路径
    private String inputFileHash;      // 输入文件哈希
    private String outputDir;          // 输出目录
    private Map<String, String> environment; // 环境信息

    public static AuditContext current() {
        return CURRENT.get();
    }

    public static void init(String runId, String toolVersion) {
        AuditContext ctx = new AuditContext();
        ctx.runId = runId;
        ctx.correlationId = runId; // 默认使用 runId 作为 correlationId
        ctx.startTime = Instant.now();
        ctx.toolVersion = toolVersion;
        ctx.environment = captureEnvironment();
        CURRENT.set(ctx);
    }

    private static Map<String, String> captureEnvironment() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("java.version", System.getProperty("java.version"));
        env.put("os.name", System.getProperty("os.name"));
        env.put("os.version", System.getProperty("os.version"));
        env.put("user.dir", System.getProperty("user.dir"));
        env.put("file.encoding", System.getProperty("file.encoding"));
        return env;
    }
}
```

### 审计日志记录器

```java
public class AuditLogger {

    private final List<AuditRecord> records = new ArrayList<>();
    private final AuditConfig config;
    private final Object lock = new Object();

    public void log(AuditEvent event, String message) {
        log(event, message, Map.of());
    }

    public void log(AuditEvent event, String message, Map<String, Object> data) {
        AuditRecord record = new AuditRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setTimestamp(Instant.now());
        record.setEvent(event);
        record.setLevel(determineLevel(event));
        record.setCategory(determineCategory(event));
        record.setMessage(message);
        record.setData(data);

        AuditContext ctx = AuditContext.current();
        if (ctx != null) {
            record.setCorrelationId(ctx.getCorrelationId());
        }

        synchronized (lock) {
            records.add(record);
        }

        // 实时输出到控制台（如配置）
        if (config.isConsoleOutput()) {
            printToConsole(record);
        }
    }

    /**
     * 记录工具启动
     */
    public void logToolStarted(String inputFile, String outputDir, String[] args) {
        AuditContext ctx = AuditContext.current();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("inputFile", inputFile);
        data.put("outputDir", outputDir);
        data.put("arguments", args);
        data.put("version", ctx.getToolVersion());
        data.put("environment", ctx.getEnvironment());

        log(AuditEvent.TOOL_STARTED,
            String.format("工具启动 - 版本: %s, 输入: %s", ctx.getToolVersion(), inputFile),
            data);
    }

    /**
     * 记录输入文件信息
     */
    public void logInputFileLoaded(String filePath, String sha256, long size, Instant modifiedTime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filePath", filePath);
        data.put("sha256", sha256);
        data.put("size", size);
        data.put("modifiedTime", modifiedTime.toString());

        log(AuditEvent.INPUT_FILE_LOADED,
            String.format("输入文件加载完成: %s (SHA-256: %s)", filePath, sha256.substring(0, 16) + "..."),
            data);
    }

    /**
     * 记录验证结果
     */
    public void logValidationResult(String validationType, boolean passed,
                                     int errorCount, int warningCount) {
        AuditEvent event = passed ? AuditEvent.VALIDATION_PASSED : AuditEvent.VALIDATION_FAILED;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("validationType", validationType);
        data.put("passed", passed);
        data.put("errorCount", errorCount);
        data.put("warningCount", warningCount);

        log(event,
            String.format("%s验证%s - 错误: %d, 警告: %d",
                validationType, passed ? "通过" : "失败", errorCount, warningCount),
            data);
    }

    /**
     * 记录输出制品
     */
    public void logOutputManifest(OutputManifest manifest) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", manifest.getTransactionId());
        data.put("totalFiles", manifest.getTotalFiles());
        data.put("totalSize", manifest.getTotalSize());

        List<Map<String, Object>> fileList = new ArrayList<>();
        for (OutputManifest.FileEntry entry : manifest.getFiles()) {
            Map<String, Object> fileData = new LinkedHashMap<>();
            fileData.put("path", entry.getPath());
            fileData.put("size", entry.getSize());
            fileData.put("sha256", entry.getSha256());
            fileData.put("type", entry.getType());
            fileList.add(fileData);
        }
        data.put("files", fileList);

        log(AuditEvent.MANIFEST_GENERATED,
            String.format("输出清单生成 - 文件数: %d, 总大小: %d bytes",
                manifest.getTotalFiles(), manifest.getTotalSize()),
            data);
    }
}
```

### 审计报告生成器

```java
public class AuditReportGenerator {

    /**
     * 生成 JSON 格式审计日志
     */
    public void generateJsonLog(List<AuditRecord> records, Path outputPath) {
        AuditLogJson logJson = new AuditLogJson();
        logJson.setGeneratedAt(Instant.now());
        logJson.setRecordCount(records.size());
        logJson.setRecords(records);

        // 使用确定性 JSON 序列化（排序 key，稳定格式）
        String json = DeterministicJsonWriter.toJson(logJson);
        Files.writeString(outputPath, json, StandardCharsets.UTF_8);
    }

    /**
     * 生成人类可读审计日志
     */
    public void generateTextLog(List<AuditRecord> records, Path outputPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("===========================================\n");
        sb.append("         MQ Tool 审计日志\n");
        sb.append("===========================================\n\n");

        for (AuditRecord record : records) {
            sb.append(formatRecord(record)).append("\n");
        }

        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 生成审计摘要报告（Markdown）
     */
    public void generateSummaryReport(List<AuditRecord> records,
                                       AuditContext context,
                                       Path outputPath) {
        StringBuilder md = new StringBuilder();
        md.append("# 审计摘要报告\n\n");

        // 运行信息
        md.append("## 运行信息\n\n");
        md.append("| 项目 | 值 |\n");
        md.append("|------|----|\n");
        md.append(String.format("| 运行 ID | `%s` |\n", context.getRunId()));
        md.append(String.format("| 工具版本 | `%s` |\n", context.getToolVersion()));
        md.append(String.format("| 开始时间 | %s |\n", context.getStartTime()));
        md.append(String.format("| 输入文件 | `%s` |\n", context.getInputFilePath()));
        md.append(String.format("| 输入哈希 | `%s` |\n", context.getInputFileHash()));
        md.append(String.format("| 输出目录 | `%s` |\n", context.getOutputDir()));
        md.append("\n");

        // 环境信息
        md.append("## 运行环境\n\n");
        md.append("| 项目 | 值 |\n");
        md.append("|------|----|\n");
        for (Map.Entry<String, String> entry : context.getEnvironment().entrySet()) {
            md.append(String.format("| %s | `%s` |\n", entry.getKey(), entry.getValue()));
        }
        md.append("\n");

        // 事件统计
        md.append("## 事件统计\n\n");
        Map<AuditEvent, Long> eventCounts = records.stream()
            .collect(Collectors.groupingBy(AuditRecord::getEvent, Collectors.counting()));
        md.append("| 事件类型 | 次数 |\n");
        md.append("|----------|------|\n");
        for (Map.Entry<AuditEvent, Long> entry : eventCounts.entrySet()) {
            md.append(String.format("| %s | %d |\n", entry.getKey(), entry.getValue()));
        }
        md.append("\n");

        // 验证结果
        md.append("## 验证结果\n\n");
        List<AuditRecord> validationRecords = records.stream()
            .filter(r -> r.getCategory().equals("VALIDATION"))
            .collect(Collectors.toList());
        for (AuditRecord record : validationRecords) {
            String status = record.getEvent() == AuditEvent.VALIDATION_PASSED ? "PASS" : "FAIL";
            md.append(String.format("- **%s**: %s\n",
                record.getData().get("validationType"), status));
        }
        md.append("\n");

        // 输出制品
        md.append("## 输出制品\n\n");
        AuditRecord manifestRecord = records.stream()
            .filter(r -> r.getEvent() == AuditEvent.MANIFEST_GENERATED)
            .findFirst()
            .orElse(null);
        if (manifestRecord != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files =
                (List<Map<String, Object>>) manifestRecord.getData().get("files");
            md.append("| 文件 | 类型 | 大小 | SHA-256 |\n");
            md.append("|------|------|------|---------||\n");
            for (Map<String, Object> file : files) {
                md.append(String.format("| `%s` | %s | %d | `%s...` |\n",
                    file.get("path"),
                    file.get("type"),
                    file.get("size"),
                    ((String) file.get("sha256")).substring(0, 16)));
            }
        }

        Files.writeString(outputPath, md.toString(), StandardCharsets.UTF_8);
    }
}
```

### 审计日志配置

**config/audit.yaml:**
```yaml
audit:
  enabled: true
  level: INFO                     # DEBUG / INFO / WARN / ERROR
  consoleOutput: true             # 是否输出到控制台
  outputDir: "{output-dir}/audit" # 审计日志输出目录
  formats:
    - json                        # JSON 格式日志
    - text                        # 人类可读文本
    - summary                     # Markdown 摘要
  includeEnvironment: true        # 是否记录环境信息
  includeStackTrace: false        # 是否记录异常堆栈（仅 DEBUG）
  timestampFormat: ISO_INSTANT    # 时间戳格式
```

### 审计日志输出示例

**audit-log.json:**
```json
{
  "generatedAt": "2026-01-05T16:00:00Z",
  "recordCount": 15,
  "records": [
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440001",
      "timestamp": "2026-01-05T15:50:00Z",
      "event": "TOOL_STARTED",
      "level": "INFO",
      "category": "LIFECYCLE",
      "message": "工具启动 - 版本: 1.0.0, 输入: spec/input/message-spec.xlsx",
      "correlationId": "run-20260105-155000",
      "data": {
        "inputFile": "spec/input/message-spec.xlsx",
        "outputDir": "output/",
        "arguments": ["--format", "all"],
        "version": "1.0.0",
        "environment": {
          "java.version": "11.0.12",
          "os.name": "Windows 10",
          "os.version": "10.0",
          "user.dir": "G:\\projects\\mq-tool",
          "file.encoding": "UTF-8"
        }
      }
    },
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440002",
      "timestamp": "2026-01-05T15:50:01Z",
      "event": "INPUT_FILE_LOADED",
      "level": "INFO",
      "category": "INPUT",
      "message": "输入文件加载完成: spec/input/message-spec.xlsx (SHA-256: a3f2e1d4c5b6a7...)",
      "correlationId": "run-20260105-155000",
      "data": {
        "filePath": "spec/input/message-spec.xlsx",
        "sha256": "a3f2e1d4c5b6a7890123456789abcdef0123456789abcdef0123456789abcdef",
        "size": 125678,
        "modifiedTime": "2026-01-04T10:30:00Z"
      }
    }
  ]
}
```

**audit-summary.md:**
```markdown
# 审计摘要报告

## 运行信息

| 项目 | 值 |
|------|----|| 运行 ID | `run-20260105-155000` |
| 工具版本 | `1.0.0` |
| 开始时间 | 2026-01-05T15:50:00Z |
| 输入文件 | `spec/input/message-spec.xlsx` |
| 输入哈希 | `a3f2e1d4c5b6a7890123456789abcdef...` |
| 输出目录 | `output/` |

## 运行环境

| 项目 | 值 |
|------|----|| java.version | `11.0.12` |
| os.name | `Windows 10` |
| os.version | `10.0` |
| user.dir | `G:\projects\mq-tool` |

## 事件统计

| 事件类型 | 次数 |
|----------|------|
| TOOL_STARTED | 1 |
| INPUT_FILE_LOADED | 1 |
| PARSE_COMPLETED | 1 |
| GENERATION_COMPLETED | 4 |
| VALIDATION_PASSED | 3 |
| TRANSACTION_COMMITTED | 1 |
| MANIFEST_GENERATED | 1 |
| TOOL_COMPLETED | 1 |

## 验证结果

- **一致性验证**: PASS
- **XML Bean 验证**: PASS
- **OpenAPI 验证**: PASS

## 输出制品

| 文件 | 类型 | 大小 | SHA-256 |
|------|------|------|---------|
| `xml/outbound-bean.xml` | XML | 15234 | `a3f2e1d4c5b6a7...` |
| `xml/inbound-bean.xml` | XML | 14892 | `b4e3d2c1a0f9...` |
| `java/Request.java` | JAVA | 8456 | `c5d4e3f2a1b0...` |
| `openapi/api.yaml` | YAML | 22341 | `d6e5f4c3b2a1...` |
```

---

## Acceptance Criteria

1. [ ] 能够初始化审计上下文，生成唯一运行 ID
2. [ ] 能够记录工具启动事件，包含版本、参数、环境信息
3. [ ] 能够记录输入文件信息，包含 SHA-256 哈希
4. [ ] 能够记录各阶段处理事件（解析、生成、验证）
5. [ ] 能够记录验证结果（通过/失败 + 错误/警告数）
6. [ ] 能够记录事务事件（开始、提交、回滚）
7. [ ] 能够记录输出清单信息
8. [ ] 生成 JSON 格式审计日志，格式确定性可验证
9. [ ] 生成人类可读文本格式审计日志
10. [ ] 生成 Markdown 格式审计摘要报告
11. [ ] 审计记录包含关联 ID，同一次运行的记录可追溯
12. [ ] 支持日志级别过滤配置
13. [ ] 线程安全，支持并发记录

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 初始化审计上下文 | 生成唯一 runId，捕获环境信息 |
| 记录工具启动事件 | 事件类型为 TOOL_STARTED，data 包含完整信息 |
| 记录输入文件加载 | 记录文件路径、SHA-256、大小、修改时间 |
| 记录验证通过 | 事件类型为 VALIDATION_PASSED |
| 记录验证失败 | 事件类型为 VALIDATION_FAILED |
| 记录输出清单 | 记录所有文件的路径、大小、哈希 |
| 生成 JSON 日志 | JSON 格式正确，可反序列化 |
| 生成文本日志 | 格式清晰，包含时间戳和事件 |
| 生成摘要报告 | Markdown 格式正确，包含所有章节 |
| 关联 ID 一致性 | 同一次运行的所有记录 correlationId 相同 |
| 日志级别过滤 | INFO 级别下不输出 DEBUG 记录 |
| 并发记录安全性 | 多线程记录不丢失、不重复 |

### 集成测试

| 测试场景 | 预期结果 |
|----------|---------|
| 完整生成流程审计 | 所有关键事件被记录 |
| 验证失败场景审计 | 记录 VALIDATION_FAILED 和 TRANSACTION_ROLLED_BACK |
| 异常场景审计 | 记录 TOOL_FAILED 和错误详情 |
| 审计日志与 manifest 一致性 | 审计日志中的输出文件与 manifest.json 一致 |

### Golden 测试

| 测试场景 | 预期结果 |
|----------|---------|
| JSON 日志格式稳定性 | 结构与 golden 文件一致 |
| 摘要报告格式稳定性 | Markdown 结构与 golden 文件一致 |

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| 审计日志过大 | 设置最大记录数限制，超出时归档旧记录 |
| 敏感信息泄露 | 不记录完整文件内容，仅记录元数据 |
| 时间戳不一致 | 使用 UTC 时间，统一 ISO-8601 格式 |
| 哈希计算失败 | 记录失败原因，标记为 UNKNOWN |
| 审计日志写入失败 | 降级到控制台输出，不中断主流程 |
| 线程竞争 | 使用同步锁保护记录列表 |
| 环境信息获取失败 | 记录为 N/A，不中断审计 |
| 大文件哈希计算慢 | 使用流式计算，显示进度 |
| 审计上下文未初始化 | 检查并抛出明确错误，提示调用 init |

---

**文档结束**
