# MQ Spec Tool - MQ 消息规范自动化工具

## 项目概述

MQ Spec Tool 是一个自动化工具，用于解析 IBM MQ 消息规范 Excel 文件，自动生成 XML Bean、Java Bean 和 OpenAPI YAML 等制品，确保跨制品的一致性，并提供完整的审计和验证能力。

### 核心能力

- **自动解析** MQ Message Spec Excel 文件（固定长度报文规范）
- **自动生成** XML Bean 定义、Java Bean 类、OpenAPI YAML 文件
- **确保一致性** 所有制品的字段定义、顺序、类型完全一致
- **支持审计** 完整的输入溯源、版本记录和字段映射表
- **支持验证** 规格与实际报文的一致性校验

### 解决的问题

在 SAPI 与 APE 之间通过 IBM MQ 进行通信时，开发人员需要手工创建：
- XML Bean 定义（用于报文序列化/反序列化）
- Java Bean 类（用于业务代码操作）
- OpenAPI YAML 文件（用于 API 定义和 Controller 生成）

这一流程存在以下痛点：
- 人工处理工作量巨大（Excel 通常包含 400-500 行字段）
- 错误率高，一致性难以保证
- 问题往往在运行期或联调期才暴露
- 缺乏统一的审计、溯源和一致性校验能力

---

## 目录

