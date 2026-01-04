# T-101 列名规范化器

## Goal

实现 Excel 列名规范化器，处理列名中的换行符、多余空格等特殊字符，使解析器能够正确匹配列名。

## In Scope / Out of Scope

**In Scope**:
- 列名规范化逻辑
- 换行符替换
- 空格合并
- 前后空白去除

**Out of Scope**:
- Excel 文件读取（T-102）
- 列验证逻辑（T-102）

## Inputs

- 架构文档: `spec/design/architecture.md` 第 5.1.2 节（列名规范化规则）
- T-004 数据模型定义

## Outputs

```
src/main/java/com/rtm/mq/tool/parser/
└── ColumnNormalizer.java

src/test/java/com/rtm/mq/tool/parser/
└── ColumnNormalizerTest.java
```

## Dependencies

- T-004 数据模型定义

## Implementation Notes

### 规范化规则

基于架构文档，Excel 列名可能包含的特殊字符：

| 原始列名 | 规范化后 | 规则 |
|---------|---------|------|
| `"Seg\nlvl"` | `"Seg lvl"` | 换行符替换为空格 |
| `"Messaging\nDatatype"` | `"Messaging Datatype"` | 同上 |
| `"Null\n(Y/N)"` | `"Null (Y/N)"` | 同上 |
| `"NLS\n(Y/N)"` | `"NLS (Y/N)"` | 同上 |
| `"  Field Name  "` | `"Field Name"` | 前后空白去除 |
| `"Opt  (O/M)"` | `"Opt (O/M)"` | 多空格合并 |

### ColumnNormalizer 类

```java
package com.rtm.mq.tool.parser;

/**
 * Excel 列名规范化器
 * 处理列名中的换行符、多余空格等特殊字符
 */
public final class ColumnNormalizer {

    private ColumnNormalizer() {}

    /**
     * 规范化列名
     * 1. 替换换行符为空格
     * 2. 替换回车符为空格
     * 3. 去除前后空白
     * 4. 合并多个连续空格为单个空格
     *
     * @param columnName 原始列名
     * @return 规范化后的列名
     */
    public static String normalize(String columnName) {
        if (columnName == null) {
            return null;
        }

        return columnName
            .replace("\n", " ")    // 替换换行符
            .replace("\r", " ")    // 替换回车符
            .trim()                // 去除前后空白
            .replaceAll("\\s+", " ");  // 合并多空格
    }

    /**
     * 判断两个列名是否等价（规范化后相等）
     *
     * @param name1 列名1
     * @param name2 列名2
     * @return 是否等价
     */
    public static boolean equals(String name1, String name2) {
        String normalized1 = normalize(name1);
        String normalized2 = normalize(name2);

        if (normalized1 == null && normalized2 == null) {
            return true;
        }
        if (normalized1 == null || normalized2 == null) {
            return false;
        }

        return normalized1.equals(normalized2);
    }

    /**
     * 判断两个列名是否等价（忽略大小写）
     *
     * @param name1 列名1
     * @param name2 列名2
     * @return 是否等价
     */
    public static boolean equalsIgnoreCase(String name1, String name2) {
        String normalized1 = normalize(name1);
        String normalized2 = normalize(name2);

        if (normalized1 == null && normalized2 == null) {
            return true;
        }
        if (normalized1 == null || normalized2 == null) {
            return false;
        }

        return normalized1.equalsIgnoreCase(normalized2);
    }
}
```

### 测试用例

```java
package com.rtm.mq.tool.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColumnNormalizerTest {

    @Test
    void normalize_withNewline_replacesWithSpace() {
        assertEquals("Seg lvl", ColumnNormalizer.normalize("Seg\nlvl"));
        assertEquals("Messaging Datatype", ColumnNormalizer.normalize("Messaging\nDatatype"));
    }

    @Test
    void normalize_withCarriageReturn_replacesWithSpace() {
        assertEquals("Seg lvl", ColumnNormalizer.normalize("Seg\rlvl"));
        assertEquals("Seg lvl", ColumnNormalizer.normalize("Seg\r\nlvl"));
    }

    @Test
    void normalize_withLeadingTrailingSpaces_trims() {
        assertEquals("Field Name", ColumnNormalizer.normalize("  Field Name  "));
        assertEquals("Description", ColumnNormalizer.normalize("\tDescription\t"));
    }

    @Test
    void normalize_withMultipleSpaces_merges() {
        assertEquals("Opt (O/M)", ColumnNormalizer.normalize("Opt   (O/M)"));
        assertEquals("Null (Y/N)", ColumnNormalizer.normalize("Null\n\n(Y/N)"));
    }

    @Test
    void normalize_withNull_returnsNull() {
        assertNull(ColumnNormalizer.normalize(null));
    }

    @Test
    void normalize_withEmpty_returnsEmpty() {
        assertEquals("", ColumnNormalizer.normalize(""));
        assertEquals("", ColumnNormalizer.normalize("   "));
    }

    @Test
    void equals_normalizedNames_returnsTrue() {
        assertTrue(ColumnNormalizer.equals("Seg\nlvl", "Seg lvl"));
        assertTrue(ColumnNormalizer.equals("  Field Name  ", "Field Name"));
    }

    @Test
    void equalsIgnoreCase_differentCase_returnsTrue() {
        assertTrue(ColumnNormalizer.equalsIgnoreCase("SEG\nLVL", "seg lvl"));
    }
}
```

## Acceptance Criteria

1. [ ] 换行符 `\n` 正确替换为空格
2. [ ] 回车符 `\r` 正确替换为空格
3. [ ] 前后空白正确去除
4. [ ] 多空格正确合并
5. [ ] null 输入返回 null
6. [ ] 空字符串输入返回空字符串
7. [ ] 单元测试覆盖率 100%

## Tests

| 测试类型 | 测试内容 | 预期结果 |
|---------|---------|---------|
| 单元测试 | 换行符替换 | 正确替换 |
| 单元测试 | 空格处理 | 正确合并/去除 |
| 单元测试 | null 处理 | 返回 null |
| 单元测试 | 边界条件 | 正确处理 |

## Risks / Edge Cases

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Unicode 空白字符 | 规范化不完整 | 使用 `\s` 匹配所有空白 |
| 特殊控制字符 | 解析错误 | 考虑移除控制字符 |
| 性能问题 | 大量列处理慢 | 正则预编译 |
