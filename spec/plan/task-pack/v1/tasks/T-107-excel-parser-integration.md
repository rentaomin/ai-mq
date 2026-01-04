# T-107 Excel 解析器集成

## Goal

集成所有解析组件，实现完整的 Excel 解析器，将 Excel 规范文件解析为 MessageModel 对象。

## In Scope / Out of Scope

**In Scope**:
- ExcelParser 主类实现
- 集成 SheetDiscovery、ColumnValidator、MetadataExtractor
- 集成 SegLevelParser、ObjectArrayDetector、CamelCaseConverter
- Request/Response/SharedHeader 解析
- Parser 接口实现

**Out of Scope**:
- JSON 序列化（T-108）
- XML/Java/OpenAPI 生成

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5 节（Excel 解析设计）
- T-101 ~ T-106 所有解析组件
- T-006 版本注册器

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
├── ExcelParser.java
└── SharedHeaderLoader.java

src/test/java/com/rtm/mq/tool/parser/
└── ExcelParserTest.java
```

## Dependencies

- T-101 列名规范化器
- T-102 Sheet 发现与验证
- T-103 元数据提取器
- T-104 Seg lvl 嵌套算法
- T-105 对象/数组检测
- T-106 命名规范化

## Implementation Notes

### ExcelParser 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Excel 规范文件解析器
 * 集成所有解析组件，实现完整解析流程
 */
public class ExcelParser implements Parser {

    private static final int HEADER_ROW_INDEX = 7;  // Row 8 (0-indexed)

    private final Config config;
    private final SheetDiscovery sheetDiscovery;
    private final MetadataExtractor metadataExtractor;
    private final SharedHeaderLoader sharedHeaderLoader;

    public ExcelParser(Config config) {
        this.config = config;
        this.sheetDiscovery = new SheetDiscovery();
        this.metadataExtractor = new MetadataExtractor();
        this.sharedHeaderLoader = new SharedHeaderLoader();
    }

    @Override
    public MessageModel parse(Path specFile, Path sharedHeaderFile) {
        validateInputFile(specFile);
        if (sharedHeaderFile != null) {
            validateInputFile(sharedHeaderFile);
        }

        try (InputStream is = Files.newInputStream(specFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            MessageModel model = new MessageModel();

            // 1. 发现 Sheets
            SheetSet sheets = sheetDiscovery.discoverSheets(workbook);

            // 2. 提取元数据
            Metadata metadata = metadataExtractor.extract(
                sheets.getRequest(), specFile, sharedHeaderFile);
            model.setMetadata(metadata);

            // 3. 解析 Shared Header
            FieldGroup sharedHeader = parseSharedHeader(sheets, sharedHeaderFile);
            model.setSharedHeader(sharedHeader);

            // 4. 解析 Request
            FieldGroup request = parseSheet(sheets.getRequest(), "Request");
            model.setRequest(request);

            // 5. 解析 Response
            FieldGroup response = parseSheet(sheets.getResponse(), "Response");
            model.setResponse(response);

            return model;

        } catch (IOException e) {
            throw new ParseException("Failed to read Excel file: " + specFile, e);
        }
    }

    /**
     * 解析 Sheet
     */
    private FieldGroup parseSheet(Sheet sheet, String sheetName) {
        // 1. 验证列
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        ColumnValidator columnValidator = new ColumnValidator();
        Map<String, Integer> columnMap = columnValidator.validateAndMapColumns(headerRow, sheetName);

        // 2. 创建解析器
        NestingDepthValidator depthValidator = new NestingDepthValidator(
            config.getParser().getMaxNestingDepth());
        SegLevelParser segLevelParser = new SegLevelParser(columnMap, sheetName, depthValidator);
        ObjectArrayDetector objectArrayDetector = new ObjectArrayDetector(columnMap);
        CamelCaseConverter camelCaseConverter = new CamelCaseConverter();
        DuplicateDetector duplicateDetector = new DuplicateDetector();

        // 3. 解析字段
        List<FieldNode> fields = segLevelParser.parseFields(sheet);

        // 4. 增强字段（对象/数组检测、命名规范化）
        enhanceFields(fields, sheet, columnMap, objectArrayDetector, camelCaseConverter);

        // 5. 检测重复
        duplicateDetector.detectDuplicates(fields);

        FieldGroup group = new FieldGroup();
        group.setFields(fields);
        return group;
    }

    /**
     * 增强字段列表
     */
    private void enhanceFields(List<FieldNode> fields, Sheet sheet,
                               Map<String, Integer> columnMap,
                               ObjectArrayDetector detector,
                               CamelCaseConverter converter) {
        for (int i = 0; i < fields.size(); i++) {
            FieldNode node = fields.get(i);

            // 获取对应行
            Row row = sheet.getRow(node.getSource().getRowIndex() - 1);  // 转回 0-indexed

            // 对象/数组检测
            FieldNode enhanced = detector.detect(node, row);

            // 命名规范化（非 transitory）
            if (!enhanced.isTransitory() && enhanced.getCamelCaseName() == null) {
                String camelName;
                if (enhanced.isObject() || enhanced.isArray()) {
                    // 对象/数组使用 ObjectDefinition 的 fieldName 部分
                    ObjectDefinition objDef = detector.parseObjectDefinition(enhanced.getOriginalName());
                    camelName = converter.toCamelCase(objDef.getFieldName());
                } else {
                    camelName = converter.toCamelCase(enhanced.getOriginalName());
                }
                enhanced = rebuildWithCamelName(enhanced, camelName);
            }

            fields.set(i, enhanced);

            // 递归处理子节点
            if (!enhanced.getChildren().isEmpty()) {
                enhanceFields(enhanced.getChildren(), sheet, columnMap, detector, converter);
            }
        }
    }

    /**
     * 重建 FieldNode 添加 camelCaseName
     */
    private FieldNode rebuildWithCamelName(FieldNode node, String camelName) {
        return FieldNode.builder()
            .originalName(node.getOriginalName())
            .camelCaseName(camelName)
            .className(node.getClassName())
            .segLevel(node.getSegLevel())
            .length(node.getLength())
            .dataType(node.getDataType())
            .optionality(node.getOptionality())
            .defaultValue(node.getDefaultValue())
            .hardCodeValue(node.getHardCodeValue())
            .groupId(node.getGroupId())
            .occurrenceCount(node.getOccurrenceCount())
            .isArray(node.isArray())
            .isObject(node.isObject())
            .isTransitory(node.isTransitory())
            .children(node.getChildren())
            .source(node.getSource())
            .build();
    }

    /**
     * 解析 Shared Header
     */
    private FieldGroup parseSharedHeader(SheetSet sheets, Path sharedHeaderFile) {
        if (sharedHeaderFile != null) {
            return sharedHeaderLoader.loadFromFile(sharedHeaderFile, config);
        } else if (sheets.hasSharedHeader()) {
            return parseSheet(sheets.getSharedHeader(), "Shared Header");
        } else {
            return new FieldGroup();  // 空 Header
        }
    }

    /**
     * 验证输入文件
     */
    private void validateInputFile(Path file) {
        if (file == null) {
            throw new ParseException("Input file path is null");
        }
        if (!Files.exists(file)) {
            throw new ParseException("Input file not found: " + file);
        }
        if (!Files.isReadable(file)) {
            throw new ParseException("Input file is not readable: " + file);
        }
        String fileName = file.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new ParseException("Input file is not an Excel file: " + file);
        }
    }
}
```

