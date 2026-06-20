package com.eyecode.project.template;

import com.eyecode.project.ProjectType;
import com.eyecode.ui.designsystem.IconManager;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class JavaTemplate implements ProjectTemplate {

    @Override
    public ProjectType getType() { return ProjectType.JAVA; }

    @Override
    public String getDisplayName() { return "Java"; }

    @Override
    public Icon getIcon() { return IconManager.forProjectType(ProjectType.JAVA); }

    @Override
    public String getShortDescription() { return "Basic Java application"; }

    @Override
    public String getDescription() {
        return "A plain Java project with a standard source layout. "
             + "Java projects separate source code, resources, and documentation.";
    }

    @Override
    public List<String> getStructurePreview() {
        return List.of(
            "src/",
            "  main/",
            "    java/",
            "resources/",
            "README.md"
        );
    }

    @Override
    public void generate(File root, ProjectConfig config) throws IOException {
        File src = new File(root, "src/main/java/" + config.getPackagePath());
        File resources = new File(root, "resources");
        Files.createDirectories(src.toPath());
        Files.createDirectories(resources.toPath());

        File mainFile = new File(src, "Main.java");
        Files.writeString(mainFile.toPath(), String.format("""
            package %s;

            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello EyeCode!");
                }
            }
            """, config.getPackageBase()));

        File readme = new File(root, "README.md");
        Files.writeString(readme.toPath(), "# " + config.getName() + "\n\nA Java project created with EyeCode.\n");
    }
}
