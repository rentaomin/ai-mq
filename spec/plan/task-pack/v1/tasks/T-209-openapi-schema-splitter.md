# T-209 OpenAPI Schema 拆分器

## Goal

实现 OpenAPI Schema 拆分器,将单体 OpenAPI 主文件中的 schemas 部分按规则拆分为独立的 Schema 文件,提升可读性和维护性,支持按消息类型、按对象类型等不同拆分策略。

## In Scope / Out of Scope

**In Scope**:
- SchemaSplitter 类实现
- 从主 OpenAPI YAML 文件中提取所有 schemas
- 按拆分策略生成独立 Schema 文件
- 支持按消息拆分策略(Request/Response 分离)
- 支持按对象拆分策略(每个 schema 独立文件)
- 更新主文件中的 `$ref` 引用路径
- 生成 `schemas/` 子目录结构
- 保持 YAML 格式一致性和可读性
- 错误处理与退出码
- 配置化拆分策略选择

**Out of Scope**:
- OpenAPI 主文件生成(T-208 已完成)
- OpenAPI Schema 映射逻辑(T-207 已完成)
- OpenAPI 验证(T-303)
- 原子输出管理(T-307)
- 多版本 OpenAPI 支持(3.0 vs 3.1)
- Schema 合并或压缩

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.3 节(OpenAPI 生成器)
- T-208 OpenApiGenerator 类(提供主 OpenAPI YAML 文件)
- T-207 OpenApiTypeMapper 类(提供 Schema 映射)
- T-003 Config(配置加载,包括拆分策略配置)
- T-005 ParseException、ExitCodes(错误处理)
- YAML 解析库(如 SnakeYAML 或 Jackson YAML)

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/openapi/
└── SchemaSplitter.java

src/test/java/com/rtm/mq/tool/generator/openapi/
└── SchemaSplitterTest.java
```

**生成文件输出**(运行时产物):
```
{output-root}/openapi/
├── api.yaml                          # 主文件(schemas 部分已移除,包含 $ref 引用)
└── schemas/
    ├── CreateApplicationRequest.yaml
    ├── CreateApplicationResponse.yaml
    ├── CBACardArray.yaml
    ├── CreateApplication.yaml
    └── ...
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-208 | OpenApiGenerator 类(主 OpenAPI YAML 文件) |
| T-207 | OpenApiTypeMapper 类(Schema 映射) |
| T-003 | Config 配置加载器(获取拆分策略) |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心接口

```java
package com.rtm.mq.tool.generator.openapi;

import java.nio.file.Path;
import java.util.Map;

/**
 * OpenAPI Schema 拆分器接口
 */
public interface SchemaSplitter {
    /**
     * 拆分 OpenAPI 主文件中的 schemas
     * @param mainFilePath 主 OpenAPI YAML 文件路径
     * @return 拆分后的 Schema 文件路径列表
     */
    java.util.List<Path> splitSchemas(Path mainFilePath);

    /**
     * 获取拆分策略
     */
    SplitStrategy getStrategy();

    /**
     * Schema 拆分策略
     */
    enum SplitStrategy {
        /** 所有 schemas 保留在主文件中 */
        NONE,
        /** 按消息拆分(Request/Response 独立文件) */
        BY_MESSAGE,
        /** 按对象拆分(每个 schema 独立文件) */
        BY_OBJECT
    }
}
```

### SchemaSplitter 类

