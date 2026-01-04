# T-003 配置加载器

## Goal

实现配置文件加载器，支持从 YAML 文件读取配置，并与命令行参数合并，遵循 CLI > Config > Default 的优先级规则。

## In Scope / Out of Scope

**In Scope**:
- Config POJO 类定义
- YAML 配置文件解析
- 命令行参数与配置合并
- 配置验证逻辑
- 默认值设置

**Out of Scope**:
- CLI 入口点实现（T-309）
- 完整命令行解析（T-309）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 11 节（配置与命令行设计）
- T-001 创建的项目结构

## Outputs

```
src/main/java/com/rtm/mq/tool/config/
├── Config.java
├── XmlConfig.java
├── JavaConfig.java
├── OpenApiConfig.java
├── AuditConfig.java
├── ValidationConfig.java
├── OutputConfig.java
├── ParserConfig.java
└── ConfigLoader.java

src/main/resources/
└── default-config.yaml
```

## Dependencies

- T-001 项目初始化

## Implementation Notes

### Config 主类

```java
package com.rtm.mq.tool.config;

public class Config {
    private OutputConfig output;
    private XmlConfig xml;
    private JavaConfig java;
    private OpenApiConfig openapi;
    private ParserConfig parser;
    private AuditConfig audit;
    private ValidationConfig validation;
    private String loggingLevel = "INFO";

    // Getters and Setters
    // ...

    public void setDefaults() {
        if (output == null) output = new OutputConfig();
        if (xml == null) xml = new XmlConfig();
        if (java == null) java = new JavaConfig();
        if (openapi == null) openapi = new OpenApiConfig();
        if (parser == null) parser = new ParserConfig();
        if (audit == null) audit = new AuditConfig();
        if (validation == null) validation = new ValidationConfig();

        output.setDefaults();
        xml.setDefaults();
        java.setDefaults();
        openapi.setDefaults();
        parser.setDefaults();
        audit.setDefaults();
        validation.setDefaults();
    }

    public void merge(Config other) {
        // 合并非空配置
        if (other.output != null) this.output.merge(other.output);
        // ... 其他字段
    }
}
```

### XmlConfig

```java
package com.rtm.mq.tool.config;

public class XmlConfig {
    private NamespaceConfig namespace;
    private ProjectConfig project;

    public static class NamespaceConfig {
        private String inbound;
        private String outbound;
        // Getters, Setters
    }

    public static class ProjectConfig {
        private String groupId;
        private String artifactId;
        // Getters, Setters
    }

    public void setDefaults() {
        // 无默认值，必须配置
    }
}
```

### ParserConfig

```java
package com.rtm.mq.tool.config;

public class ParserConfig {
    private int maxNestingDepth = 50;

    public void setDefaults() {
        if (maxNestingDepth <= 0) {
            maxNestingDepth = 50;
        }
    }
    // Getters, Setters
}
```

### ConfigLoader

```java
package com.rtm.mq.tool.config;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.*;

public class ConfigLoader {
    private final Yaml yaml = new Yaml();

    public Config load(Path configFile, Map<String, String> cliOverrides) {
        Config config = new Config();

        // 1. 设置默认值
        config.setDefaults();

        // 2. 加载配置文件
        if (configFile != null && Files.exists(configFile)) {
            Config fileConfig = loadFromFile(configFile);
            config.merge(fileConfig);
        }

        // 3. 应用 CLI 覆盖
        applyCliOverrides(config, cliOverrides);

        // 4. 验证配置
        validate(config);

        return config;
    }

    private Config loadFromFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return yaml.loadAs(is, Config.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to load config file: " + path, e);
        }
    }

    private void applyCliOverrides(Config config, Map<String, String> overrides) {
        if (overrides.containsKey("output-dir")) {
            config.getOutput().setRootDir(overrides.get("output-dir"));
        }
        if (overrides.containsKey("max-nesting-depth")) {
            config.getParser().setMaxNestingDepth(
                Integer.parseInt(overrides.get("max-nesting-depth")));
        }
        // ... 其他 CLI 参数
    }

    public void validate(Config config) {
        List<String> errors = new ArrayList<>();

        if (config.getXml().getNamespace().getInbound() == null) {
            errors.add("xml.namespace.inbound is required");
        }
        if (config.getXml().getNamespace().getOutbound() == null) {
            errors.add("xml.namespace.outbound is required");
        }
        if (config.getXml().getProject().getGroupId() == null) {
            errors.add("xml.project.groupId is required");
        }

        if (!errors.isEmpty()) {
            throw new ConfigException(
                "Configuration validation failed:\n" + String.join("\n", errors));
        }
    }
}
```

### default-config.yaml

```yaml
output:
  rootDir: "./output"

xml:
  namespace:
    inbound: null   # 必须配置
    outbound: null  # 必须配置
  project:
    groupId: null   # 必须配置
    artifactId: null

java:
  useLombok: false
  package: null     # 从 groupId + artifactId 推导

parser:
  maxNestingDepth: 50

openapi:
  version: "3.0.3"
  splitSchemas: true

audit:
  hashOutputs: false
  redactFilePaths: false

validation:
  redactPayload: true

logging:
  level: "INFO"
```

## Acceptance Criteria

1. [ ] Config 类包含所有必需的配置字段
2. [ ] 从 YAML 文件正确加载配置
3. [ ] CLI 参数正确覆盖文件配置
4. [ ] 必需配置缺失时抛出 ConfigException
5. [ ] 默认值正确设置
6. [ ] 单元测试覆盖率 > 80%

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 加载有效配置文件 | 成功解析 |
| 单元测试 | 加载无效配置文件 | 抛出 ConfigException |
| 单元测试 | CLI 覆盖配置 | CLI 值优先 |
| 单元测试 | 必需配置缺失 | 抛出 ConfigException |
| 单元测试 | 默认值设置 | 正确默认 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| YAML 解析错误 | 配置加载失败 | 提供详细错误信息 |
| 类型转换错误 | 配置值类型不匹配 | 验证类型并提示 |
| 配置文件不存在 | 程序启动失败 | 使用默认值或提示 |
