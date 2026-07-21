package com.eyecode.learning.markdown.theme;

import javax.swing.text.SimpleAttributeSet;
import java.awt.Color;

public interface MarkdownThemeProvider {

    SimpleAttributeSet h1();

    SimpleAttributeSet h2();

    SimpleAttributeSet body();

    SimpleAttributeSet bold();

    SimpleAttributeSet italic();

    SimpleAttributeSet codeInline();

    SimpleAttributeSet link();

    SimpleAttributeSet bullet();

    SimpleAttributeSet codeBlock();

    SimpleAttributeSet codeBlockParagraph(boolean firstLine, boolean lastLine);

    SimpleAttributeSet divider();

    String dividerText();

    SimpleAttributeSet arrow();
}
