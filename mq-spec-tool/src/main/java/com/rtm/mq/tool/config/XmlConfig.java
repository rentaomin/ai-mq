package com.rtm.mq.tool.config;

/**
 * Configuration for XML bean generation.
 *
 * <p>Contains namespace definitions and project metadata required
 * for generating Spring XML bean definitions.</p>
 */
public class XmlConfig {
    private NamespaceConfig namespace;
    private ProjectConfig project;

    public NamespaceConfig getNamespace() {
        return namespace;
    }

    public void setNamespace(NamespaceConfig namespace) {
        this.namespace = namespace;
    }

    public ProjectConfig getProject() {
        return project;
    }

    public void setProject(ProjectConfig project) {
        this.project = project;
    }

    public void setDefaults() {
        if (namespace == null) {
            namespace = new NamespaceConfig();
        }
        if (project == null) {
            project = new ProjectConfig();
        }
    }

    public void merge(XmlConfig other) {
        if (other == null) {
            return;
        }
        if (other.namespace != null) {
            if (this.namespace == null) {
                this.namespace = new NamespaceConfig();
            }
            this.namespace.merge(other.namespace);
        }
        if (other.project != null) {
            if (this.project == null) {
                this.project = new ProjectConfig();
            }
            this.project.merge(other.project);
        }
    }

    /**
     * Configuration for XML namespace definitions.
     */
    public static class NamespaceConfig {
        private String inbound;
        private String outbound;

        public String getInbound() {
            return inbound;
        }

        public void setInbound(String inbound) {
            this.inbound = inbound;
        }

        public String getOutbound() {
            return outbound;
        }

        public void setOutbound(String outbound) {
            this.outbound = outbound;
        }

        public void merge(NamespaceConfig other) {
            if (other == null) {
                return;
            }
            if (other.inbound != null && !other.inbound.isEmpty()) {
                this.inbound = other.inbound;
            }
            if (other.outbound != null && !other.outbound.isEmpty()) {
                this.outbound = other.outbound;
            }
        }
    }

    /**
     * Configuration for Maven project coordinates used in XML generation.
     */
    public static class ProjectConfig {
        private String groupId;
        private String artifactId;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public void merge(ProjectConfig other) {
            if (other == null) {
                return;
            }
            if (other.groupId != null && !other.groupId.isEmpty()) {
                this.groupId = other.groupId;
            }
            if (other.artifactId != null && !other.artifactId.isEmpty()) {
                this.artifactId = other.artifactId;
            }
        }
    }
}
