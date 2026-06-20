package com.eyecode.project.wizard;

import com.eyecode.project.template.ProjectTemplate;
import java.io.File;

public class WizardState {

    private ProjectTemplate template;
    private String projectName;
    private String location;
    private String packageBase;
    private String jdkVersion;
    private String description;
    private boolean initGit;
    private boolean generateReadme;

    public WizardState() {
        this.jdkVersion = "21";
        this.initGit = true;
        this.generateReadme = true;
    }

    public ProjectTemplate getTemplate() { return template; }
    public void setTemplate(ProjectTemplate t) { this.template = t; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String s) { this.projectName = s; }

    public String getLocation() { return location; }
    public void setLocation(String s) { this.location = s; }

    public String getPackageBase() { return packageBase; }
    public void setPackageBase(String s) { this.packageBase = s; }

    public String getJdkVersion() { return jdkVersion; }
    public void setJdkVersion(String s) { this.jdkVersion = s; }

    public String getDescription() { return description; }
    public void setDescription(String s) { this.description = s; }

    public boolean isInitGit() { return initGit; }
    public void setInitGit(boolean b) { this.initGit = b; }

    public boolean isGenerateReadme() { return generateReadme; }
    public void setGenerateReadme(boolean b) { this.generateReadme = b; }

    public File getProjectDir() {
        return new File(location, projectName);
    }
}