### SharedHeaderLoader 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared Header 加载器
 * 支持从独立文件或主文件加载
 */
public class SharedHeaderLoader {

    private static final String SHARED_HEADER_SHEET = "Shared Header";

    /**
     * 从独立文件加载 Shared Header
     */
    public FieldGroup loadFromFile(Path sharedHeaderFile, Config config) {
        try (InputStream is = Files.newInputStream(sharedHeaderFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet headerSheet = workbook.getSheet(SHARED_HEADER_SHEET);
            if (headerSheet == null) {
                // 尝试第一个 sheet
                if (workbook.getNumberOfSheets() > 0) {
                    headerSheet = workbook.getSheetAt(0);
                }
            }

            if (headerSheet == null) {
                return new FieldGroup();
            }

            // 复用解析逻辑
            ExcelParser parser = new ExcelParser(config);
            // 注：这里需要单独实现 parseSheet 方法或使用公共方法
            // 简化处理，返回空
            return new FieldGroup();

        } catch (IOException e) {
            throw new ParseException("Failed to load Shared Header file: " + sharedHeaderFile, e);
        }
    }
}
```

## Acceptance Criteria

1. [ ] 正确解析 Request Sheet
2. [ ] 正确解析 Response Sheet
3. [ ] 正确解析 Shared Header（可选）
4. [ ] 元数据正确提取
5. [ ] 字段顺序保持
6. [ ] 嵌套关系正确
7. [ ] 对象/数组正确识别
8. [ ] 命名规范化正确应用
9. [ ] 重复字段检测生效
10. [ ] 输入文件验证
11. [ ] 集成测试覆盖

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 集成测试 | 解析 create_app.xlsx | 所有字段正确 |
| 集成测试 | 验证字段顺序 | 与 Excel 一致 |
| 集成测试 | 验证嵌套关系 | 层级正确 |
| 单元测试 | 文件不存在 | 抛出 ParseException |
| 单元测试 | 非 Excel 文件 | 抛出 ParseException |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 大文件内存 | OOM | 流式处理 |
| 损坏的 Excel | 解析失败 | 详细错误消息 |
| 编码问题 | 乱码 | 使用 UTF-8 |
