package com.eyecode.ui.designsystem;

import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.project.ProjectType;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class IconManager {

    private static final String ICONS_PATH = "icons/";
    private static final String COMPLETION_ICONS_PATH = "icons/completion/";
    private static final int ICON_SIZE = SpacingSystem.ICON_SIZE;
    private static final int COMPLETION_ICON_SIZE = 16;

    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static final Map<CompletionItemKind, Icon> COMPLETION_CACHE = new HashMap<>();

    private IconManager() {}

    public static Icon folder()      { return load("folder"); }
    public static Icon folders() {return load("folders");}
    public static Icon projectDirectory() { return load("projectDirectory"); }
    public static Icon assets()      { return load("assets"); }
    public static Icon save()        { return load("save"); }
    public static Icon run()         { return load("run"); }
    public static Icon close()       { return load("close"); }
    public static Icon search()      { return load("search"); }
    public static Icon terminal()    { return load("terminal"); }
    public static Icon project()     { return load("project"); }
    public static Icon clear()       { return load("clear"); }
    public static Icon settings()    { return load("settings"); }
    public static Icon minimize()    { return load("minimize"); }
    public static Icon maximize()    { return load("maximize"); }
    public static Icon commit()      { return load("commit"); }
    public static Icon pr()          { return load("pr"); }
    public static Icon structure()   { return load("structure"); }
    public static Icon services()    { return load("services"); }
    public static Icon problem()     { return load("problem"); }
    public static Icon git()         { return load("git"); }

    public static Icon menu()        { return load("hamburger"); }
    public static Icon newFile()     { return load("newFile"); }
    public static Icon javaFile()    { return load("java"); }
    public static Icon textFile()    { return load("file"); }
    public static Icon modifiedDot() { return load("modifiedDot"); }

    // Sprint 9.2 – Run Controls
    public static Icon reload()      { return load("reload"); }
    public static Icon play()        { return load("play"); }
    public static Icon stop()        { return load("stop"); }
    public static Icon debug()       { return load("debug"); }

    public static Icon folderOpen()  { return load("folderOpen"); }
    public static Icon newProject()  { return load("newProject"); }

    public static Icon forFile(String filename) {

        if (filename == null) {
            return textFile();
        }

        String lower = filename.toLowerCase();

        if (lower.endsWith(".java")) {
            return javaFile();
        }

        if (lower.endsWith(".xml")) {
            return load("xml");
        }

        if (lower.endsWith(".json")) {
            return load("json");
        }

        if (lower.endsWith(".md")) {
            return load("markdown");
        }

        if (lower.equals("pom.xml")) {
            return load("maven");
        }

        if (lower.equals(".gitignore")) {
            return load("git");
        }

        return textFile();
    }

    public static Icon completion(CompletionItemKind kind) {
        if (kind == null) return completionPlaceholder(ColorManager.TEXT_MUTED);
        return COMPLETION_CACHE.computeIfAbsent(kind, IconManager::loadCompletionIcon);
    }

    private static Icon loadCompletionIcon(CompletionItemKind kind) {
        String name = kind.name().toLowerCase();
        String resourcePath = "/" + COMPLETION_ICONS_PATH + name + ".svg";

        java.net.URL svgUrl = IconManager.class.getResource(resourcePath);
        if (svgUrl != null) {
            FlatSVGIcon svg = new FlatSVGIcon(COMPLETION_ICONS_PATH + name + ".svg", COMPLETION_ICON_SIZE, COMPLETION_ICON_SIZE);
            return svg;
        }

        java.net.URL pngUrl = IconManager.class.getResource("/" + COMPLETION_ICONS_PATH + name + ".png");
        if (pngUrl != null) {
            ImageIcon raw = new ImageIcon(pngUrl);
            Image scaled = raw.getImage().getScaledInstance(COMPLETION_ICON_SIZE, COMPLETION_ICON_SIZE, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }

        return completionPlaceholder(completionKindColor(kind));
    }

    private static Icon completionPlaceholder(Color color) {
        BufferedImage img = new BufferedImage(COMPLETION_ICON_SIZE, COMPLETION_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        g.fillRoundRect(1, 1, COMPLETION_ICON_SIZE - 2, COMPLETION_ICON_SIZE - 2, 4, 4);
        g.setColor(color);
        g.drawRoundRect(1, 1, COMPLETION_ICON_SIZE - 2, COMPLETION_ICON_SIZE - 2, 4, 4);
        g.dispose();
        return new ImageIcon(img);
    }

    private static Color completionKindColor(CompletionItemKind kind) {
        return switch (kind) {
            case KEYWORD -> ColorManager.SYNTAX_KEYWORD;
            case CLASS -> ColorManager.SYNTAX_CLASS;
            case INTERFACE -> ColorManager.SYNTAX_TYPE;
            case ENUM -> ColorManager.SYNTAX_CONSTANT;
            case RECORD -> ColorManager.SYNTAX_METHOD;
            case METHOD -> ColorManager.SYNTAX_METHOD;
            case FIELD -> ColorManager.SYNTAX_CONSTANT;
            case VARIABLE -> ColorManager.AUTOCOMPLETE_FG;
            case PACKAGE -> ColorManager.SYNTAX_TYPE;
            case SNIPPET -> ColorManager.SYNTAX_ANNOTATION;
            case CONSTRUCTOR -> ColorManager.SYNTAX_CLASS;

        };
    }

    private static Icon load(String name) {
        return CACHE.computeIfAbsent(name, n -> new FlatSVGIcon(ICONS_PATH + n + ".svg", ICON_SIZE, ICON_SIZE));
    }

    public static Icon forProjectType(ProjectType type) {
        return load(type.getIconName());
    }

    public static Icon welcomeIcon(String name) {
        return new FlatSVGIcon(ICONS_PATH + name + ".svg", 22, 22);
    }

    public static Icon projectTypeIcon(String name) {
        return new FlatSVGIcon(ICONS_PATH + name + ".svg", 24, 24);
    }
}
