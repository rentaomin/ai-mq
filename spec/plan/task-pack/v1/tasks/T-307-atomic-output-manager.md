# T-307 原子输出管理器

## Goal

实现原子输出管理器 (AtomicOutputManager)，确保所有生成制品 (XML Bean、Java Bean、OpenAPI YAML、JSON Tree、偏移量表等) 的写入操作具备原子性。在生成过程中发生任何错误时，能够回滚到初始状态，避免产生部分输出或不一致的文件状态。

---

## In Scope / Out of Scope

### In Scope

- 管理所有输出文件的原子写入
- 实现事务性输出：全部成功或全部回滚
- 支持临时目录模式：先写入临时位置，验证通过后原子移动
- 支持备份模式：覆盖前备份原有文件
- 检测输出目录的写入权限和磁盘空间
- 生成输出清单 (manifest)：记录所有写入的文件路径和哈希值
- 提供回滚机制：删除部分写入的文件
- 集成一致性验证 (T-304) 和报文验证 (T-306) 结果作为提交条件

### Out of Scope

- 分布式事务 (仅支持本地文件系统)
- 数据库输出管理
- 远程文件系统写入
- 版本控制系统集成 (如 git commit)

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `ConsistencyReport` | T-304 | 跨制品一致性验证报告 |
| `MessageValidationResult` | T-306 | 报文验证结果 (可选，如启用报文验证) |
| 生成器输出 | T-111, T-202, T-206, T-209 等 | 各生成器产生的文件内容 (内存中) |
| `config/output.yaml` | 配置 | 输出目录配置、原子模式配置 |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| AtomicOutputManager.java | `src/main/java/com/example/mqtool/output/AtomicOutputManager.java` | 原子输出管理器主类 |
| OutputTransaction.java | `src/main/java/com/example/mqtool/output/OutputTransaction.java` | 输出事务封装 |
| OutputManifest.java | `src/main/java/com/example/mqtool/output/OutputManifest.java` | 输出清单数据模型 |
| FileOperation.java | `src/main/java/com/example/mqtool/output/FileOperation.java` | 单个文件操作记录 |
| output-manifest.json | `{output-dir}/manifest.json` | 输出清单文件 |
| 单元测试 | `src/test/java/com/example/mqtool/output/AtomicOutputManagerTest.java` | 管理器测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-304 | 跨制品一致性验证 | 验证通过作为提交条件 |
| T-306 | 报文验证器 | 验证通过作为提交条件 (可选) |
| T-002 | 核心接口定义 | 使用 Generator 接口约定的输出格式 |
| T-005 | 错误处理框架 | 使用 OutputException、ExitCodes |
| T-003 | 配置加载器 | 读取输出目录配置 |

---

## Implementation Notes

### 原子写入策略

#### 策略 1: 临时目录模式 (推荐)

```
1. 创建临时输出目录: {output-dir}/.tmp-{timestamp}/
2. 所有文件写入临时目录
3. 执行验证 (一致性验证 + 报文验证)
4. 验证通过:
   - 删除原输出目录 (如存在)
   - 原子重命名临时目录为正式输出目录
5. 验证失败:
   - 删除临时目录
   - 保留原输出目录不变
```

#### 策略 2: 备份模式

```
1. 备份原输出目录: {output-dir} -> {output-dir}.bak-{timestamp}/
2. 直接写入 {output-dir}
3. 验证通过: 删除备份目录
4. 验证失败:
   - 删除新写入的文件
   - 恢复备份目录
```

### 数据结构

```java
public class OutputTransaction {
    private String transactionId;           // 事务 ID (UUID)
    private Instant startTime;              // 开始时间
    private String tempDir;                 // 临时目录路径
    private String targetDir;               // 目标输出目录
    private TransactionState state;         // PENDING / WRITING / VALIDATING / COMMITTED / ROLLED_BACK
    private List<FileOperation> operations; // 文件操作列表
    private OutputManifest manifest;        // 输出清单

    public void addFile(String relativePath, byte[] content);
    public void commit() throws OutputException;
    public void rollback();
}

public class FileOperation {
    private String relativePath;      // 相对路径
    private String absolutePath;      // 绝对路径
    private OperationType type;       // CREATE / OVERWRITE / DELETE
    private long size;                // 文件大小 (字节)
    private String sha256;            // SHA-256 哈希值
    private Instant timestamp;        // 操作时间
    private boolean completed;        // 是否完成
}

public class OutputManifest {
    private String transactionId;
    private String specVersion;             // 规范版本
    private Instant generatedAt;            // 生成时间
    private int totalFiles;                 // 文件总数
    private long totalSize;                 // 总大小 (字节)
    private List<FileEntry> files;          // 文件列表

    public static class FileEntry {
        private String path;
        private long size;
        private String sha256;
        private String type;   // XML / JAVA / YAML / JSON
    }
}
```

### 核心代码模式

