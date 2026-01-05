# T-309 CLI 入口点

## Goal

实现命令行界面 (CLI) 入口点 (MqToolCli)，作为工具的主入口，负责参数解析、流程编排、子命令路由、帮助信息展示、错误码返回。提供用户友好的命令行交互体验，支持多种操作模式（完整生成、仅解析、仅验证等）。

---

## In Scope / Out of Scope

### In Scope

- 命令行参数解析与验证
- 子命令路由（generate、validate、parse、version、help）
- 配置文件加载与命令行参数合并
- 主流程编排（调用各模块协同工作）
- 进度显示与状态输出
- 错误处理与退出码返回
- 帮助信息与使用说明
- 版本信息输出
- 日志级别控制
- 审计日志集成（调用 T-308）

### Out of Scope

- GUI 界面
- Web API 服务
- 守护进程模式
- 交互式 REPL
- 插件扩展机制

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| 命令行参数 | 用户 | args[] 数组 |
| `config/app.yaml` | T-003 | 应用配置文件 |
| 环境变量 | 系统 | 可覆盖配置项 |
| `AuditLogger` | T-308 | 审计日志记录器 |
| `VersionRegistry` | T-006 | 版本注册器 |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| MqToolCli.java | `src/main/java/com/example/mqtool/cli/MqToolCli.java` | CLI 主类（含 main 方法） |
| CliArguments.java | `src/main/java/com/example/mqtool/cli/CliArguments.java` | 命令行参数数据模型 |
| CliParser.java | `src/main/java/com/example/mqtool/cli/CliParser.java` | 参数解析器 |
| SubCommand.java | `src/main/java/com/example/mqtool/cli/SubCommand.java` | 子命令接口 |
| GenerateCommand.java | `src/main/java/com/example/mqtool/cli/commands/GenerateCommand.java` | 生成命令 |
| ValidateCommand.java | `src/main/java/com/example/mqtool/cli/commands/ValidateCommand.java` | 验证命令 |
| ParseCommand.java | `src/main/java/com/example/mqtool/cli/commands/ParseCommand.java` | 解析命令 |
| ProgressReporter.java | `src/main/java/com/example/mqtool/cli/ProgressReporter.java` | 进度报告器 |
| 单元测试 | `src/test/java/com/example/mqtool/cli/MqToolCliTest.java` | CLI 测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-308 | 审计日志记录器 | 集成审计日志功能 |
| T-003 | 配置加载器 | 加载应用配置 |
| T-006 | 版本注册器 | 获取版本信息 |
| T-107 | Excel 解析器集成 | 调用 Excel 解析 |
| T-111 | Outbound XML 生成器 | 调用 XML 生成 |
| T-202 | Inbound XML 生成器 | 调用 XML 生成 |
| T-206 | 枚举辅助方法生成 | 调用 Java Bean 生成 |
| T-209 | OpenAPI Schema 拆分 | 调用 OpenAPI 生成 |
| T-304 | 跨制品一致性验证 | 调用一致性验证 |
| T-307 | 原子输出管理器 | 调用原子输出 |

---

## Implementation Notes

### 命令行语法

```
mq-tool <command> [options] [arguments]

Commands:
  generate    根据 Excel 规范生成所有制品
  validate    验证已生成的制品
  parse       仅解析 Excel 并输出 JSON Tree
  version     显示版本信息
  help        显示帮助信息

Global Options:
  -c, --config <file>     配置文件路径 (默认: config/app.yaml)
  -v, --verbose           详细输出模式
  -q, --quiet             静默模式 (仅输出错误)
  --log-level <level>     日志级别: DEBUG, INFO, WARN, ERROR
  --no-audit              禁用审计日志
  --color                 强制彩色输出
  --no-color              禁用彩色输出

Generate Options:
  -i, --input <file>      输入 Excel 文件路径 (必须)
  -o, --output <dir>      输出目录 (默认: output/)
  -f, --format <types>    输出格式: all, xml, java, openapi, json (逗号分隔)
  --request-only          仅处理 Request
  --response-only         仅处理 Response
  --skip-validation       跳过验证步骤
  --dry-run               模拟运行，不实际写入文件

Validate Options:
  -d, --dir <dir>         待验证的制品目录
  --message <file>        待验证的报文文件 (可选)

Parse Options:
  -i, --input <file>      输入 Excel 文件路径 (必须)
  -o, --output <file>     输出 JSON 文件路径 (默认: spec-tree.json)
```

