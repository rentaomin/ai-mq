# T-206 枚举辅助方法生成

## Goal

实现枚举辅助方法生成器,为 Java Bean 中的枚举类型字段生成辅助方法(如 fromCode、toCode、validation),提升枚举字段的易用性和类型安全性,支持规范化的枚举值处理。

## In Scope / Out of Scope

**In Scope**:
- EnumHelperGenerator 类实现
- 识别枚举类型字段(从 Excel spec 中的枚举约束)
- 生成 Java 枚举类(enum)
- 生成 fromCode 静态方法(String → Enum)
- 生成 toCode 实例方法(Enum → String)
- 生成 getValue/getDescription 方法
- 枚举值验证逻辑
- 枚举注释生成(包含 spec 溯源信息)
- 处理未知枚举值的策略(UNKNOWN 值)
- 与 JavaBeanGenerator 集成
- 错误处理与退出码

**Out of Scope**:
- 枚举类型检测(T-203 JavaTypeMapper 已处理)
- Java Bean 主类生成(T-204 已完成)
- 嵌套类生成(T-205 已完成)
- Java Bean 验证(T-302)
- 原子输出管理(T-307)
- 国际化枚举描述
- 枚举值范围验证(由运行时验证器处理)

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.2 节(Java Bean 生成器)
- T-205 NestedClassGenerator 类(嵌套类生成逻辑)
- T-204 JavaBeanGenerator 类(基础 Bean 生成逻辑)
- T-203 JavaTypeMapper 类(类型映射,包括枚举检测)
- T-108 DeterministicJsonWriter(提供 JSON Tree 读取)
- T-004 MessageModel、FieldGroup、FieldNode(数据模型)
- T-003 Config(配置加载,包括枚举处理策略)
- T-005 ParseException、ExitCodes(错误处理)
- Excel spec 中的枚举约束定义(如 "01|02|03" 或 "Y|N")

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/java/
└── EnumHelperGenerator.java

src/test/java/com/rtm/mq/tool/generator/java/
└── EnumHelperGeneratorTest.java
```

**生成文件输出**(运行时产物):
```
{output-root}/java/{package-path}/
├── CardType.java                # 枚举类
├── YesNoFlag.java               # 枚举类
├── ApplicationStatus.java       # 枚举类
└── ...
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-205 | NestedClassGenerator 类(嵌套类生成逻辑) |
| T-204 | JavaBeanGenerator 类(基础 Bean 生成逻辑) |
| T-203 | JavaTypeMapper 类(类型映射,枚举检测) |
| T-108 | JSON Tree 序列化结构 |
| T-004 | MessageModel、FieldGroup、FieldNode 数据模型 |
| T-003 | Config 配置加载器(获取包名、枚举处理策略) |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心接口

```java
package com.rtm.mq.tool.generator.java;

/**
 * 枚举辅助方法生成器接口
 */
public interface EnumHelperGenerator {
    /**
     * 从 FieldGroup 生成所有枚举类
     * @param fieldGroup 字段组
     * @return 生成的枚举类名列表
     */
    java.util.List<String> generateEnumClasses(FieldGroup fieldGroup);

    /**
     * 获取生成的枚举类内容
     * @param enumClassName 枚举类名
     * @return Java 源代码
     */
    String getGeneratedContent(String enumClassName);

    /**
     * 获取所有生成的枚举类名
     */
    java.util.List<String> getAllGeneratedEnumNames();
}
```

### EnumHelperGenerator 类