```java
public class AtomicOutputManager {

    private final Config config;
    private final ConsistencyValidator consistencyValidator;
    private final MessageValidator messageValidator;

    /**
     * 开始新的输出事务
     */
    public OutputTransaction beginTransaction(String outputDir) {
        String txId = UUID.randomUUID().toString();
        String tempDir = outputDir + "/.tmp-" + txId;

        Files.createDirectories(Paths.get(tempDir));

        return new OutputTransaction(txId, tempDir, outputDir);
    }

    /**
     * 提交事务：验证通过后原子移动
     */
    public void commit(OutputTransaction tx,
                       ConsistencyReport consistencyReport,
                       MessageValidationResult validationResult) throws OutputException {

        // 1. 检查验证结果
        if (!"PASS".equals(consistencyReport.getStatus())) {
            throw new OutputException("一致性验证失败，拒绝提交",
                ExitCodes.CONSISTENCY_FAILED);
        }

        if (validationResult != null && !validationResult.isValid()) {
            throw new OutputException("报文验证失败，拒绝提交",
                ExitCodes.VALIDATION_FAILED);
        }

        // 2. 生成 manifest
        OutputManifest manifest = generateManifest(tx);
        writeManifest(tx.getTempDir(), manifest);

        // 3. 原子移动
        Path tempPath = Paths.get(tx.getTempDir());
        Path targetPath = Paths.get(tx.getTargetDir());

        if (Files.exists(targetPath)) {
            // 备份旧目录
            Path backupPath = Paths.get(tx.getTargetDir() + ".bak-" + tx.getTransactionId());
            Files.move(targetPath, backupPath, StandardCopyOption.ATOMIC_MOVE);
        }

        Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
        tx.setState(TransactionState.COMMITTED);

        // 4. 删除备份 (可配置保留)
        if (!config.isKeepBackup()) {
            deleteBackup(tx);
        }
    }

    /**
     * 回滚事务：删除临时目录
     */
    public void rollback(OutputTransaction tx) {
        if (tx.getState() == TransactionState.COMMITTED) {
            throw new IllegalStateException("已提交的事务无法回滚");
        }

        Path tempPath = Paths.get(tx.getTempDir());
        if (Files.exists(tempPath)) {
            deleteRecursive(tempPath);
        }

        tx.setState(TransactionState.ROLLED_BACK);
    }

    /**
     * 预检查：验证输出目录权限和空间
     */
    public PreCheckResult preCheck(String outputDir) {
        PreCheckResult result = new PreCheckResult();

        // 检查目录是否可写
        result.setWritable(Files.isWritable(Paths.get(outputDir).getParent()));

        // 检查可用磁盘空间
        long freeSpace = new File(outputDir).getUsableSpace();
        result.setFreeSpace(freeSpace);
        result.setSufficientSpace(freeSpace > config.getMinFreeSpace());

        return result;
    }
}
```

### 使用模式

```java
// 典型使用流程
public void generateAll(String excelPath, String outputDir) {
    AtomicOutputManager outputManager = new AtomicOutputManager(config);
    OutputTransaction tx = null;

    try {
        // 预检查
        PreCheckResult preCheck = outputManager.preCheck(outputDir);
        if (!preCheck.isWritable()) {
            throw new OutputException("输出目录不可写: " + outputDir);
        }

        // 开始事务
        tx = outputManager.beginTransaction(outputDir);

        // 生成各类制品，写入临时目录
        tx.addFile("xml/outbound-bean.xml", xmlGenerator.generate());
        tx.addFile("xml/inbound-bean.xml", inboundXmlGenerator.generate());
        tx.addFile("java/Request.java", javaGenerator.generateRequest());
        tx.addFile("java/Response.java", javaGenerator.generateResponse());
        tx.addFile("openapi/api.yaml", openApiGenerator.generate());
        tx.addFile("json/spec-tree.json", jsonSerializer.serialize());

        // 执行验证
        ConsistencyReport consistencyReport = consistencyValidator.validate(...);
        MessageValidationResult validationResult = messageValidator.validate(...);

        // 提交事务
        outputManager.commit(tx, consistencyReport, validationResult);

    } catch (Exception e) {
        // 回滚事务
        if (tx != null) {
            outputManager.rollback(tx);
        }
        throw e;
    }
}
```

### 输出清单格式

