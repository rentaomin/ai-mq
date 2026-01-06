package com.rtm.mq.tool.generator;

import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.MessageModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * diff.md generator for field name mapping table.
 *
 * <p>Generates a Markdown file documenting the mapping from original field names
 * to camelCase names, organized by sheet (Shared Header, Request, Response).
 * This supports audit traceability and human review.</p>
 *
 * <p>Output format follows the specification in architecture.md section 6.4.4.</p>
 *
 * @see MessageModel
 * @see FieldNode
 */
public class DiffMdGenerator {

    private static final String LINE_SEP = System.lineSeparator();

    /**
     * Generates diff.md file to the specified output path.
     *
     * @param model the message model containing complete JSON Tree
     * @param outputPath the output file path for diff.md
     * @throws IOException if file write fails
     */
    public void generate(MessageModel model, Path outputPath) throws IOException {
        String content = generateContent(model);

        // Ensure parent directory exists
        Files.createDirectories(outputPath.getParent());

        // Write with UTF-8 encoding
        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
    }

    /**
     * Generates the Markdown content for the field mapping table.
     *
     * @param model the message model
     * @return the complete Markdown content
     */
    public String generateContent(MessageModel model) {
        StringBuilder md = new StringBuilder();
        int totalCount = 0;

        // Title and metadata
        md.append("# 字段名称映射表").append(LINE_SEP).append(LINE_SEP);
        md.append("生成时间: ").append(model.getMetadata().getParseTimestamp()).append(LINE_SEP);
        md.append("源文件: ").append(model.getMetadata().getSourceFile()).append(LINE_SEP);
        md.append("解析器版本: ").append(model.getMetadata().getParserVersion()).append(LINE_SEP);
        md.append(LINE_SEP);
        md.append("---").append(LINE_SEP).append(LINE_SEP);

        // Shared Header section
        if (model.getSharedHeader() != null &&
            !model.getSharedHeader().getFields().isEmpty()) {
            md.append("## Shared Header").append(LINE_SEP).append(LINE_SEP);
            int count = appendFieldTable(md, model.getSharedHeader().getFields());
            totalCount += count;
            md.append(LINE_SEP).append("---").append(LINE_SEP).append(LINE_SEP);
        }

        // Request section
        if (model.getRequest() != null &&
            !model.getRequest().getFields().isEmpty()) {
            md.append("## Request").append(LINE_SEP).append(LINE_SEP);
            int count = appendFieldTable(md, model.getRequest().getFields());
            totalCount += count;
            md.append(LINE_SEP).append("---").append(LINE_SEP).append(LINE_SEP);
        }

        // Response section
        if (model.getResponse() != null &&
            !model.getResponse().getFields().isEmpty()) {
            md.append("## Response").append(LINE_SEP).append(LINE_SEP);
            int count = appendFieldTable(md, model.getResponse().getFields());
            totalCount += count;
            md.append(LINE_SEP).append("---").append(LINE_SEP).append(LINE_SEP);
        }

        // Summary
        md.append("共计 ").append(totalCount).append(" 个字段映射").append(LINE_SEP);

        return md.toString();
    }

    /**
     * Appends the field mapping table for a list of fields.
     * Recursively processes nested fields.
     *
     * @param md the StringBuilder to append to
     * @param fields the list of fields to process
     * @return the count of fields processed (including nested)
     */
    private int appendFieldTable(StringBuilder md, List<FieldNode> fields) {
        // Table header
        md.append("| 原始名称 | camelCase 名称 | 来源 Sheet | 行号 |").append(LINE_SEP);
        md.append("|---------|---------------|-----------|------|").append(LINE_SEP);

        // Recursively add all fields
        return appendFieldRows(md, fields);
    }

    /**
     * Recursively appends field rows to the table.
     *
     * @param md the StringBuilder to append to
     * @param fields the list of fields to process
     * @return the count of fields processed
     */
    private int appendFieldRows(StringBuilder md, List<FieldNode> fields) {
        int count = 0;
        for (FieldNode field : fields) {
            // Add current field
            appendFieldRow(md, field);
            count++;

            // Recursively process child fields
            if (!field.getChildren().isEmpty()) {
                count += appendFieldRows(md, field.getChildren());
            }
        }
        return count;
    }

    /**
     * Appends a single field row to the table.
     *
     * @param md the StringBuilder to append to
     * @param field the field to add
     */
    private void appendFieldRow(StringBuilder md, FieldNode field) {
        String originalName = escapeMarkdown(field.getOriginalName());
        String camelCaseName = field.getCamelCaseName() != null ?
            field.getCamelCaseName() : "-";
        String sheetName = field.getSource() != null ?
            field.getSource().getSheetName() : "-";
        int rowIndex = field.getSource() != null ?
            field.getSource().getRowIndex() : 0;

        md.append("| ")
          .append(originalName)
          .append(" | ")
          .append(camelCaseName)
          .append(" | ")
          .append(sheetName)
          .append(" | ")
          .append(rowIndex)
          .append(" |")
          .append(LINE_SEP);
    }

    /**
     * Escapes Markdown special characters in text.
     *
     * @param text the text to escape
     * @return the escaped text, or "-" if null
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "-";
        }
        // Escape backslash and pipe characters
        return text.replace("\\", "\\\\")
                   .replace("|", "\\|");
    }

    /**
     * Counts the total number of fields in the message model (including nested).
     *
     * @param model the message model
     * @return the total field count
     */
    public int countTotalFields(MessageModel model) {
        int count = 0;
        if (model.getSharedHeader() != null) {
            count += countFields(model.getSharedHeader().getFields());
        }
        if (model.getRequest() != null) {
            count += countFields(model.getRequest().getFields());
        }
        if (model.getResponse() != null) {
            count += countFields(model.getResponse().getFields());
        }
        return count;
    }

    /**
     * Recursively counts fields in a field list.
     *
     * @param fields the list of fields
     * @return the count of fields (including nested)
     */
    private int countFields(List<FieldNode> fields) {
        int count = 0;
        for (FieldNode field : fields) {
            count++;
            count += countFields(field.getChildren());
        }
        return count;
    }
}
