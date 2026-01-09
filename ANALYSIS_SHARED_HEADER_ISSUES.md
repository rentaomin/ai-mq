# Shared Header 处理逻辑问题分析报告

## 执行摘要

项目中存在**两种完全不同的 Excel 文件格式**，但当前工具实现不符合真实业务逻辑：

1. **ISM v2.0 FIX mapping.xlsx** - 固定格式消息头规范（技术传输包装）
2. **create_app.xlsx** - 层级化/嵌套消息规范（业务载荷）

**当前实现的核心问题：**
- Metadata（Operation Name/ID）始终从 Request Sheet 提取，不从 Shared Header File 提取
- 对 Shared Header File 没有验证和格式检查
- 不支持 ISM v2.0 这类固定格式消息头的处理
- 当只有 Shared Header File 时（无 Request Sheet），系统无法工作

---

## 问题 1：Excel 文件格式不匹配

### 1.1 ISM v2.0 FIX mapping.xlsx 结构

**当前状况：**
```
工作簿: ISM v2.0 FIX mapping.xlsx
├─ 工作表: "ISM V2.0 FIX" (唯一工作表)
│  ├─ 行 1-8: 字段定义行（不是元数据行）
│  │  ├─ Column A: 字段名 (Element, EyeCatcher, ISMHdrVersNum, ...)
│  │  ├─ Column B: 起始位置 (固定格式消息的字节偏移)
│  │  ├─ Column C: 长度
│  │  ├─ Column D: 类型 (Character String, Numeric, ...)
│  │  ├─ Column E: 状态 (M=强制, O=可选)
│  │  └─ Columns F-L: 内容示例
│  └─ 行 9-90: 更多字段定义

特征:
✓ 固定格式消息头 (Fixed-Format Message Header)
✓ 字段按字节位置顺序定义
✗ 无 "Operation Name" / "Operation ID" 元数据
✗ 无 "Shared Header" / "Request" / "Response" 工作表
✗ 无 Seg lvl (分段级别) 列
✗ 列结构完全不同于 create_app.xlsx
```

**工具期望的格式：** (根据 MetadataExtractor 和 ExcelParser)
```
工作表: "Shared Header"（或任何其他）
├─ 行 1-7: 元数据行
│  ├─ 行 1, Col A: "Message Specification"
│  ├─ 行 2, Col C: Operation Name 值 (如 "Create Application from SMP")
│  ├─ 行 3, Col C: Operation ID 值 (如 "CreateAppSMP")
│  └─ 行 3, Col E: Version 值 (如 "01.00")
├─ 行 8: 列标题行
│  ├─ Col A: "Seg lvl"
│  ├─ Col B: "Field Name"
│  ├─ Col C: "Description"
│  └─ ... 其他标准列
└─ 行 9+: 字段数据行
```

**结论：** ISM v2.0 文件根本不符合工具当前的预期格式。

---

### 1.2 create_app.xlsx 结构（符合预期）

**当前状况：**
```
工作簿: create_app.xlsx
├─ 工作表: "Shared Header"
│  ├─ 行 1-7: 元数据行（与 Request/Response 相同）
│  │  ├─ 行 2, Col C: "Create application from SMP"
│  │  ├─ 行 3, Col C: "CreateAppSMP"
│  │  └─ 行 3, Col E: "01.00"
│  └─ 行 8: 列标题行（但无数据行）
├─ 工作表: "Request"
│  ├─ 行 1-7: 同上元数据行
│  ├─ 行 8: 列标题行 (Seg lvl, Field Name, Description, Length, ...)
│  └─ 行 9+: 字段数据行（层级化消息结构，使用 Seg lvl 表示嵌套）
└─ 工作表: "Response"
   ├─ 行 1-7: 同上元数据行
   ├─ 行 8: 列标题行
   └─ 行 9+: 字段数据行
```

**符合状况：** ✓ 完全符合工具预期格式。

**问题：** Shared Header 工作表只有元数据行，没有字段数据行。这意味着：
- 如果期望 Shared Header 包含字段定义，实际上没有字段
- 或者 Shared Header 只是一个元数据参考，实际字段在 Request 和 Response 中

---

## 问题 2：Metadata 提取逻辑缺陷

### 2.1 当前实现（ExcelParser.parse()，第 79-80 行）

```java
// Metadata 总是从 Request Sheet 提取，忽略 sharedHeaderFile
Metadata metadata = metadataExtractor.extract(
    sheets.getRequest(),      // ← 始终是 Request Sheet
    specFile,
    sharedHeaderFile          // ← 只用于记录文件路径，不用于元数据提取
);
```

**问题：**
1. **sharedHeaderFile 参数被忽略** - 不从 Shared Header File 中提取元数据
2. **假设 Request Sheet 始终存在** - 如果用户只提供 Shared Header File，系统会崩溃
3. **不支持 Shared Header File 包含元数据** - 即使文件中有 Operation Name/ID，也不会被提取

### 2.2 MetadataExtractor 设计（第 48 行）

