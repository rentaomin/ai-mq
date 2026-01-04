# T-004 数据模型定义

## Goal

定义系统核心数据模型类，包括 FieldNode、MessageModel、Metadata、SourceMetadata 等，作为中间 JSON Tree 的内存表示。

## In Scope / Out of Scope

**In Scope**:
- FieldNode 类（字段节点）
- MessageModel 类（消息模型）
- FieldGroup 类（字段组）
- Metadata 类（元数据）
- SourceMetadata 类（来源元数据）
- ValidationResult 类（验证结果）
- ValidationError 类（验证错误）

**Out of Scope**:
- JSON 序列化逻辑（T-108）
- 解析逻辑（T-107）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 4 节（核心数据模型）
- T-002 定义的接口

## Outputs

```
src/main/java/com/rtm/mq/tool/model/
├── FieldNode.java
├── MessageModel.java
├── FieldGroup.java
├── Metadata.java
├── SourceMetadata.java
├── ValidationResult.java
└── ValidationError.java
```

## Dependencies

- T-001 项目初始化
- T-002 核心接口定义

## Implementation Notes

### FieldNode 类

```java
package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示一个字段节点（可能是简单字段、对象或数组）
 * 使用 Builder 模式构造
 */
public class FieldNode {
    private String originalName;
    private String camelCaseName;
    private String className;          // 对象定义的类名
    private int segLevel;
    private Integer length;
    private String dataType;
    private String optionality;        // M/O
    private String defaultValue;
    private String hardCodeValue;
    private String groupId;
    private String occurrenceCount;
    private boolean isArray;
    private boolean isObject;
    private boolean isTransitory;
    private List<FieldNode> children = new ArrayList<>();
    private SourceMetadata source = new SourceMetadata();

    // Private constructor for Builder
    private FieldNode() {}

    // Getters
    public String getOriginalName() { return originalName; }
    public String getCamelCaseName() { return camelCaseName; }
    public String getClassName() { return className; }
    public int getSegLevel() { return segLevel; }
    public Integer getLength() { return length; }
    public String getDataType() { return dataType; }
    public String getOptionality() { return optionality; }
    public String getDefaultValue() { return defaultValue; }
    public String getHardCodeValue() { return hardCodeValue; }
    public String getGroupId() { return groupId; }
    public String getOccurrenceCount() { return occurrenceCount; }
    public boolean isArray() { return isArray; }
    public boolean isObject() { return isObject; }
    public boolean isTransitory() { return isTransitory; }
    public List<FieldNode> getChildren() { return children; }
    public SourceMetadata getSource() { return source; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FieldNode node = new FieldNode();

        public Builder originalName(String name) {
            node.originalName = name;
            return this;
        }

        public Builder camelCaseName(String name) {
            node.camelCaseName = name;
            return this;
        }

        public Builder className(String name) {
            node.className = name;
            return this;
        }

        public Builder segLevel(int level) {
            node.segLevel = level;
            return this;
        }

        public Builder length(Integer length) {
            node.length = length;
            return this;
        }

        public Builder dataType(String type) {
            node.dataType = type;
            return this;
        }

        public Builder optionality(String opt) {
            node.optionality = opt;
            return this;
        }

        public Builder defaultValue(String value) {
            node.defaultValue = value;
            return this;
        }

        public Builder hardCodeValue(String value) {
            node.hardCodeValue = value;
            return this;
        }

        public Builder groupId(String id) {
            node.groupId = id;
            return this;
        }

        public Builder occurrenceCount(String count) {
            node.occurrenceCount = count;
            return this;
        }

        public Builder isArray(boolean array) {
            node.isArray = array;
            return this;
        }

        public Builder isObject(boolean object) {
            node.isObject = object;
            return this;
        }

        public Builder isTransitory(boolean transitory) {
            node.isTransitory = transitory;
            return this;
        }

        public Builder children(List<FieldNode> children) {
            node.children = children != null ? children : new ArrayList<>();
            return this;
        }

        public Builder source(SourceMetadata source) {
            node.source = source;
            return this;
        }

        public FieldNode build() {
            return node;
        }
    }
}
```

