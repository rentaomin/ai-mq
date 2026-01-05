package com.rtm.mq.tool.config;

/**
 * Configuration for output directories and file generation settings.
 */
public class OutputConfig {
    private String rootDir = "./output";

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public void setDefaults() {
        if (rootDir == null || rootDir.isEmpty()) {
            rootDir = "./output";
        }
    }

    public void merge(OutputConfig other) {
        if (other == null) {
            return;
        }
        if (other.rootDir != null && !other.rootDir.isEmpty()) {
            this.rootDir = other.rootDir;
        }
    }
}