```java
public Metadata extract(Sheet sheet, Path sourceFile, Path sharedHeaderFile) {
    // sheet 参数期望是 Request Sheet
    // 从硬编码的行和列提取：
    // - 行 2, 列 C: Operation Name
    // - 行 3, 列 C: Operation ID
    // - 行 3, 列 E: Version
}
```

**问题：**
- 只有一个 Sheet 参数，无法同时处理 Request Sheet 和 Shared Header File
- 当 sharedHeaderFile 被提供时，应该有 fallback 逻辑从中提取元数据
- 没有验证提取到的元数据是否有效

### 2.3 业务场景无法支持

**场景 1：纯 Shared Header File（无 Request Sheet）**
```
命令: java -jar tool.jar generate --shared-header=ISM_v2.xlsx
结果: ✗ 失败 - SheetDiscovery 要求 Request Sheet 必须存在
      ✗ 崩溃 - MetadataExtractor 尝试读取 null Request Sheet
```

**场景 2：Shared Header 包含操作元数据**
```
命令: java -jar tool.jar generate --input=create_app.xlsx --shared-header=header_with_metadata.xlsx
结果: ✓ 工作 - 使用 create_app.xlsx 的 Request Sheet 元数据
      ✗ 问题 - header_with_metadata.xlsx 的元数据被忽略
              如果 create_app.xlsx 的元数据缺失，无法从 Shared Header File 补救
```

**场景 3：嵌入 Shared Header（无元数据）**
```
命令: java -jar tool.jar generate --input=create_app.xlsx
结果: ✓ 工作 - 从 Request Sheet 提取元数据
      ✓ 工作 - 从嵌入 Shared Header Sheet 提取字段
      ✗ 设计问题 - Shared Header Sheet 中已有元数据（重复），但未利用
```

---

## 问题 3：Shared Header File 验证缺失

### 3.1 SharedHeaderLoader 的验证不足（第 62-84 行）

```java
public FieldGroup loadFromFile(Path sharedHeaderFile, Config config) {
    // 问题 1: 不验证文件是否真的是 header-only 文件
    // 问题 2: 不检查文件是否包含 Request/Response Sheet
    // 问题 3: 不验证工作表结构（如必需列是否存在）
    // 问题 4: 不检查元数据是否存在（虽然可能需要提取）

    Sheet headerSheet = workbook.getSheet(SHARED_HEADER_SHEET);
    if (headerSheet == null) {
        // 静默降级到第一个工作表
        if (workbook.getNumberOfSheets() > 0) {
            headerSheet = workbook.getSheetAt(0);  // ← 可能加载错误的工作表
        }
    }

    // 将解析委托给 ExcelParser.parseSheet()
    return parser.parseSheet(headerSheet, "Shared Header");
}
```

**验证缺陷：**
1. **无工作表名称验证** - 应该验证 Shared Header File 只有一个工作表，或者只有 Shared Header 工作表
2. **无结构验证** - 应该验证工作表包含所需的列（Seg lvl, Field Name, 等）
3. **无内容验证** - 应该验证没有 Request/Response 工作表
4. **无元数据验证** - 应该检查是否包含可提取的元数据

### 3.2 对 ISM v2.0 格式的影响

当用户错误地尝试加载 ISM v2.0 文件时：
```
loadFromFile(ISM_v2.0 FIX mapping.xlsx, config)
  ↓
找不到 "Shared Header" 工作表
  ↓
降级到第一个工作表 "ISM V2.0 FIX"
  ↓
调用 parseSheet(ISM_V2.0_FIX_sheet, "Shared Header")
  ↓
ColumnValidator.validateAndMapColumns() 失败
  ↓
异常: 列 "Seg lvl" 或 "Field Name" 不存在
```

**结果：** 错误信息不清楚，用户不知道问题根源是文件格式不匹配。

---

## 问题 4：文件类型优先级不够灵活

### 4.1 parseSharedHeader() 的优先级（第 243-251 行）

```java
private FieldGroup parseSharedHeader(SheetSet sheets, Path sharedHeaderFile) {
    // 优先级 1: 分离的 Shared Header 文件
    if (sharedHeaderFile != null) {
        return sharedHeaderLoader.loadFromFile(sharedHeaderFile, config);
    }
    // 优先级 2: 嵌入的 Shared Header Sheet
    else if (sheets.hasSharedHeader()) {
        return parseSheet(sheets.getSharedHeader(), "Shared Header");
    }
    // 优先级 3: 空（无 Shared Header）
    else {
        return new FieldGroup();
    }
}
```

**问题：**
1. **优先级是隐式的** - 当两者都存在时，分离文件会覆盖嵌入 Sheet，未记录此行为
2. **无合并策略** - 不支持合并两个来源的字段（可能是业务需求）
3. **无灵活性** - 无法配置优先级或合并行为

---

## 问题 5：SheetDiscovery 的硬编码期望

### 5.1 SheetDiscovery.discoverSheets()

```java
public SheetSet discoverSheets(Workbook workbook) {
    // Request 是强制的
    sheets.setRequest(findSheet(workbook, "Request"));
    if (sheets.getRequest() == null) {
        throw new ParseException("Required sheet 'Request' not found");
    }

    // Response 和 Shared Header 是可选的
    sheets.setResponse(findSheet(workbook, "Response"));
    sheets.setSharedHeader(findSheet(workbook, "Shared Header"));
}
```

