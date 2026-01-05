# T-306 报文验证器

## Goal

实现报文验证器 (MessageValidator)，基于偏移量表 (OffsetTable) 验证实际 MQ 报文的结构正确性。支持验证报文长度、字段边界、数据类型格式、必填字段填充，并生成详细的验证报告。

---

## In Scope / Out of Scope

### In Scope

- 加载偏移量表 (T-305 输出)
- 验证报文总长度与规范定义是否一致
- 验证每个字段的字节边界是否对齐
- 验证字段内容的数据类型格式 (如 N 类型只含数字，AN 类型为字母数字混合)
- 验证必填字段是否为空白或默认填充
- 验证 hardcode 字段是否匹配预定义值
- 支持请求报文和响应报文的验证
- 生成字段级验证报告 (JSON + 人类可读格式)
- 提供 exit code 表示验证结果

### Out of Scope

- 报文编解码 (EBCDIC/ASCII 转换由外部处理)
- 业务逻辑校验 (如字段间依赖规则)
- 报文修复或自动纠错
- 运行时报文处理 (仅用于开发/测试阶段验证)

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `output/offset/offset-table.json` | T-305 | 偏移量表，包含字段路径、offset、length、datatype |
| `input/message.bin` 或 `input/message.txt` | 用户提供 | 待验证的报文 (二进制或文本格式) |
| `output/json/spec-tree.json` | T-108 | 中间 JSON Tree (含 hardcode 值、默认值等元数据) |
| `config/validation-rules.yaml` | 配置 | 验证规则配置 (可选字段宽松模式等) |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| MessageValidator.java | `src/main/java/com/example/mqtool/validator/MessageValidator.java` | 报文验证器主类 |
| MessageValidationResult.java | `src/main/java/com/example/mqtool/validator/MessageValidationResult.java` | 验证结果数据模型 |
| FieldValidationError.java | `src/main/java/com/example/mqtool/validator/FieldValidationError.java` | 字段验证错误详情 |
| validation-report.json | `output/validation/message-validation-report.json` | JSON 格式验证报告 |
| validation-report.txt | `output/validation/message-validation-report.txt` | 人类可读格式验证报告 |
| 单元测试 | `src/test/java/com/example/mqtool/validator/MessageValidatorTest.java` | 验证器测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-305 | 偏移量计算器 | 需要偏移量表作为验证基准 |
| T-108 | JSON Tree 序列化 | 需要 spec-tree.json 获取 hardcode 值、默认值等元数据 |
| T-002 | 核心接口定义 | 实现 Validator 接口 |
| T-005 | 错误处理框架 | 使用 ValidationException、ExitCodes |

---

## Implementation Notes

### 验证规则

#### 1. 长度验证
```
报文实际长度 == OffsetTable.totalLength
如果不匹配：ERROR - 报文长度不一致，期望 {expected}，实际 {actual}
```

#### 2. 字段边界验证
```
对于每个字段：
  - 提取 message[offset : offset + length]
  - 验证提取范围不超出报文边界
```

#### 3. 数据类型格式验证

| Datatype | 验证规则 |
|----------|---------|
| N (Numeric) | 全部为数字 0-9，可前导零 |
| AN (Alphanumeric) | 字母 + 数字 + 空格 |
| A (Alpha) | 仅字母 A-Z, a-z |
| X (Any) | 任意字符 |
| S (Signed) | 可选符号 +/- 后跟数字 |

#### 4. 必填字段验证
```
如果 minOccurs >= 1 或 required = true：
  - 字段内容不能全为空格
  - 字段内容不能全为默认填充值 (如 0x00)
```

#### 5. Hardcode 值验证
```
如果 spec-tree 中定义了 hardcodeValue：
  - 报文中该字段值必须完全匹配 hardcodeValue
```

### 数据结构

```java
public class MessageValidationResult {
    private String messageType;         // request / response
    private boolean valid;              // 整体是否通过
    private int totalErrors;            // 错误数
    private int totalWarnings;          // 警告数
    private List<FieldValidationError> errors;
    private String messagePath;         // 输入报文路径
    private String specTreePath;        // 规范树路径
    private Instant timestamp;
}

public class FieldValidationError {
    private String fieldPath;           // 字段路径
    private int offset;                 // 偏移量
    private int length;                 // 字段长度
    private String severity;            // ERROR / WARNING
    private String category;            // LENGTH / TYPE / REQUIRED / HARDCODE / BOUNDARY
    private String expectedValue;       // 期望值 (如适用)
    private String actualValue;         // 实际值 (如适用)
    private String message;             // 人类可读错误描述
}
```

### 代码模式

```java
public class MessageValidator implements Validator {

    private final OffsetTable offsetTable;
    private final JsonNode specTree;
    private final ValidationConfig config;

    public MessageValidationResult validate(byte[] message) {
        List<FieldValidationError> errors = new ArrayList<>();

        // 1. 长度验证
        if (message.length != offsetTable.getTotalLength()) {
            errors.add(createLengthError(offsetTable.getTotalLength(), message.length));
        }

        // 2. 遍历每个字段进行验证
        for (OffsetEntry entry : offsetTable.getEntries()) {
            validateField(entry, message, errors);
        }

        return new MessageValidationResult(
            offsetTable.getMessageType(),
            errors.isEmpty(),
            errors
        );
    }

    private void validateField(OffsetEntry entry, byte[] message,
                                List<FieldValidationError> errors) {
        // 边界检查
        if (entry.getOffset() + entry.getLength() > message.length) {
            errors.add(createBoundaryError(entry));
            return;
        }

        // 提取字段内容
        byte[] fieldBytes = Arrays.copyOfRange(message,
            entry.getOffset(),
            entry.getOffset() + entry.getLength());
        String fieldValue = new String(fieldBytes, charset);

        // 数据类型格式验证
        if (!isValidFormat(fieldValue, entry.getDatatype())) {
            errors.add(createTypeError(entry, fieldValue));
        }

        // 必填字段验证
        if (isRequired(entry) && isBlankOrDefault(fieldValue)) {
            errors.add(createRequiredError(entry, fieldValue));
        }

        // Hardcode 值验证
        String hardcodeValue = getHardcodeValue(entry.getFieldPath());
        if (hardcodeValue != null && !hardcodeValue.equals(fieldValue)) {
            errors.add(createHardcodeError(entry, hardcodeValue, fieldValue));
        }
    }
}
```

