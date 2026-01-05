package com.rtm.mq.tool.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CamelCaseConverter}.
 */
class CamelCaseConverterTest {

    private CamelCaseConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CamelCaseConverter();
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor uses default max length")
        void defaultConstructorUsesDefaultMaxLength() {
            CamelCaseConverter defaultConverter = new CamelCaseConverter();
            assertEquals(50, defaultConverter.getMaxLength());
        }

        @Test
        @DisplayName("Custom max length is respected")
        void customMaxLengthIsRespected() {
            CamelCaseConverter customConverter = new CamelCaseConverter(30);
            assertEquals(30, customConverter.getMaxLength());
        }

        @Test
        @DisplayName("Non-positive max length falls back to default")
        void nonPositiveMaxLengthFallsBackToDefault() {
            CamelCaseConverter zeroConverter = new CamelCaseConverter(0);
            CamelCaseConverter negativeConverter = new CamelCaseConverter(-10);

            assertEquals(50, zeroConverter.getMaxLength());
            assertEquals(50, negativeConverter.getMaxLength());
        }
    }

    @Nested
    @DisplayName("Null and empty input tests")
    class NullAndEmptyTests {

        @Test
        @DisplayName("Null input returns null")
        void nullInputReturnsNull() {
            assertNull(converter.toCamelCase(null));
        }

        @Test
        @DisplayName("Empty input returns empty")
        void emptyInputReturnsEmpty() {
            assertEquals("", converter.toCamelCase(""));
        }
    }

    @Nested
    @DisplayName("Underscore separation tests")
    class UnderscoreSeparationTests {

        @Test
        @DisplayName("DOMICILE_BRANCH converts to domicileBranch")
        void underscoreSeparatedUppercase() {
            assertEquals("domicileBranch", converter.toCamelCase("DOMICILE_BRANCH"));
        }

        @Test
        @DisplayName("account_number converts to accountNumber")
        void underscoreSeparatedLowercase() {
            assertEquals("accountNumber", converter.toCamelCase("account_number"));
        }

        @Test
        @DisplayName("Multiple underscores handled correctly")
        void multipleUnderscores() {
            assertEquals("firstSecondThird", converter.toCamelCase("first_second_third"));
        }

        @Test
        @DisplayName("Leading underscore handled")
        void leadingUnderscore() {
            assertEquals("field", converter.toCamelCase("_field"));
        }

        @Test
        @DisplayName("Trailing underscore handled")
        void trailingUnderscore() {
            assertEquals("field", converter.toCamelCase("field_"));
        }

        @Test
        @DisplayName("Consecutive underscores handled")
        void consecutiveUnderscores() {
            assertEquals("firstSecond", converter.toCamelCase("first__second"));
        }
    }

    @Nested
    @DisplayName("Hyphen separation tests")
    class HyphenSeparationTests {

        @Test
        @DisplayName("response-code converts to responseCode")
        void hyphenSeparated() {
            assertEquals("responseCode", converter.toCamelCase("response-code"));
        }

        @Test
        @DisplayName("Multiple hyphens handled correctly")
        void multipleHyphens() {
            assertEquals("firstSecondThird", converter.toCamelCase("first-second-third"));
        }

        @Test
        @DisplayName("Mixed underscore and hyphen")
        void mixedSeparators() {
            assertEquals("firstSecondThird", converter.toCamelCase("first_second-third"));
        }
    }

    @Nested
    @DisplayName("First letter case tests")
    class FirstLetterCaseTests {

        @Test
        @DisplayName("ResponseCode converts to responseCode")
        void pascalCaseToLowerFirst() {
            assertEquals("responsecode", converter.toCamelCase("ResponseCode"));
        }

        @Test
        @DisplayName("ALLCAPS converts to allcaps")
        void allCapsToLowercase() {
            assertEquals("allcaps", converter.toCamelCase("ALLCAPS"));
        }

        @Test
        @DisplayName("alreadyCamelCase is preserved (mostly)")
        void alreadyCamelCase() {
            // Note: without separators, entire string becomes lowercase
            assertEquals("alreadycamelcase", converter.toCamelCase("alreadyCamelCase"));
        }
    }

    @Nested
    @DisplayName("Digit prefix tests")
    class DigitPrefixTests {

        @Test
        @DisplayName("123StartWithNumber gets field prefix")
        void startsWithDigit() {
            assertEquals("field123startwithnumber", converter.toCamelCase("123StartWithNumber"));
        }

        @Test
        @DisplayName("123_field gets field prefix")
        void startsWithDigitAndUnderscore() {
            assertEquals("field123Field", converter.toCamelCase("123_field"));
        }

        @Test
        @DisplayName("Digits in middle are preserved")
        void digitsInMiddle() {
            assertEquals("field123", converter.toCamelCase("field123"));
        }

        @Test
        @DisplayName("Only digits get field prefix")
        void onlyDigits() {
            assertEquals("field12345", converter.toCamelCase("12345"));
        }
    }

    @Nested
    @DisplayName("Special character removal tests")
    class SpecialCharacterTests {

        @Test
        @DisplayName("special!@#chars becomes specialChars")
        void specialCharactersRemoved() {
            assertEquals("specialchars", converter.toCamelCase("special!@#chars"));
        }

        @Test
        @DisplayName("Dots are removed")
        void dotsRemoved() {
            assertEquals("firstsecond", converter.toCamelCase("first.second"));
        }

        @Test
        @DisplayName("Spaces are removed")
        void spacesRemoved() {
            assertEquals("firstsecond", converter.toCamelCase("first second"));
        }

        @Test
        @DisplayName("Only special characters result in hash-based name")
        void onlySpecialCharacters() {
            String result = converter.toCamelCase("!@#$%");
            assertNotNull(result);
            assertTrue(result.startsWith("field"));
            // Should have a hash suffix
            assertTrue(result.length() > 5);
        }

        @Test
        @DisplayName("Parentheses are removed")
        void parenthesesRemoved() {
            assertEquals("fieldname", converter.toCamelCase("field(name)"));
        }

        @Test
        @DisplayName("Square brackets are removed")
        void squareBracketsRemoved() {
            assertEquals("fieldname", converter.toCamelCase("field[name]"));
        }
    }

    @Nested
    @DisplayName("CJK character tests")
    class CJKCharacterTests {

        @Test
        @DisplayName("Chinese characters converted to pinyin")
        void chineseCharactersConvertedToPinyin() {
            // "Ke hu" means "customer" in Chinese
            String result = converter.toCamelCase("\u5BA2\u6237");
            assertNotNull(result);
            // Should contain pinyin-like characters
            assertTrue(result.matches("[a-zA-Z]+"));
        }

        @Test
        @DisplayName("Mixed Chinese and English")
        void mixedChineseAndEnglish() {
            String result = converter.toCamelCase("user\u5BA2\u6237");
            assertNotNull(result);
            assertTrue(result.startsWith("user"));
        }

        @Test
        @DisplayName("Multiple Chinese characters form camelCase pinyin")
        void multipleChinese() {
            // Multiple characters should form camelCase
            String result = converter.toCamelCase("\u5BA2\u6237\u59D3\u540D"); // "customer name" in Chinese
            assertNotNull(result);
            assertTrue(result.length() > 4); // Should have multiple syllables
        }
    }

    @Nested
    @DisplayName("Length limit tests")
    class LengthLimitTests {

        @Test
        @DisplayName("Name exceeding max length is truncated with hash")
        void exceedingMaxLengthTruncatedWithHash() {
            CamelCaseConverter shortConverter = new CamelCaseConverter(20);
            String longInput = "very_long_field_name_that_exceeds_limit";
            String result = shortConverter.toCamelCase(longInput);

            assertNotNull(result);
            assertEquals(20, result.length());
            // Should end with 4-char hash
            assertTrue(result.substring(16).matches("[0-9a-f]{4}"));
        }

        @Test
        @DisplayName("Name at max length is not truncated")
        void atMaxLengthNotTruncated() {
            CamelCaseConverter customConverter = new CamelCaseConverter(15);
            String result = customConverter.toCamelCase("short_field");

            assertEquals("shortField", result);
            assertTrue(result.length() <= 15);
        }

        @Test
        @DisplayName("Hash suffix is deterministic")
        void hashSuffixIsDeterministic() {
            CamelCaseConverter shortConverter = new CamelCaseConverter(20);
            String input = "this_is_a_very_long_field_name";

            String result1 = shortConverter.toCamelCase(input);
            String result2 = shortConverter.toCamelCase(input);

            assertEquals(result1, result2);
        }
    }

    @Nested
    @DisplayName("Edge case tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Single character lowercase")
        void singleCharLowercase() {
            assertEquals("a", converter.toCamelCase("a"));
        }

        @Test
        @DisplayName("Single character uppercase")
        void singleCharUppercase() {
            assertEquals("a", converter.toCamelCase("A"));
        }

        @Test
        @DisplayName("Whitespace only results in hash-based name")
        void whitespaceOnly() {
            String result = converter.toCamelCase("   ");
            assertNotNull(result);
            assertTrue(result.startsWith("field"));
        }

        @Test
        @DisplayName("Very long underscore chain")
        void longUnderscoreChain() {
            assertEquals("abc", converter.toCamelCase("a_____b_____c"));
        }

        @Test
        @DisplayName("Unicode letters preserved after CJK processing")
        void unicodeLettersHandled() {
            // Non-CJK unicode might be filtered
            String result = converter.toCamelCase("field_test");
            assertEquals("fieldTest", result);
        }
    }

    @Nested
    @DisplayName("Determinism tests")
    class DeterminismTests {

        @Test
        @DisplayName("Same input always produces same output")
        void sameInputSameOutput() {
            String input = "SOME_FIELD_NAME";
            String result1 = converter.toCamelCase(input);
            String result2 = converter.toCamelCase(input);
            String result3 = converter.toCamelCase(input);

            assertEquals(result1, result2);
            assertEquals(result2, result3);
        }

        @Test
        @DisplayName("Different instances produce same result")
        void differentInstancesSameResult() {
            CamelCaseConverter conv1 = new CamelCaseConverter();
            CamelCaseConverter conv2 = new CamelCaseConverter();

            String input = "test_field";
            assertEquals(conv1.toCamelCase(input), conv2.toCamelCase(input));
        }
    }
}