```java
package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.*;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.exception.ExitCodes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 枚举辅助方法生成器
 *
 * 从 MessageModel 中提取枚举类型字段,生成独立的 Java 枚举类。
 */
public class EnumHelperGenerator {

    private final Config config;
    private final JavaTypeMapper typeMapper;
    private final Map<String, String> generatedContents = new LinkedHashMap<>();
    private final Set<String> processedEnumNames = new HashSet<>();

    public EnumHelperGenerator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
    }

    /**
     * 生成所有枚举类
     */
    public List<String> generateEnumClasses(FieldGroup fieldGroup) throws ParseException {
        List<String> generatedEnumNames = new ArrayList<>();

        if (fieldGroup == null || fieldGroup.getFields().isEmpty()) {
            return generatedEnumNames;
        }

        // 递归遍历所有字段,收集枚举类型
        for (FieldNode field : fieldGroup.getFields()) {
            collectEnumClasses(field, generatedEnumNames);
        }

        return generatedEnumNames;
    }

    /**
     * 递归收集枚举类型
     */
    private void collectEnumClasses(FieldNode field, List<String> generatedEnumNames)
            throws ParseException {

        // 跳过 transitory 字段
        if (field.isTransitory()) {
            return;
        }

        // 检查是否为枚举类型
        if (typeMapper.isEnumType(field)) {
            String enumClassName = deriveEnumClassName(field);

            if (!processedEnumNames.contains(enumClassName)) {
                List<EnumValue> enumValues = extractEnumValues(field);
                generateEnumClass(enumClassName, enumValues, field.getOriginalName());
                generatedEnumNames.add(enumClassName);
                processedEnumNames.add(enumClassName);
            }
        }

        // 递归处理子字段
        if (field.hasChildren()) {
            for (FieldNode child : field.getChildren()) {
                collectEnumClasses(child, generatedEnumNames);
            }
        }
    }

    /**
     * 生成单个枚举类
     */
    private void generateEnumClass(String enumClassName,
                                   List<EnumValue> enumValues,
                                   String fieldOriginalName) throws ParseException {
        StringBuilder sb = new StringBuilder();

        // 包声明
        sb.append("package ").append(typeMapper.getModelPackage()).append(";\n\n");

        // 类注释
        sb.append("/**\n");
        sb.append(" * Enum for ").append(fieldOriginalName).append("\n");
        sb.append(" *\n");
        sb.append(" * @generated by MQ Tool\n");
        sb.append(" */\n");

        // 枚举声明
        sb.append("public enum ").append(enumClassName).append(" {\n\n");

        // 枚举值
        for (int i = 0; i < enumValues.size(); i++) {
            EnumValue enumValue = enumValues.get(i);
            sb.append("    /** ").append(enumValue.getDescription()).append(" */\n");
            sb.append("    ").append(enumValue.getName())
              .append("(\"").append(enumValue.getCode()).append("\", \"")
              .append(enumValue.getDescription()).append("\")");

            if (i < enumValues.size() - 1) {
                sb.append(",\n\n");
            } else {
                sb.append(";\n\n");
            }
        }

        // 字段
        sb.append("    private final String code;\n");
        sb.append("    private final String description;\n\n");

        // 构造器
        sb.append("    ").append(enumClassName)
          .append("(String code, String description) {\n");
        sb.append("        this.code = code;\n");
        sb.append("        this.description = description;\n");
        sb.append("    }\n\n");

        // Getter 方法
        sb.append("    public String getCode() {\n");
        sb.append("        return code;\n");
        sb.append("    }\n\n");

        sb.append("    public String getDescription() {\n");
        sb.append("        return description;\n");
        sb.append("    }\n\n");

        // fromCode 静态方法
        sb.append("    /**\n");
        sb.append("     * Convert code to enum\n");
        sb.append("     * @param code enum code\n");
        sb.append("     * @return enum value, or null if not found\n");
        sb.append("     */\n");
        sb.append("    public static ").append(enumClassName)
          .append(" fromCode(String code) {\n");
        sb.append("        if (code == null) {\n");
        sb.append("            return null;\n");
        sb.append("        }\n");
        sb.append("        for (").append(enumClassName).append(" value : values()) {\n");
        sb.append("            if (value.code.equals(code)) {\n");
        sb.append("                return value;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n\n");

        // isValid 静态方法
        sb.append("    /**\n");
        sb.append("     * Check if code is valid\n");
        sb.append("     * @param code enum code\n");
        sb.append("     * @return true if valid\n");
        sb.append("     */\n");
        sb.append("    public static boolean isValid(String code) {\n");
        sb.append("        return fromCode(code) != null;\n");
        sb.append("    }\n");

        sb.append("}\n");

        String content = sb.toString();
        generatedContents.put(enumClassName, content);
        writeToFile(enumClassName, content);
    }

    /**
     * 推导枚举类名
     */
    private String deriveEnumClassName(FieldNode field) {
        String camelName = field.getCamelCaseName();
        // 转为 PascalCase
        String pascalName = capitalizeFirst(camelName);

        // 如果已经以常见枚举后缀结尾,保留;否则可选择添加后缀
        if (pascalName.endsWith("Type") ||
            pascalName.endsWith("Status") ||
            pascalName.endsWith("Flag") ||
            pascalName.endsWith("Code")) {
            return pascalName;
        }

        // 可选:添加 Type 后缀(根据项目规范决定)
        return pascalName;
    }

    /**
     * 提取枚举值
     */
    private List<EnumValue> extractEnumValues(FieldNode field) throws ParseException {
        String enumConstraint = field.getEnumConstraint();
        if (enumConstraint == null || enumConstraint.isBlank()) {
            throw new ParseException(
                "Enum constraint is required for enum field: " + field.getOriginalName(),
                ExitCodes.GENERATION_ERROR
            );
        }

        // 解析枚举约束,格式: "01|02|03" 或 "Y|N"
        String[] codes = enumConstraint.split("\\|");
        List<EnumValue> enumValues = new ArrayList<>();

        for (String code : codes) {
            code = code.trim();
            if (code.isEmpty()) {
                continue;
            }

            String name = deriveEnumValueName(code);
            String description = deriveEnumValueDescription(code, field);

            enumValues.add(new EnumValue(name, code, description));
        }

        if (enumValues.isEmpty()) {
            throw new ParseException(
                "No valid enum values found for field: " + field.getOriginalName(),
                ExitCodes.GENERATION_ERROR
            );
        }

        return enumValues;
    }

    /**
     * 推导枚举值名称
     */
    private String deriveEnumValueName(String code) {
        // 将 code 转为合法的 Java 枚举常量名
        // 示例: "01" → "VALUE_01", "Y" → "Y", "AA" → "AA"

        if (code.matches("\\d+")) {
            // 纯数字,加 VALUE_ 前缀
            return "VALUE_" + code;
        } else if (code.matches("[A-Z]+")) {
            // 纯大写字母,直接使用
            return code;
        } else {
            // 其他情况,转大写并替换特殊字符
            return code.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        }
    }

    /**
     * 推导枚举值描述
     */
    private String deriveEnumValueDescription(String code, FieldNode field) {
        // 从 field metadata 中查找描述,如果没有则使用 code 本身
        // 这里简化处理,实际可从 Excel spec 的备注列读取
        return code;
    }

    /**
     * 写入 Java 文件
     */
    private void writeToFile(String enumClassName, String content) throws ParseException {
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        Path outputDir = Path.of(config.getOutput().getRoot(), "java", packagePath);
        Path outputFile = outputDir.resolve(enumClassName + ".java");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParseException(
                "Failed to write enum class file: " + enumClassName + " - " + e.getMessage(),
                ExitCodes.IO_ERROR
            );
        }
    }

    /**
     * 首字母大写
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public String getGeneratedContent(String enumClassName) {
        return generatedContents.get(enumClassName);
    }

    public List<String> getAllGeneratedEnumNames() {
        return new ArrayList<>(processedEnumNames);
    }

    /**
     * 枚举值数据类
     */
    private static class EnumValue {
        private final String name;
        private final String code;
        private final String description;

        public EnumValue(String name, String code, String description) {
            this.name = name;
            this.code = code;
            this.description = description;
        }

        public String getName() { return name; }
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
}
```

