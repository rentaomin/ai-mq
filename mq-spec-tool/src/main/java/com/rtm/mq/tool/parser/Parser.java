package com.rtm.mq.tool.parser;

import com.rtm.mq.tool.model.MessageModel;
import java.nio.file.Path;

/**
 * Excel specification file parser interface.
 *
 * <p>This interface defines the contract for parsing MQ message specification
 * Excel files and producing a unified {@link MessageModel} representation.</p>
 *
 * <p>Implementations are expected to:</p>
 * <ul>
 *   <li>Parse request/response sheets from the spec file</li>
 *   <li>Optionally merge shared header definitions</li>
 *   <li>Preserve field ordering as defined in Excel</li>
 *   <li>Retain source metadata for auditability</li>
 * </ul>
 *
 * @see MessageModel
 */
public interface Parser {

    /**
     * Parses an Excel specification file.
     *
     * <p>This method reads the MQ message specification from the given Excel file
     * and optionally references field definitions from a standalone MQ message file.</p>
     *
     * @param specFile the path to the Excel specification file (must not be null)
     * @param mqMessageFile optional path to a standalone MQ message file;
     *                      may be null if no MQ message reference is needed.
     *                      NOTE: This file is NOT used for metadata extraction.
     * @return the parsed message model containing all field definitions and metadata
     * @throws com.rtm.mq.tool.exception.ParseException if parsing fails due to
     *         invalid file format, missing required sheets, or malformed data
     */
    MessageModel parse(Path specFile, Path mqMessageFile);
}
