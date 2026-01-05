# T-304 跨制品一致性验证

## Goal

实现一个跨制品一致性验证器 (ConsistencyValidator)，用于验证从同一 Excel 规范生成的三类制品（XML Bean、Java Bean、OpenAPI YAML）之间的语义一致性和结构对齐。

确保：
1. 字段名、类型、嵌套结构在三个制品中保持一致
2. 数组/对象标记在所有制品中对齐
3. 必填/可选属性、默认值、枚举值等元数据在各制品中一致
4. 发现并报告任何不一致性，提供清晰的诊断信息

---

## In Scope / Out of Scope

### In Scope
- 读取并解析三个验证器（T-301, T-302, T-303）的验证结果
- 提取每个制品的规范化字段树（normalized field tree）
- 对比字段名、数据类型、嵌套层级、数组标记、必填属性
- 检测以下一致性问题：
  - 字段缺失（某制品有而其他制品无）
  - 类型不匹配（如 XML 为 string，Java 为 Integer）
  - 嵌套结构不一致（如 XML 为 object，Java 为 array）
  - 枚举值不对齐（Java 和 OpenAPI 枚举值顺序或内容不同）
  - 必填属性冲突（XML minOccurs=1 但 OpenAPI required=false）
- 生成一致性验证报告（JSON + Markdown）
- 提供 exit code（0=一致，非0=发现不一致）

### Out of Scope
- 修复不一致性（仅报告，不自动修复）
- 验证业务逻辑规则（如字段间依赖关系）
- 性能优化（假设制品文件数量合理）
- 验证 Excel 原始文件（依赖于各单独验证器的结果）

---

## Inputs

### 文件路径
- **XML Bean 验证结果**: `{output-dir}/validation/xml-bean-validation.json`
  - 由 T-301 生成
  - 包含 XML Bean 的字段树、类型映射、验证错误列表

- **Java Bean 验证结果**: `{output-dir}/validation/java-bean-validation.json`
  - 由 T-302 生成
  - 包含 Java Bean 的字段树、类型映射、验证错误列表

- **OpenAPI 验证结果**: `{output-dir}/validation/openapi-validation.json`
  - 由 T-303 生成
  - 包含 OpenAPI Schema 的字段树、类型映射、验证错误列表

### 配置参数
- `consistency.strict-mode`: boolean（是否严格模式，默认 true）
- `consistency.ignore-fields`: string[]（忽略的字段路径，如 `header.timestamp`）
- `consistency.type-mapping-rules`: Map（跨制品类型映射规则）

### 依赖接口
- `Validator` 接口（由 T-002 定义）
- `ValidationResult` 数据模型（由 T-005 定义）

---

## Outputs

### 文件路径
- **一致性验证报告（JSON）**: `{output-dir}/validation/consistency-report.json`
  ```json
  {
    "timestamp": "2026-01-05T10:30:00Z",
    "status": "FAIL",
    "totalIssues": 5,
    "issues": [
      {
        "severity": "ERROR",
        "category": "TYPE_MISMATCH",
        "fieldPath": "request.body.accountNumber",
        "details": {
          "xml": { "type": "xs:string", "maxLength": 16 },
          "java": { "type": "String", "annotations": ["@Size(max=16)"] },
          "openapi": { "type": "integer", "format": "int64" }
        },
        "message": "类型不一致：XML 和 Java 为 string，但 OpenAPI 为 integer"
      }
    ],
    "summary": {
      "fieldCount": { "xml": 42, "java": 42, "openapi": 41 },
      "matchedFields": 37,
      "missingFields": { "openapi": ["request.body.internalFlag"] },
      "typeMismatches": 3,
      "structureMismatches": 1
    }
  }
  ```

- **一致性验证报告（Markdown）**: `{output-dir}/validation/consistency-report.md`
  - 人类可读的报告，包含表格和详细描述

- **Exit Code**:
  - `0`: 所有制品一致
  - `41`: 发现类型不匹配
  - `42`: 发现结构不匹配
  - `43`: 发现字段缺失
  - `44`: 发现多种不一致性

### Java 接口
```java
public interface ConsistencyValidator extends Validator {
    ConsistencyReport validate(
        ValidationResult xmlResult,
        ValidationResult javaResult,
        ValidationResult openApiResult
    ) throws ValidationException;
}

public class ConsistencyReport {
    private String status; // PASS / FAIL
    private int totalIssues;
    private List<ConsistencyIssue> issues;
    private ConsistencySummary summary;
    // getters, setters
}

public class ConsistencyIssue {
    private String severity; // ERROR / WARNING
    private String category; // TYPE_MISMATCH / STRUCTURE_MISMATCH / MISSING_FIELD
    private String fieldPath;
    private Map<String, Object> details; // 各制品的字段详情
    private String message;
}
```

---

## Dependencies

### 前置任务
- **T-301**: XML Bean 验证器（提供 XML 验证结果）
- **T-302**: Java Bean 验证器（提供 Java 验证结果）
- **T-303**: OpenAPI 验证器（提供 OpenAPI 验证结果）

