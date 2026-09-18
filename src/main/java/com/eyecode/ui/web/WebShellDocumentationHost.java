package com.eyecode.ui.web;

import com.eyecode.learning.content.DocumentationTarget;

/**
 * Host capability for native documentation shown alongside the Web Shell.
 * Implementations own toolkit-specific layout and lifecycle; Web Shell
 * controllers only request documentation state through this contract.
 */
public interface WebShellDocumentationHost {
    void open(DocumentationTarget target);

    void layoutFromBrowser(double x, double y, double width, double height);

    void hide();
}
