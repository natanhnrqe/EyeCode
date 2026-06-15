package com.eyecode.ui.designsystem;

import com.eyecode.ui.UiIcons;
import javax.swing.*;

public final class IconManager {

    private IconManager() {}

    public static Icon newFile()     { return UiIcons.newFile(); }
    public static Icon folder()      { return UiIcons.folder(); }
    public static Icon save()        { return UiIcons.save(); }
    public static Icon run()         { return UiIcons.run(); }
    public static Icon close()       { return UiIcons.close(); }
    public static Icon search()      { return UiIcons.search(); }
    public static Icon terminal()    { return UiIcons.terminal(); }
    public static Icon project()     { return UiIcons.project(); }
    public static Icon clear()       { return UiIcons.clear(); }
    public static Icon settings()    { return UiIcons.settings(); }
    public static Icon minimize()    { return UiIcons.minimize(); }
    public static Icon maximize()    { return UiIcons.maximize(); }
    public static Icon commit()      { return UiIcons.commit(); }
    public static Icon pr()          { return UiIcons.pr(); }
    public static Icon structure()   { return UiIcons.structure(); }
    public static Icon services()    { return UiIcons.services(); }
    public static Icon problem()     { return UiIcons.problem(); }
    public static Icon git()         { return UiIcons.git(); }

    public static Icon javaFile()    { return UiIcons.javaFile(); }
    public static Icon textFile()    { return UiIcons.textFile(); }
    public static Icon modifiedDot() { return UiIcons.modifiedDot(); }

    public static Icon forFile(String filename) {
        if (filename == null) return textFile();
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java")) return javaFile();
        return textFile();
    }
}
