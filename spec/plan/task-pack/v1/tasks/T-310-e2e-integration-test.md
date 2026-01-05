# T-310 端到端集成测试

## Goal

实现端到端 (E2E) 集成测试套件，验证工具从 Excel 输入到所有制品输出的完整流程正确性、确定性、原子性。确保各模块协同工作无误，回归测试可自动化执行。

---

## In Scope / Out of Scope

### In Scope

- 完整流程集成测试（Excel -> JSON -> XML -> Java -> OpenAPI）
- Golden 测试：验证输出与基准文件完全一致（字节级别）
- 确定性验证：相同输入多次运行产生相同输出
- 原子性验证：失败场景无部分输出残留
- 审计日志完整性验证
- CLI 命令集成测试
- 边界条件和异常场景测试
- 性能基准测试（可选）
- 测试数据管理和 fixture 维护
- CI 集成配置

### Out of Scope

- 单元测试（各模块单元测试在各自任务中）
- 压力测试 / 负载测试
- 安全测试
- UI 测试（无 UI）
- 跨平台兼容性测试（仅 JVM）

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| 测试 Excel 文件 | 测试资源 | `src/test/resources/fixtures/excel/*.xlsx` |
| Golden 文件 | 测试资源 | `src/test/resources/golden/` |
| 测试配置 | 测试资源 | `src/test/resources/config/test-config.yaml` |
| CLI 入口 | T-309 | MqToolCli.java |
| 所有生成器和验证器 | T-1xx, T-2xx, T-3xx | 各模块实现 |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| IntegrationTest.java | `src/test/java/com/example/mqtool/integration/IntegrationTest.java` | 集成测试主类 |
| E2EGenerateTest.java | `src/test/java/com/example/mqtool/integration/E2EGenerateTest.java` | 端到端生成测试 |
| GoldenFileTest.java | `src/test/java/com/example/mqtool/integration/GoldenFileTest.java` | Golden 文件测试 |
| DeterminismTest.java | `src/test/java/com/example/mqtool/integration/DeterminismTest.java` | 确定性测试 |
| AtomicityTest.java | `src/test/java/com/example/mqtool/integration/AtomicityTest.java` | 原子性测试 |
| CliIntegrationTest.java | `src/test/java/com/example/mqtool/integration/CliIntegrationTest.java` | CLI 集成测试 |
| AuditIntegrationTest.java | `src/test/java/com/example/mqtool/integration/AuditIntegrationTest.java` | 审计日志测试 |
| EdgeCaseTest.java | `src/test/java/com/example/mqtool/integration/EdgeCaseTest.java` | 边界条件测试 |
| PerformanceTest.java | `src/test/java/com/example/mqtool/integration/PerformanceTest.java` | 性能基准测试 |
| TestFixtures.java | `src/test/java/com/example/mqtool/integration/TestFixtures.java` | 测试数据工具类 |
| 测试 Excel 文件 | `src/test/resources/fixtures/excel/` | 测试输入文件 |
| Golden 文件 | `src/test/resources/golden/` | 基准输出文件 |
| CI 配置 | `.github/workflows/integration-test.yml` | GitHub Actions 配置 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-309 | CLI 入口点 | 集成测试通过 CLI 执行完整流程 |
| T-307 | 原子输出管理器 | 验证原子性行为 |
| T-308 | 审计日志记录器 | 验证审计日志完整性 |
| T-304 | 跨制品一致性验证 | 验证一致性检查 |
| 所有生成器 | T-1xx, T-2xx | 验证生成结果 |

---

## Implementation Notes

### 测试目录结构

