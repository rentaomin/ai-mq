# T-303 OpenAPI 验证器

## Goal

实现 OpenAPI 验证器，用于验证生成的 OpenAPI (OAS 3.x) YAML 文件的规范符合性、结构完整性、Schema 引用正确性以及与中间 JSON Tree 的一致性。

---

## In Scope / Out of Scope

### In Scope

- 验证 OpenAPI 文件符合 OAS 3.0/3.1 规范
- 验证 YAML 语法正确性
- 验证 Schema 定义完整性（required、type、format）
- 验证 $ref 引用路径正确且目标存在
- 验证 Schema 拆分后的文件引用一致性
- 验证字段类型映射与 OpenApiTypeMapper 规则一致
- 验证 **不包含** groupId 和 occurrenceCount 属性（按规范要求排除）
- 验证嵌套结构与 JSON Tree 对应
- 输出验证报告（成功/失败 + 详细错误信息）

### Out of Scope

- 跨制品一致性验证（由 T-304 负责）
- API 运行时行为验证
- OpenAPI 文件的生成逻辑（由 T-207, T-208, T-209 负责）
- API 端点定义验证（本项目仅关注 Schema 部分）

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `output/openapi/openapi.yaml` | T-208 | OpenAPI 主文件 |
| `output/openapi/schemas/*.yaml` | T-209 | 拆分后的 Schema 文件 |
| `output/json/spec-tree.json` | T-108 | 中间 JSON Tree（作为对照基准） |
| `src/main/java/.../OpenApiTypeMapper.java` | T-207 | 字段类型映射规则 |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| OpenApiValidator.java | `src/main/java/com/example/mqtool/validator/OpenApiValidator.java` | 验证器主类 |
| OpenApiValidationResult.java | `src/main/java/com/example/mqtool/validator/OpenApiValidationResult.java` | 验证结果数据模型 |
| OpenApiValidationError.java | `src/main/java/com/example/mqtool/validator/OpenApiValidationError.java` | 验证错误详情模型 |
| 单元测试 | `src/test/java/com/example/mqtool/validator/OpenApiValidatorTest.java` | 验证器测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-209 | OpenAPI Schema 拆分 | 需要完整的 OpenAPI 文件（包含拆分 Schema）作为验证输入 |
| T-005 | 错误处理框架 | 复用 ParseException 和 ExitCodes |
| T-002 | 核心接口定义 | 实现 Validator 接口 |

---

## Implementation Notes

### 验证规则

1. **YAML 语法验证**
   - 使用 SnakeYAML 或 Jackson YAML 解析
   - 检测格式错误、缩进问题

2. **OAS 规范验证**
   - 验证必须包含：openapi、info、paths（可空）、components
   - 验证版本格式：`3.0.x` 或 `3.1.x`

3. **Schema 结构验证**
   ```yaml
   # 每个 Schema 必须包含
   type: object | array | string | integer | ...
   properties:  # 对于 object 类型
     fieldName:
       type: string
       # 可选：format, example, description
   required: [...]  # 必填字段列表
   ```

4. **$ref 引用验证**
   - 解析所有 `$ref` 路径
   - 验证目标 Schema 存在
   - 检测循环引用

5. **禁止属性验证**
   - 确认 Schema 中不存在 `groupId` 属性
   - 确认 Schema 中不存在 `occurrenceCount` 属性

6. **类型映射验证**
   - 从 OpenApiTypeMapper 获取预期类型
   - 验证 Schema 中声明的 type/format 匹配

### 代码模式

```java
public class OpenApiValidator implements Validator<File> {

    private final OpenApiTypeMapper typeMapper;
    private final JsonNode specTree;

    public OpenApiValidationResult validate(File openApiFile) {
        List<OpenApiValidationError> errors = new ArrayList<>();

        // 1. YAML 语法检查
        errors.addAll(validateYamlSyntax(openApiFile));

        // 2. OAS 规范检查
        errors.addAll(validateOasCompliance(openApiFile));

        // 3. Schema 结构检查
        errors.addAll(validateSchemaStructure(openApiFile));

        // 4. $ref 引用检查
        errors.addAll(validateReferences(openApiFile));

        // 5. 禁止属性检查
        errors.addAll(validateForbiddenProperties(openApiFile));

        // 6. 类型映射检查
        errors.addAll(validateTypeMapping(openApiFile));

        // 7. 层级一致性检查
        errors.addAll(validateHierarchy(openApiFile));

        return new OpenApiValidationResult(openApiFile, errors);
    }
}
```

### 依赖库建议

| 库 | 用途 | Maven 坐标 |
|---|------|-----------|
| Swagger Parser | OAS 规范验证 | `io.swagger.parser.v3:swagger-parser` |
| SnakeYAML | YAML 解析 | `org.yaml:snakeyaml` |
| OpenAPI Validator | 标准验证 | `org.openapitools:openapi-style-validator` |

---

## Acceptance Criteria

1. [ ] 能够检测 YAML 语法错误
2. [ ] 能够检测 OAS 规范不符合（缺少必须字段）
3. [ ] 能够检测 Schema 定义不完整（缺少 type）
4. [ ] 能够检测 $ref 引用目标不存在
5. [ ] 能够检测循环引用
6. [ ] 能够检测存在 groupId 或 occurrenceCount 属性
7. [ ] 能够检测字段类型与 OpenApiTypeMapper 规则不一致
8. [ ] 能够检测嵌套结构与 JSON Tree 不匹配
9. [ ] 验证结果包含：文件路径、错误位置、错误描述、建议修复
10. [ ] 验证通过时返回 ExitCode.SUCCESS (0)
11. [ ] 验证失败时返回 ExitCode.VALIDATION_ERROR (1)
12. [ ] 支持验证主文件 + 拆分 Schema 文件的整体一致性

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 有效的 OpenAPI 文件 | 验证通过，无错误 |
| YAML 语法错误（缩进问题） | 返回 ERROR，包含行号 |
| 缺少 openapi 版本字段 | 返回 ERROR，提示必须字段缺失 |
| 缺少 info 字段 | 返回 ERROR，提示必须字段缺失 |
| Schema 缺少 type | 返回 ERROR，提示 Schema 不完整 |
| $ref 引用不存在的 Schema | 返回 ERROR，列出无效引用 |
| 循环引用 A -> B -> A | 返回 WARNING，标记循环路径 |
| 包含 groupId 属性 | 返回 ERROR，提示禁止属性 |
| 包含 occurrenceCount 属性 | 返回 ERROR，提示禁止属性 |
| 字段类型不匹配 | 返回 ERROR，说明预期 vs 实际类型 |
| 拆分 Schema 文件缺失 | 返回 ERROR，列出缺失文件 |

### 集成测试

- 使用 T-208, T-209 生成的真实 OpenAPI 文件进行验证
- 验证主文件与拆分 Schema 的引用一致性
- 验证与 spec-tree.json 的结构一致性

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| OAS 3.0 与 3.1 差异 | 根据 openapi 字段值选择验证规则 |
| 大文件解析性能 | 使用流式解析，设置超时 |
| 相对路径 $ref 解析 | 以主文件目录为基准解析 |
| 外部 URL $ref | 仅验证本地引用，外部引用记录 WARNING |
| YAML 锚点和别名 | 使用支持锚点的解析器 |
| Schema 继承 (allOf/oneOf/anyOf) | 递归验证组合 Schema |

---

**文档结束**
