# T-108 JSON Tree 序列化

## Goal

实现确定性 JSON 序列化器，将 MessageModel 序列化为中间 JSON Tree 文件（spec-tree.json），保证字段顺序和格式稳定。

## In Scope / Out of Scope

**In Scope**:
- DeterministicJsonWriter 类实现
- JSON Schema 定义
- 顺序保证机制
- UTF-8 编码
- 格式化输出

**Out of Scope**:
- JSON 反序列化
- 生成器逻辑

## Inputs

- 架构文档: `spec/design/architecture.md` 第 4.1 节（中间 JSON 树）
- T-107 Excel 解析器集成

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
└── DeterministicJsonWriter.java

src/main/resources/
└── schema/spec-tree-schema.json

生成的制品:
{output-root}/parser/schema/spec-tree.json
```

## Dependencies

- T-107 Excel 解析器集成
- T-004 数据模型定义

## Implementation Notes

### JSON 结构定义

根据架构文档 4.1.2：

```json
{
  "metadata": {
    "sourceFile": "path/to/spec.xlsx",
    "sharedHeaderFile": "path/to/shared.xlsx",
    "parseTimestamp": "2026-01-04T10:00:00Z",
    "parserVersion": "1.0.0",
    "operationName": "Create application from SMP",
    "operationId": "CreateAppSMP",
    "version": "01.00"
  },
  "sharedHeader": {
    "fields": []
  },
  "request": {
    "fields": [
      {
        "originalName": "CreateApp:CreateApplication",
        "camelCaseName": "createApp",
        "className": "CreateApplication",
        "segLevel": 1,
        "length": null,
        "dataType": null,
        "isObject": true,
        "isArray": false,
        "children": [...],
        "_source": {
          "sheetName": "Request",
          "rowIndex": 9
        }
      }
    ]
  },
  "response": {
    "fields": []
  }
}
```

### DeterministicJsonWriter 类

```java
package com.rtm.mq.tool.parser;

import com.google.gson.*;
import com.rtm.mq.tool.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 确定性 JSON 写入器
 * 保证相同输入产生字节级别相同的输出
 */
public class DeterministicJsonWriter {

    private final Gson gson;

