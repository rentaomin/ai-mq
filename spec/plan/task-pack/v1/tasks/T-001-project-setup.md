# T-001 项目初始化

## Goal

创建 Maven 项目基础结构，配置必要的依赖和构建设置，为后续所有开发任务提供基础。

## In Scope / Out of Scope

**In Scope**:
- 创建 Maven pom.xml 文件
- 配置核心依赖（Apache POI, Gson, JavaPoet, SnakeYAML, JavaParser, Swagger Parser）
- 配置测试依赖（JUnit 5, Mockito）
- 创建标准 Maven 目录结构
- 创建 .gitignore 文件
- 配置 Java 11 编译器

**Out of Scope**:
- 业务代码实现
- 单元测试编写
- CI/CD 配置（Gate 3）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 6 节（技术栈）
- 需求文档中的依赖列表

## Outputs

```
mq-spec-tool/
├── pom.xml
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── rtm/
│   │   │           └── mq/
│   │   │               └── tool/
│   │   │                   └── .gitkeep
│   │   └── resources/
│   │       └── versions.properties
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── rtm/
│       │           └── mq/
│       │               └── tool/
│       │                   └── .gitkeep
│       └── resources/
│           └── .gitkeep
```

## Dependencies

无（首个任务）

## Implementation Notes

### pom.xml 核心依赖

```xml
<dependencies>
    <!-- Excel 解析 -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>

    <!-- Java 代码生成 -->
    <dependency>
        <groupId>com.squareup</groupId>
        <artifactId>javapoet</artifactId>
        <version>1.13.0</version>
    </dependency>

    <!-- YAML 处理 -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
    </dependency>

    <!-- Java 语法验证 -->
    <dependency>
        <groupId>com.github.javaparser</groupId>
        <artifactId>javaparser-core</artifactId>
        <version>3.25.8</version>
    </dependency>

    <!-- OpenAPI 验证 -->
    <dependency>
        <groupId>io.swagger.parser.v3</groupId>
        <artifactId>swagger-parser</artifactId>
        <version>2.1.19</version>
    </dependency>

    <!-- 中文转拼音 -->
    <dependency>
        <groupId>com.belerweb</groupId>
        <artifactId>pinyin4j</artifactId>
        <version>2.5.1</version>
    </dependency>

    <!-- CLI 解析 -->
    <dependency>
        <groupId>commons-cli</groupId>
        <artifactId>commons-cli</artifactId>
        <version>1.6.0</version>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 包结构规划

```
com.rtm.mq.tool
├── cli/          # CLI 入口
├── config/       # 配置加载
├── parser/       # Excel 解析
├── generator/    # 代码生成
│   ├── xml/
│   ├── java/
│   └── openapi/
├── validator/    # 验证器
├── audit/        # 审计日志
├── output/       # 输出管理
├── model/        # 数据模型
└── exception/    # 异常定义
```

## Acceptance Criteria

1. [ ] `mvn clean compile` 成功执行无错误
2. [ ] 所有依赖正确下载
3. [ ] 目录结构符合 Maven 标准
4. [ ] versions.properties 文件存在且包含初始版本号
5. [ ] .gitignore 包含 target/, *.class, *.jar, .idea/ 等

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 构建测试 | `mvn clean compile` | 成功 |
| 依赖测试 | `mvn dependency:tree` | 所有依赖正确解析 |
| 资源测试 | 读取 versions.properties | 内容正确 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 依赖版本冲突 | 编译失败 | 使用 dependencyManagement 统一版本 |
| 网络问题 | 依赖下载失败 | 配置镜像仓库 |
| JDK 版本不匹配 | 编译失败 | 明确要求 Java 11+ |
