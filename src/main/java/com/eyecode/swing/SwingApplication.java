package com.eyecode.swing;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;

public final class SwingApplication {
    private SwingApplication() { }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new SwingMainWindow().show());
    }
}
