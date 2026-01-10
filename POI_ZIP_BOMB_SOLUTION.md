# Zip Bomb 检测解决方案

## 问题描述

当解析大型或高度压缩的 Excel 文件时，POI 库会抛出以下错误：
```
Zip bomb detected: compressed size xxx, uncompressed size xxx
```

这是 POI 的安全防护机制，用来防止"压缩炸弹"攻击 - 即故意构造的压缩文件，解压后会占用大量内存。

## 根本原因

Apache POI 库有两个关键参数来控制 ZIP 文件的安全性：

1. **`ZipSecureFile.maxTextSize`** (默认: 100MB)
   - 允许的最大解压文本大小
   - 如果解压后文件超过此大小，会被拒绝

2. **`ZipSecureFile.minInflateRatio`** (默认: 0.01 = 1%)
   - 最小解压比率（压缩大小 / 解压大小）
   - 如果压缩比超过此值的倒数（即 1/0.01 = 100倍），文件会被拒绝
   - 这是防止压缩炸弹的主要防线

## 解决方案

我们已经在代码中集成了可配置的 POI 参数。

### 1. 默认配置（推荐用于大多数场景）

项目已配置如下默认值：
```
poiMaxTextSize: 500MB      (支持较大的未压缩文件)
poiMinInflateRatio: 0.001  (允许最高 1000 倍的压缩比)
```

这些默认值已在以下类中硬编码：
- `ParserConfig.java` - 配置类中的默认值
- `ExcelParser.java` - 在解析前配置 ZipSecureFile
- `SharedHeaderLoader.java` - 在加载 Shared Header 时配置

### 2. 通过配置文件自定义（可选）

在 `application.yml` 或配置文件中配置：

```yaml
parser:
  maxNestingDepth: 50
  poiMaxTextSize: 1073741824          # 1GB (1024*1024*1024)
  poiMinInflateRatio: 0.001           # 0.1%
```

### 3. 通过环境变量自定义（可选）

```bash
export MQ_SPEC_TOOL_PARSER_POI_MAX_TEXT_SIZE=1073741824
export MQ_SPEC_TOOL_PARSER_POI_MIN_INFLATE_RATIO=0.001
```

### 4. 通过命令行参数自定义（可选）

```bash
java -jar mq-spec-tool.jar \
  --generate \
  --input-file create_app.xlsx \
  --config custom-config.yml \
  -c "parser.poiMaxTextSize=1073741824" \
  -c "parser.poiMinInflateRatio=0.001"
```

## 参数调整指南

### 何时增加参数值

如果遇到"Zip bomb detected"错误：

1. **首先增加** `poiMinInflateRatio`（增加压缩比容限）
   ```
   0.001  →  0.0001  →  0.00001  （从 1% → 0.1% → 0.01%）
   ```

2. **其次增加** `poiMaxTextSize`（增加允许的最大文件大小）
   ```
   500MB  →  1GB  →  2GB  →  4GB
   ```

### 参数对应关系

| 压缩比倒数 | minInflateRatio | 说明 |
|---------|-----------------|------|
| 100倍   | 0.01 (1%)       | POI 默认（较严格） |
| 1000倍  | 0.001 (0.1%)    | 本项目默认值 |
| 10000倍 | 0.0001 (0.01%)  | 非常宽松 |

## 预期效果

实施此解决方案后：

✅ 能解析大型 Excel 文件（100MB+）
✅ 能处理高度压缩的 Excel 文件（如某些 FIX mapping 文件）
✅ 仍保持基本的安全防护
✅ 可根据需要通过配置灵活调整

## 技术细节

### 修改的文件

1. **`ParserConfig.java`**
   - 添加 `poiMaxTextSize` 字段
   - 添加 `poiMinInflateRatio` 字段
   - 添加相应的 getter/setter 和默认值设置

2. **`ExcelParser.java`**
   - 新增 `configureZipSecureFile()` 方法
   - 在 `parse()` 方法中调用配置方法
   - 导入 `ParserConfig` 和 `ZipSecureFile`

3. **`SharedHeaderLoader.java`**
   - 新增 `configureZipSecureFile(Config config)` 方法
   - 在 `loadFromFile()` 方法中调用配置方法
   - 导入 `ParserConfig` 和 `ZipSecureFile`

### 工作流程

```
用户执行解析
    ↓
ExcelParser.parse() 调用 configureZipSecureFile()
    ↓
从 ParserConfig 读取参数
    ↓
调用 ZipSecureFile.setMaxTextSize() 和 setMinInflateRatio()
    ↓
创建 Workbook（使用新的安全参数）
    ↓
解析 Excel 文件
```

## 故障排除

### 问题 1: 仍然出现 Zip bomb 错误

**解决方案：**
- 逐步降低 `minInflateRatio`（0.001 → 0.0001 → 0.00001）
- 增加 `maxTextSize`（特别是如果 Excel 包含图片或其他二进制数据）

### 问题 2: 解析变慢或内存占用增加

**解决方案：**
- 检查 Excel 文件本身是否有问题（如重复行、不必要的格式等）
- 考虑将大的 Excel 文件拆分成多个较小的文件
- 监控 JVM 内存配置

### 问题 3: 无法加载配置文件中的参数

**解决方案：**
- 确保配置文件路径正确
- 检查 YAML 格式是否有效
- 确保参数名称与代码中定义的一致（`poiMaxTextSize`, `poiMinInflateRatio`）

## 安全考虑

⚠️ **重要提示：**

这些参数调整会降低对恶意压缩文件的保护。只在以下情况下使用：

- ✅ 处理合法的、大型的企业 Excel 文件
- ✅ 在受信任的内部网络中运行
- ✅ 由可信源（如 FIX 规范文件）提供的 Excel 文件

❌ 不要：
- 处理来自不受信任源的 Excel 文件时使用宽松的配置
- 在暴露于公网的服务中使用过于宽松的参数
- 禁用所有的安全检查

## 相关资源

- [Apache POI 官方文档 - ZipSecureFile](https://poi.apache.org/apidocs/dev/org/apache/poi/util/ZipSecureFile.html)
- [Project Specification - Excel Parser](../spec/design/architecture.md)
- [ParserConfig 源代码](./mq-spec-tool/src/main/java/com/rtm/mq/tool/config/ParserConfig.java)
