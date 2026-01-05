package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.FieldNode;
import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.model.ValidationResult;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Response Sheet parser.
 *
 * <p>This parser extends the ExcelParser functionality to handle Response-specific
 * parsing logic. It reuses the core parsing components (SegLevelParser, ObjectArrayDetector,
 * CamelCaseConverter) while adding Response-specific validation.</p>
 *
 * <p>Key behaviors:</p>
 * <ul>
 *   <li>Returns empty FieldGroup when Response sheet is null (valid case for request-only messages)</li>
 *   <li>Validates Response-specific nesting depth constraints</li>
 *   <li>Maintains consistency with Request parsing logic from T-107</li>
 * </ul>
 *
 * @see ExcelParser
 * @see FieldGroup
 */
public class ResponseSheetParser {

    private static final Logger logger = LoggerFactory.getLogger(ResponseSheetParser.class);
    private static final String SHEET_NAME = "Response";

    private final ExcelParser excelParser;
    private final Config config;

    /**
     * Creates a ResponseSheetParser with the specified ExcelParser and configuration.
     *
     * @param excelParser the ExcelParser instance for reusing core parsing logic
     * @param config the configuration containing parser settings
     */
    public ResponseSheetParser(ExcelParser excelParser, Config config) {
        if (excelParser == null) {
            throw new IllegalArgumentException("ExcelParser must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config must not be null");
        }
        this.excelParser = excelParser;
        this.config = config;
    }

    /**
     * Parses the Response Sheet into a FieldGroup.
     *
     * <p>If the sheet is null, returns an empty FieldGroup since some messages
     * may only have a Request without a Response.</p>
     *
     * @param sheet the Response Sheet, may be null
     * @return the parsed FieldGroup containing the hierarchical field structure
     * @throws ParseException if parsing fails due to invalid data
     */
    public FieldGroup parse(Sheet sheet) {
        if (sheet == null) {
            logger.debug("Response sheet is null, returning empty FieldGroup");
            return createEmptyFieldGroup();
        }

        // Validate sheet name
        String actualName = sheet.getSheetName();
        if (!isValidResponseSheetName(actualName)) {
            throw new ParseException("Expected Response sheet but got: " + actualName);
        }

        logger.debug("Parsing Response sheet: {}", actualName);

        // Reuse ExcelParser's parseSheet logic
        return excelParser.parseSheet(sheet, SHEET_NAME);
    }

    /**
     * Checks if the given sheet name is a valid Response sheet name.
     *
     * <p>Valid names include "Response" (case-insensitive) or any name
     * containing "response" (case-insensitive).</p>
     *
     * @param sheetName the sheet name to check
     * @return true if the name is a valid Response sheet name
     */
    private boolean isValidResponseSheetName(String sheetName) {
        if (sheetName == null) {
            return false;
        }
        String lowerName = sheetName.toLowerCase();
        return SHEET_NAME.equalsIgnoreCase(sheetName) || lowerName.contains("response");
    }

    /**
     * Creates an empty FieldGroup.
     *
     * @return a new empty FieldGroup
     */
    private FieldGroup createEmptyFieldGroup() {
        return new FieldGroup();
    }

    /**
     * Validates a parsed Response FieldGroup.
     *
     * <p>Response-specific validation includes:</p>
     * <ul>
     *   <li>Nesting depth validation against configured maximum</li>
     * </ul>
     *
     * <p>Note: An empty Response is valid (some messages only have Request).</p>
     *
     * @param response the parsed Response FieldGroup to validate
     * @return validation result containing any errors or warnings
     */
    public ValidationResult validate(FieldGroup response) {
        ValidationResult result = new ValidationResult();

        // Empty Response is valid
        if (response == null || response.getFields().isEmpty()) {
            logger.debug("Empty Response FieldGroup - validation passed");
            return result;
        }

        // Validate nesting depth
        validateNestingDepth(response.getFields(), 1, result);

        return result;
    }

    /**
     * Recursively validates nesting depth for all fields.
     *
     * @param fields the list of fields to validate
     * @param currentDepth the current nesting depth (1-based)
     * @param result the validation result to add errors to
     */
    private void validateNestingDepth(List<FieldNode> fields, int currentDepth,
                                      ValidationResult result) {
        int maxDepth = config.getParser().getMaxNestingDepth();

        for (FieldNode field : fields) {
            if (currentDepth > maxDepth) {
                result.addError(new ValidationError(
                    "VR-104",
                    "Response nesting depth exceeds maximum",
                    String.format("Field '%s' at depth %d exceeds max %d",
                        field.getOriginalName(), currentDepth, maxDepth)
                ));
                logger.warn("Response nesting depth exceeded: field '{}' at depth {} exceeds max {}",
                    field.getOriginalName(), currentDepth, maxDepth);
            }

            if (!field.getChildren().isEmpty()) {
                validateNestingDepth(field.getChildren(), currentDepth + 1, result);
            }
        }
    }

    /**
     * Gets the expected sheet name for Response sheets.
     *
     * @return the standard Response sheet name
     */
    public String getSheetName() {
        return SHEET_NAME;
    }
}