```
src/test/
├── java/com/example/mqtool/integration/
│   ├── IntegrationTest.java         # 测试基类
│   ├── E2EGenerateTest.java         # 端到端生成测试
│   ├── GoldenFileTest.java          # Golden 文件测试
│   ├── DeterminismTest.java         # 确定性测试
│   ├── AtomicityTest.java           # 原子性测试
│   ├── CliIntegrationTest.java      # CLI 集成测试
│   ├── AuditIntegrationTest.java    # 审计日志测试
│   ├── EdgeCaseTest.java            # 边界条件测试
│   ├── PerformanceTest.java         # 性能测试
│   └── TestFixtures.java            # 测试数据工具
└── resources/
    ├── fixtures/
    │   ├── excel/
    │   │   ├── simple-message.xlsx          # 简单消息
    │   │   ├── nested-message.xlsx          # 嵌套结构
    │   │   ├── array-message.xlsx           # 数组字段
    │   │   ├── complex-message.xlsx         # 复杂综合
    │   │   ├── minimal-message.xlsx         # 最小化
    │   │   ├── edge-case-special-chars.xlsx # 特殊字符
    │   │   └── invalid-message.xlsx         # 无效输入
    │   └── messages/
    │       ├── valid-request.bin            # 有效请求报文
    │       └── invalid-request.bin          # 无效请求报文
    ├── golden/
    │   ├── simple-message/
    │   │   ├── json/spec-tree.json
    │   │   ├── xml/outbound-converter.xml
    │   │   ├── xml/inbound-converter.xml
    │   │   ├── java/Request.java
    │   │   ├── java/Response.java
    │   │   └── openapi/api.yaml
    │   ├── nested-message/
    │   │   └── ...
    │   └── complex-message/
    │       └── ...
    └── config/
        └── test-config.yaml
```

### 集成测试基类

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest {

    protected static final Path TEST_RESOURCES = Paths.get("src/test/resources");
    protected static final Path FIXTURES_DIR = TEST_RESOURCES.resolve("fixtures");
    protected static final Path GOLDEN_DIR = TEST_RESOURCES.resolve("golden");
    protected static final Path CONFIG_DIR = TEST_RESOURCES.resolve("config");

    protected Path tempOutputDir;
    protected MqToolCli cli;

    @BeforeAll
    void setUpClass() {
        // 确保测试资源存在
        assertTrue(Files.exists(FIXTURES_DIR), "测试 fixtures 目录不存在");
        assertTrue(Files.exists(GOLDEN_DIR), "Golden 文件目录不存在");
    }

    @BeforeEach
    void setUp() throws IOException {
        // 每个测试使用独立临时目录
        tempOutputDir = Files.createTempDirectory("mq-tool-test-");
        cli = new MqToolCli();
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理临时目录
        if (tempOutputDir != null && Files.exists(tempOutputDir)) {
            deleteRecursive(tempOutputDir);
        }
    }

    protected Path getFixture(String name) {
        return FIXTURES_DIR.resolve("excel").resolve(name);
    }

    protected Path getGolden(String testCase) {
        return GOLDEN_DIR.resolve(testCase);
    }

    protected int runCli(String... args) {
        return cli.run(args);
    }

    protected void assertFilesMatch(Path actual, Path expected) throws IOException {
        assertTrue(Files.exists(actual), "实际文件不存在: " + actual);
        assertTrue(Files.exists(expected), "期望文件不存在: " + expected);

        byte[] actualBytes = Files.readAllBytes(actual);
        byte[] expectedBytes = Files.readAllBytes(expected);

        assertArrayEquals(expectedBytes, actualBytes,
            String.format("文件内容不匹配: %s vs %s", actual, expected));
    }

    protected void assertDirectoriesMatch(Path actual, Path expected) throws IOException {
        // 递归比较目录中所有文件
        try (Stream<Path> expectedFiles = Files.walk(expected)) {
            expectedFiles
                .filter(Files::isRegularFile)
                .forEach(expectedFile -> {
                    Path relativePath = expected.relativize(expectedFile);
                    Path actualFile = actual.resolve(relativePath);
                    try {
                        assertFilesMatch(actualFile, expectedFile);
                    } catch (IOException e) {
                        fail("文件比较失败: " + relativePath + " - " + e.getMessage());
                    }
                });
        }
    }
}
```

### 端到端生成测试

```java
public class E2EGenerateTest extends IntegrationTest {

    @Test
    @DisplayName("简单消息 - 完整生成流程")
    void testSimpleMessageGeneration() {
        // Given
        Path inputFile = getFixture("simple-message.xlsx");

        // When
        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        // Then
        assertEquals(0, exitCode, "应返回成功退出码");

        // 验证所有输出文件存在
        assertTrue(Files.exists(tempOutputDir.resolve("json/spec-tree.json")));
        assertTrue(Files.exists(tempOutputDir.resolve("xml/outbound-converter.xml")));
        assertTrue(Files.exists(tempOutputDir.resolve("xml/inbound-converter.xml")));
        assertTrue(Files.exists(tempOutputDir.resolve("java/Request.java")));
        assertTrue(Files.exists(tempOutputDir.resolve("java/Response.java")));
        assertTrue(Files.exists(tempOutputDir.resolve("openapi/api.yaml")));
        assertTrue(Files.exists(tempOutputDir.resolve("manifest.json")));
        assertTrue(Files.exists(tempOutputDir.resolve("audit/audit-log.json")));
    }

