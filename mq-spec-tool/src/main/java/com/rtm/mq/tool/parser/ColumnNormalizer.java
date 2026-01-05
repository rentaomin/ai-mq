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
