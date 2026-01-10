package com.rtm.mq.tool.config;

/**
 * Configuration for Excel parser settings.
 */
public class ParserConfig {
    private int maxNestingDepth = 50;
    // POI ZipSecureFile configuration for handling compressed Excel files
    private long poiMaxTextSize = 500 * 1024 * 1024;  // Default: 500MB
    private double poiMinInflateRatio = 0.001;          // Default: 0.1% (0.1% uncompressed size required)

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int maxNestingDepth) {
        this.maxNestingDepth = maxNestingDepth;
    }

    public long getPoiMaxTextSize() {
        return poiMaxTextSize;
    }

    public void setPoiMaxTextSize(long poiMaxTextSize) {
        this.poiMaxTextSize = poiMaxTextSize;
    }

    public double getPoiMinInflateRatio() {
        return poiMinInflateRatio;
    }

    public void setPoiMinInflateRatio(double poiMinInflateRatio) {
        this.poiMinInflateRatio = poiMinInflateRatio;
    }

    public void setDefaults() {
        if (maxNestingDepth <= 0) {
            maxNestingDepth = 50;
        }
        if (poiMaxTextSize <= 0) {
            poiMaxTextSize = 500 * 1024 * 1024;
        }
        if (poiMinInflateRatio <= 0 || poiMinInflateRatio > 1) {
            poiMinInflateRatio = 0.001;
        }
    }

    public void merge(ParserConfig other) {
        if (other == null) {
            return;
        }
        if (other.maxNestingDepth > 0) {
            this.maxNestingDepth = other.maxNestingDepth;
        }
        if (other.poiMaxTextSize > 0) {
            this.poiMaxTextSize = other.poiMaxTextSize;
        }
        if (other.poiMinInflateRatio > 0 && other.poiMinInflateRatio <= 1) {
            this.poiMinInflateRatio = other.poiMinInflateRatio;
        }
    }
}