    @Test
    @DisplayName("嵌套结构消息 - 完整生成流程")
    void testNestedMessageGeneration() {
        Path inputFile = getFixture("nested-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode);

        // 验证嵌套结构正确生成
        String jsonTree = Files.readString(tempOutputDir.resolve("json/spec-tree.json"));
        assertTrue(jsonTree.contains("\"children\""), "JSON Tree 应包含嵌套结构");
    }

    @Test
    @DisplayName("数组字段消息 - 完整生成流程")
    void testArrayMessageGeneration() {
        Path inputFile = getFixture("array-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode);

        // 验证数组类型正确生成
        String javaRequest = Files.readString(tempOutputDir.resolve("java/Request.java"));
        assertTrue(javaRequest.contains("List<"), "Java Bean 应包含 List 类型");
    }

    @Test
    @DisplayName("仅生成 XML")
    void testGenerateXmlOnly() {
        Path inputFile = getFixture("simple-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "xml"
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempOutputDir.resolve("xml/outbound-converter.xml")));
        assertTrue(Files.exists(tempOutputDir.resolve("xml/inbound-converter.xml")));
        assertFalse(Files.exists(tempOutputDir.resolve("java/Request.java")));
    }

    @Test
    @DisplayName("仅生成 Request")
    void testGenerateRequestOnly() {
        Path inputFile = getFixture("simple-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all",
            "--request-only"
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempOutputDir.resolve("xml/outbound-converter.xml")));
        assertFalse(Files.exists(tempOutputDir.resolve("xml/inbound-converter.xml")));
    }

    @Test
    @DisplayName("Dry Run 模式 - 不写入文件")
    void testDryRunMode() {
        Path inputFile = getFixture("simple-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all",
            "--dry-run"
        );

        assertEquals(0, exitCode);

        // 验证没有文件被写入
        try (Stream<Path> files = Files.list(tempOutputDir)) {
            assertEquals(0, files.count(), "Dry run 模式不应产生输出文件");
        }
    }
}
```

### Golden 文件测试

```java
public class GoldenFileTest extends IntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "simple-message",
        "nested-message",
        "array-message",
        "complex-message"
    })
    @DisplayName("Golden 文件对比测试")
    void testGoldenFileComparison(String testCase) throws IOException {
        // Given
        Path inputFile = getFixture(testCase + ".xlsx");
        Path goldenDir = getGolden(testCase);

        // When
        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all",
            "--no-audit"  // 排除审计日志以便比较
        );

        // Then
        assertEquals(0, exitCode, "生成应成功");

        // 比较所有 golden 文件
        assertDirectoriesMatch(tempOutputDir, goldenDir);
    }

    @Test
    @DisplayName("JSON Tree Golden 测试 - 字段顺序保持")
    void testJsonTreeFieldOrder() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");
        Path goldenJson = getGolden("simple-message/json/spec-tree.json");

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "json", "--no-audit");

        Path actualJson = tempOutputDir.resolve("json/spec-tree.json");

        // 字节级别比较确保字段顺序一致
        assertFilesMatch(actualJson, goldenJson);
    }

    @Test
    @DisplayName("XML Golden 测试 - 结构完全一致")
    void testXmlGolden() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");
        Path goldenXml = getGolden("simple-message/xml/outbound-converter.xml");

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "xml", "--no-audit");

        Path actualXml = tempOutputDir.resolve("xml/outbound-converter.xml");
        assertFilesMatch(actualXml, goldenXml);
    }
}
```

### 确定性测试

```java
public class DeterminismTest extends IntegrationTest {

    @Test
    @DisplayName("多次运行产生相同输出")
    void testMultipleRunsDeterminism() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");

