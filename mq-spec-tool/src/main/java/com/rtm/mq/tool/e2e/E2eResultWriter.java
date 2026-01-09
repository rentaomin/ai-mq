package com.rtm.mq.tool.e2e;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Writes E2E verification results to JSON format as specified in T-310.
 *
 * <p>Output rules (strict):</p>
 * <ul>
 *   <li>Exactly one output artifact: e2e-verification-result.json</li>
 *   <li>Machine-readable only</li>
 *   <li>Deterministic ordering</li>
 *   <li>No narrative text</li>
 * </ul>
 *
 * <p>No Markdown reports or human-readable summaries are allowed.</p>
 */
public final class E2eResultWriter {

    /** The output filename as specified in T-310. */
    public static final String OUTPUT_FILENAME = "e2e-verification-result.json";

    /**
     * Writes the E2E verification result to a file.
     *
     * @param result the verification result to write
     * @param outputDirectory the directory to write to
     * @throws IOException if an I/O error occurs
     */
    public void write(E2eVerificationResult result, Path outputDirectory) throws IOException {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory must not be null");
        }

        Files.createDirectories(outputDirectory);
        Path outputPath = outputDirectory.resolve(OUTPUT_FILENAME);

        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writeJson(result.toMap(), writer, 0);
        }
    }

    /**
     * Writes the E2E verification result to a writer.
     *
     * @param result the verification result to write
     * @param writer the writer to write to
     * @throws IOException if an I/O error occurs
     */
    public void write(E2eVerificationResult result, Writer writer) throws IOException {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer must not be null");
        }

        writeJson(result.toMap(), writer, 0);
    }

    /**
     * Writes a map as JSON with deterministic ordering.
     *
     * @param map the map to write
     * @param writer the writer
     * @param indent the current indentation level
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    private void writeJson(Map<String, Object> map, Writer writer, int indent) throws IOException {
        writer.write("{\n");
        String indentStr = "  ".repeat(indent + 1);
        String closeIndent = "  ".repeat(indent);

        int i = 0;
        int size = map.size();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            writer.write(indentStr);
            writer.write("\"");
            writer.write(escapeJson(entry.getKey()));
            writer.write("\": ");
            writeValue(entry.getValue(), writer, indent + 1);
            if (i < size - 1) {
                writer.write(",");
            }
            writer.write("\n");
            i++;
        }

        writer.write(closeIndent);
        writer.write("}");
    }

    /**
     * Writes a JSON value.
     *
     * @param value the value to write
     * @param writer the writer
     * @param indent the current indentation level
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    private void writeValue(Object value, Writer writer, int indent) throws IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof Number) {
            writer.write(value.toString());
        } else if (value instanceof String) {
            writer.write("\"");
            writer.write(escapeJson((String) value));
            writer.write("\"");
        } else if (value instanceof List) {
            writeList((List<?>) value, writer, indent);
        } else if (value instanceof Map) {
            writeJson((Map<String, Object>) value, writer, indent);
        } else {
            writer.write("\"");
            writer.write(escapeJson(value.toString()));
            writer.write("\"");
        }
    }

    /**
     * Writes a list as JSON array.
     *
     * @param list the list to write
     * @param writer the writer
     * @param indent the current indentation level
     * @throws IOException if an I/O error occurs
     */
    private void writeList(List<?> list, Writer writer, int indent) throws IOException {
        if (list.isEmpty()) {
            writer.write("[]");
            return;
        }

        writer.write("[\n");
        String indentStr = "  ".repeat(indent + 1);
        String closeIndent = "  ".repeat(indent);

        int size = list.size();
        for (int i = 0; i < size; i++) {
            writer.write(indentStr);
            writeValue(list.get(i), writer, indent + 1);
            if (i < size - 1) {
                writer.write(",");
            }
            writer.write("\n");
        }

        writer.write(closeIndent);
        writer.write("]");
    }

    /**
     * Escapes a string for JSON.
     *
     * @param s the string to escape
     * @return the escaped string
     */
    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