**问题：**
1. **Request Sheet 强制要求** - 不支持 Shared Header Only 场景
2. **工作表名称硬编码** - 不支持自定义工作表名称
3. **不支持多个 Shared Header** - 如果需要合并多个来源

---

## 问题 6：CLI 和文件参数语义不清楚

### 6.1 CliOptions 中的参数处理

```
参数: --shared-header <file>
语义: 暂不清楚
问题:
  - 应该只接受 header-only 文件，还是支持任何包含 Shared Header 的文件？
  - 是否应该从此文件提取元数据？
  - 如果既有 --input 又有 --shared-header，如何解决冲突？
```

---

## 根本原因总结

| 层级 | 问题 | 根本原因 |
|------|------|--------|
| **架构** | 只支持一种消息类型（分层/嵌套） | 设计时未考虑固定格式消息（如 ISM） |
| **解析** | Metadata 总是从 Request 提取 | MetadataExtractor 设计假设 Request Sheet 始终存在 |
| **验证** | 无文件类型验证 | SharedHeaderLoader 过于宽松，接受任何工作表 |
| **优先级** | 当两个 Header 都存在时行为隐式 | 无配置选项，优先级硬编码 |
| **约束** | Request Sheet 强制 | SheetDiscovery 设计假设所有文件都有 Request/Response |

---

## 推荐修复方案

### R1: 增强 Metadata 提取（高优先级）

**修改 ExcelParser.parse() 和 MetadataExtractor**：
1. 添加 `extractMetadataFromSharedHeaderFile()` 方法
2. 实现 fallback 逻辑：
   - 优先尝试从 Request Sheet 提取
   - 如果 Request Sheet 不存在或缺失元数据，从 Shared Header File 提取
   - 如果两者都缺失，返回部分元数据或错误

```java
private Metadata parseMetadata(SheetSet sheets, Path sharedHeaderFile) {
    Metadata metadata = null;

    // 优先从 Request Sheet 提取（如果存在）
    if (sheets.getRequest() != null) {
        metadata = metadataExtractor.extract(sheets.getRequest(), specFile, sharedHeaderFile);
    }

    // Fallback: 如果 Request 缺失或元数据不完整，尝试 Shared Header File
    if (metadata == null || !metadataExtractor.validate(metadata)) {
        if (sharedHeaderFile != null) {
            metadata = extractMetadataFromSharedHeaderFile(sharedHeaderFile);
        }
    }

    // Fallback: 尝试嵌入 Shared Header Sheet
    if (metadata == null || !metadataExtractor.validate(metadata)) {
        if (sheets.hasSharedHeader()) {
            metadata = metadataExtractor.extract(sheets.getSharedHeader(), specFile, null);
        }
    }

    return metadata != null ? metadata : new Metadata(); // 返回空元数据
}
```

### R2: 添加文件格式验证（中优先级）

在 SharedHeaderLoader 中添加验证：
1. 验证工作表结构（必需的列）
2. 验证文件不包含 Request/Response Sheet
3. 验证元数据行存在（如果预期）
4. 对 ISM 格式提供有用的错误消息

### R3: 支持纯 Shared Header 场景（中优先级）

修改 SheetDiscovery：
1. 使 Request Sheet 可选（如果 Shared Header 存在）
2. 添加配置选项指定文件类型
3. 允许 Shared Header Only 处理模式

### R4: 改进 CLI 参数语义（低优先级）

增加文档和验证：
1. 明确 `--shared-header` 的含义和期望的文件格式
2. 添加 `--file-type` 参数以支持多种格式（HIERARCHICAL, FIXED_FORMAT 等）
3. 添加验证检查 `--input` 和 `--shared-header` 的兼容性

---

## 对 ISM v2.0 的特殊考虑

**ISM v2.0 FIX mapping.xlsx 不应该用当前工具处理**，原因：
1. 完全不同的列结构（无 Seg lvl）
2. 完全不同的编码方式（字节位置，不是层级）
3. 无操作元数据（无 Operation Name/ID）
4. 目的不同（传输包装头，不是消息载荷）

**建议：**
- 为固定格式消息创建专门的工具/模块
- 或在工具中添加格式检测和特殊处理逻辑
- 或明确文档说明 ISM 文件不支持此工具

---

## 实施优先级

1. **R1: Metadata 提取增强** - 必须修复，阻挡多个业务场景
2. **R2: 文件格式验证** - 应该修复，提高用户体验和错误诊断
3. **R3: 纯 Shared Header 支持** - 可考虑，取决于业务需求
4. **R4: CLI 改进** - 可考虑，增强文档和清晰度

---

**文档创建时间:** 2026-01-10
**分析范围:**
- ISM v2.0 FIX mapping.xlsx
- create_app.xlsx
- SharedHeaderLoader.java
- ExcelParser.java
- MetadataExtractor.java
- SheetDiscovery.java