**manifest.json:**
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "specVersion": "1.0.0",
  "generatedAt": "2026-01-05T15:00:00Z",
  "totalFiles": 8,
  "totalSize": 125678,
  "files": [
    {
      "path": "xml/outbound-bean.xml",
      "size": 15234,
      "sha256": "a3f2e1d4c5b6a7...",
      "type": "XML"
    },
    {
      "path": "xml/inbound-bean.xml",
      "size": 14892,
      "sha256": "b4e3d2c1a0f9...",
      "type": "XML"
    },
    {
      "path": "java/Request.java",
      "size": 8456,
      "sha256": "c5d4e3f2a1b0...",
      "type": "JAVA"
    },
    {
      "path": "openapi/api.yaml",
      "size": 22341,
      "sha256": "d6e5f4c3b2a1...",
      "type": "YAML"
    },
    {
      "path": "json/spec-tree.json",
      "size": 45678,
      "sha256": "e7f6d5c4b3a2...",
      "type": "JSON"
    }
  ],
  "consistencyCheck": {
    "status": "PASS",
    "timestamp": "2026-01-05T14:59:58Z"
  },
  "messageValidation": {
    "status": "PASS",
    "timestamp": "2026-01-05T14:59:59Z"
  }
}
```

### Exit Code 定义

| Exit Code | 含义 |
|-----------|------|
| 0 | 输出成功，事务已提交 |
| 61 | 输出目录不可写 |
| 62 | 磁盘空间不足 |
| 63 | 一致性验证失败，拒绝提交 |
| 64 | 报文验证失败，拒绝提交 |
| 65 | 原子移动失败 |
| 66 | 回滚失败 (严重错误) |

---

## Acceptance Criteria

1. [ ] 能够创建输出事务，生成唯一事务 ID
2. [ ] 所有文件先写入临时目录
3. [ ] 验证通过后原子移动到目标目录
4. [ ] 验证失败时自动回滚，删除临时目录
5. [ ] 一致性验证 (T-304) 失败时拒绝提交
6. [ ] 报文验证 (T-306) 失败时拒绝提交 (如启用)
7. [ ] 生成 manifest.json 记录所有输出文件
8. [ ] manifest 包含每个文件的 SHA-256 哈希值
9. [ ] 支持预检查输出目录权限和磁盘空间
10. [ ] 原子移动失败时保留原目录不变
11. [ ] 提供明确的 exit code 区分不同错误场景
12. [ ] 回滚操作不留下任何临时文件

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 事务创建 | 生成唯一事务 ID，创建临时目录 |
| 添加文件到事务 | 文件写入临时目录，记录操作 |
| 一致性验证通过 + 提交 | 原子移动成功，事务状态为 COMMITTED |
| 一致性验证失败 + 提交 | 抛出 OutputException，exit code 63 |
| 报文验证失败 + 提交 | 抛出 OutputException，exit code 64 |
| 回滚事务 | 删除临时目录，事务状态为 ROLLED_BACK |
| 已提交事务回滚 | 抛出 IllegalStateException |
| manifest 生成 | JSON 格式正确，包含所有文件 |
| SHA-256 计算 | 哈希值与文件内容一致 |
| 预检查目录可写 | 返回 writable=true |
| 预检查目录不可写 | 返回 writable=false |
| 预检查磁盘空间充足 | 返回 sufficientSpace=true |
| 预检查磁盘空间不足 | 返回 sufficientSpace=false |

### 集成测试

| 测试场景 | 预期结果 |
|----------|---------|
| 完整生成流程 - 成功 | 所有文件写入目标目录，manifest 正确 |
| 完整生成流程 - 验证失败 | 目标目录不变，无临时文件残留 |
| 覆盖已有输出 | 旧目录被备份，新文件替换 |
| 备份恢复 | 提交失败时恢复旧目录 |
| 并发写入 | 不同事务 ID 隔离，互不干扰 |

### Golden 测试

| 测试场景 | 预期结果 |
|----------|---------|
| manifest 格式稳定性 | manifest.json 结构与 golden 文件一致 |
| 哈希值确定性 | 相同输入产生相同哈希值 |

### 边界测试

| 测试场景 | 预期结果 |
|----------|---------|
| 空事务 (0 个文件) | 创建空目录和空 manifest |
| 大文件 (100MB+) | 正常处理，不内存溢出 |
| 深层嵌套目录 | 递归创建目录成功 |
| 特殊字符文件名 | 正确处理空格、中文等字符 |

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| 原子移动跨文件系统失败 | 检测是否同一文件系统，否则使用复制+删除 |
| 写入过程中进程被终止 | 下次启动时清理残留的 .tmp-* 目录 |
| 磁盘满导致写入失败 | 预检查磁盘空间，设置最小空间阈值 |
| 目标目录被其他进程占用 | 重试机制 + 明确错误提示 |
| 文件权限问题 | 检查并报告具体权限错误 |
| 符号链接处理 | 不跟随符号链接，直接处理 |
| 备份目录累积 | 配置备份保留策略，自动清理旧备份 |
| 事务超时 | 设置事务超时时间，超时自动回滚 |
| 哈希计算性能 | 大文件使用流式计算，避免全量加载 |

### 故障恢复

```java
/**
 * 启动时清理残留的临时目录
 */
public void cleanupOrphanedTransactions(String outputDir) {
    Path outputPath = Paths.get(outputDir);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath, ".tmp-*")) {
        for (Path tmpDir : stream) {
            log.warn("发现残留临时目录，正在清理: {}", tmpDir);
            deleteRecursive(tmpDir);
        }
    }
}
```

---

**文档结束**
