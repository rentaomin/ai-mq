# T-208 OpenAPI 主文件生成

## Goal

实现 OpenAPI 主文件生成器，将中间 JSON Tree 转换为符合 OpenAPI 3.x 规范的 YAML 文件。生成包含 Request/Response Schema 定义的主 OpenAPI 文件（`api.yaml`），供 API 文档和代码生成工具使用。

## In Scope / Out of Scope

**In Scope**:
- OpenApiGenerator 类实现
- 主 OpenAPI 文件（`api.yaml`）生成
- Request Schema 生成
- Response Schema 生成
- OpenAPI 3.x 版本声明
- info 元数据生成（title, version, description）
- paths 定义生成（POST 端点）
- components/schemas 定义生成
- 字段顺序保持（与 Excel/JSON Tree 一致）
- transitory 字段排除（groupId、occurrenceCount 不出现在 Schema 中）
- required 列表生成（基于 optionality）
- YAML 格式输出（确定性、可读性）
- 错误处理与退出码

**Out of Scope**:
- Schema 拆分与独立文件生成（T-209）
- OpenAPI 类型映射逻辑（T-207 已完成）
- OpenAPI 验证（T-303）
- 原子输出管理（T-307）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6.3 节（OpenAPI 生成器）
- T-207 OpenApiTypeMapper 类（提供类型映射）
- T-108 DeterministicJsonWriter（提供 JSON Tree 读取）
- T-004 MessageModel、FieldGroup、FieldNode（数据模型）
- T-003 Config（配置加载，包括 API 元数据、输出路径）
- T-005 ParseException、ExitCodes（错误处理）

## Outputs

```
src/main/java/com/rtm/mq/tool/generator/openapi/
└── OpenApiGenerator.java

src/test/java/com/rtm/mq/tool/generator/openapi/
└── OpenApiGeneratorTest.java
```

**生成文件输出**（运行时产物）:
```
{output-root}/openapi/
└── api.yaml
```

## Dependencies

| Task ID | 依赖内容 |
|---------|---------|
| T-207 | OpenApiTypeMapper 类（类型映射、Schema 生成） |
| T-108 | JSON Tree 序列化结构 |
| T-004 | MessageModel、FieldGroup、FieldNode 数据模型 |
| T-003 | Config 配置加载器（获取 API 元数据配置） |
| T-005 | 错误处理框架 |

## Implementation Notes

### 核心接口

```java
package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.generator.Generator;

/**
 * OpenAPI 生成器接口
 */
public interface OpenApiGeneratorInterface extends Generator {
    /**
     * 获取生成的 YAML 内容
     */
    String getGeneratedContent();

    /**
     * 获取生成的 Schema 名称列表
     */
    java.util.List<String> getGeneratedSchemaNames();
}
```

### OpenApiGenerator 类

