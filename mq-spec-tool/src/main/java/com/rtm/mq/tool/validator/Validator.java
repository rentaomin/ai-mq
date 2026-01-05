package com.rtm.mq.tool.validator;

import com.rtm.mq.tool.model.ValidationResult;
import java.nio.file.Path;

/**
 * Common interface for validators.
 *
 * <p>This interface defines the contract for all validation operations in the system.
 * Validators are used to check consistency, correctness, and compliance of various
 * artifacts including:</p>
 * <ul>
 *   <li>Excel specification files</li>
 *   <li>Generated XML bean definitions</li>
 *   <li>Generated Java model classes</li>
 *   <li>Generated OpenAPI YAML files</li>
 *   <li>Cross-artifact consistency</li>
 * </ul>
 *
 * @see ValidationResult
 */
public interface Validator {

    /**
     * Executes validation on the target path.
     *
     * <p>This method performs all validation checks appropriate for this validator
     * type and returns a comprehensive result containing any errors or warnings found.</p>
     *
     * <p>The target path may be:</p>
     * <ul>
     *   <li>A single file (e.g., an Excel spec or YAML file)</li>
     *   <li>A directory containing multiple files to validate</li>
     * </ul>
     *
     * @param targetPath the path to the file or directory to validate (must not be null)
     * @return a validation result containing pass/fail status and any issues found;
     *         never null
     * @throws com.rtm.mq.tool.exception.ValidationException if validation cannot be performed
     *         (distinct from validation failures, which are reported in the result)
     */
    ValidationResult validate(Path targetPath);

    /**
     * Returns the type identifier for this validator.
     *
     * <p>The type identifier is used for logging, configuration, and
     * distinguishing between different validator implementations.</p>
     *
     * <p>Example return values: "excel", "xml", "java", "openapi", "cross-artifact"</p>
     *
     * @return a non-null, non-empty string identifying this validator type
     */
    String getType();
}
