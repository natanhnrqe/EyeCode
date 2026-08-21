package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;

@FunctionalInterface
public interface DocumentationNavigator {

    void open(DocumentationTarget target);
}
