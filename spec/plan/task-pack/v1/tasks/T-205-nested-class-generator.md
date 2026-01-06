# T-205 嵌套类生成

## Goal

实现嵌套类生成器,将中间 JSON Tree 中的嵌套对象和数组元素转换为独立的 Java 嵌套类文件,生成符合项目规范的 POJO 类,确保类型引用和命名规范的一致性。

## In Scope / Out of Scope

**In Scope**:
- NestedClassGenerator 类实现
- 嵌套对象类生成(如 `CreateApplication.java`)
- 数组元素类生成(如 `CBACardArray.java`)
- 嵌套层级遍历算法
- 类名冲突检测与去重
- 字段定义生成(private 成员变量)
- Getter/Setter 方法生成
- Builder 模式支持(内部 Builder 类)
- Lombok 注解支持(可配置)
- import 语句生成
- 包声明生成
- 类注释生成(包含 spec 溯源信息)
- 字段顺序保持(与 Excel/JSON Tree 一致)
- transitory 字段排除(groupId、occurrenceCount 不生成到 Java Bean)
- 递归处理深层嵌套
- 错误处理与退出码

**Out of Scope**:
- 顶层 Request/Response Bean 生成(T-204 已完成)
- 枚举辅助方法生成(T-206)
- Java 类型映射逻辑(T-203 已完成)
- Java Bean 验证(T-302)
- 原子输出管理(T-307)
- 循环引用检测(由架构约束保证不存在)

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.2 节(Java Bean 生成器)
- T-204 JavaBeanGenerator 类(提供基础 Bean 生成逻辑)
- T-203 JavaTypeMapper 类(提供类型映射)
- T-108 DeterministicJsonWriter(提供 JSON Tree 读取)
- T-004 MessageModel、FieldGroup、FieldNode(数据模型)
- T-003 Config(配置加载,包括包名、是否使用 Lombok)
- T-005 ParseException、ExitCodes(错误处理)

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/java/
└── NestedClassGenerator.java

```

**生成文件输出**(运行时产物):
```
{output-root}/java/{package-path}/
├── CreateApplication.java        # 嵌套对象类
├── CBACardArray.java             # 数组元素类
├── PaymentDetails.java           # 深层嵌套对象类
└── ...
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-204 | JavaBeanGenerator 类(基础 Bean 生成逻辑) |
| T-203 | JavaTypeMapper 类(类型映射) |
| T-108 | JSON Tree 序列化结构 |
| T-004 | MessageModel、FieldGroup、FieldNode 数据模型 |
| T-003 | Config 配置加载器(获取包名、Lombok 配置) |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心接口

```java
package com.rtm.mq.tool.generator.java;

/**
 * 嵌套类生成器接口
 */
public interface NestedClassGenerator {
    /**
     * 从 FieldGroup 生成所有嵌套类
     * @param fieldGroup 字段组
     * @return 生成的类名列表
     */
    java.util.List<String> generateNestedClasses(FieldGroup fieldGroup);

    /**
     * 获取生成的嵌套类内容
     * @param className 类名
     * @return Java 源代码
     */
    String getGeneratedContent(String className);

    /**
     * 获取所有生成的嵌套类名
     */
    java.util.List<String> getAllGeneratedClassNames();
}
```

### NestedClassGenerator 类

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
 * 嵌套类生成器
 *
 * 从 MessageModel 中提取嵌套对象和数组元素,生成独立的 Java 类文件。
 */
public class NestedClassGenerator {

    private final Config config;
    private final JavaTypeMapper typeMapper;
    private final Map<String, String> generatedContents = new LinkedHashMap<>();
    private final Set<String> processedClassNames = new HashSet<>();

    public NestedClassGenerator(Config config) {
        this.config = config;
        this.typeMapper = new JavaTypeMapper(config);
    }

    /**
     * 生成所有嵌套类
     */
    public List<String> generateNestedClasses(FieldGroup fieldGroup) throws ParseException {
        List<String> generatedClassNames = new ArrayList<>();

        if (fieldGroup == null || fieldGroup.getFields().isEmpty()) {
            return generatedClassNames;
        }

        // 递归遍历所有字段,收集嵌套类
        for (FieldNode field : fieldGroup.getFields()) {
            collectNestedClasses(field, generatedClassNames);
        }

        return generatedClassNames;
    }

    /**
     * 递归收集嵌套类
     */
    private void collectNestedClasses(FieldNode field, List<String> generatedClassNames)
            throws ParseException {

        // 跳过 transitory 字段
        if (field.isTransitory()) {
            return;
        }

        // 处理数组类型
        if (field.isArray() && field.hasChildren()) {
            String arrayClassName = deriveArrayClassName(field);

            if (!processedClassNames.contains(arrayClassName)) {
                generateNestedClass(arrayClassName, field.getChildren(),
                    "Array element class for " + field.getOriginalName());
                generatedClassNames.add(arrayClassName);
                processedClassNames.add(arrayClassName);

                // 递归处理数组元素的子字段
                for (FieldNode child : field.getChildren()) {
                    collectNestedClasses(child, generatedClassNames);
                }
            }
        }
        // 处理对象类型
        else if (field.isObject() && field.hasChildren()) {
            String objectClassName = deriveObjectClassName(field);

            if (!processedClassNames.contains(objectClassName)) {
                generateNestedClass(objectClassName, field.getChildren(),
                    "Nested object class for " + field.getOriginalName());
                generatedClassNames.add(objectClassName);
                processedClassNames.add(objectClassName);

                // 递归处理对象的子字段
                for (FieldNode child : field.getChildren()) {
                    collectNestedClasses(child, generatedClassNames);
                }
            }
        }
    }

