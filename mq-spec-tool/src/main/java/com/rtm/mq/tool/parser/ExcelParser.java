package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.config.Config;
import com.rtm.mq.tool.exception.ParseException;
import com.rtm.mq.tool.model.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final SharedHeaderLoader sharedHeaderLoader;

    /**
     * Creates an ExcelParser with the specified configuration.
     *
     * @param config the configuration containing parser settings
     */
    public ExcelParser(Config config) {
        this.config = config;
        this.sheetDiscovery = new SheetDiscovery();
        this.metadataExtractor = new MetadataExtractor();
        this.sharedHeaderLoader = new SharedHeaderLoader(this);
    }

    @Override
    public MessageModel parse(Path specFile, Path sharedHeaderFile) {
        validateInputFile(specFile);
        if (sharedHeaderFile != null) {
            validateInputFile(sharedHeaderFile);
        }

        try (InputStream is = Files.newInputStream(specFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            MessageModel model = new MessageModel();

            // 1. Discover Sheets
            SheetSet sheets = sheetDiscovery.discoverSheets(workbook);

            // 2. Extract metadata
            Metadata metadata = metadataExtractor.extract(
                sheets.getRequest(), specFile, sharedHeaderFile);
            model.setMetadata(metadata);

            // 3. Parse Shared Header
            FieldGroup sharedHeader = parseSharedHeader(sheets, sharedHeaderFile);
            model.setSharedHeader(sharedHeader);

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
     * Parses the Shared Header.
     *
     * <p>The Shared Header can come from three sources (in priority order):</p>
     * <ol>
     *   <li>A separate shared header file (if provided)</li>
     *   <li>An embedded "Shared Header" sheet in the main file</li>
     *   <li>Empty (if neither exists)</li>
     * </ol>
     *
     * @param sheets the discovered sheets from the main file
     * @param sharedHeaderFile optional path to separate shared header file
     * @return the parsed FieldGroup for the shared header
     */
    private FieldGroup parseSharedHeader(SheetSet sheets, Path sharedHeaderFile) {
        if (sharedHeaderFile != null) {
            return sharedHeaderLoader.loadFromFile(sharedHeaderFile, config);
        } else if (sheets.hasSharedHeader()) {
            return parseSheet(sheets.getSharedHeader(), "Shared Header");
        } else {
            return new FieldGroup();  // Empty header
        }
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
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new ParseException("Input file is not an Excel file: " + file);
        }
    }
}
