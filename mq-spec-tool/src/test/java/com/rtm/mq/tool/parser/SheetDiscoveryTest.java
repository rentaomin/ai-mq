package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.exception.ParseException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SheetDiscovery.
 */
class SheetDiscoveryTest {

    private SheetDiscovery discovery;
    private Workbook workbook;

    @BeforeEach
    void setUp() {
        discovery = new SheetDiscovery();
        workbook = new XSSFWorkbook();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (workbook != null) {
            workbook.close();
        }
    }

    @Test
    @DisplayName("Should discover all sheets when all are present")
    void discoverSheets_allSheetsPresent_returnsSheetSet() {
        // Given
        workbook.createSheet("Request");
        workbook.createSheet("Response");
        workbook.createSheet("Shared Header");

        // When
        SheetSet result = discovery.discoverSheets(workbook);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRequest());
        assertNotNull(result.getResponse());
        assertNotNull(result.getSharedHeader());
        assertTrue(result.hasSharedHeader());
    }

    @Test
    @DisplayName("Should discover required sheets without Shared Header")
    void discoverSheets_noSharedHeader_returnsSheetSetWithoutSharedHeader() {
        // Given
        workbook.createSheet("Request");
        workbook.createSheet("Response");

        // When
        SheetSet result = discovery.discoverSheets(workbook);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRequest());
        assertNotNull(result.getResponse());
        assertNull(result.getSharedHeader());
        assertFalse(result.hasSharedHeader());
    }

    @Test
    @DisplayName("Should throw ParseException when Request sheet is missing")
    void discoverSheets_requestMissing_throwsParseException() {
        // Given
        workbook.createSheet("Response");
        workbook.createSheet("Shared Header");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> discovery.discoverSheets(workbook));
        assertTrue(exception.getMessage().contains("Request"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should throw ParseException when Response sheet is missing")
    void discoverSheets_responseMissing_throwsParseException() {
        // Given
        workbook.createSheet("Request");
        workbook.createSheet("Shared Header");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> discovery.discoverSheets(workbook));
        assertTrue(exception.getMessage().contains("Response"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should discover sheets with case-insensitive matching")
    void discoverSheets_caseInsensitive_findsSheets() {
        // Given - different cases
        workbook.createSheet("REQUEST");
        workbook.createSheet("response");

        // When
        SheetSet result = discovery.discoverSheets(workbook);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRequest());
        assertNotNull(result.getResponse());
    }

    @Test
    @DisplayName("Should discover Shared Header from separate workbook")
    void discoverSharedHeader_sheetPresent_returnsSheet() throws Exception {
        // Given
        try (Workbook sharedHeaderWorkbook = new XSSFWorkbook()) {
            sharedHeaderWorkbook.createSheet("Shared Header");

            // When
            Sheet result = discovery.discoverSharedHeader(sharedHeaderWorkbook);

            // Then
            assertNotNull(result);
            assertEquals("Shared Header", result.getSheetName());
        }
    }

    @Test
    @DisplayName("Should return null when Shared Header not found in separate workbook")
    void discoverSharedHeader_sheetMissing_returnsNull() throws Exception {
        // Given
        try (Workbook otherWorkbook = new XSSFWorkbook()) {
            otherWorkbook.createSheet("Other Sheet");

            // When
            Sheet result = discovery.discoverSharedHeader(otherWorkbook);

            // Then
            assertNull(result);
        }
    }

    @Test
    @DisplayName("Should handle mixed case sheet names")
    void discoverSheets_mixedCase_findsCorrectSheets() {
        // Given - mixed case sheet names
        workbook.createSheet("ReQuEsT");
        workbook.createSheet("RESPONSE");
        workbook.createSheet("shared header");

        // When
        SheetSet result = discovery.discoverSheets(workbook);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRequest());
        assertNotNull(result.getResponse());
        assertNotNull(result.getSharedHeader());
        assertEquals("ReQuEsT", result.getRequest().getSheetName());
        assertEquals("RESPONSE", result.getResponse().getSheetName());
        assertEquals("shared header", result.getSharedHeader().getSheetName());
    }

    @Test
    @DisplayName("Should throw ParseException when both Request and Response are missing")
    void discoverSheets_bothMissing_throwsParseExceptionForRequest() {
        // Given - empty workbook with only other sheets
        workbook.createSheet("Other Sheet");

        // When & Then
        ParseException exception = assertThrows(ParseException.class,
            () -> discovery.discoverSheets(workbook));
        // Request is checked first, so that error is thrown first
        assertTrue(exception.getMessage().contains("Request"));
    }
}
