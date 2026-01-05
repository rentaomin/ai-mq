package com.rtm.mq.tool.parser;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Converts field names to camelCase format.
 *
 * <p>This converter handles various input formats:</p>
 * <ul>
 *   <li>Underscore-separated names (DOMICILE_BRANCH -&gt; domicileBranch)</li>
 *   <li>Hyphen-separated names (response-code -&gt; responseCode)</li>
 *   <li>PascalCase names (ResponseCode -&gt; responseCode)</li>
 *   <li>Names starting with digits (123Field -&gt; field123Field)</li>
 *   <li>Names with special characters (special!@#chars -&gt; specialChars)</li>
 *   <li>CJK characters converted to pinyin</li>
 * </ul>
 *
 * <p>The converter also enforces a maximum length constraint. When the resulting
 * name exceeds the limit, it truncates and appends a hash suffix for uniqueness.</p>
 *
 * @see DuplicateDetector
 */
public class CamelCaseConverter {

    /** Default maximum length for converted names. */
    private static final int DEFAULT_MAX_LENGTH = 50;

    /** Length of the hash suffix used for truncated names. */
    private static final int HASH_SUFFIX_LENGTH = 4;

    /** Prefix added to names that start with a digit. */
    private static final String DIGIT_PREFIX = "field";

    private final int maxLength;
    private final HanyuPinyinOutputFormat pinyinFormat;

    /**
     * Creates a CamelCaseConverter with the default maximum length (50 characters).
     */
    public CamelCaseConverter() {
        this(DEFAULT_MAX_LENGTH);
    }

    /**
     * Creates a CamelCaseConverter with the specified maximum length.
     *
     * @param maxLength the maximum length for converted names; if non-positive, uses default
     */
    public CamelCaseConverter(int maxLength) {
        this.maxLength = maxLength > 0 ? maxLength : DEFAULT_MAX_LENGTH;
        this.pinyinFormat = new HanyuPinyinOutputFormat();
        this.pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        this.pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    /**
     * Converts the input string to camelCase format.
     *
     * <p>The conversion process:</p>
     * <ol>
     *   <li>Convert CJK characters to pinyin</li>
     *   <li>Remove special characters (keep only letters, digits, underscores, hyphens)</li>
     *   <li>Split by underscores and hyphens</li>
     *   <li>Apply camelCase: first part lowercase, subsequent parts capitalized</li>
     *   <li>Add "field" prefix if result starts with a digit</li>
     *   <li>Generate hash-based name if result is empty</li>
     *   <li>Truncate with hash suffix if exceeds max length</li>
     * </ol>
     *
     * @param input the original field name
     * @return the camelCase version of the name, or null/empty if input is null/empty
     */
    public String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Step 1: Convert CJK characters to pinyin
        String processed = convertCJKToPinyin(input);

        // Step 2: Remove special characters, keep only letters, digits, underscores, hyphens
        String cleaned = processed.replaceAll("[^a-zA-Z0-9_\\-]", "");

        // Step 3: Split by underscores or hyphens
        String[] parts = cleaned.split("[_\\-]");

        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (result.length() == 0) {
                // First part: lowercase the first character
                result.append(toLowerFirst(part));
            } else {
                // Subsequent parts: uppercase the first character
                result.append(toUpperFirst(part));
            }
        }

        String camelCase = result.toString();

        // Step 4: Handle names starting with a digit
        if (!camelCase.isEmpty() && Character.isDigit(camelCase.charAt(0))) {
            camelCase = DIGIT_PREFIX + camelCase;
        }

        // Step 5: Handle empty result (all special characters removed)
        if (camelCase.isEmpty()) {
            camelCase = DIGIT_PREFIX + generateHash(input);
        }

        // Step 6: Enforce length limit with hash suffix
        if (camelCase.length() > maxLength) {
            String hash = generateHash(camelCase);
            camelCase = camelCase.substring(0, maxLength - HASH_SUFFIX_LENGTH) + hash;
        }

        return camelCase;
    }

    /**
     * Returns the configured maximum length for converted names.
     *
     * @return the maximum length
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Converts CJK characters in the input string to their pinyin equivalents.
     *
     * <p>Consecutive CJK characters are converted to camelCase pinyin
     * (e.g., "AB" becomes "keHuXingMing").</p>
     *
     * @param input the string potentially containing CJK characters
     * @return the string with CJK characters replaced by pinyin
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
                        // Capitalize first letter of subsequent CJK pinyin for camelCase
                        if (lastWasCJK && pinyin.length() > 0) {
                            pinyin.append(toUpperFirst(py));
                        } else {
                            pinyin.append(py);
                        }
                    } else {
                        // No pinyin available, keep original character
                        pinyin.append(ch);
                    }
                } catch (Exception e) {
                    // Pinyin conversion failed, keep original character
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
     * Checks if the character is a CJK (Chinese/Japanese/Korean) unified ideograph.
     *
     * @param ch the character to check
     * @return true if the character is a CJK ideograph
     */
    private boolean isCJKCharacter(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
               block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
    }

    /**
     * Converts the first character to lowercase and the rest to lowercase.
     *
     * @param str the input string
     * @return the string with first character lowercase
     */
    private String toLowerFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) +
               (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }

    /**
     * Converts the first character to uppercase and the rest to lowercase.
     *
     * @param str the input string
     * @return the string with first character uppercase
     */
    private String toUpperFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) +
               (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }

    /**
     * Generates a 4-character hexadecimal hash suffix from the input string.
     *
     * <p>Uses MD5 to generate a deterministic hash, taking the first 2 bytes
     * as a 4-character hex string.</p>
     *
     * @param input the string to hash
     * @return a 4-character hexadecimal hash
     */
    private String generateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            return String.format("%02x%02x", hash[0] & 0xff, hash[1] & 0xff);
        } catch (NoSuchAlgorithmException e) {
            // MD5 is always available in standard JVMs
            return "0000";
        }
    }
}
