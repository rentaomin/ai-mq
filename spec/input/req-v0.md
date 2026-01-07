背景说明：

公司内部系统按照业务领域与请求链路进行分层解耦，各层职责清晰，请求链路为：
EAPI → PAPI → SAPI → APE。

其中：
- SAPI 与 APE 之间通过 MQ 通信
- MQ 使用 IBM MQ
- 报文格式为 Fixed-Length（定长报文）

SAPI 与 APE 的 MQ 报文规范由 Message Spec Excel 文件定义，报文请求与响应必须严格遵循 Excel 中的：
- 字段顺序
- 字段名称
- 数据类型
- 长度
- 默认值与校验规则

当前现状与问题：

1. 为了与 APE 通信，SAPI 需要：
    - 根据 MQ Message Spec Excel 人工编写 request / response XML bean 文件
    - XML 中字段顺序、层级、名称、默认值必须与 Excel 完全一致
    - XML bean 会在项目启动时被公司内部固定组件加载并转换为 Spring Bean
    - 该组件在 MQ 发送前，根据 XML 定义对 Java Bean 进行序列化和校验
    - 若字段顺序、类型或定义不一致，将直接抛出运行时错误

2. 同时需要：
    - 为 request / response XML 分别编写对应的 Java Bean 类
    - Java Bean 字段名称、数据类型必须与 XML 定义保持一致
    - 请求对象类名必须包含 “Request”，响应对象类名必须包含 “Response”
    - 内部嵌套对象的命名需严格遵循 XML 中定义的对象名称

3. 在 MQ 返回响应后：
    - 组件会根据 response XML 将 MQ 报文反序列化为 Java Response Bean
    - 若字段无法映射或类型不一致，则抛出异常

4. 在 SAPI 层，还需要：
    - 编写符合 OpenAPI Specification 3.x 的 API YAML 文件
    - YAML 定义 API 名称、HTTP Method、Endpoint、Headers、Request Payload、Response、Error 等
    - YAML 按目录拆分管理（如 header / request / response / error 分开定义并引用），避免单文件过大和混乱
    - 通过 Maven 编译自动生成 Controller 接口代码
    - 业务 Controller 实现调用 Kotlin 编写的业务逻辑

5. Kotlin 侧业务逻辑：
    - 采用模板化结构
    - 核心类如 postflow，用于构建 MQ 请求 flow 对象并发起调用
    - 模板差异主要体现在：
        - 请求参数
        - 请求 header
        - 响应字段
        - 响应校验规则
    - 上述内容均需与 Java Bean / XML Bean 中字段顺序和定义一一映射

6. 当前整个流程高度模板化，但完全依赖人工：
    - Excel 通常包含 400～500 行甚至更多字段
    - 人工处理极易出错
    - 错误往往在运行期或联调期才暴露，成本极高
    - 缺乏统一的审计、溯源和一致性校验能力

目标：

开发一个工具或项目，实现基于 MQ Message Spec Excel 的自动化生成与校验能力，
将当前高度人工、易出错的流程转变为规范化、可审计、可复现的自动化流程。

---

核心需求描述：

在给定 MQ Message Spec Excel 文件及相关示例文件的前提下，系统需要支持以下能力：

1. Excel 解析与 XML Bean 生成
- 解析 Message Spec Excel 内容
- 自动生成符合模板的 request / response XML bean 文件
- XML 模板应支持可替换
- XML 中字段顺序、层级、名称、类型、默认值、校验规则必须来源于 Excel
- groupId 与 occurrenceCount 字段：
    - 仅在 XML bean 中保留
    - 不得出现在 Java Bean 和 OpenAPI YAML 中
    - XML bean 将被外部组件用于序列化与反序列化

2. Java Bean 自动生成
- 根据 Excel 解析结果生成 MQ 请求与响应的 Java Bean 类
- 类名规则：
    - 请求类名包含 “Request”
    - 响应类名包含 “Response”
- 字段规则：
    - 字段名称需转换为 camelCase
    - Excel 中可能存在不规范字段名（特殊字符、非驼峰、描述性文本）
    - 需定义明确、可追溯的字段重命名规则
