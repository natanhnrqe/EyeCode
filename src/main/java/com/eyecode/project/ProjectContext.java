package com.eyecode.project;

import java.io.File;

public class ProjectContext {

    private File projectRoot;

    public ProjectContext(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(File projectRoot) {
        this.projectRoot = projectRoot;
    }
}
