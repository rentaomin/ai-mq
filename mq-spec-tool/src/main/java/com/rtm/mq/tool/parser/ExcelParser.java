package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.config.ParserConfig;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.*;
import com.rtm.mq.tool.version.VersionRegistry;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Excel specification file parser.
 * Integrates all parsing components to implement the complete parsing flow.
 *
 * <p>This parser coordinates the following steps:</p>
 * <ol>
 *   <li>Sheet discovery and validation</li>
 *   <li>Metadata extraction from header rows</li>
 *   <li>Shared header loading (from file or embedded sheet)</li>
 *   <li>Request/Response sheet parsing</li>
 *   <li>Field hierarchy construction via Seg lvl nesting</li>
 *   <li>Object/array detection and enhancement</li>
 *   <li>CamelCase naming normalization</li>
 *   <li>Duplicate field detection</li>
 * </ol>
 *
 * @see Parser
 * @see SheetDiscovery
 * @see MetadataExtractor
 * @see SegLevelParser
 * @see ObjectArrayDetector
 * @see CamelCaseConverter
 * @see DuplicateDetector
 */
public class ExcelParser implements Parser {

    private static final int HEADER_ROW_INDEX = 7;  // Row 8 (0-indexed)

    private final Config config;
    private final SheetDiscovery sheetDiscovery;
    private final MetadataExtractor metadataExtractor;
    private final MqMessageLoader mqMessageLoader;

    /**
     * Creates an ExcelParser with the specified configuration.
     *
     * @param config the configuration containing parser settings
     */
    public ExcelParser(Config config) {
        this.config = config;
        this.sheetDiscovery = new SheetDiscovery();
        this.metadataExtractor = new MetadataExtractor();
        this.mqMessageLoader = new MqMessageLoader(this);
    }

    @Override
    public MessageModel parse(Path specFile, Path mqMessageFile) {
        validateInputFile(specFile);
        if (mqMessageFile != null) {
            validateInputFile(mqMessageFile);
        }

        // Configure POI ZipSecureFile to handle larger files with higher compression ratios
        configureZipSecureFile();

        try (InputStream is = Files.newInputStream(specFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            MessageModel model = new MessageModel();

            // 1. Discover Sheets
            SheetSet sheets = sheetDiscovery.discoverSheets(workbook);

            // 2. Extract metadata (DO NOT use mqMessageFile - it has no metadata)
            Metadata metadata = parseMetadataWithoutMqMessage(sheets, specFile);
            model.setMetadata(metadata);

            // 3. Parse Standalone MQ Message File (if provided)
            MqMessageModel mqMessage = parseMqMessageFile(mqMessageFile);
            model.setMqMessage(mqMessage);

            // 4. Parse Request
            FieldGroup request = parseSheet(sheets.getRequest(), "Request");
            model.setRequest(request);

            // 5. Parse Response
            FieldGroup response = parseSheet(sheets.getResponse(), "Response");
            model.setResponse(response);

            return model;

        } catch (IOException e) {
            throw new ParseException("Failed to read Excel file: " + specFile, e);
        }
    }

    /**
     * Parses metadata without consulting MQ message file.
     *
     * <p>Metadata extraction priority:</p>
     * <ol>
     *   <li>Request Sheet (if present)</li>
     *   <li>Embedded Shared Header Sheet (if Request lacks metadata)</li>
     *   <li>Empty Metadata</li>
     * </ol>
     *
     * <p><strong>IMPORTANT:</strong> MQ message file is NOT used for metadata extraction.</p>
     *
     * @param sheets the discovered sheets from the main file
     * @param specFile path to the main specification file
     * @return extracted Metadata, or empty Metadata if all sources fail
     */
    private Metadata parseMetadataWithoutMqMessage(SheetSet sheets, Path specFile) {
        Metadata metadata = null;


        // Priority 1: Fallback to embedded Shared Header Sheet (if present)
        if (sheets.hasSharedHeader()) {
            metadata = metadataExtractor.extractSafely(sheets.getSharedHeader(), specFile);
            if (metadataExtractor.validate(metadata)) {
                return metadata;
            }
        }
        
        // Priority 2: Extract from Request Sheet (if present)
        if (sheets.getRequest() != null) {
            metadata = metadataExtractor.extract(sheets.getRequest(), specFile);
            if (metadataExtractor.validate(metadata)) {
                return metadata;
            }
        }

        // Priority 3: Return empty metadata
        if (metadata == null) {
            metadata = new Metadata();
            metadata.setSourceFile(specFile.toAbsolutePath().toString());
            metadata.setParseTimestamp(Instant.now().toString());
            metadata.setParserVersion(VersionRegistry.getParserVersion());
        }

        return metadata;
    }

    /**
     * Parses embedded Shared Header sheet from the main spec file.
     *
     * <p>This is DISTINCT from standalone MQ message files.
     * Embedded shared headers are part of the request/response spec file.</p>
     *
     * <p>The embedded Shared Header sheet is expected to have the same structure
     * as Request/Response sheets:</p>
     * <ul>
     *   <li>Rows 1-7: Metadata (Operation Name, ID, Version, etc.)</li>
     *   <li>Row 8: Column header row</li>
     *   <li>Rows 9+: Field definitions</li>
     * </ul>
     *
     * <p>If the Shared Header sheet contains only metadata rows (1-7) without
     * field definitions, returns an empty FieldGroup.</p>
     *
     * @param sheets the discovered sheets from the main file
     * @return the parsed FieldGroup containing shared header fields, or empty if none found
     */
    private FieldGroup parseEmbeddedSharedHeader(SheetSet sheets) {
        if (!sheets.hasSharedHeader()) {
            return new FieldGroup();
        }

        Sheet sharedHeaderSheet = sheets.getSharedHeader();

        // Check if header row (row 8, 0-indexed as 7) exists and has valid columns
        Row headerRow = sharedHeaderSheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            // No header row found - Shared Header contains only metadata rows (1-7)
            // Return empty FieldGroup as there are no field definitions to parse
            return new FieldGroup();
        }

        // Validate that at least one required column exists before parsing
        // This prevents unnecessary parsing attempts when header row exists but is empty
        if (isEmptyHeaderRow(headerRow)) {
            return new FieldGroup();
        }

        // Parse sheet with valid header row and field definitions
        return parseSheet(sharedHeaderSheet, "Shared Header");
    }

