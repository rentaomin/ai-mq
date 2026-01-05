package com.rtm.mq.tool.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nesting depth validator.
 *
 * <p>Validates segment level nesting depth against a configurable maximum.
 * The default maximum depth is 50 levels. When depth exceeds the limit,
 * a warning is logged but processing continues.</p>
 *
 * <p>This validator is designed to catch unusually deep nesting that may
 * indicate specification errors or impact performance and readability.</p>
 */
public class NestingDepthValidator {

    private static final Logger logger = LoggerFactory.getLogger(NestingDepthValidator.class);
    private static final int DEFAULT_MAX_DEPTH = 50;

    private final int maxDepth;

    /**
     * Creates a validator with the default maximum depth of 50.
     */
    public NestingDepthValidator() {
        this(DEFAULT_MAX_DEPTH);
    }

    /**
     * Creates a validator with a custom maximum depth.
     *
     * @param maxDepth the maximum allowed nesting depth (must be positive)
     */
    public NestingDepthValidator(int maxDepth) {
        this.maxDepth = maxDepth > 0 ? maxDepth : DEFAULT_MAX_DEPTH;
    }

    /**
     * Validates the current nesting depth.
     *
     * <p>If the depth exceeds the configured maximum, a warning is logged
     * including the row index and field name for context. Processing continues
     * regardless of the result.</p>
     *
     * @param currentDepth the current nesting depth to validate
     * @param rowIndex the 1-based row index in the source sheet
     * @param fieldName the name of the field at this depth
     * @return true if depth is within limits, false if it exceeds the maximum
     */
    public boolean validateDepth(int currentDepth, int rowIndex, String fieldName) {
        if (currentDepth > maxDepth) {
            logger.warn("Nesting depth {} exceeds recommended maximum {} at row {} (field: {}). " +
                "This may impact performance and readability.",
                currentDepth, maxDepth, rowIndex, fieldName);
            return false;
        }
        return true;
    }

    /**
     * Gets the configured maximum depth.
     *
     * @return the maximum depth
     */
    public int getMaxDepth() {
        return maxDepth;
    }
}
