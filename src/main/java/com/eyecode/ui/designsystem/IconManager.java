package com.eyecode.ui.designsystem;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;

public final class IconManager {

    private static final String ICONS_PATH = "icons/";
    private static final int ICON_SIZE = SpacingSystem.ICON_SIZE;

    private IconManager() {}

    public static Icon folder()      { return load("folder"); }
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

    public static Icon newFile()     { return load("newFile"); }
    public static Icon javaFile()    { return load("java"); }
    public static Icon textFile()    { return load("file"); }
    public static Icon modifiedDot() { return load("modifiedDot"); }

    public static Icon forFile(String filename) {
        if (filename == null) return textFile();
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java")) return javaFile();
        return textFile();
    }

    private static Icon load(String name) {
        return new FlatSVGIcon(ICONS_PATH + name + ".svg", ICON_SIZE, ICON_SIZE);
    }
}
