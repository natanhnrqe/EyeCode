package com.eyecode.learning.markdown.theme;

import com.eyecode.learning.markdown.MarkdownTheme;

import javax.swing.text.SimpleAttributeSet;

public final class DefaultMarkdownThemeProvider implements MarkdownThemeProvider {

    @Override
    public SimpleAttributeSet h1() {
        return MarkdownTheme.h1();
    }

    @Override
    public SimpleAttributeSet h2() {
        return MarkdownTheme.h2();
    }

    @Override
    public SimpleAttributeSet body() {
        return MarkdownTheme.body();
    }

    @Override
    public SimpleAttributeSet bold() {
        return MarkdownTheme.bold();
    }

    @Override
    public SimpleAttributeSet italic() {
        return MarkdownTheme.italic();
    }

    @Override
    public SimpleAttributeSet codeInline() {
        return MarkdownTheme.codeInline();
    }

    @Override
    public SimpleAttributeSet link() {
        return MarkdownTheme.link();
    }

    @Override
    public SimpleAttributeSet bullet() {
        return MarkdownTheme.bullet();
    }

    @Override
    public SimpleAttributeSet codeBlock() {
        return MarkdownTheme.codeBlock();
    }

    @Override
    public SimpleAttributeSet codeBlockParagraph(boolean firstLine, boolean lastLine) {
        return MarkdownTheme.codeBlockParagraph(firstLine, lastLine);
    }

    @Override
    public SimpleAttributeSet divider() {
        return MarkdownTheme.divider();
    }

    @Override
    public String dividerText() {
        return MarkdownTheme.dividerText();
    }

    @Override
    public SimpleAttributeSet arrow() {
        return MarkdownTheme.arrow();
    }
}
