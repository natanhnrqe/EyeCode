package com.eyecode.project.model;

import com.eyecode.project.ProjectDetector;
import com.eyecode.project.ProjectInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectModel {

    private final String name;
    private final Path rootDir;
    private final ProjectType type;
    private final BuildSystem buildSystem;
    private final List<ProjectModule> modules;
    private final boolean valid;
    private final Map<String, Object> attributes;

    private ProjectModel(Builder builder) {
        this.name = builder.name;
        this.rootDir = builder.rootDir;
        this.type = builder.type;
        this.buildSystem = builder.buildSystem;
        this.modules = Collections.unmodifiableList(new ArrayList<>(builder.modules));
        this.valid = builder.valid;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    public static ProjectModel fromDirectory(File root) {
        com.eyecode.project.ProjectType detected = ProjectDetector.detect(root);
        ProjectType type = TypeMapper.toProjectType(detected);
        BuildSystem buildSystem = TypeMapper.toBuildSystem(detected);
        boolean isValid = detected != com.eyecode.project.ProjectType.UNKNOWN;
        return new Builder()
                .name(root.getName())
                .rootDir(root.toPath())
                .type(type)
                .buildSystem(buildSystem)
                .valid(isValid)
                .build();
    }

    public static ProjectModel fromDirectory(File root, ProjectType type, BuildSystem buildSystem) {
        return new Builder()
                .name(root.getName())
                .rootDir(root.toPath())
                .type(type)
                .buildSystem(buildSystem)
                .valid(true)
                .build();
    }

    public ProjectInfo toInfo() {
        String path = rootDir.toAbsolutePath().normalize().toString();
        com.eyecode.project.ProjectType oldType = TypeMapper.toOldProjectType(type);
        return new ProjectInfo(name, path, oldType);
    }

    public String getName() { return name; }
    public Path getRootDir() { return rootDir; }
    public ProjectType getType() { return type; }
    public BuildSystem getBuildSystem() { return buildSystem; }
    public List<ProjectModule> getModules() { return modules; }
    public boolean isValid() { return valid; }
    public Map<String, Object> getAttributes() { return attributes; }

    public static class Builder {
        private String name;
        private Path rootDir;
        private ProjectType type = ProjectType.JAVA;
        private BuildSystem buildSystem = BuildSystem.NONE;
        private final List<ProjectModule> modules = new ArrayList<>();
        private boolean valid;
        private final Map<String, Object> attributes = new HashMap<>();

        public Builder name(String name) { this.name = name; return this; }
        public Builder rootDir(Path rootDir) { this.rootDir = rootDir; return this; }
        public Builder type(ProjectType type) { this.type = type; return this; }
        public Builder buildSystem(BuildSystem buildSystem) { this.buildSystem = buildSystem; return this; }
        public Builder addModule(ProjectModule module) { this.modules.add(module); return this; }
        public Builder modules(List<ProjectModule> modules) { this.modules.addAll(modules); return this; }
        public Builder valid(boolean valid) { this.valid = valid; return this; }
        public Builder attribute(String key, Object value) { this.attributes.put(key, value); return this; }
        public Builder attributes(Map<String, Object> attrs) { this.attributes.putAll(attrs); return this; }

        public ProjectModel build() {
            return new ProjectModel(this);
        }
    }
}