### MessageModel 类

```java
package com.rtm.mq.tool.model;

/**
 * 消息模型，包含完整的解析结果
 */
public class MessageModel {
    private Metadata metadata;
    private FieldGroup sharedHeader;
    private FieldGroup request;
    private FieldGroup response;

    // Getters and Setters
    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }

    public FieldGroup getSharedHeader() { return sharedHeader; }
    public void setSharedHeader(FieldGroup sharedHeader) { this.sharedHeader = sharedHeader; }

    public FieldGroup getRequest() { return request; }
    public void setRequest(FieldGroup request) { this.request = request; }

    public FieldGroup getResponse() { return response; }
    public void setResponse(FieldGroup response) { this.response = response; }
}
```

### FieldGroup 类

```java
package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段组，包含字段列表
 * 使用 List 保证顺序
 */
public class FieldGroup {
    private List<FieldNode> fields = new ArrayList<>();

    public List<FieldNode> getFields() { return fields; }
    public void setFields(List<FieldNode> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public void addField(FieldNode field) {
        this.fields.add(field);
    }
}
```

### Metadata 类

```java
package com.rtm.mq.tool.model;

/**
 * 解析元数据
 */
public class Metadata {
    private String sourceFile;
    private String sharedHeaderFile;
    private String parseTimestamp;
    private String parserVersion;
    private String operationName;
    private String operationId;
    private String version;

    // Getters and Setters
    // ...
}
```

### SourceMetadata 类

```java
package com.rtm.mq.tool.model;

/**
 * 字段来源元数据，用于审计追溯
 */
public class SourceMetadata {
    private String sheetName;
    private int rowIndex;

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }

    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
}
```

### ValidationResult 类

```java
package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 */
public class ValidationResult {
    private boolean success;
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationError> warnings = new ArrayList<>();

    public ValidationResult() {
        this.success = true;
    }

    public ValidationResult(List<ValidationError> errors) {
        this.errors = errors;
        this.success = errors.isEmpty();
    }

    public boolean isSuccess() { return success; }
    public List<ValidationError> getErrors() { return errors; }
    public List<ValidationError> getWarnings() { return warnings; }

    public void addError(ValidationError error) {
        this.errors.add(error);
        this.success = false;
    }

    public void addWarning(ValidationError warning) {
        this.warnings.add(warning);
    }
}
```

### ValidationError 类

```java
package com.rtm.mq.tool.model;

/**
 * 验证错误
 */
public class ValidationError {
    private String ruleCode;        // 如 VR-101
    private String description;
    private String details;
    private Severity severity;

    public enum Severity {
        ERROR, WARN
    }

    public ValidationError(String ruleCode, String description, String details) {
        this(ruleCode, description, details, Severity.ERROR);
    }

    public ValidationError(String ruleCode, String description, String details, Severity severity) {
        this.ruleCode = ruleCode;
        this.description = description;
        this.details = details;
        this.severity = severity;
    }

    // Getters
    public String getRuleCode() { return ruleCode; }
    public String getDescription() { return description; }
    public String getDetails() { return details; }
    public Severity getSeverity() { return severity; }
}
```

## Acceptance Criteria

1. [ ] 所有模型类编译通过
2. [ ] FieldNode 使用 Builder 模式
3. [ ] 集合类型使用 List（保证顺序）
4. [ ] 所有字段有 getter 方法
5. [ ] Javadoc 注释完整
6. [ ] 单元测试覆盖 Builder 构建

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | FieldNode Builder | 正确构建 |
| 单元测试 | MessageModel 组装 | 正确组装 |
| 单元测试 | ValidationResult 添加错误 | success=false |
| 单元测试 | 字段顺序保持 | List 顺序不变 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 字段遗漏 | 后续解析/生成错误 | 对照架构文档检查 |
| 类型不匹配 | JSON 序列化错误 | 使用正确的 Java 类型 |
| 空值处理 | NullPointerException | 初始化空集合 |
