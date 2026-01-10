package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ParserConfig;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared Header loader.
 * Supports loading from a separate file or embedded sheet.
 *
 * <p>The Shared Header contains common field definitions that appear in both
 * request and response messages. It can be provided either:</p>
 * <ul>
 *   <li>As a separate Excel file (preferred for reusable headers)</li>
 *   <li>As a "Shared Header" sheet within the main specification file</li>
 * </ul>
 *
 * <p>This loader handles the separate file case, delegating the actual parsing
 * back to the ExcelParser to ensure consistent processing logic.</p>
 *
 * @see ExcelParser
 */
public class SharedHeaderLoader {

    private static final String SHARED_HEADER_SHEET = "Shared Header";

    private final ExcelParser parser;

    /**
     * Creates a SharedHeaderLoader.
     *
     * @param parser the ExcelParser instance to use for parsing sheets
     */
    public SharedHeaderLoader(ExcelParser parser) {
        this.parser = parser;
    }

    /**
     * Loads Shared Header from a separate file.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Opens the shared header Excel file</li>
     *   <li>Validates that the file is a proper Shared Header file (not a message spec)</li>
     *   <li>Looks for a sheet named "Shared Header"</li>
     *   <li>Falls back to the first sheet if not found</li>
     *   <li>Delegates to ExcelParser.parseSheet() for consistent parsing</li>
     * </ol>
     *
     * @param sharedHeaderFile the path to the shared header Excel file
     * @param config the configuration (unused but kept for consistency)
     * @return the parsed FieldGroup, or empty FieldGroup if no sheet found
     * @throws ParseException if file cannot be read or validation fails
     */
    public FieldGroup loadFromFile(Path sharedHeaderFile, Config config) {
        // Configure POI ZipSecureFile to handle larger files with higher compression ratios
        configureZipSecureFile(config);

        try (InputStream is = Files.newInputStream(sharedHeaderFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet headerSheet = workbook.getSheet(SHARED_HEADER_SHEET);
            if (headerSheet == null) {
                // Try first sheet as fallback
                if (workbook.getNumberOfSheets() > 0) {
                    headerSheet = workbook.getSheetAt(0);
                }
            }

            if (headerSheet == null) {
                return new FieldGroup();
            }

            // Detect format and route to appropriate parser
            SharedHeaderFormatDetector detector = new SharedHeaderFormatDetector();
            SharedHeaderFormatDetector.FileFormat format = detector.detectFormat(workbook, headerSheet);

            if (format == SharedHeaderFormatDetector.FileFormat.ISM_V2_FIX) {
                // Use ISM v2.0 FIX parser
                IsmV2FixParser ismParser = new IsmV2FixParser();
                return ismParser.parse(headerSheet);
            } else {
                // Use standard format parser
                // Validate file structure only for standard format
                validateSharedHeaderFile(workbook, sharedHeaderFile);

                // Reuse the parser's parseSheet logic to ensure consistency
                return parser.parseSheet(headerSheet, "Shared Header");
            }

        } catch (IOException e) {
            throw new ParseException("Failed to load Shared Header file: " + sharedHeaderFile, e);
        }
    }

    /**
     * Validates that the provided file is a valid Shared Header file.
     *
     * <p>Validation checks:</p>
     * <ul>
     *   <li>File should not contain "Request" or "Response" sheets (indicates a message spec file)</li>
     *   <li>File should contain either a "Shared Header" sheet or be a single-sheet file</li>
     *   <li>If file contains "Request" sheet, emit warning (may be incorrectly formatted file)</li>
     * </ul>
     *
     * @param workbook the Excel workbook to validate
     * @param filePath the file path (for error messages)
     * @throws ParseException if validation fails
     */
    private void validateSharedHeaderFile(Workbook workbook, Path filePath) {
        // Check if file contains Request or Response sheets (indicates wrong file type)
        boolean hasRequest = workbook.getSheet("Request") != null ||
                           hasSheetCaseInsensitive(workbook, "Request");
        boolean hasResponse = workbook.getSheet("Response") != null ||
                            hasSheetCaseInsensitive(workbook, "Response");

        if (hasRequest || hasResponse) {
            // This is likely a message specification file, not a header-only file
            throw new ParseException(
                "Shared Header file appears to be a message specification file " +
                "(contains 'Request' or 'Response' sheets). " +
                "Please provide a header-only file or embed the header in the main specification file: " +
                filePath
            );
        }
    }

    /**
     * Configures POI ZipSecureFile settings from the provided configuration.
     *
     * @param config the configuration containing POI settings
     */
    private void configureZipSecureFile(Config config) {
        ParserConfig parserConfig = config.getParser();
        ZipSecureFile.setMaxTextSize(parserConfig.getPoiMaxTextSize());
        ZipSecureFile.setMinInflateRatio(parserConfig.getPoiMinInflateRatio());
    }

    /**
     * Checks if a sheet exists with the given name (case-insensitive).
     *
     * @param workbook the workbook to search
     * @param sheetName the sheet name to find
     * @return true if a sheet with that name exists (case-insensitive), false otherwise
     */
    private boolean hasSheetCaseInsensitive(Workbook workbook, String sheetName) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            if (name != null && name.equalsIgnoreCase(sheetName)) {
                return true;
            }
        }
        return false;
    }
}