### 输出 Java 代码示例

#### 枚举类 (CardType.java)

```java
package com.rtm.test.model;

/**
 * Enum for CARD_TYPE
 *
 * @generated by MQ Tool
 */
public enum CardType {

    /** 01 */
    VALUE_01("01", "01"),

    /** 02 */
    VALUE_02("02", "02"),

    /** 03 */
    VALUE_03("03", "03");

    private final String code;
    private final String description;

    CardType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convert code to enum
     * @param code enum code
     * @return enum value, or null if not found
     */
    public static CardType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CardType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Check if code is valid
     * @param code enum code
     * @return true if valid
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
```

#### 枚举类 (YesNoFlag.java)

```java
package com.rtm.test.model;

/**
 * Enum for YES_NO_FLAG
 *
 * @generated by MQ Tool
 */
public enum YesNoFlag {

    /** Y */
    Y("Y", "Y"),

    /** N */
    N("N", "N");

    private final String code;
    private final String description;

    YesNoFlag(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convert code to enum
     * @param code enum code
     * @return enum value, or null if not found
     */
    public static YesNoFlag fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (YesNoFlag value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Check if code is valid
     * @param code enum code
     * @return true if valid
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
```

### 关键实现要点

1. **枚举类型识别**:
   - 从 FieldNode 的 enumConstraint 属性识别
   - 调用 JavaTypeMapper.isEnumType(field) 判断
   - 示例约束: "01|02|03", "Y|N", "AA|BB|CC"

