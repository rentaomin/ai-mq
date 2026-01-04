# T-201 Response Sheet 解析

## Goal

扩展 Excel 解析器以支持 Response Sheet 的解析，处理 Response 特有的字段结构和验证规则，将 Response Sheet 数据正确转换为 FieldGroup 并填充到 MessageModel.response 中。

## In Scope / Out of Scope

**In Scope**:
- ResponseSheetParser 类实现（或 ExcelParser 扩展）
- Response Sheet 特有列的识别与处理
- Response 字段验证规则
- 与 Request Sheet 解析逻辑的复用
- Response 特有的 Seg lvl 嵌套处理
- Response 字段的对象/数组检测
- Response 字段的命名规范化

**Out of Scope**:
- Inbound XML 生成（T-202）
- Java Bean 生成（T-204）
- OpenAPI Schema 生成（T-207）
- Request Sheet 解析逻辑修改（T-107 已完成）
- Shared Header 解析逻辑修改

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5 节（Excel 解析设计）
- T-107 ExcelParser 类（提供 Request 解析参考）
- T-004 MessageModel、FieldGroup、FieldNode 数据模型
- T-102 SheetDiscovery（Sheet 发现逻辑）
- T-103 MetadataExtractor（元数据提取）
- T-104 SegLevelParser（嵌套算法）
- T-105 ObjectArrayDetector（对象/数组检测）
- T-106 CamelCaseConverter（命名规范化）

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
├── ResponseSheetParser.java        (新增，如独立实现)
└── ExcelParser.java               (修改，如扩展实现)

src/test/java/com/rtm/mq/tool/parser/
└── ResponseSheetParserTest.java
```

**运行时输出**:
- MessageModel.response 字段被正确填充
- Response FieldGroup 包含完整的字段树

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-107 | ExcelParser 主类、解析组件集成 |
| T-102 | SheetDiscovery 类 |
| T-103 | MetadataExtractor 类 |
| T-104 | SegLevelParser 类 |
| T-105 | ObjectArrayDetector 类 |
| T-106 | CamelCaseConverter 类 |
| T-004 | FieldNode、MessageModel、FieldGroup 数据模型 |
| T-005 | 错误处理框架 |

## Implementation Notes

### 设计选择

Response Sheet 解析与 Request Sheet 高度相似，建议采用以下方式之一：

**方案 A：复用 ExcelParser.parseSheet() 方法**
- T-107 中的 `parseSheet(Sheet, String)` 方法已支持通用解析
- Response Sheet 只需调用 `parseSheet(sheets.getResponse(), "Response")`
- 如无 Response 特有逻辑，无需额外代码

**方案 B：独立 ResponseSheetParser 类**
- 如果 Response 有特有逻辑（如额外验证、特殊字段处理），创建独立类
- 继承或组合 ExcelParser 的解析逻辑

### Response Sheet 特有处理

根据 MQ 规范文档，Response Sheet 可能包含以下特有元素：

1. **返回码字段**: Response 通常包含 `returnCode`、`errorMessage` 等标准字段
2. **可选字段更多**: Response 中 `O`（Optional）字段比例更高
3. **嵌套结构差异**: Response 的嵌套深度可能与 Request 不同

### 核心实现

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.exception.ExitCodes;
import com.rtm.mq.tool.model.*;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Response Sheet 解析器
 *
 * 扩展 ExcelParser 以处理 Response 特有逻辑。
 * 如果 Response 与 Request 解析完全相同，可直接使用 ExcelParser.parseSheet()。
 */
public class ResponseSheetParser {

    private static final String SHEET_NAME = "Response";

    private final ExcelParser excelParser;
    private final Config config;

    public ResponseSheetParser(ExcelParser excelParser, Config config) {
        this.excelParser = excelParser;
        this.config = config;
    }

    /**
     * 解析 Response Sheet
     *
     * @param sheet Response Sheet
     * @return FieldGroup 包含解析后的字段树
     */
    public FieldGroup parse(Sheet sheet) throws ParseException {
        if (sheet == null) {
            // Response 可能为空（某些消息只有 Request）
            return createEmptyFieldGroup();
        }

        // 验证 Sheet 名称
        String actualName = sheet.getSheetName();
        if (!SHEET_NAME.equalsIgnoreCase(actualName) &&
            !actualName.toLowerCase().contains("response")) {
            throw new ParseException(
                "Expected Response sheet but got: " + actualName,
                ExitCodes.VALIDATION_ERROR
            );
        }

        // 复用 ExcelParser 的 parseSheet 逻辑
        // 注意：需要确保 ExcelParser.parseSheet 方法可访问
        return parseSheetInternal(sheet);
    }

    /**
     * 内部解析方法
     * 复用 Request 解析的核心逻辑
     */
    private FieldGroup parseSheetInternal(Sheet sheet) throws ParseException {
        // 调用 ExcelParser 中已实现的通用解析逻辑
        // 如果 parseSheet 是 private，需要将其改为 protected 或 package-private

        // 或者复制核心解析流程：
        // 1. 验证列
        // 2. 创建解析器组件
        // 3. 解析字段
        // 4. 增强字段（对象/数组检测、命名规范化）
        // 5. 检测重复

        return excelParser.parseSheetPublic(sheet, SHEET_NAME);
    }

    /**
     * 创建空 FieldGroup
     */
    private FieldGroup createEmptyFieldGroup() {
        return new FieldGroup();
    }

    /**
     * 验证 Response FieldGroup
     * Response 特有的验证规则
     */
    public ValidationResult validate(FieldGroup response) {
        ValidationResult result = new ValidationResult();

        // Response 可以为空
        if (response == null || response.getFields().isEmpty()) {
            return result;  // 空 Response 是合法的
        }

        // 验证嵌套深度
        validateNestingDepth(response.getFields(), 1, result);

        return result;
    }

    /**
     * 递归验证嵌套深度
     */
    private void validateNestingDepth(java.util.List<FieldNode> fields,
                                      int currentDepth,
                                      ValidationResult result) {
        int maxDepth = config.getParser().getMaxNestingDepth();

        for (FieldNode field : fields) {
            if (currentDepth > maxDepth) {
                result.addError(new ValidationError(
                    "VR-104",
                    "Response nesting depth exceeds maximum",
                    String.format("Field '%s' at depth %d exceeds max %d",
                        field.getOriginalName(), currentDepth, maxDepth)
                ));
            }

            if (!field.getChildren().isEmpty()) {
                validateNestingDepth(field.getChildren(), currentDepth + 1, result);
            }
        }
    }
}
```

