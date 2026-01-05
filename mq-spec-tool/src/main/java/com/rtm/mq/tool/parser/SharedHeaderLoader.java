package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.FieldGroup;
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
     *   <li>Looks for a sheet named "Shared Header"</li>
     *   <li>Falls back to the first sheet if not found</li>
     *   <li>Delegates to ExcelParser.parseSheet() for consistent parsing</li>
     * </ol>
     *
     * @param sharedHeaderFile the path to the shared header Excel file
     * @param config the configuration (unused but kept for consistency)
     * @return the parsed FieldGroup, or empty FieldGroup if no sheet found
     * @throws ParseException if file cannot be read
     */
    public FieldGroup loadFromFile(Path sharedHeaderFile, Config config) {
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

            // Reuse the parser's parseSheet logic to ensure consistency
            return parser.parseSheet(headerSheet, "Shared Header");

        } catch (IOException e) {
            throw new ParseException("Failed to load Shared Header file: " + sharedHeaderFile, e);
        }
    }
}