```java
package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.exception.ExitCodes;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * OpenAPI Schema 拆分器
 *
 * 从主 OpenAPI YAML 文件中提取 schemas 并拆分为独立文件。
 */
public class SchemaSplitter {

    private final Config config;
    private final SplitStrategy strategy;
    private final Yaml yaml;

    public SchemaSplitter(Config config) {
        this.config = config;
        this.strategy = config.getOpenApi().getSplitStrategy();
        this.yaml = createYaml();
    }

    /**
     * 拆分 schemas
     */
    public List<Path> splitSchemas(Path mainFilePath) throws ParseException {
        if (strategy == SplitStrategy.NONE) {
            // 不拆分,直接返回
            return Collections.emptyList();
        }

        try {
            // 读取主文件
            String mainContent = Files.readString(mainFilePath, StandardCharsets.UTF_8);
            Map<String, Object> mainDoc = yaml.load(mainContent);

            // 提取 schemas
            @SuppressWarnings("unchecked")
            Map<String, Object> components = (Map<String, Object>) mainDoc.get("components");
            if (components == null || !components.containsKey("schemas")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

            // 创建 schemas 目录
            Path schemasDir = mainFilePath.getParent().resolve("schemas");
            Files.createDirectories(schemasDir);

            // 拆分并写入独立文件
            List<Path> generatedFiles = new ArrayList<>();
            for (Map.Entry<String, Object> entry : schemas.entrySet()) {
                String schemaName = entry.getKey();
                Object schemaContent = entry.getValue();

                Path schemaFile = schemasDir.resolve(schemaName + ".yaml");
                writeSchemaFile(schemaFile, schemaName, schemaContent);
                generatedFiles.add(schemaFile);
            }

            // 更新主文件中的 schemas 引用
            updateMainFileReferences(mainFilePath, mainDoc, schemas.keySet());

            return generatedFiles;

        } catch (IOException e) {
            throw new ParseException(
                "Failed to split OpenAPI schemas: " + e.getMessage(),
                ExitCodes.IO_ERROR
            );
        }
    }

    /**
     * 写入单个 Schema 文件
     */
    private void writeSchemaFile(Path schemaFile, String schemaName, Object schemaContent)
            throws IOException {

        Map<String, Object> schemaDoc = new LinkedHashMap<>();
        schemaDoc.put(schemaName, schemaContent);

        String yamlContent = yaml.dump(schemaDoc);
        Files.writeString(schemaFile, yamlContent, StandardCharsets.UTF_8);
    }

    /**
     * 更新主文件中的 $ref 引用
     */
    private void updateMainFileReferences(Path mainFilePath,
                                         Map<String, Object> mainDoc,
                                         Set<String> schemaNames) throws IOException, ParseException {

        // 移除 components.schemas 部分
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) mainDoc.get("components");
        components.remove("schemas");

        // 递归更新所有 $ref 引用
        updateReferences(mainDoc, schemaNames);

        // 写回主文件
        String updatedContent = yaml.dump(mainDoc);
        Files.writeString(mainFilePath, updatedContent, StandardCharsets.UTF_8);
    }

    /**
     * 递归更新 $ref 引用
     */
    @SuppressWarnings("unchecked")
    private void updateReferences(Object node, Set<String> schemaNames) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;

            // 检查是否有 $ref 字段
            if (map.containsKey("$ref")) {
                String ref = (String) map.get("$ref");
                // 从 "#/components/schemas/SchemaName" 转换为 "./schemas/SchemaName.yaml#/SchemaName"
                for (String schemaName : schemaNames) {
                    String oldRef = "#/components/schemas/" + schemaName;
                    if (ref.equals(oldRef)) {
                        String newRef = "./schemas/" + schemaName + ".yaml#/" + schemaName;
                        map.put("$ref", newRef);
                        break;
                    }
                }
            }

            // 递归处理子节点
            for (Object value : map.values()) {
                updateReferences(value, schemaNames);
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (Object item : list) {
                updateReferences(item, schemaNames);
            }
        }
    }

    /**
     * 创建 YAML 解析器
     */
    private Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setIndentWithIndicator(false);
        return new Yaml(options);
    }

    public SplitStrategy getStrategy() {
        return strategy;
    }

    /**
     * Schema 拆分策略
     */
    public enum SplitStrategy {
        /** 所有 schemas 保留在主文件中 */
        NONE,
        /** 按消息拆分(Request/Response 独立文件) */
        BY_MESSAGE,
        /** 按对象拆分(每个 schema 独立文件) */
        BY_OBJECT
    }
}
```

### 输出示例

#### 主文件 (api.yaml) - 拆分后

```yaml
openapi: 3.0.3
info:
  title: MQ Message API
  version: 1.0.0

paths:
  /create-application:
    post:
      operationId: CreateApplication
      requestBody:
        content:
          application/json:
            schema:
              $ref: './schemas/CreateApplicationRequest.yaml#/CreateApplicationRequest'
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: './schemas/CreateApplicationResponse.yaml#/CreateApplicationResponse'

components:
  # schemas 部分已移除
```

#### 独立 Schema 文件 (schemas/CreateApplicationRequest.yaml)

```yaml
CreateApplicationRequest:
  type: object
  required:
    - customerId
  properties:
    customerId:
      type: string
      maxLength: 20
      description: CUST_ID
    applicationAmount:
      type: number
      format: decimal
      description: APPLICATION_AMT
    cbaCardArr:
      type: array
      maxItems: 9
      items:
        $ref: './CBACardArray.yaml#/CBACardArray'
    createApp:
      $ref: './CreateApplication.yaml#/CreateApplication'
```

#### 独立 Schema 文件 (schemas/CBACardArray.yaml)

```yaml
CBACardArray:
  type: object
  properties:
    cardNo:
      type: string
      maxLength: 16
      description: CARD_NO
    cardType:
      type: string
      maxLength: 2
      description: CARD_TYPE
    cardLimit:
      type: number
      format: decimal
      description: CARD_LIMIT
```

### 关键实现要点

1. **拆分策略**:
   - NONE: 不拆分,所有 schemas 保留在主文件中
   - BY_MESSAGE: 按消息拆分,Request/Response 各一个文件(未来扩展)
   - BY_OBJECT: 按对象拆分,每个 schema 独立文件(当前实现)