### 输出格式示例

**message-validation-report.json:**
```json
{
  "messageType": "request",
  "valid": false,
  "totalErrors": 2,
  "totalWarnings": 1,
  "messagePath": "input/message.bin",
  "specTreePath": "output/json/spec-tree.json",
  "timestamp": "2026-01-05T14:30:00Z",
  "errors": [
    {
      "fieldPath": "header.messageType",
      "offset": 0,
      "length": 4,
      "severity": "ERROR",
      "category": "HARDCODE",
      "expectedValue": "CCAP",
      "actualValue": "XXXX",
      "message": "Hardcode 值不匹配：期望 'CCAP'，实际 'XXXX'"
    },
    {
      "fieldPath": "body.accountNumber",
      "offset": 50,
      "length": 16,
      "severity": "ERROR",
      "category": "TYPE",
      "expectedValue": "N (仅数字)",
      "actualValue": "123456ABCD______",
      "message": "数据类型错误：字段应为纯数字 (N)，但包含非数字字符"
    }
  ]
}
```

**message-validation-report.txt:**
```
Message Validation Report
=========================
Message Type: request
Validation Result: FAILED
Total Errors: 2
Total Warnings: 1
Timestamp: 2026-01-05 14:30:00

ERRORS:
-------
[ERROR] header.messageType (offset: 0, length: 4)
  Category: HARDCODE
  Expected: CCAP
  Actual:   XXXX
  Message:  Hardcode 值不匹配

[ERROR] body.accountNumber (offset: 50, length: 16)
  Category: TYPE
  Expected: N (仅数字)
  Actual:   123456ABCD______
  Message:  数据类型错误：字段应为纯数字，但包含非数字字符

WARNINGS:
---------
[WARNING] body.optionalField (offset: 100, length: 10)
  Category: REQUIRED
  Message:  可选字段为空白（非严格模式下忽略）
```

### Exit Code 定义

| Exit Code | 含义 |
|-----------|------|
| 0 | 验证通过，无错误 |
| 51 | 报文长度不匹配 |
| 52 | 字段数据类型错误 |
| 53 | 必填字段为空 |
| 54 | Hardcode 值不匹配 |
| 55 | 字段边界越界 |
| 56 | 多种错误 |

---

## Acceptance Criteria

1. [ ] 能够加载偏移量表 (offset-table.json)
2. [ ] 能够正确验证报文总长度
3. [ ] 能够验证每个字段的边界不越界
4. [ ] 能够验证 Numeric (N) 类型字段仅包含数字
5. [ ] 能够验证 Alphanumeric (AN) 类型字段格式
6. [ ] 能够检测必填字段为空白的情况
7. [ ] 能够验证 hardcode 字段值匹配
8. [ ] 生成 JSON 格式验证报告
9. [ ] 生成人类可读格式验证报告
10. [ ] 返回正确的 exit code
11. [ ] 支持配置宽松模式 (忽略 WARNING 级别错误)
12. [ ] 报告包含字段路径、偏移量、期望值、实际值等完整上下文

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 报文长度正确 | 长度验证通过 |
| 报文长度过短 | ERROR: LENGTH，返回 exit code 51 |
| 报文长度过长 | ERROR: LENGTH，返回 exit code 51 |
| N 类型字段全为数字 | 类型验证通过 |
| N 类型字段含字母 | ERROR: TYPE，返回 exit code 52 |
| AN 类型字段含特殊字符 | 根据配置 ERROR 或 WARNING |
| 必填字段有值 | 必填验证通过 |
| 必填字段全空格 | ERROR: REQUIRED，返回 exit code 53 |
| Hardcode 字段值正确 | Hardcode 验证通过 |
| Hardcode 字段值错误 | ERROR: HARDCODE，返回 exit code 54 |
| 字段边界在报文范围内 | 边界验证通过 |
| 字段边界越界 | ERROR: BOUNDARY，返回 exit code 55 |
| 多种错误同时存在 | 返回 exit code 56 |

### 集成测试

- 使用真实 offset-table.json 和测试报文进行验证
- 验证 golden 报文（已知正确的报文）通过验证
- 验证故意构造的错误报文产生预期错误

### Golden 测试

- 验证报告格式稳定性（JSON 结构、字段顺序）
- 验证报告内容与 golden 文件一致

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| 报文编码不确定 (EBCDIC/ASCII) | 提供配置项指定编码，默认 ASCII |
| 字段包含不可打印字符 | 在报告中使用 hex 表示，如 `\x00` |
| 大型报文 (10KB+) | 使用流式读取，避免内存溢出 |
| 偏移量表与报文版本不匹配 | 验证 spec 版本标记，不匹配时警告 |
| 数组字段的重复验证 | 对数组每个元素单独验证，路径带索引 |
| 空报文 (长度为 0) | 直接报 LENGTH 错误，totalLength 为 0 时视为配置错误 |
| 字段长度为 0 | 跳过该字段验证，记录 WARNING |

---

**文档结束**