2. **枚举类名推导规则**:
   - 从 camelCaseName 转 PascalCase
   - 示例: `cardType` → `CardType`
   - 示例: `yesNoFlag` → `YesNoFlag`

3. **枚举值名称推导规则**:
   - 纯数字: 加 `VALUE_` 前缀 (如 "01" → `VALUE_01`)
   - 纯大写字母: 直接使用 (如 "Y" → `Y`)
   - 其他: 转大写并替换特殊字符 (如 "a-b" → `A_B`)

4. **枚举约束解析**:
   - 分隔符: `|`
   - 格式: `code1|code2|code3`
   - 空值处理: 跳过空字符串

5. **辅助方法**:
   - `fromCode(String code)`: 从 code 转换为枚举值
   - `isValid(String code)`: 验证 code 是否有效
   - `getCode()`: 获取枚举值的 code
   - `getDescription()`: 获取枚举值的描述

6. **去重机制**:
   - 使用 `processedEnumNames` Set 避免重复生成同名枚举类
   - 同名枚举只生成一次

7. **与 JavaBeanGenerator 集成**:
   - 生成的枚举类供 Java Bean 字段引用
   - 字段类型从 String 替换为枚举类型

## Acceptance Criteria

1. [ ] EnumHelperGenerator 类编译通过
2. [ ] 正确识别枚举类型字段(通过 enumConstraint)
3. [ ] 枚举类名推导规则正确
4. [ ] 枚举值名称推导规则正确(纯数字加 VALUE_ 前缀)
5. [ ] 枚举约束解析正确(按 `|` 分隔)
6. [ ] 生成 fromCode 静态方法
7. [ ] 生成 isValid 静态方法
8. [ ] 生成 getCode 实例方法
9. [ ] 生成 getDescription 实例方法
10. [ ] 枚举构造器正确(private,接受 code 和 description)
11. [ ] 枚举值去重(processedEnumNames 机制)
12. [ ] 枚举注释包含 `@generated` 标记
13. [ ] 包声明正确
14. [ ] 文件输出到正确路径(`{output-root}/java/{package-path}/`)
15. [ ] 使用 UTF-8 编码
16. [ ] 空 enumConstraint 时抛出异常
17. [ ] 无效 enumConstraint 格式时抛出异常

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 简单枚举类生成(数字 code) | 类结构正确 |
| 单元测试 | 简单枚举类生成(字母 code) | 类结构正确 |
| 单元测试 | 枚举类名推导 | `cardType` → `CardType` |
| 单元测试 | 枚举值名称推导(纯数字) | "01" → `VALUE_01` |
| 单元测试 | 枚举值名称推导(纯字母) | "Y" → `Y` |
| 单元测试 | 枚举约束解析 | "01\|02\|03" → 3 个枚举值 |
| 单元测试 | fromCode 方法 | 正确转换 code 为枚举值 |
| 单元测试 | fromCode 方法(不存在的 code) | 返回 null |
| 单元测试 | isValid 方法 | 正确验证 code |
| 单元测试 | getCode 方法 | 返回正确的 code |
| 单元测试 | getDescription 方法 | 返回正确的 description |
| 单元测试 | 枚举值去重 | 相同枚举类型只生成一次 |
| 单元测试 | 空 enumConstraint | 抛出 ParseException |
| 单元测试 | 无效 enumConstraint 格式 | 抛出 ParseException |
| 单元测试 | 包声明验证 | 正确包路径 |
| 单元测试 | 类注释验证 | 包含 @generated 标记 |
| 单元测试 | UTF-8 编码 | 文件使用 UTF-8 编码 |
| 集成测试 | 从 JSON Tree 文件生成 | 文件正确写入 |
| 集成测试 | 与 JavaTypeMapper 集成 | 类型识别正确 |
| 编译测试 | 生成代码编译 | javac 编译通过 |
| Golden 测试 | 对比参考 Java 文件 | 结构一致 |