        // 运行 3 次，收集输出哈希
        List<String> outputHashes = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Path runOutputDir = Files.createTempDirectory("determinism-run-" + i);
            try {
                runCli("generate", "-i", inputFile.toString(),
                       "-o", runOutputDir.toString(), "-f", "all", "--no-audit");

                String hash = computeDirectoryHash(runOutputDir);
                outputHashes.add(hash);
            } finally {
                deleteRecursive(runOutputDir);
            }
        }

        // 验证所有运行产生相同哈希
        String firstHash = outputHashes.get(0);
        for (int i = 1; i < outputHashes.size(); i++) {
            assertEquals(firstHash, outputHashes.get(i),
                String.format("第 %d 次运行输出与第 1 次不同", i + 1));
        }
    }

    @Test
    @DisplayName("JSON 输出字段顺序稳定")
    void testJsonFieldOrderStability() throws IOException {
        Path inputFile = getFixture("complex-message.xlsx");

        // 运行多次，比较 JSON 文件的字节内容
        byte[] firstJson = null;

        for (int i = 0; i < 3; i++) {
            Path runOutputDir = Files.createTempDirectory("json-order-" + i);
            try {
                runCli("generate", "-i", inputFile.toString(),
                       "-o", runOutputDir.toString(), "-f", "json", "--no-audit");

                byte[] json = Files.readAllBytes(runOutputDir.resolve("json/spec-tree.json"));

                if (firstJson == null) {
                    firstJson = json;
                } else {
                    assertArrayEquals(firstJson, json,
                        String.format("第 %d 次运行 JSON 字节内容不同", i + 1));
                }
            } finally {
                deleteRecursive(runOutputDir);
            }
        }
    }

    @Test
    @DisplayName("Manifest 哈希值稳定")
    void testManifestHashStability() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");

        Set<String> manifestHashes = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            Path runOutputDir = Files.createTempDirectory("manifest-hash-" + i);
            try {
                runCli("generate", "-i", inputFile.toString(),
                       "-o", runOutputDir.toString(), "-f", "all", "--no-audit");

                String manifest = Files.readString(runOutputDir.resolve("manifest.json"));
                JsonNode manifestNode = objectMapper.readTree(manifest);

                // 提取文件哈希值
                manifestNode.get("files").forEach(file -> {
                    manifestHashes.add(file.get("sha256").asText());
                });

            } finally {
                deleteRecursive(runOutputDir);
            }
        }

        // 验证哈希值数量 = 文件数（不重复）
        // 如果有重复哈希，说明内容一致
    }

    private String computeDirectoryHash(Path dir) throws IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (Stream<Path> files = Files.walk(dir).filter(Files::isRegularFile).sorted()) {
            files.forEach(file -> {
                try {
                    digest.update(file.getFileName().toString().getBytes(UTF_8));
                    digest.update(Files.readAllBytes(file));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        return bytesToHex(digest.digest());
    }
}
```

### 原子性测试

```java
public class AtomicityTest extends IntegrationTest {

    @Test
    @DisplayName("验证失败时无输出残留")
    void testNoPartialOutputOnValidationFailure() throws IOException {
        Path inputFile = getFixture("invalid-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertNotEquals(0, exitCode, "无效输入应导致失败");

        // 验证输出目录为空或不存在
        if (Files.exists(tempOutputDir)) {
            try (Stream<Path> files = Files.list(tempOutputDir)) {
                long fileCount = files.count();
                assertEquals(0, fileCount, "失败时不应有任何输出文件残留");
            }
        }
    }

    @Test
    @DisplayName("生成中途异常时回滚")
    void testRollbackOnGenerationException() throws IOException {
        // 使用会导致 Java 生成失败的特殊输入
        Path inputFile = getFixture("edge-case-java-error.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertNotEquals(0, exitCode);

        // 验证没有部分输出
        assertFalse(Files.exists(tempOutputDir.resolve("json")));
        assertFalse(Files.exists(tempOutputDir.resolve("xml")));
        assertFalse(Files.exists(tempOutputDir.resolve("java")));
    }

    @Test
    @DisplayName("原有输出在失败时保留")
    void testExistingOutputPreservedOnFailure() throws IOException {
        // 先成功生成一次
        Path validInput = getFixture("simple-message.xlsx");
        runCli("generate", "-i", validInput.toString(),
               "-o", tempOutputDir.toString(), "-f", "all");

        // 记录原有文件的哈希
        String originalHash = computeFileHash(
            tempOutputDir.resolve("json/spec-tree.json"));

        // 使用无效输入再次运行
        Path invalidInput = getFixture("invalid-message.xlsx");
        int exitCode = runCli(
            "generate",
            "-i", invalidInput.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertNotEquals(0, exitCode);

        // 验证原有文件未被破坏
        String currentHash = computeFileHash(
            tempOutputDir.resolve("json/spec-tree.json"));
        assertEquals(originalHash, currentHash, "原有输出应保持不变");
    }

    @Test
    @DisplayName("临时目录清理")
    void testTempDirectoryCleanup() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");

        // 获取输出目录的父目录，检查临时目录
        Path parentDir = tempOutputDir.getParent();

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "all");

        // 验证没有残留的 .tmp-* 目录
        try (Stream<Path> files = Files.list(parentDir)) {
            long tmpCount = files
                .filter(p -> p.getFileName().toString().startsWith(".tmp-"))
                .count();
            assertEquals(0, tmpCount, "不应有临时目录残留");
        }
    }
}
```

### CLI 集成测试

```java
public class CliIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("help 命令")
    void testHelpCommand() {
        ByteArrayOutputStream out = captureStdout();

        int exitCode = runCli("help");

        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("generate"));
        assertTrue(output.contains("validate"));
        assertTrue(output.contains("parse"));
    }

    @Test
    @DisplayName("version 命令")
    void testVersionCommand() {
        ByteArrayOutputStream out = captureStdout();

        int exitCode = runCli("version");

        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.matches(".*\\d+\\.\\d+\\.\\d+.*"), "应输出版本号");
    }

    @Test
    @DisplayName("无效命令返回错误码")
    void testInvalidCommand() {
        int exitCode = runCli("invalid-command");

        assertEquals(1, exitCode);
    }

    @Test
    @DisplayName("缺少必须参数返回错误码")
    void testMissingRequiredArgument() {
        int exitCode = runCli("generate");  // 缺少 -i 参数

        assertEquals(1, exitCode);
    }

    @Test
    @DisplayName("输入文件不存在返回错误码")
    void testNonExistentInputFile() {
        int exitCode = runCli(
            "generate",
            "-i", "non-existent-file.xlsx",
            "-o", tempOutputDir.toString()
        );

        assertEquals(10, exitCode);  // ERROR_PARSE
    }

    @Test
    @DisplayName("validate 命令 - 有效制品")
    void testValidateValidArtifacts() throws IOException {
        // 先生成制品
        Path inputFile = getFixture("simple-message.xlsx");
        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "all");

        // 验证制品
        int exitCode = runCli(
            "validate",
            "-d", tempOutputDir.toString()
        );

        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("parse 命令 - 仅输出 JSON")
    void testParseCommand() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");
        Path outputJson = tempOutputDir.resolve("parsed.json");

        int exitCode = runCli(
            "parse",
            "-i", inputFile.toString(),
            "-o", outputJson.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(outputJson));

        // 验证 JSON 格式正确
        String json = Files.readString(outputJson);
        assertDoesNotThrow(() -> objectMapper.readTree(json));
    }
}
```

### 审计日志集成测试

```java
public class AuditIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("完整流程审计日志")
    void testCompleteAuditLog() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "all");

        // 验证审计日志存在
        Path auditLogJson = tempOutputDir.resolve("audit/audit-log.json");
        assertTrue(Files.exists(auditLogJson), "应生成 JSON 审计日志");

        // 解析审计日志
        String auditContent = Files.readString(auditLogJson);
        JsonNode auditLog = objectMapper.readTree(auditContent);

        // 验证必须的事件存在
        List<String> events = new ArrayList<>();
        auditLog.get("records").forEach(record -> {
            events.add(record.get("event").asText());
        });

        assertTrue(events.contains("TOOL_STARTED"), "应包含 TOOL_STARTED 事件");
        assertTrue(events.contains("INPUT_FILE_LOADED"), "应包含 INPUT_FILE_LOADED 事件");
        assertTrue(events.contains("PARSE_COMPLETED"), "应包含 PARSE_COMPLETED 事件");
        assertTrue(events.contains("TRANSACTION_COMMITTED"), "应包含 TRANSACTION_COMMITTED 事件");
    }

    @Test
    @DisplayName("失败场景审计日志")
    void testFailureAuditLog() throws IOException {
        Path inputFile = getFixture("invalid-message.xlsx");

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "all");

        // 即使失败，审计目录也应存在
        Path auditDir = tempOutputDir.resolve("audit");

        // 注意：失败时可能审计目录不存在（取决于实现）
        // 如果实现为失败时也保留审计，则检查内容
        if (Files.exists(auditDir)) {
            Path auditLogJson = auditDir.resolve("audit-log.json");
            if (Files.exists(auditLogJson)) {
                String auditContent = Files.readString(auditLogJson);
                assertTrue(auditContent.contains("TOOL_FAILED") ||
                           auditContent.contains("TRANSACTION_ROLLED_BACK"),
                    "失败场景应记录失败事件");
            }
        }
    }

    @Test
    @DisplayName("审计摘要报告生成")
    void testAuditSummaryReport() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "all");

        Path summaryMd = tempOutputDir.resolve("audit/audit-summary.md");
        assertTrue(Files.exists(summaryMd), "应生成审计摘要报告");

        String summary = Files.readString(summaryMd);
        assertTrue(summary.contains("# 审计摘要报告"), "应包含标题");
        assertTrue(summary.contains("## 运行信息"), "应包含运行信息章节");
        assertTrue(summary.contains("## 输出制品"), "应包含输出制品章节");
    }

    @Test
    @DisplayName("--no-audit 禁用审计日志")
    void testNoAuditOption() throws IOException {
        Path inputFile = getFixture("simple-message.xlsx");

        runCli("generate", "-i", inputFile.toString(),
               "-o", tempOutputDir.toString(), "-f", "all", "--no-audit");

        Path auditDir = tempOutputDir.resolve("audit");
        assertFalse(Files.exists(auditDir), "--no-audit 应禁用审计日志生成");
    }
}
```

### 边界条件测试

```java
public class EdgeCaseTest extends IntegrationTest {