    public DeterministicJsonWriter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .registerTypeAdapter(FieldNode.class, new FieldNodeSerializer())
            .create();
    }

    /**
     * 将 MessageModel 写入 JSON 文件
     *
     * @param model 消息模型
     * @param outputPath 输出路径
     */
    public void write(MessageModel model, Path outputPath) throws IOException {
        String json = serialize(model);

        // 确保父目录存在
        Files.createDirectories(outputPath.getParent());

        // 使用 UTF-8 编码写入
        Files.writeString(outputPath, json, StandardCharsets.UTF_8);
    }

    /**
     * 序列化为 JSON 字符串
     */
    public String serialize(MessageModel model) {
        return gson.toJson(model);
    }

    /**
     * 自定义 FieldNode 序列化器
     * 确保字段顺序固定
     */
    private static class FieldNodeSerializer implements JsonSerializer<FieldNode> {
        @Override
        public JsonElement serialize(FieldNode node, java.lang.reflect.Type typeOfSrc,
                                    JsonSerializationContext context) {
            JsonObject obj = new JsonObject();

            // 按固定顺序添加字段
            obj.addProperty("originalName", node.getOriginalName());
            obj.addProperty("camelCaseName", node.getCamelCaseName());
            obj.addProperty("className", node.getClassName());
            obj.addProperty("segLevel", node.getSegLevel());
            obj.add("length", node.getLength() != null ?
                new JsonPrimitive(node.getLength()) : JsonNull.INSTANCE);
            obj.addProperty("dataType", node.getDataType());
            obj.addProperty("optionality", node.getOptionality());
            obj.addProperty("defaultValue", node.getDefaultValue());
            obj.addProperty("hardCodeValue", node.getHardCodeValue());
            obj.addProperty("groupId", node.getGroupId());
            obj.addProperty("occurrenceCount", node.getOccurrenceCount());
            obj.addProperty("isArray", node.isArray());
            obj.addProperty("isObject", node.isObject());
            obj.addProperty("isTransitory", node.isTransitory());

            // children 数组
            JsonArray children = new JsonArray();
            for (FieldNode child : node.getChildren()) {
                children.add(serialize(child, typeOfSrc, context));
            }
            obj.add("children", children);

            // _source 对象
            JsonObject source = new JsonObject();
            source.addProperty("sheetName", node.getSource().getSheetName());
            source.addProperty("rowIndex", node.getSource().getRowIndex());
            obj.add("_source", source);

            return obj;
        }
    }

    /**
     * 获取 JSON Schema 路径
     */
    public static String getSchemaPath() {
        return "/schema/spec-tree-schema.json";
    }
}
```

### JSON Schema 定义

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MQ Spec Intermediate JSON Tree",
  "type": "object",
  "required": ["metadata", "request", "response"],
  "properties": {
    "metadata": {
      "type": "object",
      "required": ["sourceFile", "parseTimestamp", "parserVersion", "operationId"],
      "properties": {
        "sourceFile": {"type": "string"},
        "sharedHeaderFile": {"type": ["string", "null"]},
        "parseTimestamp": {"type": "string", "format": "date-time"},
        "parserVersion": {"type": "string"},
        "operationName": {"type": ["string", "null"]},
        "operationId": {"type": "string"},
        "version": {"type": ["string", "null"]}
      }
    },
    "sharedHeader": {"$ref": "#/definitions/fieldGroup"},
    "request": {"$ref": "#/definitions/fieldGroup"},
    "response": {"$ref": "#/definitions/fieldGroup"}
  },
  "definitions": {
    "fieldGroup": {
      "type": "object",
      "required": ["fields"],
      "properties": {
        "fields": {
          "type": "array",
          "items": {"$ref": "#/definitions/fieldNode"}
        }
      }
    },
    "fieldNode": {
      "type": "object",
      "required": ["originalName", "segLevel", "_source"],
      "properties": {
        "originalName": {"type": "string"},
        "camelCaseName": {"type": ["string", "null"]},
        "className": {"type": ["string", "null"]},
        "segLevel": {"type": "integer", "minimum": 1},
        "length": {"type": ["integer", "null"]},
        "dataType": {"type": ["string", "null"]},
        "optionality": {"type": ["string", "null"], "enum": ["M", "O", null]},
        "defaultValue": {"type": ["string", "null"]},
        "hardCodeValue": {"type": ["string", "null"]},
        "groupId": {"type": ["string", "null"]},
        "occurrenceCount": {"type": ["string", "null"]},
        "isArray": {"type": "boolean"},
        "isObject": {"type": "boolean"},
        "isTransitory": {"type": "boolean"},
        "children": {
          "type": "array",
          "items": {"$ref": "#/definitions/fieldNode"}
        },
        "_source": {
          "type": "object",
          "required": ["sheetName", "rowIndex"],
          "properties": {
            "sheetName": {"type": "string"},
            "rowIndex": {"type": "integer", "minimum": 1}
          }
        }
      }
    }
  }
}
```

### 确定性保证措施

1. **字段顺序固定**: 使用自定义序列化器按固定顺序输出字段
2. **Unicode 转义禁用**: `disableHtmlEscaping()` 避免不必要的转义
3. **Null 值包含**: `serializeNulls()` 保证 null 字段也输出
4. **格式固定**: `setPrettyPrinting()` 使用 2 空格缩进
5. **编码固定**: UTF-8 编码

## Acceptance Criteria

1. [ ] JSON 格式正确
2. [ ] 字段顺序与 Excel 一致
3. [ ] 确定性：相同输入产生字节级别相同输出
4. [ ] UTF-8 编码
5. [ ] 父目录自动创建
6. [ ] JSON Schema 验证通过
