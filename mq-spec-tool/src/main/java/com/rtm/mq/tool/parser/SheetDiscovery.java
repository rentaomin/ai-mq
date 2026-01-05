package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel Sheet discovery service.
 *
 * <p>Discovers and validates the presence of required and optional sheets
 * in an Excel workbook containing MQ message specifications.</p>
 *
 * <p>Required sheets:</p>
 * <ul>
 *   <li>Request - Contains request message field definitions</li>
 * </ul>
 *
 * <p>Optional sheets:</p>
 * <ul>
 *   <li>Response - Contains response message field definitions (some messages are request-only)</li>
 *   <li>Shared Header - Contains common header field definitions</li>
 * </ul>
 */
public class SheetDiscovery {

    private static final String REQUEST_SHEET = "Request";
    private static final String RESPONSE_SHEET = "Response";
    private static final String SHARED_HEADER_SHEET = "Shared Header";

    /**
     * Discovers and validates sheets in the workbook.
     *
     * <p>This method validates that both required sheets (Request and Response)
     * exist and optionally includes the Shared Header sheet if present.</p>
     *
     * @param workbook the Excel workbook to discover sheets from
     * @return a SheetSet containing references to the discovered sheets
     * @throws ParseException if a required sheet (Request or Response) is not found
     */
    public SheetSet discoverSheets(Workbook workbook) {
        SheetSet sheets = new SheetSet();

        // Required: Request sheet
        sheets.setRequest(findSheet(workbook, REQUEST_SHEET));
        if (sheets.getRequest() == null) {
            throw new ParseException("Required sheet '" + REQUEST_SHEET + "' not found");
        }

        // Optional: Response sheet (some messages are request-only)
        sheets.setResponse(findSheet(workbook, RESPONSE_SHEET));

        // Optional: Shared Header sheet
        sheets.setSharedHeader(findSheet(workbook, SHARED_HEADER_SHEET));

        return sheets;
    }

    /**
     * Discovers the Shared Header sheet from a separate workbook.
     *
     * <p>This method is used when the Shared Header is provided as a separate file.</p>
     *
     * @param workbook the Shared Header workbook
     * @return the Shared Header sheet, or null if not found
     */
    public Sheet discoverSharedHeader(Workbook workbook) {
        return findSheet(workbook, SHARED_HEADER_SHEET);
    }

    /**
     * Finds a sheet by name, with case-insensitive fallback.
     *
     * <p>First attempts exact match, then falls back to case-insensitive search.</p>
     *
     * @param workbook the workbook to search
     * @param sheetName the expected sheet name
     * @return the found sheet, or null if not found
     */
    private Sheet findSheet(Workbook workbook, String sheetName) {
        // Try exact match first
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet != null) {
            return sheet;
        }

        // Fallback: case-insensitive search
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            if (name != null && name.equalsIgnoreCase(sheetName)) {
                return workbook.getSheetAt(i);
            }
        }

        return null;
    }
}