### ExcelParser 修改

如果采用方案 A（推荐），需确保 ExcelParser 中的 Response 解析逻辑正确：

```java
// 在 ExcelParser.parse() 方法中，已有：
// 5. 解析 Response
FieldGroup response = parseSheet(sheets.getResponse(), "Response");
model.setResponse(response);
```

确认 `parseSheet` 方法正确处理 null Sheet（Response 可能不存在）：

```java
private FieldGroup parseSheet(Sheet sheet, String sheetName) {
    if (sheet == null) {
        return new FieldGroup();  // 返回空 FieldGroup
    }
    // ... existing logic
}
```

### 关键实现要点

1. **Sheet 可空**: Response Sheet 可能不存在（某些消息只有 Request）
2. **复用解析逻辑**: 与 Request 共享相同的解析组件和流程
3. **字段顺序保持**: 使用 List 保证字段顺序与 Excel 一致
4. **嵌套深度验证**: 复用 NestingDepthValidator
5. **命名规范化**: 使用相同的 CamelCaseConverter

## Acceptance Criteria

1. [ ] Response Sheet 正确解析为 FieldGroup
2. [ ] Response Sheet 不存在时返回空 FieldGroup（不抛异常）
3. [ ] 字段顺序与 Excel Response Sheet 完全一致
4. [ ] 嵌套结构（Seg lvl）正确解析
5. [ ] 对象/数组字段正确识别（isObject、isArray 标记正确）
6. [ ] 字段名正确转换为 camelCase
7. [ ] transitory 字段（groupId、occurrenceCount）正确标记
8. [ ] SourceMetadata 正确记录（sheetName="Response", rowIndex）
9. [ ] 嵌套深度超限时产生验证错误
10. [ ] 与 T-107 的 Request 解析逻辑保持一致性
11. [ ] MessageModel.response 正确填充
12. [ ] 单元测试覆盖所有场景

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 解析简单 Response Sheet | FieldGroup 包含正确字段 |
| 单元测试 | Response Sheet 不存在 | 返回空 FieldGroup |
| 单元测试 | 嵌套对象解析 | children 正确填充 |
| 单元测试 | 数组字段解析 | isArray=true |
| 单元测试 | 字段顺序验证 | 顺序与 Excel 一致 |
| 单元测试 | SourceMetadata 验证 | sheetName="Response" |
| 单元测试 | 嵌套深度超限 | ValidationResult 包含错误 |
| 集成测试 | 完整 Excel 文件解析 | Request 和 Response 都正确 |
| 集成测试 | 与 T-107 ExcelParser 集成 | MessageModel 完整填充 |
| Golden 测试 | 对比参考 Response 输出 | 结构一致 |

