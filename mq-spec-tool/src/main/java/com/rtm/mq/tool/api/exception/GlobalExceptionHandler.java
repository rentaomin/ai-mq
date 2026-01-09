package com.rtm.mq.tool.api.exception;

import com.rtm.mq.tool.api.dto.ErrorResponse;
import com.rtm.mq.tool.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;

/**
 * Global exception handler for REST API.
 *
 * <p>Maps MqToolException hierarchy to HTTP status codes:</p>
 * <ul>
 *   <li>{@link ParseException} → 400 BAD_REQUEST</li>
 *   <li>{@link ValidationException} → 422 UNPROCESSABLE_ENTITY</li>
 *   <li>{@link GenerationException} → 500 INTERNAL_SERVER_ERROR</li>
 *   <li>{@link ConfigException} → 400 BAD_REQUEST</li>
 *   <li>{@link OutputException} → 500 INTERNAL_SERVER_ERROR</li>
 *   <li>{@link IllegalArgumentException} → 400 BAD_REQUEST</li>
 *   <li>Generic exceptions → 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Excel parsing errors.
     *
     * @param ex the parse exception
     * @param request the web request
     * @return error response with 400 status
     */
    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ErrorResponse> handleParseException(
            ParseException ex,
            WebRequest request) {
        logger.error("Parse error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "PARSE_ERROR",
                "Failed to parse Excel specification: " + ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles validation errors.
     *
     * @param ex the validation exception
     * @param request the web request
     * @return error response with 422 status
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            WebRequest request) {
        logger.error("Validation error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "VALIDATION_ERROR",
                "Validation failed: " + ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handles code generation errors.
     *
     * @param ex the generation exception
     * @param request the web request
     * @return error response with 500 status
     */
    @ExceptionHandler(GenerationException.class)
    public ResponseEntity<ErrorResponse> handleGenerationException(
            GenerationException ex,
            WebRequest request) {
        logger.error("Generation error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "GENERATION_ERROR",
                "Code generation failed: " + ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles configuration errors.
     *
     * @param ex the config exception
     * @param request the web request
     * @return error response with 400 status
     */
    @ExceptionHandler(ConfigException.class)
    public ResponseEntity<ErrorResponse> handleConfigException(
            ConfigException ex,
            WebRequest request) {
        logger.error("Configuration error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "CONFIG_ERROR",
                "Configuration error: " + ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles output/file I/O errors.
     *
     * @param ex the output exception
     * @param request the web request
     * @return error response with 500 status
     */
    @ExceptionHandler(OutputException.class)
    public ResponseEntity<ErrorResponse> handleOutputException(
            OutputException ex,
            WebRequest request) {
        logger.error("Output error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "OUTPUT_ERROR",
                "Failed to write output: " + ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles illegal argument errors (input validation).
     *
     * @param ex the illegal argument exception
     * @param request the web request
     * @return error response with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        logger.error("Illegal argument: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INPUT_VALIDATION_ERROR",
                ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles multipart file upload errors.
     *
     * @param ex the multipart exception
     * @param request the web request
     * @return error response with 400 status
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException ex,
            WebRequest request) {
        logger.error("Multipart upload error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "FILE_UPLOAD_ERROR",
                "File upload failed: " + ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles generic MqToolException.
     *
     * @param ex the MQ tool exception
     * @param request the web request
     * @return error response with appropriate status
     */
    @ExceptionHandler(MqToolException.class)
    public ResponseEntity<ErrorResponse> handleMqToolException(
            MqToolException ex,
            WebRequest request) {
        logger.error("MQ Tool error: {}", ex.getMessage(), ex);

        // Map exit code to HTTP status
        HttpStatus status = mapExitCodeToHttpStatus(ex.getExitCode());

        ErrorResponse error = new ErrorResponse(
                status.value(),
                "MQ_TOOL_ERROR",
                ex.getMessage(),
                getRequestPath(request)
        );

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handles all other unexpected exceptions.
     *
     * @param ex the exception
     * @param request the web request
     * @return error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support.",
                getRequestPath(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Maps CLI exit code to HTTP status code.
     *
     * @param exitCode the CLI exit code
     * @return corresponding HTTP status
     */
    private HttpStatus mapExitCodeToHttpStatus(int exitCode) {
        switch (exitCode) {
            case ExitCodes.SUCCESS:
                return HttpStatus.OK;
            case ExitCodes.INPUT_VALIDATION_ERROR:
            case ExitCodes.CLI_MISSING_COMMAND:
            case ExitCodes.CLI_UNKNOWN_COMMAND:
            case ExitCodes.CONFIG_ERROR:
                return HttpStatus.BAD_REQUEST;
            case ExitCodes.PARSE_ERROR:
                return HttpStatus.BAD_REQUEST;
            case ExitCodes.VALIDATION_ERROR:
                return HttpStatus.UNPROCESSABLE_ENTITY;
            case ExitCodes.GENERATION_ERROR:
            case ExitCodes.OUTPUT_ERROR:
            case ExitCodes.INTERNAL_ERROR:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Extracts request path from WebRequest.
     *
     * @param request the web request
     * @return request path
     */
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