### 依赖组件
- T-002: `Validator` 接口
- T-005: `ValidationException`, `ExitCodes`
- T-004: `FieldNode`, `Metadata`（用于字段树规范化）

---

## Implementation Notes

### 核心算法

#### 1. 字段树规范化
- 从三个验证结果中提取字段树
- 规范化字段路径（如 `request.body.accountNumber`）
- 构建统一的字段树结构用于对比

#### 2. 类型映射对照表
定义跨制品类型映射规则：
```yaml
type-mappings:
  - xml: "xs:string"
    java: "String"
    openapi: "string"
  - xml: "xs:int"
    java: "Integer"
    openapi: "integer/int32"
  - xml: "xs:long"
    java: "Long"
    openapi: "integer/int64"
  - xml: "xs:decimal"
    java: "BigDecimal"
    openapi: "number"
  - xml: "xs:boolean"
    java: "Boolean"
    openapi: "boolean"
```

#### 3. 对比策略
- **字段存在性检查**: 遍历所有字段路径，检查是否在三个制品中都存在
- **类型一致性检查**: 使用类型映射表验证类型对应关系
- **结构一致性检查**:
  - 数组 vs 对象（XML maxOccurs > 1 → Java List → OpenAPI array）
  - 嵌套层级对齐
- **元数据一致性检查**:
  - 必填属性（XML minOccurs=1 → Java @NotNull → OpenAPI required）
  - 枚举值（Java enum values → OpenAPI enum list）
  - 默认值、长度限制等

#### 4. 错误聚合
- 按字段路径分组
- 按严重性排序（ERROR > WARNING）
- 提供修复建议（如"检查 Excel 规范中 accountNumber 的 datatype 定义"）

### 关键实现细节

#### 字段路径生成
```java
// 示例：从嵌套结构生成路径
// XML: <request><body><accountNumber>...</accountNumber></body></request>
// 路径: "request.body.accountNumber"
private String buildFieldPath(FieldNode node, String prefix) {
    String currentPath = prefix.isEmpty() ? node.getName() : prefix + "." + node.getName();
    return currentPath;
}
```

#### 类型对比逻辑
```java
private boolean isTypeConsistent(String xmlType, String javaType, String openApiType) {
    TypeMapping mapping = typeMappingRules.get(xmlType);
    if (mapping == null) return false;
    return mapping.getJavaType().equals(javaType)
        && mapping.getOpenApiType().equals(openApiType);
}
```

### 特殊情况处理

1. **groupId 和 occurrenceCount**:
   - 这两个字段仅在 XML Bean 生成时保留
   - Java Bean 和 OpenAPI 中应排除
   - 验证时需忽略这些字段

2. **数组表示差异**:
   - XML: `maxOccurs="unbounded"` 或数值 > 1
   - Java: `List<T>`
   - OpenAPI: `type: array, items: {...}`

3. **枚举处理**:
   - Java: enum class 带辅助方法
   - OpenAPI: `enum: [...]` 数组
   - 验证枚举值顺序和完整性

4. **保留字段**:
   - Java 可能使用 `_` 前缀规避保留字（如 `_class`）
   - OpenAPI 使用 `x-` 扩展字段
   - 需建立映射规则

---

## Acceptance Criteria

### 功能完整性
1. ✅ 成功读取三个验证器的 JSON 结果文件
2. ✅ 正确提取并规范化所有字段路径
3. ✅ 准确检测以下不一致性：
   - 类型不匹配
   - 字段缺失
   - 数组/对象结构不匹配
   - 必填属性冲突
   - 枚举值不一致
4. ✅ 生成 JSON 和 Markdown 两种格式的报告
5. ✅ 返回正确的 exit code（见 Outputs 章节）

### 准确性
6. ✅ 使用类型映射表正确识别等价类型（如 xs:string = String = string）
7. ✅ 忽略配置中指定的字段（`consistency.ignore-fields`）
8. ✅ 正确处理 groupId/occurrenceCount 特殊字段
9. ✅ 数组检测逻辑与三个制品的表示方式一致

### 可用性
10. ✅ 报告包含清晰的错误描述和字段路径
11. ✅ Markdown 报告以表格形式展示，便于人工审查
12. ✅ 提供修复建议（如"检查 Excel 第 X 行字段定义"）

---

## Tests

### 单元测试

#### 1. 类型映射验证
```java
@Test
void testTypeConsistency_AllMatch() {
    // XML: xs:string, Java: String, OpenAPI: string
    assertTrue(validator.isTypeConsistent("xs:string", "String", "string"));
}

@Test
void testTypeConsistency_Mismatch() {
    // XML: xs:string, Java: String, OpenAPI: integer (不一致)
    assertFalse(validator.isTypeConsistent("xs:string", "String", "integer"));
}
```

