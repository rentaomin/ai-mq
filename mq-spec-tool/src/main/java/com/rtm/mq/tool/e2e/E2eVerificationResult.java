package com.rtm.mq.tool.e2e;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the E2E verification result as specified in T-310.
 *
 * <p>This is a machine-readable only output format. It produces
 * deterministic JSON output with no narrative text.</p>
 *
 * <p>Output file: e2e-verification-result.json</p>
 */
public final class E2eVerificationResult {

    /** Overall verification passed. */
    private boolean passed;

    /** Exit code for the verification. */
    private int exitCode;

    /** Completeness verification result. */
    private VerificationDimension completeness;

    /** Determinism verification result. */
    private VerificationDimension determinism;

    /** Atomicity verification result. */
    private VerificationDimension atomicity;

    /** Audit integrity verification result. */
    private VerificationDimension auditIntegrity;

    /** CLI integration verification result. */
    private VerificationDimension cliIntegration;

    /**
     * Creates a new E2E verification result.
     */
    public E2eVerificationResult() {
        this.passed = true;
        this.exitCode = E2eExitCodes.E2E_VERIFICATION_PASS;
        this.completeness = new VerificationDimension("completeness");
        this.determinism = new VerificationDimension("determinism");
        this.atomicity = new VerificationDimension("atomicity");
        this.auditIntegrity = new VerificationDimension("auditIntegrity");
        this.cliIntegration = new VerificationDimension("cliIntegration");
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public VerificationDimension getCompleteness() {
        return completeness;
    }

    public void setCompleteness(VerificationDimension completeness) {
        this.completeness = completeness;
    }

    public VerificationDimension getDeterminism() {
        return determinism;
    }

    public void setDeterminism(VerificationDimension determinism) {
        this.determinism = determinism;
    }

    public VerificationDimension getAtomicity() {
        return atomicity;
    }

    public void setAtomicity(VerificationDimension atomicity) {
        this.atomicity = atomicity;
    }

    public VerificationDimension getAuditIntegrity() {
        return auditIntegrity;
    }

    public void setAuditIntegrity(VerificationDimension auditIntegrity) {
        this.auditIntegrity = auditIntegrity;
    }

    public VerificationDimension getCliIntegration() {
        return cliIntegration;
    }

    public void setCliIntegration(VerificationDimension cliIntegration) {
        this.cliIntegration = cliIntegration;
    }

    /**
     * Computes overall pass/fail and exit code based on dimensions.
     */
    public void computeOverallResult() {
        // Check each dimension in order; first failure determines exit code
        if (!completeness.isPassed()) {
            this.passed = false;
            this.exitCode = E2eExitCodes.E2E_COMPLETENESS_FAILED;
        } else if (!determinism.isPassed()) {
            this.passed = false;
            this.exitCode = E2eExitCodes.E2E_DETERMINISM_FAILED;
        } else if (!atomicity.isPassed()) {
            this.passed = false;
            this.exitCode = E2eExitCodes.E2E_ATOMICITY_FAILED;
        } else if (!auditIntegrity.isPassed()) {
            this.passed = false;
            this.exitCode = E2eExitCodes.E2E_AUDIT_INTEGRITY_FAILED;
        } else if (!cliIntegration.isPassed()) {
            this.passed = false;
            this.exitCode = E2eExitCodes.E2E_CLI_INTEGRATION_FAILED;
        } else {
            this.passed = true;
            this.exitCode = E2eExitCodes.E2E_VERIFICATION_PASS;
        }
    }

    /**
     * Converts the result to a deterministic map for JSON serialization.
     * Keys are ordered alphabetically for determinism.
     *
     * @return a LinkedHashMap with deterministic key ordering
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("atomicity", atomicity.toMap());
        map.put("auditIntegrity", auditIntegrity.toMap());
        map.put("cliIntegration", cliIntegration.toMap());
        map.put("completeness", completeness.toMap());
        map.put("determinism", determinism.toMap());
        map.put("exitCode", exitCode);
        map.put("passed", passed);
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        E2eVerificationResult that = (E2eVerificationResult) obj;
        return passed == that.passed
                && exitCode == that.exitCode
                && Objects.equals(completeness, that.completeness)
                && Objects.equals(determinism, that.determinism)
                && Objects.equals(atomicity, that.atomicity)
                && Objects.equals(auditIntegrity, that.auditIntegrity)
                && Objects.equals(cliIntegration, that.cliIntegration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passed, exitCode, completeness, determinism,
                atomicity, auditIntegrity, cliIntegration);
    }

    /**
     * Represents a single verification dimension result.
     */
    public static final class VerificationDimension {
        private final String name;
        private boolean passed;
        private final List<String> checks;

        public VerificationDimension(String name) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.passed = true;
            this.checks = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public List<String> getChecks() {
            return checks;
        }

        /**
         * Adds a check result.
         *
         * @param checkName the name of the check
         * @param checkPassed whether the check passed
         */
        public void addCheck(String checkName, boolean checkPassed) {
            checks.add(checkName + ":" + checkPassed);
            if (!checkPassed) {
                this.passed = false;
            }
        }

        /**
         * Converts to a deterministic map.
         *
         * @return map with deterministic ordering
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("checks", new ArrayList<>(checks));
            map.put("name", name);
            map.put("passed", passed);
            return map;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            VerificationDimension that = (VerificationDimension) obj;
            return passed == that.passed
                    && Objects.equals(name, that.name)
                    && Objects.equals(checks, that.checks);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, passed, checks);
        }
    }
}
