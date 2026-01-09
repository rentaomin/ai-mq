# Spring Boot REST API 使用指南

## 概述

MQ Spec Tool 现在支持**双模式运行**：
1. **CLI 模式** - 命令行工具（原有功能）
2. **REST API 模式** - HTTP 服务（新增功能）

## 快速开始

### 1. 编译项目

```bash
cd mq-spec-tool
mvn clean package
```

编译完成后会生成：`target/mq-spec-tool-1.0.0-SNAPSHOT.jar`

### 2. 启动 REST API 服务

```bash
# 无参数启动 = REST API 模式
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar
```

服务启动后默认监听端口：**8080**

### 3. 使用 CLI 模式（原有功能）

```bash
# 有参数 = CLI 模式
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar generate -i spec.xlsx -o ./output
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar validate -i ./output
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar version
```

---

## REST API 端点

### 1. 生成代码（Generate）

**端点**: `POST /api/v1/generate`

**请求格式**: `multipart/form-data`

**参数**:
- `specFile` (必填) - MQ 规格说明 Excel 文件
- `sharedHeaderFile` (可选) - 共享头 Excel 文件
- `xmlNamespaceInbound` (可选) - XML 入站命名空间
- `xmlNamespaceOutbound` (可选) - XML 出站命名空间
- `xmlProjectGroupId` (可选) - Maven groupId
- `xmlProjectArtifactId` (可选) - Maven artifactId
- `javaPackageName` (可选) - Java 包名
- `useLombok` (可选, 默认 false) - 是否使用 Lombok
- `openApiVersion` (可选, 默认 3.0.3) - OpenAPI 版本
- `splitSchemas` (可选, 默认 true) - 是否拆分 Schema

**响应**: ZIP 文件下载（包含所有生成的 XML/Java/OpenAPI 文件）

**示例（使用 curl）**:
```bash
curl -X POST http://localhost:8080/api/v1/generate \
  -F "specFile=@/path/to/spec.xlsx" \
  -F "sharedHeaderFile=@/path/to/shared-header.xlsx" \
  -F "javaPackageName=com.example.model" \
  -F "useLombok=true" \
  --output output.zip
```

**示例（使用 Postman）**:
1. 选择 POST 方法，URL: `http://localhost:8080/api/v1/generate`
2. 在 Body 选项卡选择 `form-data`
3. 添加字段:
   - `specFile`: 选择文件类型，上传 Excel
   - `javaPackageName`: 输入 `com.example.model`
   - `useLombok`: 输入 `true`
4. 点击 Send，保存下载的 ZIP 文件

---

### 2. 健康检查（Health Check）

**端点**: `GET /api/v1/health`

**响应**: `OK`

**示例**:
```bash
curl http://localhost:8080/api/v1/health
```

---

### 3. 版本信息（Version）

**端点**: `GET /api/v1/version`

**响应**: JSON 格式的版本信息

**示例**:
```bash
curl http://localhost:8080/api/v1/version
```

---

## 错误处理

所有错误返回统一格式的 JSON：

```json
{
  "status": 400,
  "code": "PARSE_ERROR",
  "message": "Failed to parse Excel specification: ...",
  "timestamp": "2026-01-09T10:30:00Z",
  "path": "/api/v1/generate"
}
```

**常见错误码**:
- `PARSE_ERROR` (400) - Excel 解析失败
- `VALIDATION_ERROR` (422) - 验证失败
- `GENERATION_ERROR` (500) - 代码生成失败
- `CONFIG_ERROR` (400) - 配置错误
- `FILE_UPLOAD_ERROR` (400) - 文件上传失败
- `INTERNAL_ERROR` (500) - 内部错误

---

## 配置

### application.yml 配置文件

位置：`src/main/resources/application.yml`