#### 2. 字段路径规范化
```java
@Test
void testFieldPathNormalization() {
    FieldNode root = createTestTree(); // request -> body -> accountNumber
    String path = validator.buildFieldPath(root.getChildren().get(0).getChildren().get(0), "");
    assertEquals("request.body.accountNumber", path);
}
```

#### 3. 数组检测
```java
@Test
void testArrayDetection() {
    // XML: maxOccurs="unbounded"
    // Java: List<Transaction>
    // OpenAPI: type: array
    assertTrue(validator.isArrayConsistent(xmlField, javaField, openApiField));
}
```

#### 4. 字段缺失检测
```java
@Test
void testMissingFieldDetection() {
    ValidationResult xml = createXmlResult(List.of("field1", "field2"));
    ValidationResult java = createJavaResult(List.of("field1", "field2"));
    ValidationResult openApi = createOpenApiResult(List.of("field1")); // 缺少 field2

    ConsistencyReport report = validator.validate(xml, java, openApi);
    assertEquals(1, report.getSummary().getMissingFields().get("openapi").size());
    assertEquals("field2", report.getSummary().getMissingFields().get("openapi").get(0));
}
```

### 集成测试

#### 5. 端到端验证（一致场景）
```java
@Test
void testEndToEnd_AllConsistent() {
    ValidationResult xml = loadFromFile("test/validation/xml-valid.json");
    ValidationResult java = loadFromFile("test/validation/java-valid.json");
    ValidationResult openApi = loadFromFile("test/validation/openapi-valid.json");

    ConsistencyReport report = validator.validate(xml, java, openApi);
    assertEquals("PASS", report.getStatus());
    assertEquals(0, report.getTotalIssues());
}
```

#### 6. 端到端验证（不一致场景）
```java
@Test
void testEndToEnd_TypeMismatch() {
    ValidationResult xml = loadFromFile("test/validation/xml-valid.json");
    ValidationResult java = loadFromFile("test/validation/java-valid.json");
    ValidationResult openApi = loadFromFile("test/validation/openapi-type-mismatch.json");

    ConsistencyReport report = validator.validate(xml, java, openApi);
    assertEquals("FAIL", report.getStatus());
    assertTrue(report.getTotalIssues() > 0);
    assertTrue(report.getIssues().stream()
        .anyMatch(i -> i.getCategory().equals("TYPE_MISMATCH")));
}
```

### Golden 测试

#### 7. 报告格式稳定性
```java
@Test
void testReportFormat_GoldenFile() {
    ConsistencyReport report = validator.validate(xmlResult, javaResult, openApiResult);
    String actualJson = JsonWriter.toJson(report);
    String expectedJson = readGoldenFile("golden/consistency-report.json");

    assertEquals(expectedJson, actualJson);
}
```

### 边界条件测试

#### 8. 空字段树
```java
@Test
void testEmptyFieldTree() {
    ValidationResult empty = createEmptyResult();
    ConsistencyReport report = validator.validate(empty, empty, empty);
    assertEquals("PASS", report.getStatus());
}
```

#### 9. 大型字段树（100+ 字段）
```java
@Test
void testLargeFieldTree() {
    ValidationResult large = createLargeResult(150); // 150 个字段
    ConsistencyReport report = validator.validate(large, large, large);
    assertEquals("PASS", report.getStatus());
    assertEquals(150, report.getSummary().getMatchedFields());
}
```

---

## Risks / Edge Cases

### 风险

1. **类型映射不完整**
   - 风险：Excel 中存在未定义的 datatype，导致类型映射失败
   - 缓解：维护完整的类型映射表，未知类型记录 WARNING 而非 ERROR

2. **字段路径冲突**
   - 风险：不同制品中字段路径格式不一致（如 camelCase vs snake_case）
   - 缓解：规范化所有路径为统一格式（camelCase）

3. **性能问题**
   - 风险：大型规范（1000+ 字段）导致验证缓慢
   - 缓解：使用索引结构（HashMap）加速查找，避免嵌套循环

4. **版本兼容性**
   - 风险：验证器输出格式变化导致解析失败
   - 缓解：定义稳定的验证结果 schema，使用版本标记

### 边界情况

1. **循环引用**
   - 场景：字段树中存在循环引用（虽然 Excel 规范不应出现）
   - 处理：检测循环并抛出 ValidationException

2. **特殊字符字段名**
   - 场景：字段名包含 `.` 或 `/` 等字符
   - 处理：使用转义规则或替代分隔符（如 `->` ）

3. **多版本制品**
   - 场景：验证不同版本生成的制品
   - 处理：检查版本标记，不同版本间不进行一致性验证

4. **部分验证失败**
   - 场景：某个单独验证器（T-301/302/303）失败，无输出
   - 处理：检查输入文件是否存在，提前抛出异常并提示依赖任务失败

5. **枚举顺序差异**
   - 场景：Java enum 顺序与 OpenAPI enum 数组顺序不同（但值相同）
   - 处理：配置是否严格检查顺序（默认仅检查值集合一致性）

---

**任务结束**
