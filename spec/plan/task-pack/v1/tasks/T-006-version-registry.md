# T-006 版本注册器

## Goal

实现版本管理组件，读取 versions.properties 文件，提供各组件版本号的统一访问接口，支持审计日志记录。

## In Scope / Out of Scope

**In Scope**:
- VersionRegistry 类实现
- versions.properties 文件格式定义
- 版本号读取和缓存

**Out of Scope**:
- 版本号自动更新
- 版本比较逻辑

## Inputs

- 架构文档: `spec/design/architecture.md` 第 9.3 节（版本捕获策略）
- T-003 配置加载器

## Outputs

```
src/main/java/com/rtm/mq/tool/version/
└── VersionRegistry.java

src/main/resources/
└── versions.properties
```

## Dependencies

- T-001 项目初始化
- T-003 配置加载器

## Implementation Notes

### versions.properties

```properties
# MQ Spec Tool 版本配置
# 遵循语义化版本号规范 (SemVer)

# 解析器版本
parser.version=1.0.0

# XML 模板版本
xml.template.version=1.0.0

# Java 模板版本
java.template.version=1.0.0

# YAML 模板版本
yaml.template.version=1.0.0

# 规则版本
rules.version=1.0.0

# 工具总版本
tool.version=1.0.0
```

### VersionRegistry 类

```java
package com.rtm.mq.tool.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 版本注册器
 * 提供各组件版本号的统一访问
 */
public final class VersionRegistry {
    private static final String VERSIONS_FILE = "/versions.properties";
    private static final Properties versions = new Properties();
    private static boolean loaded = false;

    private VersionRegistry() {}

    /**
     * 加载版本配置
     */
    private static synchronized void ensureLoaded() {
        if (loaded) return;

        try (InputStream is = VersionRegistry.class.getResourceAsStream(VERSIONS_FILE)) {
            if (is == null) {
                throw new RuntimeException("Cannot find " + VERSIONS_FILE);
            }
            versions.load(is);
            loaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load versions", e);
        }
    }

    /**
     * 获取解析器版本
     */
    public static String getParserVersion() {
        ensureLoaded();
        return versions.getProperty("parser.version", "unknown");
    }

    /**
     * 获取 XML 模板版本
     */
    public static String getXmlTemplateVersion() {
        ensureLoaded();
        return versions.getProperty("xml.template.version", "unknown");
    }

    /**
     * 获取 Java 模板版本
     */
    public static String getJavaTemplateVersion() {
        ensureLoaded();
        return versions.getProperty("java.template.version", "unknown");
    }

    /**
     * 获取 YAML 模板版本
     */
    public static String getYamlTemplateVersion() {
        ensureLoaded();
        return versions.getProperty("yaml.template.version", "unknown");
    }

    /**
     * 获取规则版本
     */
    public static String getRulesVersion() {
        ensureLoaded();
        return versions.getProperty("rules.version", "unknown");
    }

    /**
     * 获取工具总版本
     */
    public static String getToolVersion() {
        ensureLoaded();
        return versions.getProperty("tool.version", "unknown");
    }

    /**
     * 获取所有版本信息
     * @return 版本信息副本
     */
    public static Properties getAllVersions() {
        ensureLoaded();
        Properties copy = new Properties();
        copy.putAll(versions);
        return copy;
    }

    /**
     * 获取版本摘要字符串（用于审计日志）
     */
    public static String getVersionSummary() {
        ensureLoaded();
        return String.format(
            "tool=%s, parser=%s, xml=%s, java=%s, yaml=%s, rules=%s",
            getToolVersion(),
            getParserVersion(),
            getXmlTemplateVersion(),
            getJavaTemplateVersion(),
            getYamlTemplateVersion(),
            getRulesVersion()
        );
    }

    /**
     * 重置加载状态（仅用于测试）
     */
    static void reset() {
        loaded = false;
        versions.clear();
    }
}
```

### 版本信息结构类（用于审计日志）

```java
package com.rtm.mq.tool.version;

/**
 * 版本信息结构类，用于 JSON 序列化
 */
public class VersionInfo {
    private String parser;
    private String xmlTemplate;
    private String javaTemplate;
    private String yamlTemplate;
    private String rules;
    private String tool;

    public static VersionInfo fromRegistry() {
        VersionInfo info = new VersionInfo();
        info.parser = VersionRegistry.getParserVersion();
        info.xmlTemplate = VersionRegistry.getXmlTemplateVersion();
        info.javaTemplate = VersionRegistry.getJavaTemplateVersion();
        info.yamlTemplate = VersionRegistry.getYamlTemplateVersion();
        info.rules = VersionRegistry.getRulesVersion();
        info.tool = VersionRegistry.getToolVersion();
        return info;
    }

    // Getters
    public String getParser() { return parser; }
    public String getXmlTemplate() { return xmlTemplate; }
    public String getJavaTemplate() { return javaTemplate; }
    public String getYamlTemplate() { return yamlTemplate; }
    public String getRules() { return rules; }
    public String getTool() { return tool; }
}
```

## Acceptance Criteria

1. [ ] versions.properties 文件存在且格式正确
2. [ ] VersionRegistry 正确读取版本号
3. [ ] 缺失配置时返回 "unknown"
4. [ ] 版本摘要字符串格式正确
5. [ ] 线程安全（synchronized 加载）
6. [ ] 单元测试覆盖

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 读取存在的版本号 | 返回正确值 |
| 单元测试 | 读取不存在的版本号 | 返回 "unknown" |
| 单元测试 | 版本摘要格式 | 包含所有版本 |
| 单元测试 | getAllVersions | 返回副本 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 文件不存在 | 启动失败 | 抛出明确异常 |
| 并发加载 | 数据不一致 | synchronized 加载 |
| 版本格式错误 | 审计日志异常 | 验证版本格式 |
