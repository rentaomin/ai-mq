# T-204 Java Bean 生成器

## Goal

实现 Java Bean 生成器，将中间 JSON Tree 转换为 Java Bean 类文件（Request/Response），生成符合项目规范的 POJO 类，包含字段定义、getter/setter 方法、Builder 模式支持。

## In Scope / Out of Scope

**In Scope**:
- JavaBeanGenerator 类实现
- Request Bean 类生成（`{OperationId}Request.java`）
- Response Bean 类生成（`{OperationId}Response.java`）
- 字段定义生成（private 成员变量）
- Getter/Setter 方法生成
- Builder 模式支持（内部 Builder 类）
- Lombok 注解支持（可配置）
- import 语句生成
- 包声明生成
- 类注释生成（包含 spec 溯源信息）
- 字段顺序保持（与 Excel/JSON Tree 一致）
- transitory 字段排除（groupId、occurrenceCount 不生成到 Java Bean）
- 错误处理与退出码

**Out of Scope**:
- 嵌套类生成（T-205）
- 枚举辅助方法生成（T-206）
- Java 类型映射逻辑（T-203 已完成）
- Java Bean 验证（T-302）
- 原子输出管理（T-307）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.2 节（Java Bean 生成器）
- T-203 JavaTypeMapper 类（提供类型映射）
- T-108 DeterministicJsonWriter（提供 JSON Tree 读取）
- T-004 MessageModel、FieldGroup、FieldNode（数据模型）
- T-003 Config（配置加载，包括包名、是否使用 Lombok）
- T-005 ParseException、ExitCodes（错误处理）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/java/
└── JavaBeanGenerator.java

```

**生成文件输出**（运行时产物）:
```
{output-root}/java/{package-path}/
├── {OperationId}Request.java
└── {OperationId}Response.java
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-203 | JavaTypeMapper 类（类型映射） |
| T-108 | JSON Tree 序列化结构 |
| T-004 | MessageModel、FieldGroup、FieldNode 数据模型 |
| T-003 | Config 配置加载器（获取包名、Lombok 配置） |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心接口

```java
package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.generator.Generator;

/**
 * Java Bean 生成器接口
 */
public interface JavaGenerator extends Generator {
    /**
     * 获取生成的 Java 代码内容
     * @param className 类名
     * @return Java 源代码
     */
    String getGeneratedContent(String className);

    /**
     * 获取生成的所有类名列表
     */
    java.util.List<String> getGeneratedClassNames();
}
```

### JavaBeanGenerator 类

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
 * Java Bean 生成器
 *
 * 从 MessageModel 生成 Request 和 Response Java Bean 类。
 */
public class JavaBeanGenerator implements JavaGenerator {

    private final Config config;
    private final JavaTypeMapper typeMapper;
    private final Map<String, String> generatedContents = new LinkedHashMap<>();
    private final List<String> generatedClassNames = new ArrayList<>();

