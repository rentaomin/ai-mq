# T-302 Java Bean 验证器

## Goal

实现 Java Bean 验证器，用于验证生成的 Java Bean 类文件的语法正确性、编译可行性、命名规范符合性以及与中间 JSON Tree 的结构一致性。

---

## In Scope / Out of Scope

### In Scope

- 验证 Java 文件语法正确性（可编译）
- 验证类命名遵循 PascalCase 规范
- 验证字段命名遵循 camelCase 规范
- 验证 getter/setter 方法命名正确
- 验证字段类型映射与 JavaTypeMapper 规则一致
- 验证嵌套类结构与 JSON Tree 对应
- 验证 **不包含** groupId 和 occurrenceCount 字段（按规范要求排除）
- 验证枚举辅助方法的存在性和正确性
- 输出验证报告（成功/失败 + 详细错误信息）

### Out of Scope

- 跨制品一致性验证（由 T-304 负责）
- 运行时行为验证（单元测试覆盖）
- Java Bean 的生成逻辑（由 T-204, T-205, T-206 负责）
- 业务逻辑验证

---

## Inputs

| 输入 | 来源 | 说明 |
|------|------|------|
| `output/java/**/*.java` | T-204, T-205, T-206 | 生成的 Java Bean 类文件 |
| `output/json/spec-tree.json` | T-108 | 中间 JSON Tree（作为对照基准） |
| `src/main/java/.../JavaTypeMapper.java` | T-203 | 字段类型映射规则 |
| `config/naming-conventions.yaml` | 配置 | 命名规范配置 |

---

## Outputs

| 输出 | 路径 | 说明 |
|------|------|------|
| JavaBeanValidator.java | `src/main/java/com/example/mqtool/validator/JavaBeanValidator.java` | 验证器主类 |
| JavaValidationResult.java | `src/main/java/com/example/mqtool/validator/JavaValidationResult.java` | 验证结果数据模型 |
| JavaValidationError.java | `src/main/java/com/example/mqtool/validator/JavaValidationError.java` | 验证错误详情模型 |
| 单元测试 | `src/test/java/com/example/mqtool/validator/JavaBeanValidatorTest.java` | 验证器测试用例 |

---

## Dependencies

| Task ID | 任务名称 | 依赖原因 |
|---------|---------|---------|
| T-206 | 枚举辅助方法生成 | 需要完整的 Java Bean 文件（包含枚举辅助方法）作为验证输入 |
| T-005 | 错误处理框架 | 复用 ParseException 和 ExitCodes |
| T-002 | 核心接口定义 | 实现 Validator 接口 |

---

## Implementation Notes

### 验证规则

1. **语法验证**
   - 使用 Java Compiler API (javax.tools.JavaCompiler) 进行编译检查
   - 或使用 JavaParser 库进行 AST 解析

2. **命名规范验证**
   ```java
   // 类名：PascalCase
   Pattern.matches("[A-Z][a-zA-Z0-9]*", className)

   // 字段名：camelCase
   Pattern.matches("[a-z][a-zA-Z0-9]*", fieldName)

   // Getter：get + PascalCase
   Pattern.matches("get[A-Z][a-zA-Z0-9]*", methodName)

   // Setter：set + PascalCase
   Pattern.matches("set[A-Z][a-zA-Z0-9]*", methodName)
   ```

3. **禁止字段验证**
   - 确认不存在 `groupId` 字段
   - 确认不存在 `occurrenceCount` 字段
   - 这些字段按规范仅保留在 XML Bean 和中间 JSON 中

4. **类型映射验证**
   - 从 JavaTypeMapper 获取预期类型
   - 验证字段声明的类型匹配
   - 验证数组/List 类型对应 occurrenceCount > 1 的场景

5. **嵌套类验证**
   - 验证内部类存在性与 JSON Tree 对应
   - 验证嵌套深度正确

### 代码模式

```java
public class JavaBeanValidator implements Validator<File> {

    private final JavaTypeMapper typeMapper;
    private final JsonNode specTree;

    public JavaValidationResult validate(File javaFile) {
        List<JavaValidationError> errors = new ArrayList<>();

        // 1. 语法/编译检查
        errors.addAll(validateCompilable(javaFile));

        // 2. 命名规范检查
        errors.addAll(validateNamingConventions(javaFile));

        // 3. 禁止字段检查
        errors.addAll(validateForbiddenFields(javaFile));

        // 4. 类型映射检查
        errors.addAll(validateTypeMapping(javaFile));

        // 5. 嵌套结构检查
        errors.addAll(validateNestedStructure(javaFile));

        // 6. 枚举辅助方法检查
        errors.addAll(validateEnumHelpers(javaFile));

        return new JavaValidationResult(javaFile, errors);
    }
}
```

### 依赖库建议

| 库 | 用途 | Maven 坐标 |
|---|------|-----------|
| JavaParser | AST 解析 | `com.github.javaparser:javaparser-core` |
| javax.tools | 编译检查 | JDK 内置 |

---

## Acceptance Criteria

1. [ ] 能够检测 Java 语法错误（无法编译）
2. [ ] 能够检测类名不符合 PascalCase
3. [ ] 能够检测字段名不符合 camelCase
4. [ ] 能够检测 getter/setter 命名不正确
5. [ ] 能够检测存在 groupId 或 occurrenceCount 字段
6. [ ] 能够检测字段类型与 JavaTypeMapper 规则不一致
7. [ ] 能够检测嵌套类结构与 JSON Tree 不匹配
8. [ ] 能够检测枚举字段缺少辅助方法
9. [ ] 验证结果包含：文件路径、错误行号、错误描述、建议修复
10. [ ] 验证通过时返回 ExitCode.SUCCESS (0)
11. [ ] 验证失败时返回 ExitCode.VALIDATION_ERROR (1)

---

## Tests

### 单元测试

| 测试场景 | 预期结果 |
|----------|---------|
| 有效的 Java Bean 文件 | 验证通过，无错误 |
| Java 语法错误（缺少分号） | 返回 ERROR，包含行号 |
| 类名使用 camelCase | 返回 ERROR，提示应为 PascalCase |
| 字段名使用 PascalCase | 返回 ERROR，提示应为 camelCase |
| 包含 groupId 字段 | 返回 ERROR，提示禁止字段 |
| 包含 occurrenceCount 字段 | 返回 ERROR，提示禁止字段 |
| 字段类型不匹配（预期 Integer 实际 String） | 返回 ERROR，说明预期 vs 实际类型 |
| 缺少嵌套类 | 返回 ERROR，列出缺失的内部类 |
| 枚举字段缺少 fromCode 方法 | 返回 ERROR，提示缺少辅助方法 |
| getter 方法缺失 | 返回 ERROR，列出缺失的 getter |
| setter 方法缺失 | 返回 ERROR，列出缺失的 setter |

### 集成测试

- 使用 T-204, T-205, T-206 生成的真实 Java Bean 文件进行验证
- 验证与 spec-tree.json 的结构一致性

---

## Risks / Edge Cases

| 风险 | 缓解措施 |
|------|---------|
| JavaParser 库版本兼容性 | 锁定版本，在 CI 中测试 |
| 复杂泛型类型解析失败 | 使用 raw type 备选解析 |
| 匿名内部类干扰 | 仅验证命名内部类 |
| 生成的文件包含注解 | 忽略注解，专注于结构验证 |
| 循环依赖的嵌套类 | 设置最大递归深度 |
| 字段使用保留字命名 | 检测并报告为 ERROR |

---

**文档结束**
