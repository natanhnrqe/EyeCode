package com.eyecode.learning.markdown;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownCodeHighlighter {

    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "\\b(?:abstract|break|case|catch|class|continue|default|do|else|enum|extends|" +
            "finally|final|for|if|implements|import|interface|instanceof|native|new|package|" +
            "private|protected|public|record|return|static|strictfp|super|switch|" +
            "synchronized|this|throw|throws|transient|try|void|volatile|while)\\b");
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(?:String|Integer|Boolean|Object|Pessoa|Cliente|int|long|double|float|" +
            "char|byte|short|boolean|void|List|Set|Map|ArrayList|HashMap|HashSet|" +
            "Optional|Collection|Stream|Runnable|Thread)\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+[lLfFdD]?(?:\\.\\d+[fFdD]?)?\\b");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@\\w+");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*");

    private static final SimpleAttributeSet KEYWORD_STYLE;
    private static final SimpleAttributeSet TYPE_STYLE;
    private static final SimpleAttributeSet STRING_STYLE;
    private static final SimpleAttributeSet NUMBER_STYLE;
    private static final SimpleAttributeSet ANNOTATION_STYLE;
    private static final SimpleAttributeSet COMMENT_STYLE;

    static {
        KEYWORD_STYLE = syntax(ColorManager.SYNTAX_KEYWORD, true);
        TYPE_STYLE = syntax(ColorManager.SYNTAX_CLASS, false);
        STRING_STYLE = syntax(ColorManager.SYNTAX_STRING, false);
        NUMBER_STYLE = syntax(ColorManager.SYNTAX_NUMBER, false);
        ANNOTATION_STYLE = syntax(ColorManager.SYNTAX_ANNOTATION, false);
        COMMENT_STYLE = syntax(ColorManager.SYNTAX_COMMENT, false);
    }

    private MarkdownCodeHighlighter() {
    }

    public static void highlight(StyledDocument doc, int startOffset, String code, String language) {
        if ("java".equalsIgnoreCase(language)) {
            highlightJava(doc, startOffset, code);
        }
    }

    private static void highlightJava(StyledDocument doc, int startOffset, String code) {
        int offset = startOffset;
        for (String line : code.split("\n", -1)) {
            if (!line.isEmpty()) {
                applyToLine(doc, offset, line);
            }
            offset += line.length() + 1;
        }
    }

    private static void applyToLine(StyledDocument doc, int offset, String line) {
        boolean[] protectedRanges = new boolean[line.length()];
        applyProtectedPattern(doc, offset, line, STRING_PATTERN, STRING_STYLE, protectedRanges);
        applyProtectedPattern(doc, offset, line, COMMENT_PATTERN, COMMENT_STYLE, protectedRanges);
        applyTokenPattern(doc, offset, line, KEYWORD_PATTERN, KEYWORD_STYLE, protectedRanges);
        applyTokenPattern(doc, offset, line, TYPE_PATTERN, TYPE_STYLE, protectedRanges);
        applyTokenPattern(doc, offset, line, NUMBER_PATTERN, NUMBER_STYLE, protectedRanges);
        applyTokenPattern(doc, offset, line, ANNOTATION_PATTERN, ANNOTATION_STYLE, protectedRanges);
    }

    private static void applyProtectedPattern(
            StyledDocument doc, int offset, String line,
            Pattern pattern, SimpleAttributeSet style,
            boolean[] protectedRanges) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            if (overlapsProtectedRange(matcher.start(), matcher.end(), protectedRanges)) {
                continue;
            }
            doc.setCharacterAttributes(offset + matcher.start(),
                    matcher.end() - matcher.start(), style, false);
            markProtectedRange(matcher.start(), matcher.end(), protectedRanges);
        }
    }

    private static void applyTokenPattern(
            StyledDocument doc, int offset, String line,
            Pattern pattern, SimpleAttributeSet style,
            boolean[] protectedRanges) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            if (!overlapsProtectedRange(matcher.start(), matcher.end(), protectedRanges)) {
                doc.setCharacterAttributes(offset + matcher.start(),
                        matcher.end() - matcher.start(), style, false);
            }
        }
    }

    private static boolean overlapsProtectedRange(int start, int end, boolean[] protectedRanges) {
        for (int i = start; i < end; i++) {
            if (protectedRanges[i]) {
                return true;
            }
        }
        return false;
    }

    private static void markProtectedRange(int start, int end, boolean[] protectedRanges) {
        for (int i = start; i < end; i++) {
            protectedRanges[i] = true;
        }
    }

    private static SimpleAttributeSet syntax(Color color, boolean bold) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, TypographyManager.monoRegular(12).getFamily());
        StyleConstants.setFontSize(attrs, 12);
        StyleConstants.setBold(attrs, bold);
        StyleConstants.setForeground(attrs, color);
        return attrs;
    }
}
