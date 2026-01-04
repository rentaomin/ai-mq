# T-106 命名规范化

## Goal

实现字段名规范化器，将 Excel 中的字段名转换为 camelCase 格式，支持 CJK 字符转拼音，处理重复字段名检测。

## In Scope / Out of Scope

**In Scope**:
- CamelCaseConverter 类实现
- CJK 字符转拼音
- 长度限制和哈希后缀
- 重复字段名检测
- 重命名记录（用于 diff.md）

**Out of Scope**:
- diff.md 生成（T-210）
- Excel 解析

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5.6 节（命名规范化）
- T-105 对象/数组检测

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
├── CamelCaseConverter.java
└── DuplicateDetector.java

src/test/java/com/rtm/mq/tool/parser/
├── CamelCaseConverterTest.java
└── DuplicateDetectorTest.java
```

## Dependencies

- T-105 对象/数组检测
- T-005 错误处理框架
- pinyin4j 库

## Implementation Notes

### 转换规则

根据架构文档 5.6.1：

| 原始名称 | 转换后 | 规则 |
|---------|--------|------|
| DOMICILE_BRANCH | domicileBranch | 下划线分隔 → 驼峰 |
| response-code | responseCode | 连字符分隔 → 驼峰 |
| ResponseCode | responseCode | 首字母小写 |
| 123StartWithNumber | field123StartWithNumber | 数字开头添加前缀 |
| special!@#chars | specialChars | 移除非字母数字字符 |
| 客户姓名 | keHuXingMing | CJK → 拼音驼峰 |

### CamelCaseConverter 类

```java
package com.rtm.mq.tool.parser;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

import java.security.MessageDigest;

/**
 * camelCase 转换器
 * 支持 CJK 字符转拼音
 */
public class CamelCaseConverter {

    private static final int MAX_LENGTH = 50;  // 默认最大长度
    private static final int HASH_SUFFIX_LENGTH = 4;

    private final int maxLength;
    private final HanyuPinyinOutputFormat pinyinFormat;

    public CamelCaseConverter() {
        this(MAX_LENGTH);
    }

    public CamelCaseConverter(int maxLength) {
        this.maxLength = maxLength > 0 ? maxLength : MAX_LENGTH;
        this.pinyinFormat = new HanyuPinyinOutputFormat();
        this.pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        this.pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    /**
     * 转换为 camelCase
     *
     * @param input 原始名称
     * @return camelCase 名称
     */
    public String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Step 1: 处理 CJK 字符转拼音
        String processed = convertCJKToPinyin(input);

        // Step 2: 移除特殊字符，仅保留字母、数字、下划线、连字符
        String cleaned = processed.replaceAll("[^a-zA-Z0-9_\\-]", "");

        // Step 3: 按下划线或连字符分隔
        String[] parts = cleaned.split("[_\\-]");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            if (result.length() == 0) {
                // 首部分首字母小写
                result.append(toLowerFirst(part));
            } else {
                // 后续部分首字母大写
                result.append(toUpperFirst(part));
            }
        }

        String camelCase = result.toString();

        // Step 4: 处理数字开头
        if (!camelCase.isEmpty() && Character.isDigit(camelCase.charAt(0))) {
            camelCase = "field" + camelCase;
        }

        // Step 5: 处理空结果
        if (camelCase.isEmpty()) {
            camelCase = "field" + generateHash(input);
        }

        // Step 6: 长度限制处理
        if (camelCase.length() > maxLength) {
            String hash = generateHash(camelCase);
            camelCase = camelCase.substring(0, maxLength - HASH_SUFFIX_LENGTH) + hash;
        }