    /**
     * Checks if a header row is empty (contains no recognized column names).
     *
     * @param headerRow the header row to check
     * @return true if the header row has no cells or all cells are empty
     */
    private boolean isEmptyHeaderRow(Row headerRow) {
        if (headerRow == null) {
            return true;
        }

        // Check if any cell in the header row contains non-empty content
        for (Cell cell : headerRow) {
            if (cell != null) {
                String cellValue = getCellStringValue(cell);
                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Extracts string value from a cell, handling different cell types.
     *
     * @param cell the cell to read
     * @return the cell value as a string, or null if empty/unsupported
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((int) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    /**
     * Parses standalone MQ message file (if provided).
     *
     * <p>MQ message files are independent field definitions used for
     * comparison and validation purposes. They do not contain metadata.</p>
     *
     * @param mqMessageFile path to the MQ message file, or null if not provided
     * @return populated MqMessageModel, or null if no file provided
     * @throws ParseException if file cannot be read or parsed
     */
    private MqMessageModel parseMqMessageFile(Path mqMessageFile) {
        if (mqMessageFile != null) {
            return mqMessageLoader.loadFromFile(mqMessageFile, config);
        } else {
            return null;
        }
    }

    /**
     * Parses a sheet into a FieldGroup.
     *
     * <p>This is the main parsing logic that coordinates all parsing components
     * for a single sheet (Request, Response, or Shared Header).</p>
     *
     * <p>If the sheet is null, returns an empty FieldGroup. This supports
     * messages that may not have a Response sheet.</p>
     *
     * @param sheet the Excel sheet to parse, may be null
     * @param sheetName the sheet name for error reporting
     * @return the parsed FieldGroup containing the hierarchical field structure
     */
    public FieldGroup parseSheet(Sheet sheet, String sheetName) {
        // Handle null sheet - return empty FieldGroup
        if (sheet == null) {
            return new FieldGroup();
        }

        // 1. Validate columns
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        ColumnValidator columnValidator = new ColumnValidator();
        Map<String, Integer> columnMap = columnValidator.validateAndMapColumns(headerRow, sheetName);

        // 2. Create parsers
        NestingDepthValidator depthValidator = new NestingDepthValidator(
            config.getParser().getMaxNestingDepth());
        SegLevelParser segLevelParser = new SegLevelParser(columnMap, sheetName, depthValidator);
        ObjectArrayDetector objectArrayDetector = new ObjectArrayDetector(columnMap);
        CamelCaseConverter camelCaseConverter = new CamelCaseConverter();
        DuplicateDetector duplicateDetector = new DuplicateDetector();

        // 3. Parse fields
        List<FieldNode> fields = segLevelParser.parseFields(sheet);

        // 4. Enhance fields (object/array detection, naming normalization)
        enhanceFields(fields, sheet, columnMap, objectArrayDetector, camelCaseConverter);

        // 5. Detect duplicates
        duplicateDetector.detectDuplicates(fields);

        FieldGroup group = new FieldGroup();
        group.setFields(fields);
        return group;
    }

    /**
     * Enhances field list recursively.
     *
     * <p>Enhancement includes:</p>
     * <ul>
     *   <li>Object/array detection based on field format and occurrenceCount</li>
     *   <li>CamelCase naming for Java bean properties</li>
     * </ul>
     *
     * @param fields the list of fields to enhance
     * @param sheet the Excel sheet (for retrieving row data)
     * @param columnMap the column name to index mapping
     * @param detector the object/array detector
     * @param converter the camelCase converter
     */
    private void enhanceFields(List<FieldNode> fields, Sheet sheet,
                               Map<String, Integer> columnMap,
                               ObjectArrayDetector detector,
                               CamelCaseConverter converter) {
        for (int i = 0; i < fields.size(); i++) {
            FieldNode node = fields.get(i);

            // Get corresponding row (convert 1-based rowIndex back to 0-indexed)
            Row row = sheet.getRow(node.getSource().getRowIndex() - 1);

            // Object/array detection
            FieldNode enhanced = detector.detect(node, row);

            // Naming normalization (non-transitory fields only)
            if (!enhanced.isTransitory() && enhanced.getCamelCaseName() == null) {
                String camelName;
                if (enhanced.isObject() || enhanced.isArray()) {
                    // Object/array uses the fieldName part from ObjectDefinition
                    ObjectDefinition objDef = detector.parseObjectDefinition(enhanced.getOriginalName());
                    camelName = converter.toCamelCase(objDef.getFieldName());
                } else {
                    camelName = converter.toCamelCase(enhanced.getOriginalName());
                }
                enhanced = rebuildWithCamelName(enhanced, camelName);
            }

            fields.set(i, enhanced);

            // Recursively enhance children
            if (!enhanced.getChildren().isEmpty()) {
                enhanceFields(enhanced.getChildren(), sheet, columnMap, detector, converter);
            }
        }
    }

    /**
     * Rebuilds a FieldNode with an added camelCaseName.
     *
     * <p>Since FieldNode properties are set via builder, we need to rebuild
     * the node to add the camelCaseName property.</p>
     *
     * @param node the original node
     * @param camelName the camelCase name to set
     * @return a new FieldNode with the camelCaseName set
     */
    private FieldNode rebuildWithCamelName(FieldNode node, String camelName) {
        return FieldNode.builder()
            .originalName(node.getOriginalName())
            .camelCaseName(camelName)
            .className(node.getClassName())
            .segLevel(node.getSegLevel())
            .length(node.getLength())
            .dataType(node.getDataType())
            .optionality(node.getOptionality())
            .defaultValue(node.getDefaultValue())
            .hardCodeValue(node.getHardCodeValue())
            .groupId(node.getGroupId())
            .occurrenceCount(node.getOccurrenceCount())
            .isArray(node.isArray())
            .isObject(node.isObject())
            .isTransitory(node.isTransitory())
            .children(node.getChildren())
            .source(node.getSource())
            .build();
    }

    /**
     * Configures POI ZipSecureFile settings for handling compressed Excel files.
     *
     * <p>This method applies settings from ParserConfig to allow processing of
     * larger or more highly compressed Excel files.</p>
     */
    private void configureZipSecureFile() {
        ParserConfig parserConfig = config.getParser();
        ZipSecureFile.setMaxTextSize(parserConfig.getPoiMaxTextSize());
        ZipSecureFile.setMinInflateRatio(parserConfig.getPoiMinInflateRatio());
    }

    /**
     * Validates input file exists, is readable, and has correct extension.
     *
     * @param file the file path to validate
     * @throws ParseException if validation fails
     */
    private void validateInputFile(Path file) {
        if (file == null) {
            throw new ParseException("Input file path is null");
        }
        if (!Files.exists(file)) {
            throw new ParseException("Input file not found: " + file);
        }
        if (!Files.isReadable(file)) {
            throw new ParseException("Input file is not readable: " + file);
        }
        String fileName = file.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls") && !fileName.endsWith(".xlsm")) {
            throw new ParseException("Input file is not an Excel file: " + file);
        }
    }
}