### 使用示例

```bash
# 完整生成
mq-tool generate -i spec/message-spec.xlsx -o output/ -f all

# 仅生成 XML
mq-tool generate -i spec/message-spec.xlsx -o output/ -f xml

# 仅生成 Java Bean 和 OpenAPI
mq-tool generate -i spec/message-spec.xlsx -o output/ -f java,openapi

# 模拟运行
mq-tool generate -i spec/message-spec.xlsx -o output/ --dry-run

# 验证制品
mq-tool validate -d output/

# 验证报文
mq-tool validate -d output/ --message input/message.bin

# 仅解析 Excel
mq-tool parse -i spec/message-spec.xlsx -o spec-tree.json

# 显示版本
mq-tool version

# 显示帮助
mq-tool help
mq-tool help generate
```

### CLI 主类

```java
public class MqToolCli {

    private static final String TOOL_NAME = "mq-tool";
    private static final int SUCCESS = 0;
    private static final int ERROR_INVALID_ARGS = 1;
    private static final int ERROR_CONFIG = 2;
    private static final int ERROR_PARSE = 10;
    private static final int ERROR_GENERATE = 20;
    private static final int ERROR_VALIDATE = 30;

    private final CliParser cliParser;
    private final ConfigLoader configLoader;
    private final VersionRegistry versionRegistry;
    private final AuditLogger auditLogger;

    public static void main(String[] args) {
        MqToolCli cli = new MqToolCli();
        int exitCode = cli.run(args);
        System.exit(exitCode);
    }

    public int run(String[] args) {
        try {
            // 1. 解析命令行参数
            CliArguments cliArgs = cliParser.parse(args);

            // 2. 处理全局选项
            if (cliArgs.isShowHelp()) {
                printHelp(cliArgs.getCommand());
                return SUCCESS;
            }
            if (cliArgs.isShowVersion()) {
                printVersion();
                return SUCCESS;
            }

            // 3. 加载配置
            Config config = loadConfig(cliArgs);

            // 4. 初始化审计
            if (!cliArgs.isNoAudit()) {
                initAudit(cliArgs, config);
            }

            // 5. 路由到子命令
            SubCommand command = resolveCommand(cliArgs.getCommand());
            if (command == null) {
                printError("未知命令: " + cliArgs.getCommand());
                printUsage();
                return ERROR_INVALID_ARGS;
            }

            // 6. 执行命令
            int result = command.execute(cliArgs, config);

            // 7. 完成审计
            if (!cliArgs.isNoAudit()) {
                finalizeAudit(result);
            }

            return result;

        } catch (CliParseException e) {
            printError("参数错误: " + e.getMessage());
            printUsage();
            return ERROR_INVALID_ARGS;
        } catch (ConfigException e) {
            printError("配置错误: " + e.getMessage());
            return ERROR_CONFIG;
        } catch (Exception e) {
            printError("执行错误: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            return ERROR_GENERAL;
        }
    }

    private void initAudit(CliArguments cliArgs, Config config) {
        String runId = generateRunId();
        String version = versionRegistry.getToolVersion();
        AuditContext.init(runId, version);

        auditLogger.logToolStarted(
            cliArgs.getInputFile(),
            cliArgs.getOutputDir(),
            cliArgs.getRawArgs()
        );
    }
}
```

### 参数数据模型

```java
public class CliArguments {
    // 全局选项
    private String command;              // 子命令
    private String configFile;           // 配置文件路径
    private boolean verbose;             // 详细模式
    private boolean quiet;               // 静默模式
    private String logLevel;             // 日志级别
    private boolean noAudit;             // 禁用审计
    private boolean showHelp;            // 显示帮助
    private boolean showVersion;         // 显示版本
    private boolean color;               // 彩色输出

    // Generate 选项
    private String inputFile;            // 输入文件
    private String outputDir;            // 输出目录
    private Set<String> formats;         // 输出格式
    private boolean requestOnly;         // 仅 Request
    private boolean responseOnly;        // 仅 Response
    private boolean skipValidation;      // 跳过验证
    private boolean dryRun;              // 模拟运行

    // Validate 选项
    private String validateDir;          // 验证目录
    private String messageFile;          // 报文文件

    // Parse 选项
    private String parseOutput;          // 解析输出文件

    // 原始参数
    private String[] rawArgs;
}
```

