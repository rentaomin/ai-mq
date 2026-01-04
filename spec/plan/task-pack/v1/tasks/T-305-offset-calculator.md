# T-305 偏移量计算器

## Goal

实现偏移量计算器，根据中间 JSON Tree 中的字段定义（length、datatype）计算每个字段在定长消息中的字节偏移量（offset）和累计位置，用于后续报文验证和调试。

---

## In Scope / Out of Scope

### In Scope

- 解析中间 JSON Tree 获取字段列表及其长度信息
- 计算每个字段的起始偏移量（从 0 开始）
- 处理嵌套结构（对象/数组）的偏移量累计
- 处理数组类型字段的重复偏移量计算
- 生成偏移量表（字段名 -> offset, length）
- 支持请求消息和响应消息的偏移量计算
- 输出偏移量报告（可读格式 + JSON 格式）

### Out of Scope

- 实际消息内容验证（由 T-306 负责）
- 消息编解码（不涉及 EBCDIC/ASCII 转换）
- 运行时消息处理

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `output/json/spec-tree.json` | T-108 | 中间 JSON Tree（包含 length、datatype、occurrenceCount） |
| `config/datatype-sizes.yaml` | 配置 | 数据类型的默认字节大小（可选覆盖） |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| OffsetCalculator.java | `src/main/java/com/example/mqtool/calculator/OffsetCalculator.java` | 偏移量计算器主类 |
| OffsetTable.java | `src/main/java/com/example/mqtool/calculator/OffsetTable.java` | 偏移量表数据模型 |
| OffsetEntry.java | `src/main/java/com/example/mqtool/calculator/OffsetEntry.java` | 单个字段偏移量条目 |
| offset-table.json | `output/offset/offset-table.json` | 偏移量表 JSON 输出 |
| offset-report.txt | `output/offset/offset-report.txt` | 可读格式偏移量报告 |
| 单元测试 | `src/test/java/com/example/mqtool/calculator/OffsetCalculatorTest.java` | 计算器测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-108 | JSON Tree 序列化 | 需要完整的中间 JSON Tree 作为输入 |
| T-004 | 数据模型定义 | 复用 FieldNode、Metadata 模型 |

---

## Implementation Notes

### 偏移量计算规则

1. **基本字段**
   ```
   offset = 前一个字段的 (offset + length)
   第一个字段 offset = 0
   ```

2. **嵌套对象**
   ```
   对象内字段连续排列
   对象的总长度 = 所有子字段长度之和
   对象后续字段的 offset = 对象起始 offset + 对象总长度
   ```

3. **数组类型（occurrenceCount > 1）**
   ```
   数组总长度 = 单元素长度 * occurrenceCount
   数组内每个元素的 offset 需要单独计算
   ```

4. **共享头部**
   - 请求和响应消息可能共享头部字段
   - 头部偏移量固定，从 0 开始

### 数据结构

```java
public class OffsetEntry {
    private String fieldPath;      // 完整字段路径，如 "header.messageType"
    private String fieldName;      // 字段名
    private int offset;            // 起始偏移量（字节）
    private int length;            // 字段长度（字节）
    private int endOffset;         // 结束偏移量 = offset + length - 1
    private String datatype;       // 原始数据类型
    private int occurrenceCount;   // 重复次数
    private int nestingLevel;      // 嵌套层级
}

public class OffsetTable {
    private String messageType;     // 消息类型（request/response）
    private int totalLength;        // 消息总长度
    private List<OffsetEntry> entries;

    public OffsetEntry findByPath(String path);
    public OffsetEntry findByOffset(int offset);
    public List<OffsetEntry> getFieldsInRange(int start, int end);
}
```

### 代码模式

