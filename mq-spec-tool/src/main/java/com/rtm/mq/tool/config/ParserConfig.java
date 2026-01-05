package com.rtm.mq.tool.config;

/**
 * Configuration for Excel parser settings.
 */
public class ParserConfig {
    private int maxNestingDepth = 50;

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        this.maxNestingDepth = maxNestingDepth;
    }

    public void setDefaults() {
        if (maxNestingDepth <= 0) {
            maxNestingDepth = 50;
        }
    }

    public void merge(ParserConfig other) {
        if (other == null) {
            return;
        }
        if (other.maxNestingDepth > 0) {
            this.maxNestingDepth = other.maxNestingDepth;
        }
    }
}