2. **$ref 引用更新**:
   - 原引用: `#/components/schemas/CreateApplicationRequest`
   - 新引用: `./schemas/CreateApplicationRequest.yaml#/CreateApplicationRequest`
   - 递归更新所有嵌套引用

3. **YAML 格式**:
   - 使用 SnakeYAML 库
   - Block 风格,非 Flow 风格
   - 2 空格缩进
   - 保持可读性

4. **目录结构**:
   - 主文件: `{output-root}/openapi/api.yaml`
   - Schema 文件: `{output-root}/openapi/schemas/{SchemaName}.yaml`

5. **幂等性**:
   - 多次运行产生相同输出
   - Schema 文件覆盖写入

## Acceptance Criteria

1. [ ] SchemaSplitter 类编译通过
2. [ ] 支持 NONE 策略(不拆分)
3. [ ] 支持 BY_OBJECT 策略(每个 schema 独立文件)
4. [ ] 正确提取主文件中的所有 schemas
5. [ ] 生成 `schemas/` 子目录
6. [ ] 每个 schema 生成独立 YAML 文件
7. [ ] 主文件中 components.schemas 部分被移除
8. [ ] 主文件中所有 `#/components/schemas/X` 引用更新为 `./schemas/X.yaml#/X`
9. [ ] 独立 Schema 文件中的嵌套引用也被更新
10. [ ] YAML 格式正确(Block 风格,2 空格缩进)
11. [ ] 使用 UTF-8 编码
12. [ ] 文件输出到正确路径
13. [ ] 拆分策略可通过 Config 配置
14. [ ] 空 schemas 时不生成 schemas 目录
15. [ ] 异常处理正确(文件读写失败、YAML 解析失败)

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | NONE 策略 | 不拆分,返回空列表 |
| 单元测试 | BY_OBJECT 策略,简单 schema | 生成 1 个文件 |
| 单元测试 | BY_OBJECT 策略,多个 schemas | 生成 N 个文件 |
| 单元测试 | 提取 schemas | 正确提取所有 schema 定义 |
| 单元测试 | 生成 schemas 目录 | 目录创建成功 |
| 单元测试 | 写入单个 Schema 文件 | 文件内容正确 |
| 单元测试 | 移除主文件 components.schemas | 主文件不再包含 schemas |
| 单元测试 | 更新主文件 $ref 引用 | 引用路径正确更新 |
| 单元测试 | 更新嵌套 $ref 引用 | 嵌套引用也被更新 |
| 单元测试 | YAML 格式验证 | Block 风格,2 空格缩进 |
| 单元测试 | UTF-8 编码 | 文件使用 UTF-8 编码 |
| 单元测试 | 空 schemas 处理 | 不生成 schemas 目录 |
| 单元测试 | 文件读取失败 | 抛出 ParseException |
| 单元测试 | YAML 解析失败 | 抛出 ParseException |
| 集成测试 | 从真实 OpenAPI 文件拆分 | 所有文件正确生成 |
| 集成测试 | 与 OpenApiGenerator 集成 | 拆分后的文件结构正确 |
| 验证测试 | 拆分后的 OpenAPI 验证 | OpenAPI Validator 通过 |
| Golden 测试 | 对比参考 YAML 文件 | 结构一致 |

### 测试代码示例

