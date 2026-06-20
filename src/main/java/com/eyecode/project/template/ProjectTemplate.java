package com.eyecode.project.template;

import com.eyecode.project.ProjectType;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ProjectTemplate {

    ProjectType getType();

    String getDisplayName();

    Icon getIcon();

    String getShortDescription();

    String getDescription();

    List<String> getStructurePreview();

    void generate(File root, ProjectConfig config) throws IOException;
}