```java
public class OffsetCalculator {

    public OffsetTable calculate(JsonNode specTree) {
        List<OffsetEntry> entries = new ArrayList<>();
        int currentOffset = 0;

        currentOffset = calculateRecursive(specTree, "", currentOffset, 0, entries);

        return new OffsetTable(specTree.get("messageType").asText(),
                               currentOffset, entries);
    }

    private int calculateRecursive(JsonNode node, String parentPath,
                                    int startOffset, int level,
                                    List<OffsetEntry> entries) {
        int currentOffset = startOffset;

        for (JsonNode field : node.get("fields")) {
            String fieldPath = parentPath + "." + field.get("name").asText();
            int length = field.get("length").asInt();
            int occurrenceCount = field.get("occurrenceCount").asInt(1);

            if (isNestedObject(field)) {
                // 递归处理嵌套对象
                for (int i = 0; i < occurrenceCount; i++) {
                    currentOffset = calculateRecursive(
                        field, fieldPath + "[" + i + "]",
                        currentOffset, level + 1, entries);
                }
            } else {
                // 简单字段
                entries.add(new OffsetEntry(
                    fieldPath, field.get("name").asText(),
                    currentOffset, length,
                    field.get("datatype").asText(),
                    occurrenceCount, level
                ));
                currentOffset += length * occurrenceCount;
            }
        }

        return currentOffset;
    }
}
```

### 输出格式示例

**offset-table.json:**
```json
{
  "messageType": "request",
  "totalLength": 1024,
  "entries": [
    {
      "fieldPath": "header.messageType",
      "fieldName": "messageType",
      "offset": 0,
      "length": 4,
      "endOffset": 3,
      "datatype": "AN",
      "occurrenceCount": 1,
      "nestingLevel": 1
    },
    {
      "fieldPath": "header.version",
      "fieldName": "version",
      "offset": 4,
      "length": 2,
      "endOffset": 5,
      "datatype": "N",
      "occurrenceCount": 1,
      "nestingLevel": 1
    }
  ]
}
```

**offset-report.txt:**
```
Message Type: request
Total Length: 1024 bytes

OFFSET  LENGTH  END     PATH                          DATATYPE
------  ------  ------  ----------------------------  --------
0       4       3       header.messageType            AN
4       2       5       header.version                N
6       10      15      header.transactionId          AN
...
```

---

## Acceptance Criteria

1. [ ] 能够正确计算简单字段的偏移量
2. [ ] 能够正确计算嵌套对象的偏移量
3. [ ] 能够正确处理数组类型字段（occurrenceCount > 1）
4. [ ] 能够正确计算消息总长度
5. [ ] 偏移量表支持按路径查询
6. [ ] 偏移量表支持按偏移量范围查询
7. [ ] 输出 JSON 格式偏移量表
8. [ ] 输出人类可读格式报告
9. [ ] 处理空消息（无字段）返回空表
10. [ ] 处理字段长度为 0 的情况（记录警告）

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 简单平铺字段列表 | 偏移量递增，总长度正确 |
| 单层嵌套对象 | 嵌套字段偏移量连续，父对象后续字段正确跳过 |
| 多层嵌套对象 | 递归计算正确，层级标记正确 |
| 数组字段 (occurrenceCount=3) | 总长度 = 单元素长度 * 3 |
| 嵌套数组（对象数组） | 每个数组元素的内部字段偏移量正确 |
| 空字段列表 | 返回空表，总长度 = 0 |
| 字段长度为 0 | 记录警告，偏移量不变 |
| 按路径查询 | 返回正确的 OffsetEntry |
| 按偏移量范围查询 | 返回范围内所有字段 |

### 集成测试

- 使用真实 spec-tree.json 计算偏移量
- 验证总长度与 Excel 规范中的预期值一致
- 验证首字段 offset=0，末字段 endOffset = totalLength - 1

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| 字段长度缺失或为空 | 报错并中止，要求 JSON Tree 完整 |
| 变长字段（长度由其他字段决定） | 本版本不支持，记录警告，使用最大长度 |
| 超大消息导致溢出 | 使用 long 类型存储偏移量 |
| occurrenceCount 为 0 | 视为可选字段，不计入偏移量 |
| 嵌套层级过深 | 设置最大深度限制（如 10 层） |
| 字段名重复 | 使用完整路径区分 |

---

**文档结束**
