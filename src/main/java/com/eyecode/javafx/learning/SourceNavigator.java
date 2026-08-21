package com.eyecode.javafx.learning;

import com.eyecode.language.documentation.JdkSourceTarget;

@FunctionalInterface
public interface SourceNavigator {

    void open(JdkSourceTarget target);
}
