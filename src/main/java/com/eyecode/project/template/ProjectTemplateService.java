package com.eyecode.project.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ProjectTemplateService {

    private final List<ProjectTemplate> templates;

    public ProjectTemplateService() {
        templates = new ArrayList<>();
        templates.add(new JavaTemplate());
        templates.add(new MavenTemplate());
        templates.add(new GradleTemplate());
        templates.add(new SpringBootTemplate());
    }

    public List<ProjectTemplate> getTemplates() {
        return new ArrayList<>(templates);
    }

    private void generateReadme(File projectDir, ProjectConfig config) throws IOException {
        String nl = System.lineSeparator();
        String desc = config.getDescription() != null && !config.getDescription().isBlank()
                ? config.getDescription() : "A project created with EyeCode.";
        StringBuilder readme = new StringBuilder();
        readme.append("# ").append(config.getName()).append(nl).append(nl);
        readme.append(desc).append(nl).append(nl);
        readme.append("## Technology Stack").append(nl).append(nl);
        readme.append("- Java ").append(config.getJdkVersion()).append(nl);
        readme.append("- ").append(config.getTemplate().getDisplayName()).append(nl).append(nl);
        readme.append("## Project Structure").append(nl).append(nl);
        readme.append("```").append(nl);
        readme.append("src/").append(nl);
        readme.append("  main/").append(nl);
        readme.append("    java/").append(nl);
        readme.append("    resources/").append(nl);
        readme.append("  test/").append(nl);
        readme.append("    java/").append(nl);
        readme.append("```").append(nl).append(nl);
        readme.append("Created with [EyeCode](https://github.com/anomalyco/opencode).").append(nl);

        File readmeFile = new File(projectDir, "README.md");
        Files.writeString(readmeFile.toPath(), readme.toString());
    }

    public ProjectTemplate findByType(com.eyecode.project.ProjectType type) {
        return templates.stream()
                .filter(t -> t.getType() == type)
                .findFirst()
                .orElse(null);
    }

    public File createProject(ProjectConfig config) throws IOException {
        File projectDir = new File(config.getLocation(), config.getName());
        if (projectDir.exists()) {
            throw new IOException("Project directory already exists: " + projectDir.getAbsolutePath());
        }
        projectDir.mkdirs();
        config.getTemplate().generate(projectDir, config);

        if (config.isGenerateReadme()) {
            generateReadme(projectDir, config);
        }

        return projectDir;
    }
}
