package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.SourceMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DuplicateDetector}.
 */
class DuplicateDetectorTest {

    private DuplicateDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DuplicateDetector();
    }

    private FieldNode createField(String originalName, String camelCaseName) {
        return FieldNode.builder()
                .originalName(originalName)
                .camelCaseName(camelCaseName)
                .source(new SourceMetadata("TestSheet", 1))
                .build();
    }

    private FieldNode createField(String originalName, String camelCaseName, String sheetName, int rowIndex) {
        return FieldNode.builder()
                .originalName(originalName)
                .camelCaseName(camelCaseName)
                .source(new SourceMetadata(sheetName, rowIndex))
                .build();
    }

    private FieldNode createTransitoryField(String originalName, String camelCaseName) {
        return FieldNode.builder()
                .originalName(originalName)
                .camelCaseName(camelCaseName)
                .isTransitory(true)
                .source(new SourceMetadata("TestSheet", 1))
                .build();
    }

    private FieldNode createFieldWithChildren(String originalName, String camelCaseName, List<FieldNode> children) {
        return FieldNode.builder()
                .originalName(originalName)
                .camelCaseName(camelCaseName)
                .children(children)
                .source(new SourceMetadata("TestSheet", 1))
                .build();
    }

    @Nested
    @DisplayName("detectDuplicates() tests")
    class DetectDuplicatesTests {

        @Test
        @DisplayName("Null list does not throw")
        void nullListDoesNotThrow() {
            assertDoesNotThrow(() -> detector.detectDuplicates(null));
        }

        @Test
        @DisplayName("Empty list does not throw")
        void emptyListDoesNotThrow() {
            assertDoesNotThrow(() -> detector.detectDuplicates(Collections.emptyList()));
        }

        @Test
        @DisplayName("Unique field names do not throw")
        void uniqueFieldNamesDoNotThrow() {
            List<FieldNode> fields = Arrays.asList(
                    createField("field_one", "fieldOne"),
                    createField("field_two", "fieldTwo"),
                    createField("field_three", "fieldThree")
            );

            assertDoesNotThrow(() -> detector.detectDuplicates(fields));
        }

        @Test
        @DisplayName("Duplicate field names throw ParseException")
        void duplicateFieldNamesThrow() {
            List<FieldNode> fields = Arrays.asList(
                    createField("field_one", "fieldOne", "Request", 10),
                    createField("another_field", "fieldOne", "Request", 15)
            );

            ParseException ex = assertThrows(ParseException.class,
                    () -> detector.detectDuplicates(fields));

            assertTrue(ex.getMessage().contains("Duplicate field name 'fieldOne'"));
            assertEquals("Request", ex.getSheetName());
            assertEquals(Integer.valueOf(15), ex.getRowIndex());
        }

        @Test
        @DisplayName("Transitory fields are skipped")
        void transitoryFieldsSkipped() {
            List<FieldNode> fields = Arrays.asList(
                    createField("field_one", "fieldOne"),
                    createTransitoryField("group_id", "fieldOne"), // Same name but transitory
                    createField("field_two", "fieldTwo")
            );

            assertDoesNotThrow(() -> detector.detectDuplicates(fields));
        }

        @Test
        @DisplayName("Null camelCaseName fields are skipped")
        void nullCamelCaseNameSkipped() {
            List<FieldNode> fields = Arrays.asList(
                    createField("field_one", "fieldOne"),
                    createField("field_two", null),
                    createField("field_three", "fieldThree")
            );

            assertDoesNotThrow(() -> detector.detectDuplicates(fields));
        }

        @Test
        @DisplayName("Empty camelCaseName fields are skipped")
        void emptyCamelCaseNameSkipped() {
            List<FieldNode> fields = Arrays.asList(
                    createField("field_one", "fieldOne"),
                    createField("field_two", ""),
                    createField("field_three", "fieldThree")
            );

            assertDoesNotThrow(() -> detector.detectDuplicates(fields));
        }

        @Test
        @DisplayName("Same name in different scopes is allowed")
        void sameNameDifferentScopes() {
            FieldNode child1 = createField("name", "name");
            FieldNode child2 = createField("name", "name");

            List<FieldNode> fields = Arrays.asList(
                    createFieldWithChildren("parent1", "parent1", Arrays.asList(child1)),
                    createFieldWithChildren("parent2", "parent2", Arrays.asList(child2))
            );

            assertDoesNotThrow(() -> detector.detectDuplicates(fields));
        }

        @Test
        @DisplayName("Duplicate in nested scope throws")
        void duplicateInNestedScope() {
            FieldNode child1 = createField("name", "name", "Request", 20);
            FieldNode child2 = createField("other_name", "name", "Request", 25);

            List<FieldNode> parentChildren = new ArrayList<>();
            parentChildren.add(child1);
            parentChildren.add(child2);

            List<FieldNode> fields = Collections.singletonList(
                    createFieldWithChildren("parent", "parent", parentChildren)
            );

            ParseException ex = assertThrows(ParseException.class,
                    () -> detector.detectDuplicates(fields));

            assertTrue(ex.getMessage().contains("Duplicate field name 'name'"));
        }
    }

    @Nested
    @DisplayName("findDuplicates() tests")
    class FindDuplicatesTests {

        @Test
        @DisplayName("Null list returns empty map")
        void nullListReturnsEmptyMap() {
            Map<String, List<FieldNode>> result = detector.findDuplicates(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Empty list returns empty map")
        void emptyListReturnsEmptyMap() {
            Map<String, List<FieldNode>> result = detector.findDuplicates(Collections.emptyList());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Unique names return empty map")
        void uniqueNamesReturnEmptyMap() {
            List<FieldNode> fields = Arrays.asList(
                    createField("one", "fieldOne"),
                    createField("two", "fieldTwo"),
                    createField("three", "fieldThree")
            );

            Map<String, List<FieldNode>> result = detector.findDuplicates(fields);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Duplicates are collected in map")
        void duplicatesCollected() {
            FieldNode field1 = createField("field_one", "fieldOne");
            FieldNode field2 = createField("another", "fieldOne");
            FieldNode field3 = createField("third", "fieldThree");

            List<FieldNode> fields = Arrays.asList(field1, field2, field3);

            Map<String, List<FieldNode>> result = detector.findDuplicates(fields);

            assertEquals(1, result.size());
            assertTrue(result.containsKey("fieldOne"));
            assertEquals(2, result.get("fieldOne").size());
            assertTrue(result.get("fieldOne").contains(field1));
            assertTrue(result.get("fieldOne").contains(field2));
        }

        @Test
        @DisplayName("Multiple duplicate groups are found")
        void multipleDuplicateGroups() {
            List<FieldNode> fields = Arrays.asList(
                    createField("a1", "groupA"),
                    createField("a2", "groupA"),
                    createField("b1", "groupB"),
                    createField("b2", "groupB"),
                    createField("c1", "unique")
            );

            Map<String, List<FieldNode>> result = detector.findDuplicates(fields);

            assertEquals(2, result.size());
            assertEquals(2, result.get("groupA").size());
            assertEquals(2, result.get("groupB").size());
        }

        @Test
        @DisplayName("Transitory fields are excluded")
        void transitoryFieldsExcluded() {
            List<FieldNode> fields = Arrays.asList(
                    createField("one", "sameName"),
                    createTransitoryField("two", "sameName")
            );

            Map<String, List<FieldNode>> result = detector.findDuplicates(fields);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Nested duplicates are found across levels")
        void nestedDuplicatesFoundAcrossLevels() {
            FieldNode child = createField("child", "sameName");
            FieldNode parent = createFieldWithChildren("parent", "parent",
                    Collections.singletonList(child));
            FieldNode sibling = createField("sibling", "sameName");

            List<FieldNode> fields = Arrays.asList(parent, sibling);

            Map<String, List<FieldNode>> result = detector.findDuplicates(fields);

            assertEquals(1, result.size());
            assertEquals(2, result.get("sameName").size());
        }
    }

    @Nested
    @DisplayName("findDuplicatesInScope() tests")
    class FindDuplicatesInScopeTests {

        @Test
        @DisplayName("Null list returns empty map")
        void nullListReturnsEmptyMap() {
            Map<String, List<FieldNode>> result = detector.findDuplicatesInScope(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Duplicates in same scope are found")
        void duplicatesInSameScope() {
            List<FieldNode> fields = Arrays.asList(
                    createField("a", "duplicate"),
                    createField("b", "duplicate"),
                    createField("c", "unique")
            );

            Map<String, List<FieldNode>> result = detector.findDuplicatesInScope(fields);

            assertEquals(1, result.size());
            assertEquals(2, result.get("duplicate").size());
        }

        @Test
        @DisplayName("Same name in different scopes is not flagged")
        void sameNameDifferentScopesNotFlagged() {
            FieldNode child1 = createField("a", "sameName");
            FieldNode child2 = createField("b", "sameName");

            List<FieldNode> fields = Arrays.asList(
                    createFieldWithChildren("parent1", "parent1", Collections.singletonList(child1)),
                    createFieldWithChildren("parent2", "parent2", Collections.singletonList(child2))
            );

            Map<String, List<FieldNode>> result = detector.findDuplicatesInScope(fields);

            // Should not flag cross-scope duplicates
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Duplicates in nested scope are found")
        void duplicatesInNestedScope() {
            FieldNode child1 = createField("a", "nested");
            FieldNode child2 = createField("b", "nested");

            List<FieldNode> children = new ArrayList<>();
            children.add(child1);
            children.add(child2);

            List<FieldNode> fields = Collections.singletonList(
                    createFieldWithChildren("parent", "parent", children)
            );

            Map<String, List<FieldNode>> result = detector.findDuplicatesInScope(fields);

            assertEquals(1, result.size());
            assertEquals(2, result.get("nested").size());
        }

        @Test
        @DisplayName("Transitory fields are excluded")
        void transitoryFieldsExcluded() {
            List<FieldNode> fields = Arrays.asList(
                    createField("one", "sameName"),
                    createTransitoryField("two", "sameName")
            );

            Map<String, List<FieldNode>> result = detector.findDuplicatesInScope(fields);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Exception context tests")
    class ExceptionContextTests {

        @Test
        @DisplayName("Exception contains sheet name and row index")
        void exceptionContainsContext() {
            List<FieldNode> fields = Arrays.asList(
                    createField("first", "duplicate", "Sheet1", 5),
                    createField("second", "duplicate", "Sheet1", 10)
            );

            ParseException ex = assertThrows(ParseException.class,
                    () -> detector.detectDuplicates(fields));

            assertNotNull(ex.getSheetName());
            assertNotNull(ex.getRowIndex());
            assertNotNull(ex.getFieldName());
        }

        @Test
        @DisplayName("Exception contains original field name")
        void exceptionContainsOriginalFieldName() {
            FieldNode field1 = createField("original_name_one", "duplicateName", "Sheet1", 5);
            FieldNode field2 = createField("original_name_two", "duplicateName", "Sheet1", 10);

            List<FieldNode> fields = Arrays.asList(field1, field2);

            ParseException ex = assertThrows(ParseException.class,
                    () -> detector.detectDuplicates(fields));

            assertEquals("original_name_two", ex.getFieldName());
        }
    }

    @Nested
    @DisplayName("Order preservation tests")
    class OrderPreservationTests {

        @Test
        @DisplayName("findDuplicates preserves insertion order")
        void findDuplicatesPreservesOrder() {
            List<FieldNode> fields = Arrays.asList(
                    createField("z", "zebra"),
                    createField("a", "alpha"),
                    createField("z2", "zebra"),
                    createField("a2", "alpha"),
                    createField("m", "mike"),
                    createField("m2", "mike")
            );

            Map<String, List<FieldNode>> result = detector.findDuplicates(fields);

            // LinkedHashMap should preserve insertion order
            List<String> keys = new ArrayList<>(result.keySet());
            assertEquals("zebra", keys.get(0));
            assertEquals("alpha", keys.get(1));
            assertEquals("mike", keys.get(2));
        }
    }
}
