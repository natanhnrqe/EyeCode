package com.eyecode.project.template;

import java.io.File;
import java.io.IOException;
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
        return projectDir;
    }
}