- [快速开始](#快速开始)
- [安装与构建](#安装与构建)
- [使用指南](#使用指南)
- [配置说明](#配置说明)
- [功能模块](#功能模块)
- [输出制品](#输出制品)
- [验证能力](#验证能力)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [常见问题](#常见问题)

---

## 快速开始

### 前置要求

- **JDK**: 11 或以上版本
- **Maven**: 3.6 或以上版本
- **输入文件**: MQ Message Spec Excel 文件（.xlsx 格式）

### 构建项目

```bash
cd mq-spec-tool
mvn clean package
```

### 基本使用

```bash
# 解析 Excel 并生成所有制品
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar \
  --input spec.xlsx \
  --output ./output \
  --config config.yml

# 指定共享头部文件
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar \
  --input spec.xlsx \
  --shared-header shared-header.xlsx \
  --output ./output

# 仅生成特定制品
java -jar target/mq-spec-tool-1.0.0-SNAPSHOT.jar \
  --input spec.xlsx \
  --output ./output \
  --generate xml,java
```

---

## 安装与构建

### 克隆仓库

```bash
git clone <repository-url>
cd ai-mq
```

### 编译项目

```bash
cd mq-spec-tool
mvn clean compile
```

### 运行测试

```bash
mvn test
```

### 打包

```bash
mvn package
```

生成的 JAR 文件位于 `target/mq-spec-tool-1.0.0-SNAPSHOT.jar`

---

## 使用指南

### 命令行参数

| 参数 | 简写 | 说明 | 必需 | 默认值 |
|------|------|------|------|--------|
| `--input` | `-i` | 输入的 Message Spec Excel 文件路径 | 是 | - |
| `--output` | `-o` | 输出目录路径 | 否 | `./output` |
| `--config` | `-c` | 配置文件路径（YAML 格式） | 否 | - |
| `--shared-header` | `-s` | 共享头部 Excel 文件路径 | 否 | - |
| `--generate` | `-g` | 指定要生成的制品类型（xml,java,openapi） | 否 | 全部 |
| `--validate-only` | `-v` | 仅执行验证，不生成文件 | 否 | `false` |
| `--skip-validation` | | 跳过验证步骤 | 否 | `false` |
| `--log-level` | `-l` | 日志级别（DEBUG, INFO, WARN, ERROR） | 否 | `INFO` |
| `--version` | | 显示工具版本信息 | 否 | - |
| `--help` | `-h` | 显示帮助信息 | 否 | - |

### 使用示例

#### 1. 基本使用 - 生成所有制品

```bash
java -jar mq-spec-tool.jar \
  --input /path/to/message-spec.xlsx \
  --output ./output
```

输出目录结构：
```
output/
├── xml/
│   ├── outbound-bean.xml      # Request XML Bean
│   └── inbound-bean.xml       # Response XML Bean
├── java/
│   └── com/example/model/
│       ├── RequestBean.java
│       └── ResponseBean.java
├── openapi/
│   └── api-spec.yaml          # OpenAPI YAML
├── intermediate/
│   └── message-tree.json      # 中间 JSON 树
└── audit/
    ├── audit-log.json         # 审计日志
    └── field-mapping.csv      # 字段映射表
```

#### 2. 使用配置文件

创建配置文件 `config.yml`：

```yaml
output:
  baseDir: ./output
  atomic: true

xml:
  outboundPath: xml/request-bean.xml
  inboundPath: xml/response-bean.xml
  template: default

java:
  basePackage: com.example.mq.model
  generateGettersSetters: true
  generateToString: true

openapi:
  version: 3.0.0
  title: MQ Message API
  basePath: /api/v1

parser:
  strictMode: true
  maxNestingDepth: 10

audit:
  enabled: true
  outputPath: audit/

validation:
  enabled: true
  strictMode: true
```

运行：

```bash
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --config config.yml
```

#### 3. 仅生成特定制品

```bash
# 仅生成 XML Bean
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --output ./output \
  --generate xml

# 生成 XML 和 Java Bean
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --output ./output \
  --generate xml,java
```

#### 4. 验证模式

仅验证 Excel 规范文件的正确性，不生成文件：

```bash
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --validate-only
```

#### 5. 使用共享头部

```bash
java -jar mq-spec-tool.jar \
  --input message-spec.xlsx \
  --shared-header shared-header.xlsx \
  --output ./output
```

#### 6. 调试模式

```bash
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --output ./output \
  --log-level DEBUG
```

---

## 配置说明

### 配置文件格式

工具支持 YAML 格式的配置文件。配置优先级：**CLI 参数 > 配置文件 > 默认值**

### 完整配置示例

```yaml
# 输出配置
output:
  baseDir: ./output              # 输出基础目录
  atomic: true                   # 原子化输出（全部成功或全部失败）
  overwrite: true                # 是否覆盖已存在的文件
  keepIntermediateFiles: true    # 是否保留中间文件

# XML Bean 配置
xml:
  outboundPath: xml/outbound-bean.xml    # Request XML Bean 输出路径
  inboundPath: xml/inbound-bean.xml      # Response XML Bean 输出路径
  template: default                       # 模板名称
  namespace: http://example.com/mq        # XML 命名空间
  indent: 2                               # 缩进空格数

# Java Bean 配置
java:
  basePackage: com.example.mq.model      # 基础包名
  outputPath: java/                       # 输出路径
  generateGettersSetters: true            # 生成 getter/setter
  generateToString: true                  # 生成 toString
  generateEquals: false                   # 生成 equals/hashCode
  useJavaxValidation: true                # 使用 javax.validation 注解
  useLombok: false                        # 使用 Lombok 注解

# OpenAPI 配置
openapi:
  outputPath: openapi/api-spec.yaml      # 输出路径
  version: 3.0.0                          # OpenAPI 版本
  title: MQ Message API                   # API 标题
  description: Auto-generated from MQ spec # API 描述
  basePath: /api/v1                       # 基础路径
  generateExamples: true                  # 生成示例数据

# 解析器配置
parser:
  strictMode: true                        # 严格模式（遇到错误立即停止）
  maxNestingDepth: 10                     # 最大嵌套深度
  allowDuplicateNames: false              # 是否允许重复字段名
  encoding: UTF-8                         # 文件编码

# 审计配置
audit:
  enabled: true                           # 是否启用审计
  outputPath: audit/                      # 审计文件输出路径
  includeEnvironment: true                # 记录环境信息
  includeTimestamp: true                  # 记录时间戳

# 验证配置
validation:
  enabled: true                           # 是否启用验证
  strictMode: true                        # 严格验证模式
  validateXmlSyntax: true                 # 验证 XML 语法
  validateJavaSyntax: true                # 验证 Java 语法
  validateOpenApiSpec: true               # 验证 OpenAPI 规范
  crossArtifactConsistency: true          # 跨制品一致性验证

# 日志配置
loggingLevel: INFO                        # 日志级别
```

### 配置项详解

#### 输出配置 (output)

- `baseDir`: 所有输出文件的根目录
- `atomic`: 原子化输出，确保全部成功或全部失败（失败时回滚）
- `overwrite`: 是否覆盖已存在的文件
- `keepIntermediateFiles`: 是否保留中间 JSON 树文件

#### XML 配置 (xml)

- `outboundPath`: Request（Outbound）XML Bean 相对路径
- `inboundPath`: Response（Inbound）XML Bean 相对路径
- `template`: 使用的 XML 模板（default / custom）
- `namespace`: XML 命名空间
- `indent`: XML 缩进空格数

#### Java 配置 (java)

- `basePackage`: Java Bean 的基础包名
- `outputPath`: Java 文件输出目录
- `generateGettersSetters`: 是否生成 getter/setter 方法
- `generateToString`: 是否生成 toString 方法
- `generateEquals`: 是否生成 equals 和 hashCode 方法
- `useJavaxValidation`: 是否添加 JSR-303 验证注解
- `useLombok`: 是否使用 Lombok 注解（@Data, @Getter, @Setter）

#### OpenAPI 配置 (openapi)

- `outputPath`: OpenAPI YAML 文件输出路径
- `version`: OpenAPI 规范版本（3.0.0 / 3.1.0）
- `title`: API 标题
- `description`: API 描述
- `basePath`: API 基础路径
- `generateExamples`: 是否生成示例数据

#### 解析器配置 (parser)

- `strictMode`: 严格模式，遇到任何错误立即停止
- `maxNestingDepth`: 最大字段嵌套深度（防止过深嵌套）
- `allowDuplicateNames`: 是否允许重复的字段名称
- `encoding`: Excel 文件编码

#### 审计配置 (audit)

- `enabled`: 是否启用审计功能
- `outputPath`: 审计文件输出路径
- `includeEnvironment`: 是否记录系统环境信息
- `includeTimestamp`: 是否记录时间戳信息

#### 验证配置 (validation)

- `enabled`: 是否启用验证功能
- `strictMode`: 严格验证模式
- `validateXmlSyntax`: 验证生成的 XML 语法正确性
- `validateJavaSyntax`: 验证生成的 Java 代码语法正确性
- `validateOpenApiSpec`: 验证生成的 OpenAPI 规范正确性
- `crossArtifactConsistency`: 跨制品一致性验证

---

## 功能模块

### 1. Excel 解析器 (Parser)

**位置**: `com.rtm.mq.tool.parser`

**主要功能**：
- 解析 Message Spec Excel 文件（.xlsx 格式）
- 提取消息头部元数据（系统名称、版本号等）
- 识别 Request 和 Response Sheet
- 解析字段定义（字段名、数据类型、长度、Seg lvl、OccurrenceCount 等）
- 构建嵌套层级关系（基于 Seg lvl）
- 检测对象与数组（基于 OccurrenceCount：1..1 为对象，0..N / 1..N 为数组）
- 字段名称规范化（转换为 camelCase）
- 重复字段检测

**核心类**：
- `ExcelParser`: Excel 解析主入口
- `SheetDiscovery`: Sheet 发现和识别
- `MetadataExtractor`: 元数据提取
- `SegLevelParser`: 层级解析
- `ObjectArrayDetector`: 对象/数组检测
- `CamelCaseConverter`: 命名规范化
- `DuplicateDetector`: 重复字段检测

### 2. 生成器 (Generator)

#### 2.1 XML Bean 生成器

**位置**: `com.rtm.mq.tool.generator.xml`

**功能**：
- 生成 Outbound（Request）XML Bean
- 生成 Inbound（Response）XML Bean
- 保持字段顺序与 Excel 完全一致
- 数据类型映射（Excel 类型 → XML 类型）
- 嵌套结构生成
- Converter 映射

**核心类**：
- `OutboundXmlGenerator`: Request XML 生成器
- `InboundXmlGenerator`: Response XML 生成器
- `XmlTypeMapper`: 类型映射器
- `XmlTemplateEngine`: XML 模板引擎
- `XmlFormatter`: XML 格式化器

#### 2.2 Java Bean 生成器

**位置**: `com.rtm.mq.tool.generator.java`

**功能**：
- 生成 Request 和 Response Java Bean 类
- 生成嵌套内部类
- 数据类型映射（Excel 类型 → Java 类型）
- 自动生成 getter/setter、toString、equals/hashCode
- 支持 JSR-303 验证注解
- 支持 Lombok 注解

**核心类**：
- `JavaBeanGenerator`: Java Bean 生成器
- `JavaTypeMapper`: 类型映射器
- `NestedClassGenerator`: 嵌套类生成器

#### 2.3 OpenAPI 生成器

**位置**: `com.rtm.mq.tool.generator.openapi`

**功能**：
- 生成 OpenAPI 3.0 YAML 规范
- 定义 Request/Response Schema
- 数据类型映射（Excel 类型 → OpenAPI 类型）
- 生成示例数据
- 支持嵌套对象和数组
- 生成字段描述和约束

**核心类**：
- `OpenApiGenerator`: OpenAPI 生成器
- `OpenApiTypeMapper`: 类型映射器
- `OpenApiSchemaBuilder`: Schema 构建器

### 3. 验证器 (Validator)

**位置**: `com.rtm.mq.tool.validator`

**功能**：
- XML Bean 语法验证（使用 XMLStreamReader）
- Java Bean 语法验证（使用 JavaParser）
- OpenAPI 规范验证（使用 Swagger Parser）
- 跨制品一致性验证（字段名、类型、顺序）
- 规格与实际报文一致性验证

**核心类**：
- `XmlBeanValidator`: XML 验证器
- `JavaBeanValidator`: Java 验证器
- `OpenApiValidator`: OpenAPI 验证器
- `CrossArtifactValidator`: 跨制品一致性验证器
- `MessageValidator`: 报文验证器

### 4. 配置管理 (Config)

**位置**: `com.rtm.mq.tool.config`

**功能**：
- 加载 YAML 配置文件
- 合并 CLI 参数与配置文件
- 设置默认值
- 配置验证

**核心类**：
- `Config`: 配置根类
- `ConfigLoader`: 配置加载器
- `OutputConfig`, `XmlConfig`, `JavaConfig`, `OpenApiConfig`: 各模块配置类

### 5. 数据模型 (Model)

**位置**: `com.rtm.mq.tool.model`

**功能**：
- 消息模型（MessageModel）
- 字段节点（FieldNode）：表示字段树节点
- 字段组（FieldGroup）：表示对象/数组分组
- 源数据元数据（SourceMetadata）：记录字段来源
- 验证结果（ValidationResult）

**核心类**：
- `MessageModel`: 消息模型根类
- `FieldNode`: 字段节点（支持树形结构）
- `FieldGroup`: 字段分组
- `Metadata`: 消息元数据
- `SourceMetadata`: 源数据追踪

### 6. 异常处理 (Exception)

**位置**: `com.rtm.mq.tool.exception`

**功能**：
- 统一异常体系
- 退出码定义
- 错误信息格式化

**核心类**：
- `MqToolException`: 根异常类
- `ParseException`: 解析异常
- `GenerationException`: 生成异常
- `ValidationException`: 验证异常
- `ConfigException`: 配置异常
- `OutputException`: 输出异常

---

## 输出制品

### 1. 中间 JSON 树 (Intermediate JSON Tree)

**路径**: `output/intermediate/message-tree.json`

**作用**: 作为所有生成器的统一输入，保证一致性

**结构示例**：

```json
{
  "metadata": {
    "systemName": "SAPI-APE",
    "version": "1.0.0",
    "sourceFile": "message-spec.xlsx",
    "sharedHeaderFile": "shared-header.xlsx",
    "generatedAt": "2026-01-06T10:00:00Z"
  },
  "request": {
    "rootObject": "RequestMessage",
    "fields": [
      {
        "name": "msgHeader",
        "type": "object",
        "occurrenceCount": "1..1",
        "segLevel": 1,
        "children": [
          {
            "name": "msgId",
            "type": "string",
            "length": 20,
            "occurrenceCount": "1..1",
            "segLevel": 2,
            "sourceMetadata": {
              "sheetName": "Request",
              "rowIndex": 10,
              "originalName": "msg_id"
            }
          }
        ]
      }
    ]
  },
  "response": {
    "rootObject": "ResponseMessage",
    "fields": [...]
  }
}
```

### 2. XML Bean 文件

#### Outbound (Request) Bean

**路径**: `output/xml/outbound-bean.xml`

**示例**：

```xml
<bean id="requestMsgFormat" class="com.company.mq.FixedLengthMessageFormat">
    <property name="fields">
        <list>
            <bean class="com.company.mq.Field">
                <property name="name" value="msgId"/>
                <property name="type" value="string"/>
                <property name="length" value="20"/>
                <property name="converter" ref="stringConverter"/>
            </bean>
            <!-- 更多字段 -->
        </list>
    </property>
</bean>
```

#### Inbound (Response) Bean

**路径**: `output/xml/inbound-bean.xml`

结构类似 Outbound Bean，用于 Response 消息定义。

### 3. Java Bean 文件

**路径**: `output/java/com/example/mq/model/`

**示例**：

```java
package com.example.mq.model;

import javax.validation.constraints.*;

/**
 * Request message bean
 * Generated from: message-spec.xlsx
 */
public class RequestBean {

    @NotNull
    @Size(max = 20)
    private String msgId;

    @NotNull
    private HeaderBean header;

    private List<ItemBean> items;

    // Getters and Setters
    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    // 嵌套类
    public static class HeaderBean {
        private String systemName;
        private String version;

        // Getters and Setters
    }

    public static class ItemBean {
        private String itemId;
        private String itemName;

        // Getters and Setters
    }
}
```

### 4. OpenAPI YAML 文件

**路径**: `output/openapi/api-spec.yaml`

**示例**：

```yaml
openapi: 3.0.0
info:
  title: MQ Message API
  version: 1.0.0
  description: Auto-generated from MQ message specification

paths:
  /api/v1/message:
    post:
      summary: Send MQ message
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RequestBean'
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ResponseBean'

components:
  schemas:
    RequestBean:
      type: object
      required:
        - msgId
        - header
      properties:
        msgId:
          type: string
          maxLength: 20
          description: Message ID
        header:
          $ref: '#/components/schemas/HeaderBean'
        items:
          type: array
          items:
            $ref: '#/components/schemas/ItemBean'

    HeaderBean:
      type: object
      properties:
        systemName:
          type: string
        version:
          type: string

    ItemBean:
      type: object
      properties:
        itemId:
          type: string
        itemName:
          type: string
```

### 5. 审计文件

#### 审计日志 (audit-log.json)

**路径**: `output/audit/audit-log.json`

记录完整的生成过程信息：

```json
{
  "timestamp": "2026-01-06T10:00:00Z",
  "toolVersion": "1.0.0-SNAPSHOT",
  "inputFiles": {
    "specFile": "/path/to/message-spec.xlsx",
    "sharedHeaderFile": "/path/to/shared-header.xlsx"
  },
  "environment": {
    "os": "Windows 10",
    "javaVersion": "11.0.12",
    "user": "developer"
  },
  "configuration": {
    "strictMode": true,
    "maxNestingDepth": 10
  },
  "executionTime": "2.5s",
  "outputFiles": [
    "xml/outbound-bean.xml",
    "xml/inbound-bean.xml",
    "java/com/example/mq/model/RequestBean.java",
    "openapi/api-spec.yaml"
  ],
  "warnings": [],
  "errors": []
}
```

#### 字段映射表 (field-mapping.csv)

**路径**: `output/audit/field-mapping.csv`

记录每个字段在各制品中的名称映射：

```csv
Sheet,Row,Excel Name,Seg Lvl,Java Name,XML Name,OpenAPI Name,Type,Length
Request,10,msg_id,1,msgId,msgId,msgId,string,20
Request,11,system_name,2,systemName,systemName,systemName,string,50
...
```

---

## 验证能力

### 1. 解析时验证

在解析 Excel 文件时进行的验证：

- 文件存在性和格式正确性
- 必需 Sheet 存在性（Request、Response）
- 必需列存在性（字段名、数据类型、Seg lvl、OccurrenceCount 等）
- 数据类型有效性（string、int、decimal 等）
- Seg lvl 层级正确性（不跳级、不越界）
- OccurrenceCount 格式正确性（1..1、0..N、1..N 等）
- 字段名称重复检测
- 嵌套深度限制

### 2. 生成后验证

生成制品后的验证：

- **XML 语法验证**: 使用 XMLStreamReader 验证生成的 XML 格式正确性
- **Java 语法验证**: 使用 JavaParser 验证生成的 Java 代码可编译性
- **OpenAPI 规范验证**: 使用 Swagger Parser 验证 YAML 符合 OpenAPI 规范

### 3. 跨制品一致性验证

确保所有制品的字段定义一致：

- 字段名称一致性（考虑命名规范差异）
- 字段类型一致性
- 字段顺序一致性
- 嵌套结构一致性
- 必填/可选属性一致性

### 4. 规格与报文验证

验证实际 MQ 报文是否符合 Excel 规范：

```bash
# 验证报文文件
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --validate-message /path/to/actual-message.txt

# 批量验证
java -jar mq-spec-tool.jar \
  --input spec.xlsx \
  --validate-message-dir /path/to/messages/
```

验证内容：
- 报文总长度
- 字段偏移量
- 字段长度
- 必填字段存在性
- 数据类型格式

---

## 项目结构

```
ai-mq/
├── mq-spec-tool/                    # 主项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/rtm/mq/tool/
│   │   │   │       ├── cli/         # CLI 入口（待实现）
│   │   │   │       ├── parser/      # Excel 解析模块
│   │   │   │       ├── generator/   # 代码生成模块
│   │   │   │       │   ├── xml/     # XML Bean 生成器
│   │   │   │       │   ├── java/    # Java Bean 生成器
│   │   │   │       │   └── openapi/ # OpenAPI 生成器
│   │   │   │       ├── validator/   # 验证模块
│   │   │   │       ├── config/      # 配置管理
│   │   │   │       ├── model/       # 数据模型
│   │   │   │       ├── exception/   # 异常处理
│   │   │   │       └── version/     # 版本管理
│   │   │   └── resources/
│   │   │       ├── templates/       # 模板文件
│   │   │       └── versions.properties
│   │   └── test/                    # 单元测试
│   └── pom.xml
├── spec/                            # 项目规范文档
│   ├── requirement/                 # 需求文档
│   │   └── 需求分析文档.md
│   ├── design/                      # 架构设计文档
│   │   └── architecture.md
│   └── plan/                        # 实施计划
│       └── task-pack/
├── input/                           # 输入文件示例
│   └── req-v0.md
├── CLAUDE.md                        # AI 辅助开发规则
└── README.md                        # 项目使用手册（本文件）
```

---

## 开发指南

### 技术栈

- **语言**: Java 11
- **构建工具**: Maven 3.6+
- **Excel 解析**: Apache POI 5.2.5
- **JSON 处理**: Gson 2.10.1
- **Java 代码生成**: JavaPoet 1.13.0
- **YAML 处理**: SnakeYAML 2.2
- **Java 代码验证**: JavaParser 3.25.8
- **OpenAPI 验证**: Swagger Parser 2.1.19
- **CLI 解析**: Apache Commons CLI 1.6.0
- **中文转拼音**: Pinyin4j 2.5.1
- **测试框架**: JUnit 5.10.1, Mockito 5.8.0

### 开发环境设置

1. 安装 JDK 11+
2. 安装 Maven 3.6+
3. 克隆项目并导入 IDE（推荐 IntelliJ IDEA）
4. 运行 `mvn clean install` 安装依赖

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ExcelParserTest

# 生成测试覆盖率报告
mvn jacoco:report
```

### 代码风格

- 使用 Google Java Style Guide
- 每个类、方法必须有 Javadoc 注释
- 单元测试覆盖率 > 80%

### 扩展指南

#### 添加新的数据类型映射

1. 修改 `XmlTypeMapper`、`JavaTypeMapper`、`OpenApiTypeMapper`
2. 添加相应的单元测试
3. 更新文档

#### 添加新的生成器

1. 实现 `Generator` 接口
2. 在 `Orchestrator` 中注册新生成器
3. 添加配置类和配置项
4. 添加单元测试和集成测试

---

## 常见问题

### Q1: 工具支持哪些 Excel 格式？

**A**: 仅支持 `.xlsx` 格式（Office 2007+），不支持旧的 `.xls` 格式。

### Q2: Excel 文件必须包含哪些 Sheet？

**A**: 必须包含 `Request` 和 `Response` Sheet。共享头部可以在单独的 Excel 文件中，或作为 `Shared Header` Sheet 嵌入主文件。

### Q3: 必需列有哪些？

**A**:
- `字段名` / `Field Name`
- `数据类型` / `Data Type`
- `长度` / `Length`
- `Seg lvl` / `Segment Level`
- `OccurrenceCount` / `Occurrence Count`

### Q4: 如何处理中文字段名？

**A**: 工具会自动将中文字段名转换为拼音，并转换为 camelCase 命名规范。例如：`账户名称` → `zhangHuMingCheng`

### Q5: 生成的文件编码是什么？

**A**: 所有生成的文件使用 UTF-8 编码。

### Q6: 如何确保生成的制品一致性？

**A**:
1. 所有生成器都基于同一个中间 JSON 树
2. 自动执行跨制品一致性验证
3. 审计日志记录完整的生成过程

### Q7: 工具是否支持增量更新？

**A**: 当前版本不支持增量更新，每次运行会重新生成所有制品。建议使用版本控制系统（Git）管理生成的文件。

### Q8: 如何处理字段顺序？

**A**: Excel 中字段的顺序会严格保持在所有生成的制品中（XML、Java、OpenAPI），顺序非常重要，因为 MQ 报文是固定长度格式。

### Q9: 生成失败如何回滚？

**A**: 如果启用了原子化输出（`output.atomic: true`），生成失败时会自动回滚，不会产生部分文件。

### Q10: 项目当前开发状态如何?

**A**: 项目正在开发中：
- ✅ 核心解析模块已完成
- ✅ XML、Java、OpenAPI 生成器已完成
- ✅ 验证器模块已完成
- ✅ 配置管理已完成
- 🚧 CLI 入口正在开发
- 🚧 端到端集成测试正在开发

---

## 相关文档

- [需求分析文档](spec/requirement/需求分析文档.md)
- [架构设计文档](spec/design/architecture.md)
- [实施计划](spec/plan/task-pack/v1/README.md)
- [AI 辅助开发规则](CLAUDE.md)

---

## 许可证

[待定]

---

## 联系方式

如有问题或建议，请联系项目维护团队。

---

**最后更新**: 2026-01-06