关键配置项：
```yaml
server:
  port: 8080  # REST API 端口

spring:
  servlet:
    multipart:
      max-file-size: 50MB      # 单个文件最大大小
      max-request-size: 100MB  # 请求最大大小

logging:
  level:
    com.rtm.mq.tool: DEBUG  # 日志级别

mq-spec-tool:
  output:
    root-dir: ./output
  parser:
    max-nesting-depth: 50
```

---

## 重要提示

### ⚠️ 需要实现的 Bean

当前 `BeanConfiguration.java` 中以下 Bean 需要提供具体实现类：

```java
// 需要替换为实际实现
@Bean
public Parser parser() {
    // TODO: 替换为 ExcelParserImpl 或其他实现
}

@Bean
public XmlGenerator xmlGenerator() {
    // TODO: 替换为 XmlGeneratorImpl
}

@Bean
public JavaGenerator javaGenerator() {
    // TODO: 替换为 JavaBeanGenerator
}

@Bean
public OpenApiGenerator openApiGenerator() {
    // TODO: 替换为 OpenApiGeneratorImpl
}
```

**解决方法**：
1. 找到这些接口的具体实现类
2. 在 `BeanConfiguration.java` 中替换 `throw new UnsupportedOperationException()` 为实际的实例化代码

示例：
```java
@Bean
public Parser parser() {
    return new ExcelParser();  // 假设 ExcelParser 是实现类
}
```

---

## 项目结构

```
mq-spec-tool/
├── src/main/java/com/rtm/mq/tool/
│   ├── Application.java                    # 双模式启动类
│   ├── cli/                                # CLI 相关（原有）
│   │   └── Main.java
│   ├── api/                                # REST API 相关（新增）
│   │   ├── controller/
│   │   │   └── SpecGenerationController.java
│   │   ├── service/
│   │   │   └── GenerationOrchestrator.java
│   │   ├── dto/
│   │   │   ├── GenerationRequest.java
│   │   │   ├── GenerationResponse.java
│   │   │   ├── ValidationResponse.java
│   │   │   └── ErrorResponse.java
│   │   ├── config/
│   │   │   └── BeanConfiguration.java
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   ├── parser/                             # 解析器（原有）
│   ├── generator/                          # 生成器（原有）
│   ├── validator/                          # 验证器（原有）
│   └── ...
└── src/main/resources/
    ├── application.yml                     # Spring Boot 配置
    └── default-config.yaml                 # 工具默认配置
```

---

## 常见问题

### Q1: 启动时报 Bean 未配置错误？
**A**: 请参考上面"需要实现的 Bean"章节，替换为实际实现类。

### Q2: 如何修改服务端口？
**A**: 编辑 `application.yml`，修改 `server.port`。

### Q3: 如何增大上传文件大小限制？
**A**: 编辑 `application.yml`，修改 `spring.servlet.multipart.max-file-size`。

### Q4: 生成的文件在哪里？
**A**:
- REST API 模式：直接返回 ZIP 文件下载
- CLI 模式：写入 `-o` 参数指定的目录

---

## 开发建议

### 添加新的 REST 端点

1. 在 `SpecGenerationController.java` 添加新方法
2. 使用 `@PostMapping` 或 `@GetMapping` 注解
3. 在 `GlobalExceptionHandler.java` 添加相应异常处理

### 集成到前端

推荐使用 AJAX 或 Fetch API 上传文件：

```javascript
const formData = new FormData();
formData.append('specFile', fileInput.files[0]);
formData.append('javaPackageName', 'com.example.model');
formData.append('useLombok', 'true');

fetch('http://localhost:8080/api/v1/generate', {
  method: 'POST',
  body: formData
})
.then(response => response.blob())
.then(blob => {
  // 下载 ZIP 文件
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'output.zip';
  a.click();
});
```

---

## 后续改进方向

- [ ] 添加异步生成支持（长时间任务）
- [ ] 添加 Swagger/OpenAPI 文档自动生成
- [ ] 添加生成历史记录查询
- [ ] 添加 WebSocket 进度推送
- [ ] 添加用户认证与授权
- [ ] Docker 镜像打包

---

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