    /**
     * 生成单个嵌套类
     */
    private void generateNestedClass(String className,
                                     List<FieldNode> fields,
                                     String description) throws ParseException {
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

        // Lombok 注解(如启用)
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

        // 如果未使用 Lombok,生成 getter/setter 和 Builder
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

        String content = sb.toString();
        generatedContents.put(className, content);
        writeToFile(className, content);
    }

    /**
     * 推导数组类名
     */
    private String deriveArrayClassName(FieldNode field) {
        String camelName = field.getCamelCaseName();
        // 去除末尾的 Arr/Array/List 后缀,然后转为 PascalCase
        String baseName = camelName
            .replaceAll("(?i)(Arr|Array|List)$", "");
        return capitalizeFirst(baseName) + "Array";
    }

    /**
     * 推导对象类名
     */
    private String deriveObjectClassName(FieldNode field) {
        String camelName = field.getCamelCaseName();
        // 转为 PascalCase
        return capitalizeFirst(camelName);
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
                "Failed to write nested class file: " + className + " - " + e.getMessage(),
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

    public String getGeneratedContent(String className) {
        return generatedContents.get(className);
    }

    public List<String> getAllGeneratedClassNames() {
        return new ArrayList<>(processedClassNames);
    }
}
```

### 输出 Java 代码示例

#### 数组元素类 (CBACardArray.java)

```java
package com.rtm.test.model;

import java.math.BigDecimal;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Array element class for CBA_CARD_ARR
 *
 * @generated by MQ Tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CBACardArray {

    /**
     * CARD_NO (length: 16)
     */
    private String cardNo;

    /**
     * CARD_TYPE (length: 2)
     */
    private String cardType;

    /**
     * CARD_LIMIT (length: 15)
     */
    private BigDecimal cardLimit;
}
```

#### 嵌套对象类 (CreateApplication.java)

```java
package com.rtm.test.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Nested object class for CREATE_APP
 *
 * @generated by MQ Tool
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplication {

    /**
     * APP_DATE (length: 8)
     */
    private String appDate;

    /**
     * APP_TIME (length: 6)
     */
    private String appTime;

    /**
     * PAYMENT_DETAILS (object)
     */
    private PaymentDetails paymentDetails;
}
```

### 关键实现要点

1. **嵌套类识别**:
   - 遍历 FieldNode 树,识别 isArray() 且有子节点的字段
   - 识别 isObject() 且有子节点的字段
   - 递归处理所有层级

2. **类名推导规则**:
   - 数组类: 去除 Arr/Array/List 后缀,加 "Array" 后缀,转 PascalCase
   - 对象类: 直接转 PascalCase
   - 示例: `cbaCardArr` → `CBACardArray`
   - 示例: `createApp` → `CreateApplication`

3. **去重机制**:
   - 使用 `processedClassNames` Set 避免重复生成同名类
   - 同名类只生成一次(第一次遇到时)

4. **递归深度处理**:
   - 支持任意深度嵌套
   - 深度优先遍历(DFS)
   - 先生成叶子节点类,再生成父节点类

5. **transitory 字段处理**:
   - groupId、occurrenceCount 字段不生成到嵌套类
   - 使用 `field.isTransitory()` 判断

6. **与 JavaBeanGenerator 集成**:
   - 共享 JavaTypeMapper 实例
   - 共享字段生成逻辑(getter/setter/Builder)
   - 输出到相同包路径

## Acceptance Criteria

1. [ ] NestedClassGenerator 类编译通过
2. [ ] 正确识别数组元素类型(isArray() && hasChildren())
3. [ ] 正确识别嵌套对象类型(isObject() && hasChildren())
4. [ ] 类名推导规则正确(数组类加 "Array" 后缀,对象类直接 PascalCase)
5. [ ] 递归处理深层嵌套(至少支持 3 层)
6. [ ] 同名类去重(processedClassNames 机制)
7. [ ] transitory 字段被排除(groupId、occurrenceCount)
8. [ ] 字段顺序与 JSON Tree 完全一致
9. [ ] 字段类型正确(String、BigDecimal、List<T>、嵌套类型)
10. [ ] Lombok 注解正确生成(当配置启用时)
11. [ ] Getter/Setter 正确生成(当 Lombok 禁用时)
12. [ ] Builder 模式正确生成
13. [ ] import 语句正确且无重复
14. [ ] 包声明正确
15. [ ] 类注释包含 `@generated` 标记
16. [ ] 数组字段默认初始化为空 ArrayList
17. [ ] 文件输出到正确路径(`{output-root}/java/{package-path}/`)
18. [ ] 使用 UTF-8 编码
19. [ ] 空嵌套结构时不生成对应文件

---

**文档结束**