- 数据类型必须与 XML Bean 中定义一致
- groupId 与 occurrenceCount 不得出现在 Java Bean 中

3. OpenAPI Specification (OAS 3.x) YAML 生成
- 自动生成符合 OAS 3.x 的 API YAML 定义
- API 名称、HTTP Method、Endpoint：
    - 可根据 Excel / shared header 自动推导
    - 或支持用户显式提供
- YAML 拆分管理：
    - headers / request / response / error 分目录定义并引用
- YAML 中字段定义必须与 Java Bean / XML Bean 保持一致（名称、类型、必填、默认值）
- groupId 与 occurrenceCount 不得出现在 YAML 中

4. 一致性与校验要求（强约束）
- 确保以下产物字段定义完全一致：
    - XML Bean
    - Java Bean
    - OpenAPI YAML
- 包括但不限于：
    - 字段名称（重命名后的）
    - 数据类型
    - 是否必填
    - 默认值
    - 顺序（对 XML / MQ 报文）

5. Shared Header 支持
- Excel 中包含 shared header / request / response 多个 sheet
- shared header 的字段定义来自独立的 Excel 文件
- 系统需支持加载并合并 shared header 定义

6. 字段差异输出（diff.md）
- 对 Excel 原始字段名与生成后的 Java/XML/YAML 字段名进行对比
- 生成 diff.md 文件
- 以表格形式输出：
    - 原始字段名称
    - 重命名后的字段名称
    - 适用范围（request / response / header）

7. 中间结构输出（Intermediate JSON Tree）
- 将 Excel 内容转换为嵌套层级正确的 JSON 树结构
- 明确表示：
    - 对象 / 数组关系（基于 occurrenceCount）
    - 字段顺序
    - 元数据（sheet、行号、原始字段名、长度、datatype、默认值等）
- 将该 JSON 结果输出为独立文件保存
- 该中间结构将作为后续生成 XML / Java / YAML 的统一输入
- 该文件用于 AI 模型与自动化流程复用

8. 审计与溯源
- 整个生成过程需支持审计与溯源
- 能够明确：
    - 输入 Excel 版本
    - 使用的模板版本
    - 生成时间
    - 生成规则版本
- 支持问题回溯与结果复现

9. 报文一致性验证
- 提供能力对比：
    - 实际 MQ 测试报文内容
    - Excel Message Spec 定义
- 确保每个字段：
    - 长度
    - 顺序
    - 值
    - 默认填充规则
      均与 Excel 保持一致
- 对不一致项提供明确、可定位的错误报告

---

Excel 规范补充说明：

1. Seg lvl：
- 表示对象层级关系

2. Description 列：
- 对于 groupId：表示字段默认值，用于 XML bean
- 对于 occurrenceCount：表示父对象最小/最大出现次数

3. 复杂对象与数组识别规则：
- Field Name 形如 a:A 且 Length、Message Datatype 为空 → 表示对象定义
- occurrenceCount 为 0..N 或 1..N → 数组对象
- occurrenceCount 为 1..1 → 复合对象
- A 为对象类名称

4. Request 报文特殊规则：
- “Hard code Value for MNL” 列：
    - 基本类型：固定值
    - 枚举类型（如 a=1,b=2）：默认使用 1
    - “BLANK”：按字段长度填充空字符串
    - “Refer Column S for Value Listing”：根据指定列规则取值，若无则按业务规则处理

5. Shared Header 额外规则：
- “Operation Name” 后单元格值：
    - 作为 API 功能语义
    - 可用于生成默认 API 名称
- “Operation ID” 后单元格值：
    - 可作为 Java Bean 顶层对象名称（如 CreateAppSmpRequest / Response）

---

示例文件（用于补充与约束需求）：

- Message Spec 示例 Excel：
  markdown/task/sample/create_app.xlsx

- Shared Header 示例 Excel：
  markdown/task/sample/ISM v2.0 FIX mapping.xlsx

- Request XML Bean 示例：
  markdown/task/sample/outbound-converter.xml

- Response XML Bean 示例：
  markdown/task/sample/inbound-converter.xml

- 模板与处理代码示例：
  markdown/task/sample/sample_code.md
