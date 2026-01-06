# T-203 Java 类型映射

## Goal

实现 Java 类型映射器（JavaTypeMapper），负责将中间 JSON Tree 中的 `dataType` 属性映射为对应的 Java 类型。此组件为 Java Bean 生成器（T-204）提供类型解析支持。

## In Scope / Out of Scope

**In Scope**:
- JavaTypeMapper 类实现
- Excel dataType 到 Java 类型的映射规则
- 数组类型（List<T>）映射
- 嵌套对象类型映射
- 包名解析（根据配置生成完整类型引用）
- 类型导入语句收集
- 单元测试

**Out of Scope**:
- Java Bean 代码生成（T-204）
- 嵌套类生成（T-205）
- 枚举辅助方法生成（T-206）
- XML 类型映射（T-109 已完成）
- OpenAPI 类型映射（T-207）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.2.4 节（数据类型映射规则）
- T-108 JSON Tree 序列化（MessageModel、FieldNode 数据结构）
- T-004 数据模型定义（FieldNode.dataType、isArray、isObject 属性）
- T-003 配置加载器（Config.java，获取 groupId、artifactId）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/java/
└── JavaTypeMapper.java
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-108 | DeterministicJsonWriter、JSON Tree 结构定义 |
| T-004 | FieldNode、MessageModel 数据模型 |
| T-003 | Config 配置类（获取包名配置） |

## Implementation Notes

### 类型映射规则表

根据架构文档 6.2.4：

| Excel dataType | Java Type | 说明 |
|---------------|----------|------|
| String | String | 字符串 |
| AN | String | 字母数字字符串 |
| Number | String | 保持字符串（定长报文特性） |
| N | String | 数字字符串 |
| Unsigned Integer | String | 无符号整数（保持字符串） |
| Amount | java.math.BigDecimal | 金额类型 |
| Currency | java.math.BigDecimal | 货币金额 |
| Date | String | 日期字符串（格式化） |
| Object (1..1, 0..1) | {ClassName} | 嵌套对象 |
| Array (0..N, 1..N) | java.util.List<{ClassName}> | 数组 |

### JavaTypeMapper 类

```java
package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.FieldNode;

import java.util.*;

/**
 * Java 类型映射器
 * 将 FieldNode 的 dataType 映射为 Java 类型
 */
public class JavaTypeMapper {

    private static final Map<String, String> TYPE_MAP = new LinkedHashMap<>();

    static {
        // 字符串类型
        TYPE_MAP.put("String", "String");
        TYPE_MAP.put("AN", "String");
        TYPE_MAP.put("A", "String");

        // 数字类型（保持字符串）
        TYPE_MAP.put("Number", "String");
        TYPE_MAP.put("N", "String");
        TYPE_MAP.put("Unsigned Integer", "String");

        // 金额类型
        TYPE_MAP.put("Amount", "java.math.BigDecimal");
        TYPE_MAP.put("Currency", "java.math.BigDecimal");

        // 日期类型
        TYPE_MAP.put("Date", "String");
    }

    private final Config config;

    public JavaTypeMapper(Config config) {
        this.config = config;
    }

    /**
     * 映射 FieldNode 到 Java 类型
     *
     * @param field 字段节点
     * @return Java 类型字符串（简单名称或全限定名）
     */
    public String mapType(FieldNode field) {
        // 数组类型
        if (field.isArray()) {
            String itemType = resolveObjectType(field);
            return "java.util.List<" + itemType + ">";
        }

        // 对象类型
        if (field.isObject()) {
            return resolveObjectType(field);
        }

        // 基本类型
        String dataType = field.getDataType();
        if (dataType == null || dataType.isEmpty()) {
            return "String"; // 默认 String
        }

        return TYPE_MAP.getOrDefault(dataType, "String");
    }

    /**
     * 解析对象类型的类名
     * 使用 FieldNode.className 作为类名
     */
    private String resolveObjectType(FieldNode field) {
        String className = field.getClassName();
        if (className == null || className.isEmpty()) {
            // 回退到 camelCaseName 的首字母大写形式
            className = capitalizeFirst(field.getCamelCaseName());
        }
        return className;
    }

    /**
     * 获取字段的简单类型名（不含包名）
     */
    public String getSimpleTypeName(FieldNode field) {
        String fullType = mapType(field);
        // 处理泛型
        if (fullType.contains("<")) {
            // java.util.List<ClassName> -> List<ClassName>
            return fullType.replace("java.util.List", "List")
                          .replace("java.math.BigDecimal", "BigDecimal");
        }
        // 移除包名
        int lastDot = fullType.lastIndexOf('.');
        return lastDot >= 0 ? fullType.substring(lastDot + 1) : fullType;
    }

    /**
     * 收集字段所需的 import 语句
     */
    public Set<String> collectImports(FieldNode field) {
        Set<String> imports = new LinkedHashSet<>();
        String type = mapType(field);

        if (type.startsWith("java.util.List")) {
            imports.add("java.util.List");
        }
        if (type.contains("BigDecimal")) {
            imports.add("java.math.BigDecimal");
        }

        return imports;
    }

    /**
     * 收集多个字段所需的全部 import 语句
     */
    public Set<String> collectAllImports(List<FieldNode> fields) {
        Set<String> imports = new TreeSet<>(); // 按字母排序
        for (FieldNode field : fields) {
            imports.addAll(collectImports(field));
        }
        return imports;
    }

    /**
     * 获取完整包路径
     */
    public String getModelPackage() {
        return config.getGeneration().getGroupId() + "."
             + config.getGeneration().getArtifactId() + ".model";
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

    /**
     * 判断类型是否需要 import
     */
    public boolean requiresImport(String type) {
        return type.startsWith("java.util.") ||
               type.startsWith("java.math.") ||
               type.contains(".");
    }

    /**
     * 获取默认值表达式
     * 用于字段初始化（如 List 初始化为 new ArrayList<>()）
     */
    public String getDefaultInitializer(FieldNode field) {
        if (field.isArray()) {
            return "new java.util.ArrayList<>()";
        }
        return null; // 非数组字段不需要默认初始化
    }
}
```

### 配置依赖

从 Config 中获取以下配置项：
- `generation.groupId`: Maven groupId（如 `com.rtm`）
- `generation.artifactId`: Maven artifactId（如 `test`）

### 特殊类型处理

1. **BigDecimal**: 用于金额类型，需要 `import java.math.BigDecimal`
2. **List**: 用于数组类型，需要 `import java.util.List`
3. **嵌套对象**: 使用 `className` 属性，同包无需 import

### 与其他组件的协作

- **T-204 Java Bean Generator**: 调用 `mapType()` 获取字段类型，调用 `collectAllImports()` 生成 import 语句
- **T-205 Nested Class Generator**: 复用类型映射逻辑生成嵌套类字段

## Acceptance Criteria

1. [ ] JavaTypeMapper 类编译通过
2. [ ] 所有 Excel dataType 正确映射到 Java 类型
3. [ ] 数组类型正确映射为 `List<T>`
4. [ ] 对象类型正确解析 className
5. [ ] BigDecimal 类型正确处理 Amount/Currency
6. [ ] import 语句正确收集（无重复、按字母排序）
7. [ ] 默认类型为 String（dataType 为空或未知时）

---

**文档结束**
