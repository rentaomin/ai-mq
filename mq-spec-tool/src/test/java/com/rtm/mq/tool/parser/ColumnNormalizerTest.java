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
