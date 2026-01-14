package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ParserConfig;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
import com.rtm.mq.tool.model.MqMessageFormat;
import com.rtm.mq.tool.model.MqMessageModel;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loader for standalone MQ message field definitions.
 *
 * <p>This loader is for independent MQ message files like ISM v2.0 FIX mapping.xlsx,
 * NOT for embedded Shared Header sheets in request/response files.</p>
 *
 * <p>Characteristics of MQ message files:</p>
 * <ul>
 *   <li>Standalone file, separate from message specifications</li>
 *   <li>Contains field definitions only (no metadata like Operation Name/ID)</li>
 *   <li>Used as a reference for field validation and comparison</li>
 *   <li>Can be in different formats: STANDARD hierarchical or ISM_V2_FIX fixed-format</li>
 * </ul>
 *
 * @see MqMessageModel
 * @see MqMessageFormat
 */
public class MqMessageLoader {

    private static final String SHARED_HEADER_SHEET = "Shared Header";

    private final ExcelParser parser;

    /**
     * Creates a MqMessageLoader.
     *
     * @param parser the ExcelParser instance to use for parsing sheets
     */
    public MqMessageLoader(ExcelParser parser) {
        this.parser = parser;
    }

    /**
     * Loads MQ message fields from a standalone file.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Opens the MQ message Excel file</li>
     *   <li>Validates that the file is a proper MQ message file (not a message spec)</li>
     *   <li>Detects the file format (STANDARD or ISM_V2_FIX)</li>
     *   <li>Delegates to appropriate parser based on format</li>
     *   <li>Builds search indices for field lookup</li>
     * </ol>
     *
     * @param mqMessageFile the path to the MQ message Excel file
     * @param config the configuration
     * @return populated MqMessageModel with searchable fields
     * @throws ParseException if file cannot be read or validation fails
     */
    public MqMessageModel loadFromFile(Path mqMessageFile, Config config) {
        // Configure POI ZipSecureFile to handle larger files with higher compression ratios
        configureZipSecureFile(config);

        try (InputStream is = Files.newInputStream(mqMessageFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            // Find sheet (prefer "Shared Header", fallback to first sheet)
            Sheet sheet = workbook.getSheet(SHARED_HEADER_SHEET);
            if (sheet == null) {
                if (workbook.getNumberOfSheets() > 0) {
                    sheet = workbook.getSheetAt(0);
                }
            }

            if (sheet == null) {
                throw new ParseException("No sheet found in MQ message file: " + mqMessageFile);
            }

            // Detect format
            SharedHeaderFormatDetector detector = new SharedHeaderFormatDetector();
            SharedHeaderFormatDetector.FileFormat detectedFormat = detector.detectFormat(workbook, sheet);

            // Parse fields based on format
            FieldGroup fields;
            if (detectedFormat == SharedHeaderFormatDetector.FileFormat.ISM_V2_FIX) {
                IsmV2FixParser ismParser = new IsmV2FixParser();
                fields = ismParser.parse(sheet);
            } else {
                // Standard format
                validateMqMessageFile(workbook, mqMessageFile);
                fields = parser.parseSheet(sheet, "MQ Message");
            }

            // Build MqMessageModel
            MqMessageModel model = new MqMessageModel();
            model.setSourceFile(mqMessageFile.toAbsolutePath().toString());
            model.setFormat(mapFormat(detectedFormat));
            model.setFields(fields);
            model.buildIndices();  // Build search indices for lookups

            return model;

        } catch (IOException e) {
            throw new ParseException("Failed to load MQ message file: " + mqMessageFile, e);
        }
    }

    /**
     * Maps SharedHeaderFormatDetector.FileFormat to MqMessageFormat.
     *
     * @param detectedFormat the detected format from SharedHeaderFormatDetector
     * @return the corresponding MqMessageFormat
     */
    private MqMessageFormat mapFormat(SharedHeaderFormatDetector.FileFormat detectedFormat) {
        switch (detectedFormat) {
            case STANDARD:
                return MqMessageFormat.STANDARD;
            case ISM_V2_FIX:
                return MqMessageFormat.ISM_V2_FIX;
            default:
                return MqMessageFormat.UNKNOWN;
        }
    }

    /**
     * Validates that the provided file is a valid MQ message file.
     *
     * <p>Validation checks:</p>
     * <ul>
     *   <li>File should not contain "Request" or "Response" sheets (indicates a message spec file)</li>
     *   <li>If it does contain these sheets, throw an error</li>
     * </ul>
     *
     * @param workbook the Excel workbook to validate
     * @param filePath the file path (for error messages)
     * @throws ParseException if validation fails
     */
    private void validateMqMessageFile(Workbook workbook, Path filePath) {
        // Check if file contains Request or Response sheets (indicates wrong file type)
        boolean hasRequest = workbook.getSheet("Request") != null ||
                hasSheetCaseInsensitive(workbook, "Request");
        boolean hasResponse = workbook.getSheet("Response") != null ||
                hasSheetCaseInsensitive(workbook, "Response");

        if (hasRequest || hasResponse) {
            // This is likely a message specification file, not an MQ message file
            throw new ParseException(
                "MQ message file appears to be a message specification file " +
                "(contains 'Request' or 'Response' sheets). " +
                "Please provide a field-only MQ message file: " + filePath
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