```java
package com.rtm.mq.tool.generator.openapi;

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
 * OpenAPI 主文件生成器
 *
 * 从 MessageModel 生成 OpenAPI 3.x 规范的 api.yaml 文件。
 */
public class OpenApiGenerator implements OpenApiGeneratorInterface {

    private static final String OUTPUT_FILENAME = "api.yaml";
    private static final String OPENAPI_VERSION = "3.0.3";

    private final Config config;
    private final OpenApiTypeMapper typeMapper;
    private String generatedContent;
    private final List<String> generatedSchemaNames = new ArrayList<>();

    public OpenApiGenerator(Config config) {
        this.config = config;
        this.typeMapper = new OpenApiTypeMapper(config);
    }

    @Override
    public void generate(MessageModel model) throws ParseException {
        String operationId = model.getMetadata().getOperationId();
        if (operationId == null || operationId.isBlank()) {
            throw new ParseException(
                "Operation ID is required for OpenAPI generation",
                ExitCodes.GENERATION_ERROR
            );
        }

        // 构建 OpenAPI 文档结构
        Map<String, Object> openApiDoc = new LinkedHashMap<>();

        // 1. openapi 版本
        openApiDoc.put("openapi", OPENAPI_VERSION);

        // 2. info 元数据
        openApiDoc.put("info", buildInfo(model));

        // 3. servers（可选）
        openApiDoc.put("servers", buildServers());

        // 4. paths
        openApiDoc.put("paths", buildPaths(model, operationId));

        // 5. components/schemas
        openApiDoc.put("components", buildComponents(model, operationId));

        // 转换为 YAML
        this.generatedContent = toYaml(openApiDoc);

        // 写入文件
        writeToFile();
    }

    /**
     * 构建 info 部分
     */
    private Map<String, Object> buildInfo(MessageModel model) {
        Map<String, Object> info = new LinkedHashMap<>();

        String title = config.getOpenApi().getTitle();
        if (title == null || title.isEmpty()) {
            title = model.getMetadata().getOperationId() + " API";
        }
        info.put("title", title);

        String version = config.getOpenApi().getVersion();
        if (version == null || version.isEmpty()) {
            version = "1.0.0";
        }
        info.put("version", version);

        String description = config.getOpenApi().getDescription();
        if (description == null || description.isEmpty()) {
            description = "Generated from MQ message specification";
        }
        info.put("description", description);

        return info;
    }

    /**
     * 构建 servers 部分
     */
    private List<Map<String, Object>> buildServers() {
        List<Map<String, Object>> servers = new ArrayList<>();

        String serverUrl = config.getOpenApi().getServerUrl();
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = "http://localhost:8080";
        }

        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", serverUrl);
        server.put("description", "Default server");
        servers.add(server);

        return servers;
    }

    /**
     * 构建 paths 部分
     */
    private Map<String, Object> buildPaths(MessageModel model, String operationId) {
        Map<String, Object> paths = new LinkedHashMap<>();

        String pathName = "/" + camelToKebab(operationId);
        Map<String, Object> pathItem = new LinkedHashMap<>();

        // POST 操作
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("operationId", operationId);
        post.put("summary", operationId + " operation");
        post.put("description", "MQ message operation: " + operationId);
        post.put("tags", Collections.singletonList(operationId));

        // requestBody
        if (model.getRequest() != null && !model.getRequest().getFields().isEmpty()) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("required", true);
            requestBody.put("content", buildContent(operationId + "Request"));
            post.put("requestBody", requestBody);
        }

        // responses
        Map<String, Object> responses = new LinkedHashMap<>();

        // 200 响应
        Map<String, Object> response200 = new LinkedHashMap<>();
        response200.put("description", "Successful response");
        if (model.getResponse() != null && !model.getResponse().getFields().isEmpty()) {
            response200.put("content", buildContent(operationId + "Response"));
        }
        responses.put("200", response200);

        // 默认错误响应
        Map<String, Object> responseDefault = new LinkedHashMap<>();
        responseDefault.put("description", "Error response");
        responses.put("default", responseDefault);

        post.put("responses", responses);
        pathItem.put("post", post);
        paths.put(pathName, pathItem);

        return paths;
    }

    /**
     * 构建 content 部分（application/json）
     */
    private Map<String, Object> buildContent(String schemaName) {
        Map<String, Object> content = new LinkedHashMap<>();
        Map<String, Object> jsonContent = new LinkedHashMap<>();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$ref", "#/components/schemas/" + schemaName);
        jsonContent.put("schema", schema);
        content.put("application/json", jsonContent);
        return content;
    }

    /**
     * 构建 components 部分
     */
    private Map<String, Object> buildComponents(MessageModel model, String operationId) {
        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();

        // Request Schema
        if (model.getRequest() != null && !model.getRequest().getFields().isEmpty()) {
            String requestSchemaName = operationId + "Request";
            schemas.put(requestSchemaName,
                typeMapper.generateObjectSchema(model.getRequest().getFields()));
            generatedSchemaNames.add(requestSchemaName);

            // 收集嵌套 Schema
            collectNestedSchemas(model.getRequest().getFields(), schemas);
        }

        // Response Schema
        if (model.getResponse() != null && !model.getResponse().getFields().isEmpty()) {
            String responseSchemaName = operationId + "Response";
            schemas.put(responseSchemaName,
                typeMapper.generateObjectSchema(model.getResponse().getFields()));
            generatedSchemaNames.add(responseSchemaName);

            // 收集嵌套 Schema
            collectNestedSchemas(model.getResponse().getFields(), schemas);
        }

        components.put("schemas", schemas);
        return components;
    }

    /**
     * 递归收集嵌套 Schema
     */
    private void collectNestedSchemas(List<FieldNode> fields,
                                      Map<String, Object> schemas) {
        for (FieldNode field : fields) {
            if (field.isTransitory()) {
                continue;
            }

            if (field.isObject() || field.isArray()) {
                String className = field.getClassName();
                if (className != null && !className.isEmpty()
                    && !schemas.containsKey(className)) {

                    // 生成嵌套 Schema
                    if (!field.getChildren().isEmpty()) {
                        schemas.put(className,
                            typeMapper.generateObjectSchema(field.getChildren()));
                        generatedSchemaNames.add(className);

                        // 递归收集子嵌套
                        collectNestedSchemas(field.getChildren(), schemas);
                    }
                }
            }
        }
    }

    /**
     * 转换为 YAML 字符串
     * 使用确定性排序和格式化
     */
    private String toYaml(Map<String, Object> data) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Generated by MQ Tool\n");
        yaml.append("# DO NOT EDIT - This file is auto-generated\n\n");
        writeYamlMap(yaml, data, 0);
        return yaml.toString();
    }

    /**
     * 递归写入 YAML Map
     */
    private void writeYamlMap(StringBuilder yaml, Map<String, Object> map, int indent) {
        String indentStr = "  ".repeat(indent);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            yaml.append(indentStr).append(key).append(":");

            if (value instanceof Map) {
                yaml.append("\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                writeYamlMap(yaml, mapValue, indent + 1);
            } else if (value instanceof List) {
                yaml.append("\n");
                @SuppressWarnings("unchecked")
                List<Object> listValue = (List<Object>) value;
                writeYamlList(yaml, listValue, indent + 1);
            } else {
                yaml.append(" ").append(formatYamlValue(value)).append("\n");
            }
        }
    }

    /**
     * 递归写入 YAML List
     */
    private void writeYamlList(StringBuilder yaml, List<Object> list, int indent) {
        String indentStr = "  ".repeat(indent);

        for (Object item : list) {
            if (item instanceof Map) {
                yaml.append(indentStr).append("-\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> mapItem = (Map<String, Object>) item;
                writeYamlMap(yaml, mapItem, indent + 1);
            } else {
                yaml.append(indentStr).append("- ")
                    .append(formatYamlValue(item)).append("\n");
            }
        }
    }

    /**
     * 格式化 YAML 值
     */
    private String formatYamlValue(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            // 需要引号的情况
            if (str.contains(":") || str.contains("#") || str.contains("'")
                || str.contains("\"") || str.contains("\n")
                || str.startsWith(" ") || str.endsWith(" ")
                || str.isEmpty()) {
                return "\"" + str.replace("\"", "\\\"") + "\"";
            }
            return str;
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    /**
     * 写入 YAML 文件
     */
    private void writeToFile() throws ParseException {
        Path outputDir = Path.of(config.getOutput().getRoot(), "openapi");
        Path outputFile = outputDir.resolve(OUTPUT_FILENAME);

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputFile, generatedContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParseException(
                "Failed to write OpenAPI file: " + e.getMessage(),
                ExitCodes.IO_ERROR
            );
        }
    }

    /**
     * camelCase 转 kebab-case
     */
    private String camelToKebab(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    @Override
    public String getGeneratedContent() {
        return generatedContent;
    }

    @Override
    public List<String> getGeneratedSchemaNames() {
        return new ArrayList<>(generatedSchemaNames);
    }

    /**
     * 获取输出文件路径
     */
    public Path getOutputPath() {
        return Path.of(config.getOutput().getRoot(), "openapi", OUTPUT_FILENAME);
    }
}
```

