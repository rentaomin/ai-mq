# T-002 核心接口定义

## Goal

定义系统核心接口契约，包括 Parser、Generator、Validator 接口，为后续组件实现提供统一规范。

## In Scope / Out of Scope

**In Scope**:
- Parser 接口定义
- Generator 接口定义（XML, Java, OpenAPI）
- Validator 接口定义
- 接口方法签名和 Javadoc

**Out of Scope**:
- 接口实现
- 数据模型类（T-004）
- 异常类（T-005）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 3 节（高层架构）
- T-001 创建的项目结构

## Outputs

```
src/main/java/com/rtm/mq/tool/
├── parser/
│   └── Parser.java
├── generator/
│   ├── Generator.java
│   ├── xml/
│   │   └── XmlGenerator.java
│   ├── java/
│   │   └── JavaGenerator.java
│   └── openapi/
│       └── OpenApiGenerator.java
└── validator/
    └── Validator.java
```

## Dependencies

- T-001 项目初始化

## Implementation Notes

### Parser 接口

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.model.MessageModel;
import java.nio.file.Path;

/**
 * Excel 规范文件解析器接口
 */
public interface Parser {
    /**
     * 解析 Excel 规范文件
     * @param specFile Excel 规范文件路径
     * @param sharedHeaderFile 可选的共享头文件路径
     * @return 解析后的消息模型
     * @throws ParseException 解析失败时抛出
     */
    MessageModel parse(Path specFile, Path sharedHeaderFile);
}
```

### Generator 接口

```java
package com.rtm.mq.tool.generator;

import com.rtm.mq.tool.model.MessageModel;
import java.nio.file.Path;
import java.util.Map;

/**
 * 代码生成器通用接口
 */
public interface Generator {
    /**
     * 生成制品
     * @param model 消息模型
     * @param outputDir 输出目录
     * @return 生成的文件映射 (相对路径 -> 内容)
     * @throws GenerationException 生成失败时抛出
     */
    Map<String, String> generate(MessageModel model, Path outputDir);

    /**
     * 获取生成器类型
     * @return 生成器类型标识
     */
    String getType();
}
```

### XmlGenerator 接口

```java
package com.rtm.mq.tool.generator.xml;

import com.rtm.mq.tool.generator.Generator;

/**
 * XML Bean 生成器接口
 */
public interface XmlGenerator extends Generator {
    /**
     * 生成 Outbound XML (Request)
     */
    String generateOutbound();

    /**
     * 生成 Inbound XML (Response)
     */
    String generateInbound();
}
```

### JavaGenerator 接口

```java
package com.rtm.mq.tool.generator.java;

import com.rtm.mq.tool.generator.Generator;
import com.rtm.mq.tool.model.FieldNode;
import java.util.List;

/**
 * Java Bean 生成器接口
 */
public interface JavaGenerator extends Generator {
    /**
     * 为单个类生成代码
     * @param className 类名
     * @param fields 字段列表
     * @return Java 源代码
     */
    String generateClass(String className, List<FieldNode> fields);

    /**
     * 是否使用 Lombok
     */
    boolean isUseLombok();
}
```

### OpenApiGenerator 接口

```java
package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.generator.Generator;

/**
 * OpenAPI YAML 生成器接口
 */
public interface OpenApiGenerator extends Generator {
    /**
     * 生成主 API 文件
     * @return api.yaml 内容
     */
    String generateMainApi();

    /**
     * 生成 Schema 文件
     * @param schemaName Schema 名称
     * @return Schema YAML 内容
     */
    String generateSchema(String schemaName);
}
```

### Validator 接口

```java
package com.rtm.mq.tool.validator;

import com.rtm.mq.tool.model.ValidationResult;
import java.nio.file.Path;

/**
 * 验证器通用接口
 */
public interface Validator {
    /**
     * 执行验证
     * @param targetPath 验证目标路径
     * @return 验证结果
     */
    ValidationResult validate(Path targetPath);

    /**
     * 获取验证器类型
     * @return 验证器类型标识
     */
    String getType();
}
```

## Acceptance Criteria

1. [ ] 所有接口编译通过
2. [ ] 接口方法签名完整
3. [ ] Javadoc 注释完整
4. [ ] 接口之间无循环依赖
5. [ ] 返回类型使用正确的模型类（可先使用占位符）

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 编译测试 | `mvn compile` | 成功 |
| 接口一致性 | 检查方法签名 | 符合架构设计 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 接口设计不完整 | 后续实现需修改接口 | 参考架构文档完整定义 |
| 模型类未定义 | 编译错误 | 使用 Object 或 TODO 注释 |
| 泛型使用不当 | 类型安全问题 | 审查泛型参数 |
