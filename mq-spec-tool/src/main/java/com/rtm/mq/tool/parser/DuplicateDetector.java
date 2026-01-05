package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects duplicate field names within the same scope.
 *
 * <p>In a message specification tree, field names must be unique within each
 * scope (sibling level). However, fields in different parent nodes (different
 * scopes) may have the same name.</p>
 *
 * <p>Transitory fields (such as groupId and occurrenceCount metadata) are
 * excluded from duplicate detection since they do not appear in the final
 * output.</p>
 *
 * <p>Example valid structure:</p>
 * <pre>
 * root
 *   customer          (unique in root scope)
 *     name            (unique in customer scope)
 *     address         (unique in customer scope)
 *   order
 *     name            (OK - different scope from customer.name)
 * </pre>
 *
 * <p>Example invalid structure:</p>
 * <pre>
 * root
 *   customer
 *   customer          (ERROR - duplicate in root scope)
 * </pre>
 *
 * @see CamelCaseConverter
 */
public class DuplicateDetector {

    /**
     * Detects duplicate field names and throws an exception if found.
     *
     * <p>This method recursively checks all levels of the field tree.
     * Each level has its own independent scope for duplicate checking.</p>
     *
     * @param fields the list of field nodes to check
     * @throws ParseException if duplicate field names are found in the same scope
     */
    public void detectDuplicates(List<FieldNode> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        detectDuplicatesInternal(fields);
    }

    /**
     * Internal recursive method for duplicate detection.
     *
     * @param fields the list of field nodes at the current scope
     * @throws ParseException if duplicates are found
     */
    private void detectDuplicatesInternal(List<FieldNode> fields) {
        Set<String> namesInScope = new HashSet<>();

        for (FieldNode field : fields) {
            // Skip transitory fields (groupId, occurrenceCount metadata)
            if (field.isTransitory()) {
                continue;
            }

            String name = field.getCamelCaseName();
            if (name == null || name.isEmpty()) {
                continue;
            }

            if (namesInScope.contains(name)) {
                throw new ParseException("Duplicate field name '" + name + "'")
                    .withContext(
                        field.getSource() != null ? field.getSource().getSheetName() : null,
                        field.getSource() != null ? field.getSource().getRowIndex() : 0
                    )
                    .withField(field.getOriginalName());
            }
            namesInScope.add(name);

            // Recursively check children (children have their own independent scope)
            if (field.getChildren() != null && !field.getChildren().isEmpty()) {
                detectDuplicatesInternal(field.getChildren());
            }
        }
    }

    /**
     * Finds duplicate field names without throwing an exception.
     *
     * <p>This method provides a non-throwing alternative for scenarios where
     * all duplicates need to be collected rather than failing on the first one.</p>
     *
     * <p>Note: This method checks duplicates across all levels combined,
     * which is different from the scope-based checking in {@link #detectDuplicates}.
     * Use this for reporting purposes when you need to show all duplicates.</p>
     *
     * @param fields the list of field nodes to check
     * @return a map of duplicate names to the list of fields with that name;
     *         empty map if no duplicates found
     */
    public Map<String, List<FieldNode>> findDuplicates(List<FieldNode> fields) {
        Map<String, List<FieldNode>> nameToNodes = new LinkedHashMap<>();

        if (fields != null) {
            collectFieldNames(fields, nameToNodes);
        }

        // Filter to keep only entries with more than one field (duplicates)
        Map<String, List<FieldNode>> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, List<FieldNode>> entry : nameToNodes.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicates;
    }

    /**
     * Finds duplicate field names within the same scope only.
     *
     * <p>This method respects the hierarchical scope rules, checking for
     * duplicates only among siblings at each level.</p>
     *
     * @param fields the list of field nodes to check
     * @return a map of duplicate names to the list of fields with that name;
     *         empty map if no duplicates found
     */
    public Map<String, List<FieldNode>> findDuplicatesInScope(List<FieldNode> fields) {
        Map<String, List<FieldNode>> allDuplicates = new LinkedHashMap<>();

        if (fields != null) {
            collectDuplicatesInScope(fields, allDuplicates);
        }

        return allDuplicates;
    }

    /**
     * Recursively collects duplicates respecting scope boundaries.
     *
     * @param fields the fields at current scope
     * @param allDuplicates the accumulated duplicates map
     */
    private void collectDuplicatesInScope(List<FieldNode> fields, Map<String, List<FieldNode>> allDuplicates) {
        Map<String, List<FieldNode>> scopeNames = new LinkedHashMap<>();

        for (FieldNode field : fields) {
            if (field.isTransitory()) {
                continue;
            }

            String name = field.getCamelCaseName();
            if (name != null && !name.isEmpty()) {
                scopeNames.computeIfAbsent(name, k -> new ArrayList<>()).add(field);
            }

            // Recursively check children
            if (field.getChildren() != null && !field.getChildren().isEmpty()) {
                collectDuplicatesInScope(field.getChildren(), allDuplicates);
            }
        }

        // Add duplicates from this scope to the overall result
        for (Map.Entry<String, List<FieldNode>> entry : scopeNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                allDuplicates.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Recursively collects field names from all levels.
     *
     * @param fields the fields to process
     * @param nameToNodes the map to populate
     */
    private void collectFieldNames(List<FieldNode> fields, Map<String, List<FieldNode>> nameToNodes) {
        for (FieldNode field : fields) {
            if (field.isTransitory()) {
                continue;
            }

            String name = field.getCamelCaseName();
            if (name != null && !name.isEmpty()) {
                nameToNodes.computeIfAbsent(name, k -> new ArrayList<>()).add(field);
            }

            if (field.getChildren() != null && !field.getChildren().isEmpty()) {
                collectFieldNames(field.getChildren(), nameToNodes);
            }
        }
    }
}