### 输出 OpenAPI YAML 示例

```yaml
# Generated by MQ Tool
# DO NOT EDIT - This file is auto-generated

openapi: 3.0.3
info:
  title: CreateApplication API
  version: 1.0.0
  description: Generated from MQ message specification
servers:
  - url: http://localhost:8080
    description: Default server
paths:
  /create-application:
    post:
      operationId: CreateApplication
      summary: CreateApplication operation
      description: MQ message operation: CreateApplication
      tags:
        - CreateApplication
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateApplicationRequest'
      responses:
        200:
          description: Successful response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CreateApplicationResponse'
        default:
          description: Error response
components:
  schemas:
    CreateApplicationRequest:
      type: object
      required:
        - createApp
        - domicileBranch
      properties:
        createApp:
          $ref: './CreateApplication.yaml'
        productDel:
          $ref: './ProductDetails.yaml'
        cbaCardArr:
          type: array
          items:
            $ref: './CBACardArray.yaml'
          maxItems: 9
        domicileBranch:
          type: string
          maxLength: 10
    CreateApplicationResponse:
      type: object
      required:
        - returnCode
      properties:
        returnCode:
          type: string
          maxLength: 4
        returnMessage:
          type: string
          maxLength: 100
        applicationId:
          type: string
          maxLength: 20
    CreateApplication:
      type: object
      properties:
        applicationDate:
          type: string
          format: date
        applicationAmount:
          type: string
          format: decimal
    ProductDetails:
      type: object
      properties:
        productCode:
          type: string
          maxLength: 10
    CBACardArray:
      type: object
      properties:
        cardNumber:
          type: string
          maxLength: 16
        cardType:
          type: string
          maxLength: 2
```

