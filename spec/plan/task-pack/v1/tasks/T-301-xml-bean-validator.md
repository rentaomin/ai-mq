# T-301 XML Bean 验证器

## Goal

实现 XML Bean 验证器，用于验证生成的 Outbound 和 Inbound XML Bean 定义文件的结构完整性、语法正确性以及与中间 JSON Tree 的一致性。

---

## In Scope / Out of Scope

### In Scope

- 验证 XML 文件的基础语法（well-formed XML）
- 验证 XML Bean 结构符合预定义的 Schema 或 DTD
- 验证字段命名规范（驼峰命名、无非法字符）
- 验证字段类型映射正确性（与 XmlTypeMapper 规则一致）
- 验证必填字段完整性
- 验证嵌套层级与中间 JSON Tree 一致
- 验证 groupId 和 occurrenceCount 在 XML Bean 中的正确保留
- 输出验证报告（成功/失败 + 详细错误信息）

### Out of Scope

- 跨制品一致性验证（由 T-304 负责）
- 运行时消息内容验证（由 T-306 负责）
- XML 文件的生成逻辑（由 T-111, T-202 负责）

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `output/xml/outbound/*.xml` | T-111 | Outbound XML Bean 定义文件 |
| `output/xml/inbound/*.xml` | T-202 | Inbound XML Bean 定义文件 |
| `output/json/spec-tree.json` | T-108 | 中间 JSON Tree（作为对照基准） |
| `src/main/java/.../XmlTypeMapper.java` | T-109 | 字段类型映射规则 |
| `config/xml-bean-schema.xsd` | 配置 | XML Bean 结构 Schema（可选） |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| XmlBeanValidator.java | `src/main/java/com/example/mqtool/validator/XmlBeanValidator.java` | 验证器主类 |
| XmlValidationResult.java | `src/main/java/com/example/mqtool/validator/XmlValidationResult.java` | 验证结果数据模型 |
| XmlValidationError.java | `src/main/java/com/example/mqtool/validator/XmlValidationError.java` | 验证错误详情模型 |
| 单元测试 | `src/test/java/com/example/mqtool/validator/XmlBeanValidatorTest.java` | 验证器测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-111 | Outbound XML 生成器 | 需要 Outbound XML 文件作为验证输入 |
| T-202 | Inbound XML 生成器 | 需要 Inbound XML 文件作为验证输入 |
| T-005 | 错误处理框架 | 复用 ParseException 和 ExitCodes |
| T-002 | 核心接口定义 | 实现 Validator 接口 |

---

## Implementation Notes

### 验证规则

1. **语法验证**
   - 使用 Java 标准 XML 解析器（DocumentBuilder）检查 well-formed
   - 可选：使用 XSD Schema 验证结构

2. **结构验证**
   ```java
   // 必须包含的根元素属性
   - messageType (String)
   - version (String)

   // 字段节点必须包含
   - name (驼峰命名)
   - type (映射后的 XML 类型)
   - required (boolean)
   ```

3. **类型映射验证**
   - 从 XmlTypeMapper 获取预期类型
   - 比对 XML 中声明的类型
   - 记录不匹配项

4. **层级一致性验证**
   - 加载 spec-tree.json
   - 遍历 XML 树结构
   - 验证嵌套深度和字段顺序

### 代码模式

```java
public class XmlBeanValidator implements Validator<File> {

    public XmlValidationResult validate(File xmlFile) {
        List<XmlValidationError> errors = new ArrayList<>();

        // 1. 语法检查
        errors.addAll(validateWellFormed(xmlFile));

        // 2. Schema 检查（可选）
        errors.addAll(validateSchema(xmlFile));

        // 3. 类型映射检查
        errors.addAll(validateTypeMapping(xmlFile));

        // 4. 层级一致性检查
        errors.addAll(validateHierarchy(xmlFile));

        return new XmlValidationResult(xmlFile, errors);
    }
}
```

### 错误分类

| 错误级别 | 含义 | 处理方式 |
|----------|------|---------|
| ERROR | 必须修复的问题 | 阻止后续流程 |
| WARNING | 可能的问题 | 记录但不阻止 |
| INFO | 信息性提示 | 仅记录 |

---

## Acceptance Criteria

1. [ ] 能够检测并报告 XML 语法错误（非 well-formed）
2. [ ] 能够检测字段类型与 XmlTypeMapper 规则不一致
3. [ ] 能够检测必填字段缺失
4. [ ] 能够检测嵌套层级与 JSON Tree 不匹配
5. [ ] 验证结果包含：文件路径、错误行号、错误描述、建议修复
6. [ ] 验证通过时返回 ExitCode.SUCCESS (0)
7. [ ] 验证失败时返回 ExitCode.VALIDATION_ERROR (1)
8. [ ] 验证器可处理空文件并返回明确错误

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 有效的 Outbound XML | 验证通过，无错误 |
| 有效的 Inbound XML | 验证通过，无错误 |
| XML 语法错误（未闭合标签） | 返回 ERROR，包含行号 |
| 字段类型不匹配 | 返回 ERROR，说明预期 vs 实际类型 |
| 缺少必填字段 | 返回 ERROR，列出缺失字段 |
| 嵌套层级错误 | 返回 ERROR，说明层级差异 |
| 空文件 | 返回 ERROR，提示文件为空 |
| 不存在的文件 | 返回 ERROR，提示文件不存在 |

### 集成测试

- 使用 T-111 和 T-202 生成的真实 XML 文件进行验证
- 验证与 spec-tree.json 的一致性

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| XML 文件过大导致内存溢出 | 使用 SAX 解析器替代 DOM |
| XSD Schema 不存在 | 跳过 Schema 验证，仅做语法检查 |
| spec-tree.json 格式变化 | 验证前先校验 JSON Tree 版本 |
| 编码问题（非 UTF-8） | 检测编码并记录警告 |
| 循环引用（自引用类型） | 设置最大递归深度，超过则报错 |

---

**文档结束**
