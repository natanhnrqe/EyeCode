package com.eyecode.designsystem.icon;

public enum EyeCodeIcon {

    HAMBURGER("hamburger"),
    FOLDER("folder"),
    FOLDERS("folders"),
    FOLDER_OPEN("folderOpen"),
    PROJECT_DIRECTORY("projectDirectory"),
    PROJECT("project"),
    ASSETS("assets"),
    SAVE("save"),
    RUN("run"),
    PLAY("play"),
    STOP("stop"),
    DEBUG("debug"),
    CLOSE("close"),
    SEARCH("search"),
    TERMINAL("terminal"),
    CLEAR("clear"),
    SETTINGS("settings"),
    MINIMIZE("minimize"),
    MAXIMIZE("maximize"),
    COMMIT("commit"),
    PR("pr"),
    STRUCTURE("structure"),
    SERVICES("services"),
    PROBLEM("problem"),
    GIT("git"),
    RELOAD("reload"),
    NEW_FILE("newFile"),
    NEW_PROJECT("newProject"),
    JAVA_FILE("java"),
    TEXT_FILE("file"),
    HTML("html"),
    CSS("css"),
    JSON("json"),
    XML("xml"),
    MARKDOWN("markdown"),
    IMAGE("image"),
    PACKAGE("package"),
    MODULE("module"),
    MODIFIED_DOT("modifiedDot");

    public static final EyeCodeIcon PLACEHOLDER_OUTPUT = TEXT_FILE;
    public static final EyeCodeIcon PLACEHOLDER_TODO   = MODIFIED_DOT;

    private static final String RESOURCE_DIR = "icons/";
    private static final String EXTENSION = ".svg";

    private final String resourceKey;

    EyeCodeIcon(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String resourceKey() {
        return resourceKey;
    }

    public String resourcePath() {
        return RESOURCE_DIR + resourceKey + EXTENSION;
    }
}