    public JavaBeanGenerator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
    }

    @Override
    public void generate(MessageModel model) throws ParseException {
        String operationId = model.getMetadata().getOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new ParseException(
                "Operation ID is required for Java Bean generation",
                ExitCodes.GENERATION_ERROR
            );
        }

        // 生成 Request Bean
        if (model.getRequest() != null && !model.getRequest().getFields().isEmpty()) {
            String requestClassName = operationId + "Request";
            String requestContent = generateBeanClass(
                requestClassName,
                model.getRequest().getFields(),
                "Request Bean for " + operationId
            );
            generatedContents.put(requestClassName, requestContent);
            generatedClassNames.add(requestClassName);
            writeToFile(requestClassName, requestContent);
        }

        // 生成 Response Bean
        if (model.getResponse() != null && !model.getResponse().getFields().isEmpty()) {
            String responseClassName = operationId + "Response";
            String responseContent = generateBeanClass(
                responseClassName,
                model.getResponse().getFields(),
                "Response Bean for " + operationId
            );
            generatedContents.put(responseClassName, responseContent);
            generatedClassNames.add(responseClassName);
            writeToFile(responseClassName, responseContent);
        }
    }

    /**
     * 生成单个 Bean 类
     */
    private String generateBeanClass(String className,
                                     List<FieldNode> fields,
                                     String description) {
        StringBuilder sb = new StringBuilder();

        // 过滤 transitory 字段
        List<FieldNode> filteredFields = filterTransitoryFields(fields);

        // 包声明
        sb.append("package ").append(typeMapper.getModelPackage()).append(";\n\n");

        // import 语句
        Set<String> imports = typeMapper.collectAllImports(filteredFields);
        if (config.getGeneration().isUseLombok()) {
            imports.add("lombok.Data");
            imports.add("lombok.Builder");
            imports.add("lombok.NoArgsConstructor");
            imports.add("lombok.AllArgsConstructor");
        }
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        // 类注释
        sb.append("/**\n");
        sb.append(" * ").append(description).append("\n");
        sb.append(" *\n");
        sb.append(" * @generated by MQ Tool\n");
        sb.append(" */\n");

        // Lombok 注解（如启用）
        if (config.getGeneration().isUseLombok()) {
            sb.append("@Data\n");
            sb.append("@Builder\n");
            sb.append("@NoArgsConstructor\n");
            sb.append("@AllArgsConstructor\n");
        }

        // 类声明
        sb.append("public class ").append(className).append(" {\n\n");

        // 字段定义
        for (FieldNode field : filteredFields) {
            generateFieldDefinition(sb, field);
        }

        // 如果未使用 Lombok，生成 getter/setter 和 Builder
        if (!config.getGeneration().isUseLombok()) {
            // 无参构造器
            sb.append("\n    public ").append(className).append("() {\n    }\n");

            // getter/setter
            for (FieldNode field : filteredFields) {
                generateGetterSetter(sb, field);
            }

            // Builder 类
            generateBuilderClass(sb, className, filteredFields);
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 生成字段定义
     */
    private void generateFieldDefinition(StringBuilder sb, FieldNode field) {
        String type = typeMapper.getSimpleTypeName(field);
        String name = field.getCamelCaseName();

        // 字段注释
        if (field.getOriginalName() != null && !field.getOriginalName().equals(name)) {
            sb.append("    /**\n");
            sb.append("     * ").append(field.getOriginalName());
            if (field.getLength() != null) {
                sb.append(" (length: ").append(field.getLength()).append(")");
            }
            sb.append("\n");
            sb.append("     */\n");
        }

        // 字段声明
        sb.append("    private ").append(type).append(" ").append(name);

        // 数组字段默认初始化
        if (field.isArray()) {
            sb.append(" = new java.util.ArrayList<>()");
        }

        sb.append(";\n\n");
    }

    /**
     * 生成 getter/setter 方法
     */
    private void generateGetterSetter(StringBuilder sb, FieldNode field) {
        String type = typeMapper.getSimpleTypeName(field);
        String name = field.getCamelCaseName();
        String capitalizedName = capitalizeFirst(name);

        // Getter
        sb.append("\n    public ").append(type).append(" get")
          .append(capitalizedName).append("() {\n");
        sb.append("        return this.").append(name).append(";\n");
        sb.append("    }\n");

        // Setter
        sb.append("\n    public void set").append(capitalizedName)
          .append("(").append(type).append(" ").append(name).append(") {\n");
        sb.append("        this.").append(name).append(" = ").append(name).append(";\n");
        sb.append("    }\n");
    }

    /**
     * 生成 Builder 内部类
     */
    private void generateBuilderClass(StringBuilder sb, String className,
                                      List<FieldNode> fields) {
        sb.append("\n    /**\n");
        sb.append("     * Builder for ").append(className).append("\n");
        sb.append("     */\n");
        sb.append("    public static class Builder {\n\n");

        // Builder 字段
        for (FieldNode field : fields) {
            String type = typeMapper.getSimpleTypeName(field);
            String name = field.getCamelCaseName();
            sb.append("        private ").append(type).append(" ").append(name);
            if (field.isArray()) {
                sb.append(" = new java.util.ArrayList<>()");
            }
            sb.append(";\n");
        }

        // Builder 方法
        for (FieldNode field : fields) {
            String type = typeMapper.getSimpleTypeName(field);
            String name = field.getCamelCaseName();

            sb.append("\n        public Builder ").append(name)
              .append("(").append(type).append(" ").append(name).append(") {\n");
            sb.append("            this.").append(name).append(" = ").append(name).append(";\n");
            sb.append("            return this;\n");
            sb.append("        }\n");
        }

        // build() 方法
        sb.append("\n        public ").append(className).append(" build() {\n");
        sb.append("            ").append(className).append(" obj = new ")
          .append(className).append("();\n");
        for (FieldNode field : fields) {
            String name = field.getCamelCaseName();
            sb.append("            obj.").append(name).append(" = this.")
              .append(name).append(";\n");
        }
        sb.append("            return obj;\n");
        sb.append("        }\n");

        sb.append("    }\n");

        // builder() 静态方法
        sb.append("\n    public static Builder builder() {\n");
        sb.append("        return new Builder();\n");
        sb.append("    }\n");
    }

    /**
     * 过滤 transitory 字段
     * groupId 和 occurrenceCount 不生成到 Java Bean
     */
    private List<FieldNode> filterTransitoryFields(List<FieldNode> fields) {
        List<FieldNode> filtered = new ArrayList<>();
        for (FieldNode field : fields) {
            if (!field.isTransitory()) {
                filtered.add(field);
            }
        }
        return filtered;
    }

    /**
     * 写入 Java 文件
     */
    private void writeToFile(String className, String content) throws ParseException {
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        Path outputDir = Path.of(config.getOutput().getRoot(), "java", packagePath);
        Path outputFile = outputDir.resolve(className + ".java");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParseException(
                "Failed to write Java file: " + className + " - " + e.getMessage(),
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

    @Override
    public String getGeneratedContent(String className) {
        return generatedContents.get(className);
    }

    @Override
    public List<String> getGeneratedClassNames() {
        return new ArrayList<>(generatedClassNames);
    }

    /**
     * 获取输出文件路径
     */
    public Path getOutputPath(String className) {
        String packagePath = typeMapper.getModelPackage().replace('.', '/');
        return Path.of(config.getOutput().getRoot(), "java", packagePath, className + ".java");
    }
}
```

### 输出 Java 代码示例

#### 使用 Lombok

```java
package com.rtm.test.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request Bean for CreateApplication
 *
 * @generated by MQ Tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    /**
     * CUST_ID (length: 20)
     */
    private String customerId;

    /**
     * APPLICATION_AMT (length: 15)
     */
    private BigDecimal applicationAmount;

    /**
     * CBA_CARD_ARR (array, maxItems: 9)
     */
    @Builder.Default
    private List<CBACardArray> cbaCardArr = new java.util.ArrayList<>();

    /**
     * CREATE_APP (object)
     */
    private CreateApplication createApp;
}
```

#### 不使用 Lombok

```java
package com.rtm.test.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request Bean for CreateApplication
 *
 * @generated by MQ Tool
 */
public class CreateApplicationRequest {

    /**
     * CUST_ID (length: 20)
     */
    private String customerId;

    /**
     * APPLICATION_AMT (length: 15)
     */
    private BigDecimal applicationAmount;

    /**
     * CBA_CARD_ARR (array, maxItems: 9)
     */
    private List<CBACardArray> cbaCardArr = new java.util.ArrayList<>();

    public CreateApplicationRequest() {
    }

    public String getCustomerId() {
        return this.customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getApplicationAmount() {
        return this.applicationAmount;
    }

    public void setApplicationAmount(BigDecimal applicationAmount) {
        this.applicationAmount = applicationAmount;
    }

    public List<CBACardArray> getCbaCardArr() {
        return this.cbaCardArr;
    }

    public void setCbaCardArr(List<CBACardArray> cbaCardArr) {
        this.cbaCardArr = cbaCardArr;
    }

    /**
     * Builder for CreateApplicationRequest
     */
    public static class Builder {
        private String customerId;
        private BigDecimal applicationAmount;
        private List<CBACardArray> cbaCardArr = new java.util.ArrayList<>();

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder applicationAmount(BigDecimal applicationAmount) {
            this.applicationAmount = applicationAmount;
            return this;
        }

        public Builder cbaCardArr(List<CBACardArray> cbaCardArr) {
            this.cbaCardArr = cbaCardArr;
            return this;
        }

        public CreateApplicationRequest build() {
            CreateApplicationRequest obj = new CreateApplicationRequest();
            obj.customerId = this.customerId;
            obj.applicationAmount = this.applicationAmount;
            obj.cbaCardArr = this.cbaCardArr;
            return obj;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
```

### 关键实现要点

1. **字段顺序**:
   - 必须与 Excel/JSON Tree 中的顺序完全一致
   - 使用 List 保证顺序

2. **transitory 字段处理**:
   - groupId、occurrenceCount 字段不生成到 Java Bean
   - 使用 `field.isTransitory()` 判断

3. **类型映射**:
   - 调用 T-203 JavaTypeMapper 获取 Java 类型
   - 数组类型: `List<T>`，默认初始化为 `new ArrayList<>()`
   - 金额类型: `BigDecimal`

4. **Lombok 支持**:
   - 可通过 Config 配置启用/禁用
   - 启用时生成 `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
   - 数组字段使用 `@Builder.Default`

5. **Builder 模式**:
   - 如果未使用 Lombok，手动生成 Builder 内部类
   - 支持链式调用

6. **代码规范**:
   - UTF-8 编码
   - 适当的缩进（4 空格）
   - 包含 `@generated` 标记

## Acceptance Criteria

1. [ ] JavaBeanGenerator 类编译通过
2. [ ] Request Bean 正确生成（`{OperationId}Request.java`）
3. [ ] Response Bean 正确生成（`{OperationId}Response.java`）
4. [ ] 字段顺序与 JSON Tree 完全一致
5. [ ] transitory 字段被排除（groupId、occurrenceCount）
6. [ ] 字段类型正确（String、BigDecimal、List<T>、嵌套类型）
7. [ ] Lombok 注解正确生成（当配置启用时）
8. [ ] Getter/Setter 正确生成（当 Lombok 禁用时）
9. [ ] Builder 模式正确生成
10. [ ] import 语句正确且无重复
11. [ ] 包声明正确
12. [ ] 类注释包含 `@generated` 标记
13. [ ] 数组字段默认初始化为空 ArrayList
14. [ ] 文件输出到正确路径（`{output-root}/java/{package-path}/`）
15. [ ] 使用 UTF-8 编码
16. [ ] 空 Request/Response 时不生成对应文件
17. [ ] 缺少 operationId 时抛出 ParseException

---

**文档结束**
