package com.rtm.mq.tool.api.service;

import com.rtm.mq.tool.model.MessageModel;
import com.rtm.mq.tool.model.ValidationError;
import com.rtm.mq.tool.model.ValidationResult;
import com.rtm.mq.tool.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Service layer for specification validation.
 *
 * <p>This service provides validation capabilities for MQ message specifications,
 * including Excel file validation and message model validation.</p>
 */
@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private final Parser parser;

    /**
     * Creates a new ValidationService with the injected parser.
     *
     * @param parser the Excel parser
     */
    public ValidationService(Parser parser) {
        this.parser = parser;
    }

    /**
     * Validates an Excel specification file.
     *
     * <p>This method attempts to parse the specification file and returns
     * a validation result indicating whether the file is valid.</p>
     *
     * @param specFile path to the Excel specification file
     * @param sharedHeaderFile optional path to a shared header file
     * @return validation result with status and any errors
     */
    public ValidationResult validateSpecFile(Path specFile, Path sharedHeaderFile) {
        logger.info("Validating spec file: {}", specFile);

        try {
            // Attempt to parse the file
            MessageModel model = parser.parse(specFile, sharedHeaderFile);

            // If parsing succeeds, validate the model
            ValidationResult result = validateModel(model);

            if (result.isSuccess()) {
                logger.info("Spec file validation passed");
            } else {
                logger.warn("Spec file validation failed: {} errors, {} warnings",
                        result.getErrors().size(), result.getWarnings().size());
            }

            return result;

        } catch (Exception e) {
            logger.error("Error during validation: {}", e.getMessage(), e);
            ValidationResult result = new ValidationResult();
            result.addError(new ValidationError("VR-001", "Validation error", e.getMessage()));
            return result;
        }
    }

    /**
     * Validates a parsed message model.
     *
     * <p>This method performs semantic validation on the message model,
     * including structure validation and consistency checks.</p>
     *
     * @param model the message model to validate
     * @return validation result with status and any issues
     */
    public ValidationResult validateModel(MessageModel model) {
        logger.info("Validating message model");

        ValidationResult result = new ValidationResult();

        try {
            // Validate metadata
            if (model.getMetadata() == null) {
                result.addError(new ValidationError("VR-002", "Message metadata is missing", ""));
            } else {
                validateMetadata(model, result);
            }

            // Validate request and response
            if (model.getRequest() == null || model.getRequest().getFields() == null
                    || model.getRequest().getFields().isEmpty()) {
                result.addWarning(new ValidationError("VR-003", "Request definition is empty", "",
                        ValidationError.Severity.WARN));
            }

            if (model.getResponse() == null || model.getResponse().getFields() == null
                    || model.getResponse().getFields().isEmpty()) {
                result.addWarning(new ValidationError("VR-004", "Response definition is empty", "",
                        ValidationError.Severity.WARN));
            }

            return result;

        } catch (Exception e) {
            logger.error("Error during model validation: {}", e.getMessage(), e);
            result.addError(new ValidationError("VR-005", "Model validation error", e.getMessage()));
            return result;
        }
    }

    /**
     * Validates message metadata.
     *
     * @param model the message model
     * @param result the result object to accumulate errors/warnings
     */
    private void validateMetadata(MessageModel model, ValidationResult result) {
        var metadata = model.getMetadata();

        if (metadata.getOperationId() == null || metadata.getOperationId().isBlank()) {
            result.addError(new ValidationError("VR-006", "Operation ID is required", ""));
        }

        if (metadata.getVersion() == null || metadata.getVersion().isBlank()) {
            result.addWarning(new ValidationError("VR-007", "Version information is recommended", "",
                    ValidationError.Severity.WARN));
        }
    }
}
