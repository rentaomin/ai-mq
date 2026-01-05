package com.rtm.mq.tool.parser;

import org.apache.poi.ss.usermodel.Sheet;

/**
 * Excel Sheet collection containing the discovered sheets from a workbook.
 *
 * <p>Holds references to:</p>
 * <ul>
 *   <li>Request sheet (required)</li>
 *   <li>Response sheet (required)</li>
 *   <li>Shared Header sheet (optional)</li>
 * </ul>
 */
public class SheetSet {
    private Sheet request;
    private Sheet response;
    private Sheet sharedHeader;

    /**
     * Gets the Request sheet.
     *
     * @return the Request sheet
     */
    public Sheet getRequest() {
        return request;
    }

    /**
     * Sets the Request sheet.
     *
     * @param request the Request sheet
     */
    public void setRequest(Sheet request) {
        this.request = request;
    }

    /**
     * Gets the Response sheet.
     *
     * @return the Response sheet
     */
    public Sheet getResponse() {
        return response;
    }

    /**
     * Sets the Response sheet.
     *
     * @param response the Response sheet
     */
    public void setResponse(Sheet response) {
        this.response = response;
    }

    /**
     * Gets the Shared Header sheet.
     *
     * @return the Shared Header sheet, or null if not present
     */
    public Sheet getSharedHeader() {
        return sharedHeader;
    }

    /**
     * Sets the Shared Header sheet.
     *
     * @param sharedHeader the Shared Header sheet
     */
    public void setSharedHeader(Sheet sharedHeader) {
        this.sharedHeader = sharedHeader;
    }

    /**
     * Checks if a Shared Header sheet is present.
     *
     * @return true if Shared Header sheet exists, false otherwise
     */
    public boolean hasSharedHeader() {
        return sharedHeader != null;
    }
}