### 测试代码示例

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.*;
import org.junit.jupiter.api.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.junit.jupiter.api.Assertions.*;

class ResponseSheetParserTest {

    private ResponseSheetParser parser;
    private ExcelParser excelParser;
    private Config config;

    @BeforeEach
    void setUp() {
        config = TestConfig.createDefault();
        excelParser = new ExcelParser(config);
        parser = new ResponseSheetParser(excelParser, config);
    }

    @Test
    void shouldReturnEmptyFieldGroupWhenSheetIsNull() {
        // When
        FieldGroup result = parser.parse(null);

        // Then
        assertNotNull(result);
        assertTrue(result.getFields().isEmpty());
    }

    @Test
    void shouldParseSimpleResponseSheet() {
        // Given
        Sheet sheet = createTestSheet("Response", new String[][] {
            {"ResponseCode", "10", "String", "M"},
            {"ResponseMessage", "100", "String", "O"}
        });

        // When
        FieldGroup result = parser.parse(sheet);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getFields().size());
        assertEquals("responseCode", result.getFields().get(0).getCamelCaseName());
        assertEquals("responseMessage", result.getFields().get(1).getCamelCaseName());
    }

    @Test
    void shouldPreserveFieldOrder() {
        // Given
        Sheet sheet = createTestSheet("Response", new String[][] {
            {"Field1", "10", "String", "M"},
            {"Field2", "20", "String", "M"},
            {"Field3", "30", "String", "M"}
        });

        // When
        FieldGroup result = parser.parse(sheet);

        // Then
        assertEquals("field1", result.getFields().get(0).getCamelCaseName());
        assertEquals("field2", result.getFields().get(1).getCamelCaseName());
        assertEquals("field3", result.getFields().get(2).getCamelCaseName());
    }

    @Test
    void shouldSetSourceMetadataCorrectly() {
        // Given
        Sheet sheet = createTestSheet("Response", new String[][] {
            {"TestField", "10", "String", "M"}
        });

        // When
        FieldGroup result = parser.parse(sheet);

        // Then
        SourceMetadata source = result.getFields().get(0).getSource();
        assertEquals("Response", source.getSheetName());
        assertTrue(source.getRowIndex() > 0);
    }

    // Helper method to create test sheets
    private Sheet createTestSheet(String name, String[][] data) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(name);
        // ... populate with header row and data rows
        return sheet;
    }
}
```

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Response Sheet 结构与 Request 差异大 | 解析逻辑不适用 | 详细分析 Response 样本，必要时独立实现 |
| Response 为空 | 下游生成器需处理 | 返回空 FieldGroup，文档说明 |
| Response 列名与 Request 不同 | 列映射失败 | ColumnNormalizer 支持别名 |
| 嵌套深度过深 | 性能问题 | 配置最大深度限制 |
| Response 特有字段类型 | 类型映射错误 | 扩展类型映射表 |
| 编码问题 | 字段名乱码 | 使用 UTF-8 |
| 循环引用 | 无限递归 | 引用检测机制 |

---

**文档结束**