### 关键实现要点

1. **OpenAPI 版本**:
   - 使用 OpenAPI 3.0.3 版本
   - 确保与 swagger-ui、swagger-codegen 兼容

2. **字段顺序**:
   - 必须与 Excel/JSON Tree 中的顺序完全一致
   - 使用 LinkedHashMap 保证顺序

3. **transitory 字段处理**:
   - groupId、occurrenceCount 字段不出现在 Schema 中
   - 由 T-207 OpenApiTypeMapper 处理过滤

4. **required 列表**:
   - 基于 `optionality == "M"` 生成
   - 仅在有必填字段时添加 required 键

5. **$ref 引用**:
   - 主文件中使用 `#/components/schemas/{SchemaName}`
   - 嵌套对象使用相对路径 `./{ClassName}.yaml`（T-209 处理拆分）

6. **YAML 格式**:
   - 确定性输出（字段顺序固定）
   - 适当缩进（2 空格）
   - 添加生成标记注释

7. **路径命名**:
   - operationId 转换为 kebab-case
   - 示例: `CreateApplication` -> `/create-application`

### 配置依赖

从 Config 中获取以下配置项：
- `openApi.title`: API 标题
- `openApi.version`: API 版本
- `openApi.description`: API 描述
- `openApi.serverUrl`: 服务器 URL

## Acceptance Criteria