### 生成命令实现

```java
public class GenerateCommand implements SubCommand {

    private final ExcelParser excelParser;
    private final DeterministicJsonWriter jsonWriter;
    private final OutboundXmlGenerator outboundXmlGenerator;
    private final InboundXmlGenerator inboundXmlGenerator;
    private final JavaBeanGenerator javaBeanGenerator;
    private final OpenApiGenerator openApiGenerator;
    private final ConsistencyValidator consistencyValidator;
    private final AtomicOutputManager outputManager;
    private final ProgressReporter progressReporter;
    private final AuditLogger auditLogger;

    @Override
    public int execute(CliArguments args, Config config) {
        progressReporter.start("开始处理 " + args.getInputFile());

        try {
            // 1. 解析 Excel
            progressReporter.update("正在解析 Excel...");
            auditLogger.log(AuditEvent.PARSE_STARTED, "开始解析 Excel");

            MessageModel model = excelParser.parse(args.getInputFile());

            auditLogger.log(AuditEvent.PARSE_COMPLETED, "Excel 解析完成");
            progressReporter.update("Excel 解析完成");

            // 2. 开始输出事务
            OutputTransaction tx = outputManager.beginTransaction(args.getOutputDir());
            auditLogger.log(AuditEvent.TRANSACTION_STARTED, "输出事务开始");

            try {
                // 3. 生成 JSON Tree
                if (shouldGenerate(args, "json")) {
                    progressReporter.update("正在生成 JSON Tree...");
                    String jsonTree = jsonWriter.serialize(model);
                    tx.addFile("json/spec-tree.json", jsonTree.getBytes(UTF_8));
                }

                // 4. 生成 XML
                if (shouldGenerate(args, "xml")) {
                    progressReporter.update("正在生成 XML Bean...");

                    if (!args.isResponseOnly()) {
                        String outboundXml = outboundXmlGenerator.generate(model.getRequest());
                        tx.addFile("xml/outbound-converter.xml", outboundXml.getBytes(UTF_8));
                    }

                    if (!args.isRequestOnly()) {
                        String inboundXml = inboundXmlGenerator.generate(model.getResponse());
                        tx.addFile("xml/inbound-converter.xml", inboundXml.getBytes(UTF_8));
                    }
                }

                // 5. 生成 Java Bean
                if (shouldGenerate(args, "java")) {
                    progressReporter.update("正在生成 Java Bean...");

                    if (!args.isResponseOnly()) {
                        List<JavaFile> requestFiles = javaBeanGenerator.generate(model.getRequest());
                        for (JavaFile file : requestFiles) {
                            tx.addFile("java/" + file.getPath(), file.getContent());
                        }
                    }

                    if (!args.isRequestOnly()) {
                        List<JavaFile> responseFiles = javaBeanGenerator.generate(model.getResponse());
                        for (JavaFile file : responseFiles) {
                            tx.addFile("java/" + file.getPath(), file.getContent());
                        }
                    }
                }

                // 6. 生成 OpenAPI
                if (shouldGenerate(args, "openapi")) {
                    progressReporter.update("正在生成 OpenAPI...");
                    OpenApiResult apiResult = openApiGenerator.generate(model);
                    tx.addFile("openapi/api.yaml", apiResult.getMainFile());
                    for (Map.Entry<String, byte[]> schema : apiResult.getSchemas().entrySet()) {
                        tx.addFile("openapi/schemas/" + schema.getKey(), schema.getValue());
                    }
                }

                // 7. 执行验证
                if (!args.isSkipValidation()) {
                    progressReporter.update("正在验证制品...");
                    auditLogger.log(AuditEvent.VALIDATION_STARTED, "开始验证");

                    ConsistencyReport report = consistencyValidator.validate(tx.getTempDir());
                    auditLogger.logValidationResult("一致性验证",
                        "PASS".equals(report.getStatus()),
                        report.getErrorCount(),
                        report.getWarningCount());

                    if (!"PASS".equals(report.getStatus())) {
                        throw new ValidationException("一致性验证失败", report);
                    }
                }

                // 8. 模拟运行检查
                if (args.isDryRun()) {
                    progressReporter.complete("模拟运行完成，未写入文件");
                    outputManager.rollback(tx);
                    return SUCCESS;
                }

                // 9. 提交事务
                progressReporter.update("正在提交输出...");
                outputManager.commit(tx, consistencyReport, null);
                auditLogger.log(AuditEvent.TRANSACTION_COMMITTED, "事务已提交");
                auditLogger.logOutputManifest(tx.getManifest());

                progressReporter.complete("生成完成，输出目录: " + args.getOutputDir());
                return SUCCESS;

            } catch (Exception e) {
                progressReporter.error("生成失败: " + e.getMessage());
                auditLogger.log(AuditEvent.TRANSACTION_ROLLED_BACK, "事务已回滚: " + e.getMessage());
                outputManager.rollback(tx);
                throw e;
            }

        } catch (ParseException e) {
            auditLogger.log(AuditEvent.TOOL_FAILED, "解析失败: " + e.getMessage());
            return ERROR_PARSE;
        } catch (ValidationException e) {
            auditLogger.log(AuditEvent.TOOL_FAILED, "验证失败: " + e.getMessage());
            return ERROR_VALIDATE;
        } catch (Exception e) {
            auditLogger.log(AuditEvent.TOOL_FAILED, "生成失败: " + e.getMessage());
            return ERROR_GENERATE;
        }
    }

    private boolean shouldGenerate(CliArguments args, String format) {
        Set<String> formats = args.getFormats();
        return formats.contains("all") || formats.contains(format);
    }
}
```

