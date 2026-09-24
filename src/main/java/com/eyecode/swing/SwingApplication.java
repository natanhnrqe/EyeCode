package com.eyecode.swing;

import com.eyecode.project.ProjectLaunchPathResolver;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;

public final class SwingApplication {
    private SwingApplication() { }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        var startupProject = ProjectLaunchPathResolver.resolve(args);
        SwingUtilities.invokeLater(() -> new SwingMainWindow(startupProject).show());
    }
}