```java
package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import org.junit.jupiter.api.*;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaSplitterTest {

    private SchemaSplitter splitter;
    private Config config;
    private Path tempDir;
    private Yaml yaml;

    @BeforeEach
    void setUp() throws Exception {
        config = TestConfig.createDefault();
        config.getOpenApi().setSplitStrategy(SchemaSplitter.SplitStrategy.BY_OBJECT);
        splitter = new SchemaSplitter(config);
        tempDir = Files.createTempDirectory("openapi-test");
        yaml = new Yaml();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up temp directory
        Files.walk(tempDir)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(path -> {
                try { Files.deleteIfExists(path); } catch (Exception e) {}
            });
    }

    @Test
    void shouldNotSplitWhenStrategyIsNone() throws ParseException {
        // Given
        config.getOpenApi().setSplitStrategy(SchemaSplitter.SplitStrategy.NONE);
        splitter = new SchemaSplitter(config);
        Path mainFile = createSimpleOpenApiFile();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSplitSingleSchema() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithSingleSchema();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertEquals(1, result.size());
        assertTrue(Files.exists(result.get(0)));
        assertTrue(result.get(0).toString().endsWith("CreateApplicationRequest.yaml"));
    }

    @Test
    void shouldSplitMultipleSchemas() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithMultipleSchemas();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(p -> p.toString().endsWith("CreateApplicationRequest.yaml")));
        assertTrue(result.stream().anyMatch(p -> p.toString().endsWith("CreateApplicationResponse.yaml")));
        assertTrue(result.stream().anyMatch(p -> p.toString().endsWith("CBACardArray.yaml")));
    }

    @Test
    void shouldCreateSchemasDirectory() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithSingleSchema();
        Path schemasDir = mainFile.getParent().resolve("schemas");

        // When
        splitter.splitSchemas(mainFile);

        // Then
        assertTrue(Files.exists(schemasDir));
        assertTrue(Files.isDirectory(schemasDir));
    }

    @Test
    void shouldWriteSchemaFileWithCorrectContent() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithSingleSchema();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        String content = Files.readString(result.get(0));
        Map<String, Object> schemaDoc = yaml.load(content);
        assertTrue(schemaDoc.containsKey("CreateApplicationRequest"));
    }

    @Test
    void shouldRemoveSchemasFromMainFile() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithMultipleSchemas();

        // When
        splitter.splitSchemas(mainFile);

        // Then
        String mainContent = Files.readString(mainFile);
        Map<String, Object> mainDoc = yaml.load(mainContent);
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) mainDoc.get("components");
        assertFalse(components.containsKey("schemas"));
    }

    @Test
    void shouldUpdateReferencesInMainFile() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithReferences();

        // When
        splitter.splitSchemas(mainFile);

        // Then
        String mainContent = Files.readString(mainFile);
        assertTrue(mainContent.contains("./schemas/CreateApplicationRequest.yaml#/CreateApplicationRequest"));
        assertFalse(mainContent.contains("#/components/schemas/CreateApplicationRequest"));
    }

    @Test
    void shouldUpdateNestedReferences() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithNestedReferences();

        // When
        splitter.splitSchemas(mainFile);

        // Then
        Path schemaFile = mainFile.getParent().resolve("schemas/CreateApplicationRequest.yaml");
        String schemaContent = Files.readString(schemaFile);
        assertTrue(schemaContent.contains("./CBACardArray.yaml#/CBACardArray"));
    }

    @Test
    void shouldUseBlockYamlStyle() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithSingleSchema();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        String content = Files.readString(result.get(0));
        // Block 风格不包含 { } [ ]
        assertFalse(content.contains("{"));
        assertFalse(content.contains("["));
    }

    @Test
    void shouldUseUtf8Encoding() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithChineseDescription();

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        String content = Files.readString(result.get(0), StandardCharsets.UTF_8);
        assertTrue(content.contains("客户编号")); // Chinese characters
    }

    @Test
    void shouldNotCreateDirectoryForEmptySchemas() throws Exception {
        // Given
        Path mainFile = createOpenApiFileWithoutSchemas();
        Path schemasDir = mainFile.getParent().resolve("schemas");

        // When
        List<Path> result = splitter.splitSchemas(mainFile);

        // Then
        assertTrue(result.isEmpty());
        assertFalse(Files.exists(schemasDir));
    }

    @Test
    void shouldThrowExceptionForMissingFile() {
        // Given
        Path nonExistentFile = tempDir.resolve("non-existent.yaml");

        // When & Then
        assertThrows(ParseException.class, () -> splitter.splitSchemas(nonExistentFile));
    }

    // Helper methods
    private Path createSimpleOpenApiFile() throws Exception {
        Path file = tempDir.resolve("api.yaml");
        String content = """
            openapi: 3.0.3
            info:
              title: Test API
              version: 1.0.0
            paths: {}
            """;
        Files.writeString(file, content);
        return file;
    }

    private Path createOpenApiFileWithSingleSchema() throws Exception {
        Path file = tempDir.resolve("api.yaml");
        String content = """
            openapi: 3.0.3
            info:
              title: Test API
              version: 1.0.0
            paths: {}
            components:
              schemas:
                CreateApplicationRequest:
                  type: object
                  properties:
                    customerId:
                      type: string
            """;
        Files.writeString(file, content);
        return file;
    }

    // ... other helper methods
}
```

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| YAML 解析失败 | 拆分失败 | 添加 YAML 格式验证 |
| 文件写入失败 | 部分文件丢失 | 原子输出管理(T-307) |
| $ref 引用更新不完整 | 验证失败 | 递归更新所有嵌套节点 |
| 同名 schema 冲突 | 文件覆盖 | Schema 名称应唯一(由架构保证) |
| 相对路径错误 | 引用失败 | 使用 `./schemas/` 相对路径 |
| YAML 格式不一致 | 可读性差 | 统一 DumperOptions 配置 |
| 特殊字符在文件名中 | 文件系统错误 | Schema 名称应符合 Java 标识符规范 |
| 拆分后文件过多 | 维护困难 | 提供配置选项,支持 NONE 策略 |
| 循环引用 | 无限循环 | 由架构约束保证不存在循环引用 |
| Schema 为空 | 无效文件 | 检查 schema 内容,空 schema 不生成 |

---

**文档结束**