### 进度报告器

```java
public class ProgressReporter {

    private final PrintStream out;
    private final boolean colorEnabled;
    private final boolean quietMode;
    private Instant startTime;
    private String currentPhase;

    public void start(String message) {
        startTime = Instant.now();
        if (!quietMode) {
            out.println(colorize("[START] ", Color.CYAN) + message);
        }
    }

    public void update(String message) {
        currentPhase = message;
        if (!quietMode) {
            out.println(colorize("[  ->  ] ", Color.BLUE) + message);
        }
    }

    public void complete(String message) {
        Duration elapsed = Duration.between(startTime, Instant.now());
        if (!quietMode) {
            out.println(colorize("[  OK  ] ", Color.GREEN) + message);
            out.println(colorize("[TIME ] ", Color.GRAY) + formatDuration(elapsed));
        }
    }

    public void error(String message) {
        out.println(colorize("[ERROR] ", Color.RED) + message);
    }

    public void warn(String message) {
        if (!quietMode) {
            out.println(colorize("[WARN ] ", Color.YELLOW) + message);
        }
    }
}
```

### Exit Code 定义

| Exit Code | 常量名 | 含义 |
|-----------|--------|------|
| 0 | SUCCESS | 执行成功 |
| 1 | ERROR_INVALID_ARGS | 参数错误 |
| 2 | ERROR_CONFIG | 配置错误 |
| 3 | ERROR_IO | IO 错误 |
| 10 | ERROR_PARSE | 解析错误 |
| 11 | ERROR_PARSE_EXCEL | Excel 解析错误 |
| 12 | ERROR_PARSE_SHEET | Sheet 发现错误 |
| 20 | ERROR_GENERATE | 生成错误 |
| 21 | ERROR_GENERATE_XML | XML 生成错误 |
| 22 | ERROR_GENERATE_JAVA | Java 生成错误 |
| 23 | ERROR_GENERATE_OPENAPI | OpenAPI 生成错误 |
| 30 | ERROR_VALIDATE | 验证错误 |
| 31 | ERROR_VALIDATE_CONSISTENCY | 一致性验证错误 |
| 32 | ERROR_VALIDATE_MESSAGE | 报文验证错误 |
| 40 | ERROR_OUTPUT | 输出错误 |
| 41 | ERROR_OUTPUT_PERMISSION | 输出权限错误 |
| 42 | ERROR_OUTPUT_DISK_FULL | 磁盘空间不足 |
| 99 | ERROR_GENERAL | 未分类错误 |