        return camelCase;
    }

    /**
     * 将 CJK 字符转换为拼音
     */
    private String convertCJKToPinyin(String input) {
        StringBuilder pinyin = new StringBuilder();
        boolean lastWasCJK = false;

        for (char ch : input.toCharArray()) {
            if (isCJKCharacter(ch)) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, pinyinFormat);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        String py = pinyinArray[0];
                        // 如果前一个也是 CJK，首字母大写形成驼峰
                        if (lastWasCJK && pinyin.length() > 0) {
                            pinyin.append(toUpperFirst(py));
                        } else {
                            pinyin.append(py);
                        }
                    } else {
                        pinyin.append(ch);
                    }
                } catch (Exception e) {
                    pinyin.append(ch);
                }
                lastWasCJK = true;
            } else {
                pinyin.append(ch);
                lastWasCJK = false;
            }
        }
        return pinyin.toString();
    }

    /**
     * 判断是否为 CJK 字符
     */
    private boolean isCJKCharacter(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
               block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
    }

    /**
     * 首字母小写
     */
    private String toLowerFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) +
               (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }

    /**
     * 首字母大写
     */
    private String toUpperFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) +
               (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }

    /**
     * 生成 4 位哈希后缀确保唯一性
     */
    private String generateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            return String.format("%02x%02x", hash[0] & 0xff, hash[1] & 0xff);
        } catch (Exception e) {
            return "0000";
        }
    }
}
```

### DuplicateDetector 类

```java
package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;

import java.util.*;

/**
 * 重复字段名检测器
 * 同一层级内字段名不得重复
 */
public class DuplicateDetector {

    /**
     * 检测重复字段名
     *
     * @param fields 字段列表
     * @throws ParseException 如果发现重复
     */
    public void detectDuplicates(List<FieldNode> fields) {
        detectDuplicatesInternal(fields, new HashSet<>());
    }

    private void detectDuplicatesInternal(List<FieldNode> fields, Set<String> parentContext) {
        Set<String> namesInScope = new HashSet<>();

        for (FieldNode field : fields) {
            // 跳过 transitory 字段（groupId, occurrenceCount）
            if (field.isTransitory()) {
                continue;
            }

            String name = field.getCamelCaseName();
            if (name == null) {
                continue;
            }

            if (namesInScope.contains(name)) {
                throw new ParseException("Duplicate field name '" + name + "'")
                    .withContext(field.getSource().getSheetName(), field.getSource().getRowIndex())
                    .withField(field.getOriginalName());
            }
            namesInScope.add(name);

            // 递归检查子节点（子节点有独立作用域）
            if (!field.getChildren().isEmpty()) {
                detectDuplicatesInternal(field.getChildren(), new HashSet<>());
            }
        }
    }

    /**
     * 获取重复字段列表（不抛异常版本）
     *
     * @param fields 字段列表
     * @return 重复字段名及其位置
     */
    public Map<String, List<FieldNode>> findDuplicates(List<FieldNode> fields) {
        Map<String, List<FieldNode>> nameToNodes = new LinkedHashMap<>();

        collectFieldNames(fields, nameToNodes);

        // 过滤只保留重复的
        Map<String, List<FieldNode>> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, List<FieldNode>> entry : nameToNodes.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicates;
    }

    private void collectFieldNames(List<FieldNode> fields, Map<String, List<FieldNode>> nameToNodes) {
        for (FieldNode field : fields) {
            if (field.isTransitory()) continue;

            String name = field.getCamelCaseName();
            if (name != null) {
                nameToNodes.computeIfAbsent(name, k -> new ArrayList<>()).add(field);
            }

            if (!field.getChildren().isEmpty()) {
                collectFieldNames(field.getChildren(), nameToNodes);
            }
        }
    }
}
```

## Acceptance Criteria

1. [ ] 下划线分隔正确转换为驼峰
2. [ ] 连字符分隔正确转换为驼峰
3. [ ] 首字母正确小写
4. [ ] CJK 字符正确转换为拼音
5. [ ] 数字开头添加 "field" 前缀
6. [ ] 特殊字符正确移除
7. [ ] 长度超限时添加哈希后缀
8. [ ] 重复字段名检测抛出 ParseException
9. [ ] 单元测试覆盖率 > 90%

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 下划线转换 | DOMICILE_BRANCH → domicileBranch |
| 单元测试 | 连字符转换 | response-code → responseCode |
| 单元测试 | CJK 转换 | 客户姓名 → keHuXingMing |
| 单元测试 | 数字开头 | 123abc → field123Abc |
| 单元测试 | 长度限制 | 超长名称 → 截断+哈希 |
| 单元测试 | 重复检测 | 抛出 ParseException |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 拼音库缺失 | 转换失败 | 回退保留原字符 |
| 哈希冲突 | 名称仍重复 | 极低概率，可增加哈希长度 |
| 空字符串 | 无效名称 | 生成哈希前缀 |