### 测试代码示例

```java
package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.*;
import com.rtm.mq.tool.exception.ParseException;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnumHelperGeneratorTest {

    private EnumHelperGenerator generator;
    private Config config;

    @BeforeEach
    void setUp() {
        config = TestConfig.createDefault();
        generator = new EnumHelperGenerator(config);
    }

    @Test
    void shouldGenerateEnumClassWithNumericCodes() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        List<String> enumNames = generator.generateEnumClasses(fieldGroup);

        // Then
        assertEquals(1, enumNames.size());
        assertTrue(enumNames.contains("CardType"));

        String content = generator.getGeneratedContent("CardType");
        assertNotNull(content);
        assertTrue(content.contains("public enum CardType"));
        assertTrue(content.contains("VALUE_01"));
        assertTrue(content.contains("VALUE_02"));
    }

    @Test
    void shouldGenerateEnumClassWithAlphaCodes() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithAlphaEnum();

        // When
        List<String> enumNames = generator.generateEnumClasses(fieldGroup);

        // Then
        assertEquals(1, enumNames.size());
        assertTrue(enumNames.contains("YesNoFlag"));

        String content = generator.getGeneratedContent("YesNoFlag");
        assertNotNull(content);
        assertTrue(content.contains("public enum YesNoFlag"));
        assertTrue(content.contains("Y(\"Y\""));
        assertTrue(content.contains("N(\"N\""));
    }

    @Test
    void shouldDeriveEnumClassName() throws ParseException {
        // Given
        FieldNode enumField = FieldNode.builder()
            .camelCaseName("cardType")
            .enumConstraint("01|02")
            .build();
        FieldGroup fieldGroup = new FieldGroup();
        fieldGroup.addField(enumField);

        // When
        List<String> enumNames = generator.generateEnumClasses(fieldGroup);

        // Then
        assertTrue(enumNames.contains("CardType"));
    }

    @Test
    void shouldDeriveEnumValueNameForNumericCode() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("CardType");
        assertTrue(content.contains("VALUE_01"));
        assertTrue(content.contains("VALUE_02"));
    }

    @Test
    void shouldDeriveEnumValueNameForAlphaCode() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithAlphaEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("YesNoFlag");
        assertTrue(content.contains("Y(\"Y\""));
        assertTrue(content.contains("N(\"N\""));
    }

    @Test
    void shouldParseEnumConstraint() throws ParseException {
        // Given
        FieldNode enumField = FieldNode.builder()
            .camelCaseName("status")
            .enumConstraint("01|02|03")
            .build();
        FieldGroup fieldGroup = new FieldGroup();
        fieldGroup.addField(enumField);

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("Status");
        assertTrue(content.contains("VALUE_01"));
        assertTrue(content.contains("VALUE_02"));
        assertTrue(content.contains("VALUE_03"));
    }

    @Test
    void shouldGenerateFromCodeMethod() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("CardType");
        assertTrue(content.contains("public static CardType fromCode(String code)"));
        assertTrue(content.contains("return value;"));
        assertTrue(content.contains("return null;"));
    }

    @Test
    void shouldGenerateIsValidMethod() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("CardType");
        assertTrue(content.contains("public static boolean isValid(String code)"));
        assertTrue(content.contains("return fromCode(code) != null;"));
    }

    @Test
    void shouldGenerateGetCodeMethod() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("CardType");
        assertTrue(content.contains("public String getCode()"));
        assertTrue(content.contains("return code;"));
    }

    @Test
    void shouldGenerateGetDescriptionMethod() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("CardType");
        assertTrue(content.contains("public String getDescription()"));
        assertTrue(content.contains("return description;"));
    }

    @Test
    void shouldDeduplicateSameEnumType() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithDuplicateEnums();

        // When
        List<String> enumNames = generator.generateEnumClasses(fieldGroup);

        // Then
        // 虽然有 2 个相同类型的枚举字段,但只生成 1 个枚举类
        assertEquals(1, enumNames.size());
    }

    @Test
    void shouldThrowExceptionForEmptyEnumConstraint() {
        // Given
        FieldNode enumField = FieldNode.builder()
            .camelCaseName("status")
            .enumConstraint("")
            .build();
        FieldGroup fieldGroup = new FieldGroup();
        fieldGroup.addField(enumField);

        // When & Then
        assertThrows(ParseException.class, () -> generator.generateEnumClasses(fieldGroup));
    }

    @Test
    void shouldThrowExceptionForNullEnumConstraint() {
        // Given
        FieldNode enumField = FieldNode.builder()
            .camelCaseName("status")
            .enumConstraint(null)
            .build();
        FieldGroup fieldGroup = new FieldGroup();
        fieldGroup.addField(enumField);

        // When & Then
        assertThrows(ParseException.class, () -> generator.generateEnumClasses(fieldGroup));
    }

    @Test
    void shouldIncludeGeneratedAnnotation() throws ParseException {
        // Given
        FieldGroup fieldGroup = createFieldGroupWithNumericEnum();

        // When
        generator.generateEnumClasses(fieldGroup);

        // Then
        String content = generator.getGeneratedContent("CardType");
        assertTrue(content.contains("@generated by MQ Tool"));
    }

    // Helper methods
    private FieldGroup createFieldGroupWithNumericEnum() {
        FieldGroup fieldGroup = new FieldGroup();
        FieldNode enumField = FieldNode.builder()
            .camelCaseName("cardType")
            .originalName("CARD_TYPE")
            .enumConstraint("01|02")
            .build();
        fieldGroup.addField(enumField);
        return fieldGroup;
    }

    private FieldGroup createFieldGroupWithAlphaEnum() {
        FieldGroup fieldGroup = new FieldGroup();
        FieldNode enumField = FieldNode.builder()
            .camelCaseName("yesNoFlag")
            .originalName("YES_NO_FLAG")
            .enumConstraint("Y|N")
            .build();
        fieldGroup.addField(enumField);
        return fieldGroup;
    }

    // ... other helper methods
}
```

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 枚举类名冲突 | 后生成的类覆盖先生成的类 | processedEnumNames 去重机制 |
| 枚举值名称与 Java 关键字冲突 | 编译失败 | 添加关键字检测,加前缀 |
| 特殊字符在 code 中 | 无效枚举常量名 | 替换特殊字符为 `_` |
| 空 enumConstraint | 无法生成枚举类 | 抛出 ParseException |
| 无效 enumConstraint 格式 | 解析失败 | 抛出 ParseException |
| 枚举值过多 | 枚举类过大 | 限制最大枚举值数量(如 100) |
| 枚举描述缺失 | 使用 code 作为描述 | 可接受的降级策略 |
| 文件覆盖 | 丢失已有修改 | 输出到独立目录,不覆盖源码 |
| 枚举值重复 | 编译错误 | 检测重复 code,抛出异常 |
| fromCode 性能 | 大枚举遍历慢 | 可使用 Map 优化(未来增强) |

---

**文档结束**
