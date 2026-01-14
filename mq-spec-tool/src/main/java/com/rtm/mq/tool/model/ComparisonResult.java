package com.rtm.mq.tool.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of comparing MQ message fields with request/response fields.
 *
 * <p>Contains four categories of comparison results:</p>
 * <ul>
 *   <li>Matches: Fields present in both MQ message and target</li>
 *   <li>Missing: Fields in MQ message but not in target</li>
 *   <li>Extra: Fields in target but not in MQ message</li>
 *   <li>Differences: Fields with same name but different properties</li>
 * </ul>
 */
public class ComparisonResult {

    /** Fields present in both MQ message and target */
    private List<FieldMatch> matches = new ArrayList<>();

    /** Fields in MQ message but not in target */
    private List<FieldNode> missingInTarget = new ArrayList<>();

    /** Fields in target but not in MQ message */
    private List<FieldNode> extraInTarget = new ArrayList<>();

    /** Fields with same name but different properties */
    private List<FieldDifference> differences = new ArrayList<>();

    /**
     * Gets the list of matching fields.
     *
     * @return list of FieldMatch objects
     */
    public List<FieldMatch> getMatches() {
        return matches;
    }

    /**
     * Sets the list of matching fields.
     *
     * @param matches the list to set
     */
    public void setMatches(List<FieldMatch> matches) {
        this.matches = matches != null ? matches : new ArrayList<>();
    }

    /**
     * Gets the list of missing fields.
     *
     * @return list of FieldNode objects in MQ message but not in target
     */
    public List<FieldNode> getMissingInTarget() {
        return missingInTarget;
    }

    /**
     * Sets the list of missing fields.
     *
     * @param missingInTarget the list to set
     */
    public void setMissingInTarget(List<FieldNode> missingInTarget) {
        this.missingInTarget = missingInTarget != null ? missingInTarget : new ArrayList<>();
    }

    /**
     * Gets the list of extra fields.
     *
     * @return list of FieldNode objects in target but not in MQ message
     */
    public List<FieldNode> getExtraInTarget() {
        return extraInTarget;
    }

    /**
     * Sets the list of extra fields.
     *
     * @param extraInTarget the list to set
     */
    public void setExtraInTarget(List<FieldNode> extraInTarget) {
        this.extraInTarget = extraInTarget != null ? extraInTarget : new ArrayList<>();
    }

    /**
     * Gets the list of field differences.
     *
     * @return list of FieldDifference objects
     */
    public List<FieldDifference> getDifferences() {
        return differences;
    }

    /**
     * Sets the list of field differences.
     *
     * @param differences the list to set
     */
    public void setDifferences(List<FieldDifference> differences) {
        this.differences = differences != null ? differences : new ArrayList<>();
    }

    /**
     * Gets the total number of comparisons.
     *
     * @return count of all matched, missing, extra, and different fields
     */
    public int getTotalComparisons() {
        return matches.size() + missingInTarget.size() + extraInTarget.size() + differences.size();
    }

    /**
     * Gets the match rate percentage.
     *
     * @return match rate as percentage (0-100), or 0 if no total comparisons
     */
    public double getMatchRate() {
        int total = getTotalComparisons();
        if (total == 0) {
            return 0;
        }
        int exactMatches = matches.size();
        return (exactMatches * 100.0) / total;
    }

    /**
     * Checks if comparison is successful (no missing or extra fields).
     *
     * @return true if all fields match without missing or extra fields
     */
    public boolean isFullMatch() {
        return missingInTarget.isEmpty() && extraInTarget.isEmpty() && differences.isEmpty();
    }

    @Override
    public String toString() {
        return String.format(
                "ComparisonResult{matches=%d, missing=%d, extra=%d, differences=%d, matchRate=%.1f%%}",
                matches.size(),
                missingInTarget.size(),
                extraInTarget.size(),
                differences.size(),
                getMatchRate()
        );
    }
}