---

## Acceptance Criteria

1. [ ] 能够解析所有定义的命令行参数
2. [ ] 能够正确路由到子命令（generate、validate、parse、version、help）
3. [ ] generate 命令能够完成完整生成流程
4. [ ] validate 命令能够验证已生成的制品
5. [ ] parse 命令能够仅解析 Excel 并输出 JSON
6. [ ] version 命令显示正确的版本信息
7. [ ] help 命令显示正确的帮助信息
8. [ ] 支持配置文件路径指定
9. [ ] 支持命令行参数覆盖配置文件
10. [ ] 支持 --dry-run 模拟运行
11. [ ] 支持 --skip-validation 跳过验证
12. [ ] 支持 --request-only 和 --response-only
13. [ ] 支持多种输出格式选择 (-f)
14. [ ] 正确返回各种 exit code
15. [ ] 集成审计日志记录
16. [ ] 进度显示清晰易读
17. [ ] 错误信息包含足够上下文
18. [ ] 支持 --verbose 和 --quiet 模式

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 解析 generate 命令 | 正确识别子命令和选项 |
| 解析 -i 和 -o 参数 | 正确设置 inputFile 和 outputDir |
| 解析 -f all | formats 包含 all |
| 解析 -f xml,java | formats 包含 xml 和 java |
| 解析 --dry-run | dryRun = true |
| 解析 --skip-validation | skipValidation = true |
| 缺少必须参数 -i | 抛出 CliParseException |
| 无效子命令 | 返回 ERROR_INVALID_ARGS |
| help 命令 | 输出帮助信息，返回 SUCCESS |
| version 命令 | 输出版本信息，返回 SUCCESS |
| --config 指定配置文件 | 正确加载指定配置文件 |
| 配置文件不存在 | 返回 ERROR_CONFIG |

### 集成测试

| 测试场景 | 预期结果 |
|----------|---------|
| 完整 generate 流程 | 所有制品生成成功，返回 SUCCESS |
| generate --dry-run | 不写入文件，返回 SUCCESS |
| generate 输入文件不存在 | 返回 ERROR_PARSE |
| generate 验证失败 | 返回 ERROR_VALIDATE |
| validate 通过 | 返回 SUCCESS |
| validate 失败 | 返回 ERROR_VALIDATE |
| parse 成功 | 输出 JSON 文件，返回 SUCCESS |

### 端到端测试

| 测试场景 | 预期结果 |
|----------|---------|
| 从 Excel 到全部制品 | 所有文件生成，验证通过 |
| 审计日志生成 | audit/ 目录包含完整日志 |
| 进度输出 | 控制台显示各阶段进度 |

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| 参数解析歧义 | 使用成熟的 CLI 库（如 picocli、JCommander） |
| 路径含空格 | 正确处理引号包裹的路径 |
| 中文路径 | 使用 UTF-8 编码处理 |
| 相对路径解析 | 统一转换为绝对路径处理 |
| 环境变量覆盖冲突 | 明确优先级：命令行 > 环境变量 > 配置文件 |
| 长时间运行无输出 | 定期输出心跳/进度信息 |
| Ctrl+C 中断处理 | 注册 shutdown hook 执行清理 |
| 输出目录已存在 | 提示用户确认或使用 --force 覆盖 |
| 彩色输出兼容性 | 检测终端支持，提供 --no-color 选项 |
| Windows/Unix 路径差异 | 使用 Path API 统一处理 |

### Shutdown Hook 处理

```java
public class MqToolCli {

    private volatile OutputTransaction currentTransaction;

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (currentTransaction != null) {
                System.err.println("\n检测到中断，正在回滚事务...");
                try {
                    outputManager.rollback(currentTransaction);
                    System.err.println("事务已回滚");
                } catch (Exception e) {
                    System.err.println("回滚失败: " + e.getMessage());
                }
            }
        }));
    }
}
```

---

**文档结束**