1. [ ] OpenApiGenerator 类编译通过
2. [ ] 生成的 YAML 文件符合 OpenAPI 3.0.3 规范
3. [ ] openapi 版本声明正确（3.0.3）
4. [ ] info 部分包含 title、version、description
5. [ ] paths 部分包含 POST 端点定义
6. [ ] requestBody 引用正确的 Request Schema
7. [ ] responses 包含 200 和 default 响应
8. [ ] components/schemas 包含 Request 和 Response Schema
9. [ ] 字段顺序与 JSON Tree 完全一致
10. [ ] transitory 字段被排除（groupId、occurrenceCount 不出现）
11. [ ] required 列表正确（仅包含 optionality="M" 的字段）
12. [ ] maxLength 正确生成
13. [ ] default 值正确生成
14. [ ] 数组类型正确（type: array, items）
15. [ ] 嵌套对象引用正确（$ref）
16. [ ] YAML 格式正确、可读
17. [ ] 文件输出到 `{output-root}/openapi/api.yaml`
18. [ ] 使用 UTF-8 编码
19. [ ] 缺少 operationId 时抛出 ParseException
20. [ ] 空 Request/Response 时对应部分不生成

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 简单 API 生成 | YAML 结构正确 |
| 单元测试 | openapi 版本 | 3.0.3 |
| 单元测试 | info 部分 | title、version、description 正确 |
| 单元测试 | paths 部分 | POST 端点正确 |
| 单元测试 | requestBody 引用 | $ref 指向正确 Schema |
| 单元测试 | responses 部分 | 200 和 default 响应 |
| 单元测试 | Request Schema | type: object, properties 正确 |
| 单元测试 | Response Schema | type: object, properties 正确 |
| 单元测试 | 字段顺序验证 | 顺序与输入一致 |
| 单元测试 | transitory 字段排除 | groupId、occurrenceCount 不出现 |
| 单元测试 | required 列表 | 仅 optionality="M" 字段 |
| 单元测试 | maxLength 生成 | length 正确映射 |
| 单元测试 | 数组类型 | type: array, items.$ref |
| 单元测试 | 对象引用 | $ref: './ClassName.yaml' |
| 单元测试 | 嵌套 Schema 收集 | 递归收集所有嵌套类型 |
| 单元测试 | 空 Request 处理 | 无 requestBody |
| 单元测试 | 空 Response 处理 | 200 响应无 content |
| 单元测试 | 缺少 operationId | 抛出 ParseException |
| 集成测试 | 从 JSON Tree 文件生成 | 文件正确写入 |
| 集成测试 | 与 OpenApiTypeMapper 集成 | 类型映射正确 |
| 验证测试 | OpenAPI 规范验证 | 使用 swagger-parser 验证 |
| Golden 测试 | 对比参考 api.yaml | 结构一致 |

### 测试代码示例

