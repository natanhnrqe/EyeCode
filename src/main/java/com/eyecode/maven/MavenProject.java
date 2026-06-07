package com.eyecode.maven;

import java.util.ArrayList;
import java.util.List;

public class MavenProject {

    private String groupId;

    private String artifactId;

    private String version;

    private final List<MavenDependency> dependencies = new ArrayList<>();

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<MavenDependency> getDependencies() {
        return dependencies;
    }
}
