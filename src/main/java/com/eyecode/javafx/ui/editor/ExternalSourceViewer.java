package com.eyecode.javafx.ui.editor;

import com.eyecode.language.documentation.JdkSourceTarget;

public interface ExternalSourceViewer {
    void open(JdkSourceTarget target);

    boolean contains(String sourceIdentity);

    void close(String sourceIdentity);
}
