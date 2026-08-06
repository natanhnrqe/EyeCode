package com.eyecode.javafx.explorer;

import com.eyecode.designsystem.icon.EyeCodeIcon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ExplorerIconResolver {

    private static final Map<String, EyeCodeIcon> BY_EXTENSION = Map.ofEntries(
            Map.entry("java", EyeCodeIcon.JAVA_FILE),
            Map.entry("html", EyeCodeIcon.HTML),
            Map.entry("htm", EyeCodeIcon.HTML),
            Map.entry("css", EyeCodeIcon.CSS),
            Map.entry("json", EyeCodeIcon.JSON),
            Map.entry("xml", EyeCodeIcon.XML),
            Map.entry("md", EyeCodeIcon.MARKDOWN),
            Map.entry("markdown", EyeCodeIcon.MARKDOWN),
            Map.entry("png", EyeCodeIcon.IMAGE),
            Map.entry("jpg", EyeCodeIcon.IMAGE),
            Map.entry("jpeg", EyeCodeIcon.IMAGE),
            Map.entry("gif", EyeCodeIcon.IMAGE),
            Map.entry("svg", EyeCodeIcon.IMAGE),
            Map.entry("webp", EyeCodeIcon.IMAGE),
            Map.entry("ico", EyeCodeIcon.IMAGE),
            Map.entry("bmp", EyeCodeIcon.IMAGE)
    );

    private static final Pattern PACKAGE_NAME = Pattern.compile("[a-z][a-z0-9]*");

    private ExplorerIconResolver() {}

    public static EyeCodeIcon forNode(ProjectNode node) {
        return forNode(node, false);
    }

    public static EyeCodeIcon forNode(ProjectNode node, boolean underJavaSource) {
        return switch (node.type()) {
            case PROJECT -> EyeCodeIcon.PROJECT_DIRECTORY;
            case DIRECTORY -> directoryIcon(node, underJavaSource);
            case FILE -> fileIcon(node);
        };
    }

    private static EyeCodeIcon directoryIcon(ProjectNode node, boolean underJavaSource) {
        if (Files.exists(node.path().resolve("module-info.java"))) {
            return EyeCodeIcon.MODULE;
        }
        if (underJavaSource && PACKAGE_NAME.matcher(node.name()).matches()) {
            return EyeCodeIcon.PACKAGE;
        }
        return EyeCodeIcon.FOLDER;
    }

    private static EyeCodeIcon fileIcon(ProjectNode node) {
        String name = node.name().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return EyeCodeIcon.TEXT_FILE;
        }
        return BY_EXTENSION.getOrDefault(name.substring(dot + 1), EyeCodeIcon.TEXT_FILE);
    }
}