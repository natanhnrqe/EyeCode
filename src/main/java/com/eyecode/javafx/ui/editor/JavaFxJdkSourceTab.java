package com.eyecode.javafx.ui.editor;

import com.eyecode.language.documentation.JdkSourceDeclarationLocator;
import com.eyecode.language.documentation.JdkSourceTarget;

public final class JavaFxJdkSourceTab {

    private final JdkSourceTarget target;
    private final JdkSourceDeclarationLocator declarationLocator = new JdkSourceDeclarationLocator();
    private final String source;
    private int revealedOffset;

    public JavaFxJdkSourceTab(JdkSourceTarget target, String source) {
        this.target = target;
        this.source = source == null ? "" : source;
        reveal(target);
    }

    public JdkSourceTarget target() { return target; }

    public String sourceIdentity() { return target.sourceIdentity(); }

    public String source() { return source; }

    public boolean isReadOnly() { return true; }

    public int revealedOffset() { return revealedOffset; }

    public void reveal(JdkSourceTarget target) {
        revealedOffset = declarationLocator.find(source, target);
    }

    public void dispose() { }
}