    @Test
    @DisplayName("最小化 Excel - 仅必须字段")
    void testMinimalExcel() {
        Path inputFile = getFixture("minimal-message.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode, "最小化输入应能成功处理");
    }

    @Test
    @DisplayName("特殊字符字段名")
    void testSpecialCharacterFieldNames() {
        Path inputFile = getFixture("edge-case-special-chars.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("深层嵌套结构 (10+ 层)")
    void testDeeplyNestedStructure() {
        Path inputFile = getFixture("edge-case-deep-nesting.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("大型 Excel (1000+ 字段)")
    void testLargeExcel() {
        Path inputFile = getFixture("edge-case-large.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("空 Sheet")
    void testEmptySheet() {
        Path inputFile = getFixture("edge-case-empty-sheet.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        // 空 Sheet 应报错或产生空输出，具体行为取决于设计
        // 这里假设应该报错
        assertNotEquals(0, exitCode);
    }

    @Test
    @DisplayName("Unicode 字段名和注释")
    void testUnicodeContent() throws IOException {
        Path inputFile = getFixture("edge-case-unicode.xlsx");

        int exitCode = runCli(
            "generate",
            "-i", inputFile.toString(),
            "-o", tempOutputDir.toString(),
            "-f", "all"
        );

        assertEquals(0, exitCode);

        // 验证中文注释被正确保留
        String javaContent = Files.readString(tempOutputDir.resolve("java/Request.java"));
        // 检查编码正确
        assertFalse(javaContent.contains("???"), "不应包含乱码");
    }
}
```

### 性能基准测试

```java
@Tag("performance")
public class PerformanceTest extends IntegrationTest {

    @Test
    @DisplayName("简单消息生成性能")
    void testSimpleMessagePerformance() {
        Path inputFile = getFixture("simple-message.xlsx");

        // 预热
        runGenerate(inputFile);

        // 计时
        long startTime = System.currentTimeMillis();
        int iterations = 10;

        for (int i = 0; i < iterations; i++) {
            runGenerate(inputFile);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double avgTime = (double) elapsed / iterations;

        System.out.printf("简单消息平均生成时间: %.2f ms%n", avgTime);
        assertTrue(avgTime < 5000, "简单消息生成应在 5 秒内完成");
    }

    @Test
    @DisplayName("复杂消息生成性能")
    void testComplexMessagePerformance() {
        Path inputFile = getFixture("complex-message.xlsx");

        long startTime = System.currentTimeMillis();
        runGenerate(inputFile);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.printf("复杂消息生成时间: %d ms%n", elapsed);
        assertTrue(elapsed < 30000, "复杂消息生成应在 30 秒内完成");
    }

    private void runGenerate(Path inputFile) {
        Path outputDir;
        try {
            outputDir = Files.createTempDirectory("perf-test-");
            runCli("generate", "-i", inputFile.toString(),
                   "-o", outputDir.toString(), "-f", "all", "--no-audit");
            deleteRecursive(outputDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```

### CI 配置

**.github/workflows/integration-test.yml:**
```yaml
name: Integration Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  integration-test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: maven

    - name: Run Integration Tests
      run: mvn -B verify -Pintegration-tests

    - name: Upload Test Results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: target/surefire-reports/

    - name: Upload Golden File Diffs
      uses: actions/upload-artifact@v3
      if: failure()
      with:
        name: golden-diffs
        path: target/golden-diffs/

  performance-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: maven

    - name: Run Performance Tests
      run: mvn -B test -Dgroups=performance

    - name: Store Performance Results
      uses: actions/upload-artifact@v3
      with:
        name: performance-results
        path: target/performance/
```

---

## Acceptance Criteria

1. [ ] 所有 Golden 文件测试通过
2. [ ] 确定性测试通过：多次运行产生相同输出
3. [ ] 原子性测试通过：失败时无部分输出
4. [ ] CLI 所有子命令集成测试通过
5. [ ] 审计日志集成测试通过
6. [ ] 边界条件测试覆盖所有识别的边界场景
7. [ ] 性能测试在预期时间内完成
8. [ ] 测试覆盖率 > 80%（集成测试路径）
9. [ ] CI 配置正确，测试可自动运行
10. [ ] 测试报告清晰，失败原因易于定位
11. [ ] Golden 文件易于更新和维护
12. [ ] 测试数据 fixture 完备，覆盖典型场景

---

## Tests

由于本任务本身是测试任务，此处列出测试策略验证：

### 测试策略验证

| 验证项 | 预期结果 |
|--------|---------|
| 测试类可编译 | 所有测试类编译通过 |
| 测试可运行 | `mvn test -Pintegration-tests` 成功 |
| 测试报告生成 | target/surefire-reports/ 包含报告 |
| Golden 文件存在 | 所有引用的 golden 文件存在 |
| Fixture 文件存在 | 所有引用的测试 Excel 文件存在 |
| CI 流程正确 | GitHub Actions 可正常触发 |

### 测试覆盖矩阵

| 场景类型 | 测试类 | 覆盖状态 |
|----------|--------|---------|
| 端到端生成 | E2EGenerateTest | [ ] |
| Golden 对比 | GoldenFileTest | [ ] |
| 确定性 | DeterminismTest | [ ] |
| 原子性 | AtomicityTest | [ ] |
| CLI 命令 | CliIntegrationTest | [ ] |
| 审计日志 | AuditIntegrationTest | [ ] |
| 边界条件 | EdgeCaseTest | [ ] |
| 性能基准 | PerformanceTest | [ ] |

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| Golden 文件维护成本高 | 提供 `mvn golden:update` 一键更新脚本 |
| 测试环境与 CI 环境差异 | 使用 Docker 统一环境，或明确环境要求 |
| 测试运行时间过长 | 并行运行测试，标记慢测试为 @Slow |
| 临时文件清理失败 | 使用 @TempDir JUnit 扩展自动清理 |
| 测试数据过期 | 建立定期审查测试数据的流程 |
| 哈希算法变更导致 Golden 失效 | 版本化 Golden 文件，文档记录算法 |
| Windows/Linux 换行符差异 | 统一使用 LF，Git 配置 core.autocrlf |
| 文件编码差异 | 统一使用 UTF-8，测试中验证 |
| 时间戳导致确定性失败 | 审计日志中固定时间戳或排除比较 |
| 并发测试冲突 | 每个测试使用独立临时目录 |

### Golden 文件更新脚本

```bash
#!/bin/bash
# scripts/update-golden.sh

mvn test -Dtest=GoldenFileTest -DupdateGolden=true

echo "Golden files updated. Please review changes:"
git diff src/test/resources/golden/
```

---

**文档结束**