```java
package com.rtm.mq.tool.generator.openapi;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.model.*;
import com.rtm.mq.tool.exception.ParseException;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiGeneratorTest {

    private OpenApiGenerator generator;
    private Config config;

    @BeforeEach
    void setUp() {
        config = TestConfig.createDefault();
        generator = new OpenApiGenerator(config);
    }

    @Test
    void shouldGenerateValidOpenApiDocument() throws ParseException {
        // Given
        MessageModel model = createSimpleModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertNotNull(content);
        assertTrue(content.contains("openapi: 3.0.3"));
    }

    @Test
    void shouldContainInfoSection() throws ParseException {
        // Given
        MessageModel model = createSimpleModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("info:"));
        assertTrue(content.contains("title:"));
        assertTrue(content.contains("version:"));
    }

    @Test
    void shouldContainPathsSection() throws ParseException {
        // Given
        MessageModel model = createSimpleModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("paths:"));
        assertTrue(content.contains("/create-application:"));
        assertTrue(content.contains("post:"));
    }

    @Test
    void shouldContainRequestBodyReference() throws ParseException {
        // Given
        MessageModel model = createSimpleModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("requestBody:"));
        assertTrue(content.contains("#/components/schemas/CreateApplicationRequest"));
    }

    @Test
    void shouldContainResponseReference() throws ParseException {
        // Given
        MessageModel model = createModelWithResponse();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("#/components/schemas/CreateApplicationResponse"));
    }

    @Test
    void shouldContainComponentsSchemas() throws ParseException {
        // Given
        MessageModel model = createSimpleModel();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("components:"));
        assertTrue(content.contains("schemas:"));
        assertTrue(content.contains("CreateApplicationRequest:"));
    }

    @Test
    void shouldFilterTransitoryFields() throws ParseException {
        // Given
        MessageModel model = createModelWithTransitoryFields();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertFalse(content.contains("groupId"));
        assertFalse(content.contains("occurrenceCount"));
    }

    @Test
    void shouldGenerateRequiredList() throws ParseException {
        // Given
        MessageModel model = createModelWithRequiredFields();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("required:"));
    }

    @Test
    void shouldGenerateMaxLength() throws ParseException {
        // Given
        MessageModel model = createModelWithLengthConstraint();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("maxLength:"));
    }

    @Test
    void shouldGenerateArrayType() throws ParseException {
        // Given
        MessageModel model = createModelWithArrayField();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("type: array"));
        assertTrue(content.contains("items:"));
    }

    @Test
    void shouldGenerateObjectRef() throws ParseException {
        // Given
        MessageModel model = createModelWithNestedObject();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertTrue(content.contains("$ref:"));
    }

    @Test
    void shouldCollectNestedSchemas() throws ParseException {
        // Given
        MessageModel model = createModelWithNestedObject();

        // When
        generator.generate(model);

        // Then
        List<String> schemaNames = generator.getGeneratedSchemaNames();
        assertTrue(schemaNames.size() > 1);
    }

    @Test
    void shouldThrowExceptionForMissingOperationId() {
        // Given
        MessageModel model = new MessageModel();
        model.setRequest(new FieldGroup());
        model.setMetadata(new Metadata(null));

        // When & Then
        assertThrows(ParseException.class, () -> generator.generate(model));
    }

    @Test
    void shouldHandleEmptyRequest() throws ParseException {
        // Given
        MessageModel model = new MessageModel();
        model.setRequest(new FieldGroup()); // empty
        model.setResponse(createResponseFieldGroup());
        model.setMetadata(new Metadata("TestOp"));

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        assertFalse(content.contains("requestBody:"));
    }

    @Test
    void shouldPreserveFieldOrder() throws ParseException {
        // Given
        MessageModel model = createModelWithMultipleFields();

        // When
        generator.generate(model);

        // Then
        String content = generator.getGeneratedContent();
        int pos1 = content.indexOf("field1:");
        int pos2 = content.indexOf("field2:");
        int pos3 = content.indexOf("field3:");
        assertTrue(pos1 < pos2 && pos2 < pos3, "Field order must be preserved");
    }

    // Helper methods
    private MessageModel createSimpleModel() {
        MessageModel model = new MessageModel();
        FieldGroup request = new FieldGroup();
        request.addField(FieldNode.builder()
            .camelCaseName("customerId")
            .dataType("String")
            .length(20)
            .optionality("M")
            .build());
        model.setRequest(request);
        model.setMetadata(new Metadata("CreateApplication"));
        return model;
    }

    // ... other helper methods
}
```

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| YAML 格式错误 | 下游工具无法解析 | YAML 规范验证测试 |
| $ref 路径错误 | Schema 无法解析 | 路径生成单元测试 |
| 循环引用 | OpenAPI 验证失败 | 本组件不处理，由 T-303 验证 |
| 嵌套层级过深 | YAML 可读性差 | 配置最大嵌套深度警告 |
| 特殊字符在键名中 | YAML 语法错误 | 适当引号转义 |
| 超长描述文本 | 格式问题 | 多行字符串处理 |
| required 为空数组 | 不必要的空数组 | 仅非空时添加 |
| 重复 Schema 名称 | 覆盖问题 | 检测并警告 |
| OpenAPI 版本兼容性 | 部分工具不支持 | 使用广泛支持的 3.0.3 |
| 文件覆盖 | 丢失已有修改 | 输出到独立目录 |

---

**文档结束**
