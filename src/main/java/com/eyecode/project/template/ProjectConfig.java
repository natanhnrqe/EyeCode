package com.eyecode.project.template;

import java.io.File;

public class ProjectConfig {
    private String name;
    private File location;
    private String packageBase;
    private ProjectTemplate template;
    private String jdkVersion;
    private String description;
    private boolean initGit;
    private boolean generateReadme;

    public ProjectConfig() {
        this.jdkVersion = "21";
        this.initGit = true;
        this.generateReadme = true;
    }

    public ProjectConfig(String name, File location, String packageBase, ProjectTemplate template) {
        this();
        this.name = name;
        this.location = location;
        this.packageBase = packageBase;
        this.template = template;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public File getLocation() { return location; }
    public void setLocation(File location) { this.location = location; }

    public String getPackageBase() { return packageBase; }
    public void setPackageBase(String packageBase) { this.packageBase = packageBase; }

    public ProjectTemplate getTemplate() { return template; }
    public void setTemplate(ProjectTemplate template) { this.template = template; }

    public String getJdkVersion() { return jdkVersion; }
    public void setJdkVersion(String v) { this.jdkVersion = v; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public boolean isInitGit() { return initGit; }
    public void setInitGit(boolean b) { this.initGit = b; }

    public boolean isGenerateReadme() { return generateReadme; }
    public void setGenerateReadme(boolean b) { this.generateReadme = b; }

    public String getPackagePath() {
        return packageBase == null ? "" : packageBase.replace('.', File.separatorChar);
    }

    public String validate() {
        if (name == null || name.isBlank()) return "Project name is required.";
        if (name.contains(" ")) return "Project name must not contain spaces.";
        if (location == null) return "Project location is required.";
        if (packageBase == null || packageBase.isBlank()) return "Package base is required.";
        if (!packageBase.matches("[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*"))
            return "Invalid package syntax. Use lowercase identifiers separated by dots.";
        if (template == null) return "No project type selected.";
        return null;
    }
}
