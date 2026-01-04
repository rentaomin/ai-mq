# T-005 错误处理框架

## Goal

定义系统异常类和退出码，提供统一的错误处理机制，支持详细的错误消息（包含行号、Sheet 名等）。

## In Scope / Out of Scope

**In Scope**:
- 自定义异常类定义
- 退出码常量定义
- 错误消息格式化工具

**Out of Scope**:
- 具体业务逻辑中的异常处理
- 日志记录（使用 SLF4J）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 12 节（错误处理与退出码）
- T-002 定义的接口

## Outputs

```
src/main/java/com/rtm/mq/tool/exception/
├── MqToolException.java
├── ParseException.java
├── GenerationException.java
├── ValidationException.java
├── ConfigException.java
├── OutputException.java
└── ExitCodes.java
```

## Dependencies

- T-001 项目初始化
- T-002 核心接口定义

## Implementation Notes

### MqToolException 基类

```java
package com.rtm.mq.tool.exception;

/**
 * MQ 工具异常基类
 */
public class MqToolException extends RuntimeException {
    private final int exitCode;

    public MqToolException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public MqToolException(String message, Throwable cause, int exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
```

### ParseException

```java
package com.rtm.mq.tool.exception;

/**
 * Excel 解析异常
 * 包含 Sheet 名、行号等上下文信息
 */
public class ParseException extends MqToolException {
    private String sheetName;
    private Integer rowIndex;
    private String fieldName;

    public ParseException(String message) {
        super(message, ExitCodes.PARSE_ERROR);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause, ExitCodes.PARSE_ERROR);
    }

    public ParseException withContext(String sheetName, int rowIndex) {
        this.sheetName = sheetName;
        this.rowIndex = rowIndex;
        return this;
    }

    public ParseException withField(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (sheetName != null) {
            sb.append(" in sheet '").append(sheetName).append("'");
        }
        if (rowIndex != null) {
            sb.append(" at row ").append(rowIndex);
        }
        if (fieldName != null) {
            sb.append(" (field: '").append(fieldName).append("')");
        }
        return sb.toString();
    }

    // Getters
    public String getSheetName() { return sheetName; }
    public Integer getRowIndex() { return rowIndex; }
    public String getFieldName() { return fieldName; }
}
```

### GenerationException

```java
package com.rtm.mq.tool.exception;

/**
 * 代码生成异常
 */
public class GenerationException extends MqToolException {
    private String generatorType;
    private String artifactName;

    public GenerationException(String message) {
        super(message, ExitCodes.GENERATION_ERROR);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause, ExitCodes.GENERATION_ERROR);
    }

    public GenerationException withGenerator(String type) {
        this.generatorType = type;
        return this;
    }

    public GenerationException withArtifact(String name) {
        this.artifactName = name;
        return this;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (generatorType != null) {
            sb.append(" [Generator: ").append(generatorType).append("]");
        }
        if (artifactName != null) {
            sb.append(" [Artifact: ").append(artifactName).append("]");
        }
        return sb.toString();
    }
}
```

### ValidationException

```java
package com.rtm.mq.tool.exception;

import com.rtm.mq.tool.model.ValidationResult;

/**
 * 验证异常
 */
public class ValidationException extends MqToolException {
    private ValidationResult result;

    public ValidationException(String message) {
        super(message, ExitCodes.VALIDATION_ERROR);
    }

    public ValidationException(String message, ValidationResult result) {
        super(message, ExitCodes.VALIDATION_ERROR);
        this.result = result;
    }

    public ValidationResult getResult() {
        return result;
    }
}
```

### ConfigException

```java
package com.rtm.mq.tool.exception;

/**
 * 配置异常
 */
public class ConfigException extends MqToolException {
    public ConfigException(String message) {
        super(message, ExitCodes.CONFIG_ERROR);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause, ExitCodes.CONFIG_ERROR);
    }
}
```

### OutputException

```java
package com.rtm.mq.tool.exception;

/**
 * 输出管理异常
 */
public class OutputException extends MqToolException {
    public OutputException(String message) {
        super(message, ExitCodes.GENERATION_ERROR);
    }

    public OutputException(String message, Throwable cause) {
        super(message, cause, ExitCodes.GENERATION_ERROR);
    }
}
```

### ExitCodes

```java
package com.rtm.mq.tool.exception;

/**
 * 标准退出码
 */
public final class ExitCodes {
    private ExitCodes() {}

    /** 成功 */
    public static final int SUCCESS = 0;

    /** 输入验证错误（如文件不存在） */
    public static final int INPUT_VALIDATION_ERROR = 1;

    /** 解析错误（如 Seg lvl 无效） */
    public static final int PARSE_ERROR = 2;

    /** 生成错误（如模板加载失败） */
    public static final int GENERATION_ERROR = 3;

    /** 验证错误（如跨制品一致性失败） */
    public static final int VALIDATION_ERROR = 4;

    /** 配置错误（如必需配置缺失） */
    public static final int CONFIG_ERROR = 5;

    /** 内部错误 */
    public static final int INTERNAL_ERROR = 99;

    /**
     * 获取退出码描述
     */
    public static String getDescription(int code) {
        switch (code) {
            case SUCCESS: return "Success";
            case INPUT_VALIDATION_ERROR: return "Input validation error";
            case PARSE_ERROR: return "Parse error";
            case GENERATION_ERROR: return "Generation error";
            case VALIDATION_ERROR: return "Validation error";
            case CONFIG_ERROR: return "Configuration error";
            case INTERNAL_ERROR: return "Internal error";
            default: return "Unknown error";
        }
    }
}
```

### 错误消息示例

架构文档中的错误消息示例：

```java
// 解析错误
throw new ParseException("Invalid Seg lvl value '0'")
    .withContext("Request", 15);
// 输出: ERROR: Invalid Seg lvl value '0' in sheet 'Request' at row 15.

// 对象定义错误
throw new ParseException("Invalid object definition 'CreateAppCreateApplication'. Expected format: 'fieldName:ClassName'")
    .withContext("Request", 9);

// 重复字段名错误
throw new ParseException("Duplicate field name 'domicleBranche'")
    .withContext("Request", 18)
    .withField("domicleBranche");

// 一致性验证错误
throw new ValidationException("VR-106 - groupId found in Java Bean");
```

## Acceptance Criteria

1. [ ] 所有异常类正确继承 MqToolException
2. [ ] 退出码覆盖所有错误类型
3. [ ] ParseException 支持上下文信息（Sheet, Row）
4. [ ] 错误消息格式符合架构文档规范
5. [ ] 单元测试覆盖异常构造

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | ParseException 上下文 | 消息包含 Sheet 和 Row |
| 单元测试 | 退出码值 | 符合定义 |
| 单元测试 | 异常链传递 | cause 正确传递 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 错误消息不清晰 | 调试困难 | 包含完整上下文 |
| 退出码冲突 | 脚本判断错误 | 使用唯一退出码 |
| 异常堆栈丢失 | 调试困难 | 传递 cause |
