package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class CompletionIconManager {

    private static final int ICON_SIZE = 16;
    private static final Map<CompletionItemKind, ImageIcon> CACHE = new HashMap<>();

    private CompletionIconManager() {}

    public static ImageIcon getIcon(CompletionItemKind kind) {
        if (kind == null) return placeholder(ColorManager.TEXT_MUTED);
        return CACHE.computeIfAbsent(kind, CompletionIconManager::loadIcon);
    }

    private static ImageIcon loadIcon(CompletionItemKind kind) {
        String path = "/icons/completion/" + kind.name().toLowerCase() + ".svg";
        ImageIcon svgIcon = tryLoadSvg(path);
        if (svgIcon != null) return svgIcon;

        path = "/icons/completion/" + kind.name().toLowerCase() + ".png";
        ImageIcon pngIcon = tryLoadPng(path);
        if (pngIcon != null) return pngIcon;

        return createPlaceholder(kind);
    }

    private static ImageIcon tryLoadSvg(String path) {
        try (InputStream is = CompletionIconManager.class.getResourceAsStream(path)) {
            if (is == null) return null;
            com.formdev.flatlaf.extras.FlatSVGIcon svg =
                    new com.formdev.flatlaf.extras.FlatSVGIcon(path, ICON_SIZE, ICON_SIZE);
            if (svg.hasFound()) return svg;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ImageIcon tryLoadPng(String path) {
        java.net.URL url = CompletionIconManager.class.getResource(path);
        if (url == null) return null;
        ImageIcon icon = new ImageIcon(url);
        Image scaled = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static ImageIcon createPlaceholder(CompletionItemKind kind) {
        return placeholder(kindColor(kind));
    }

    private static ImageIcon placeholder(Color color) {
        BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        g.fillRoundRect(1, 1, ICON_SIZE - 2, ICON_SIZE - 2, 4, 4);
        g.setColor(color);
        g.drawRoundRect(1, 1, ICON_SIZE - 2, ICON_SIZE - 2, 4, 4);
        g.dispose();
        return new ImageIcon(img);
    }

    private static Color kindColor(CompletionItemKind kind) {
        return switch (kind) {
            case KEYWORD -> ColorManager.SYNTAX_KEYWORD;
            case CLASS -> ColorManager.SYNTAX_CLASS;
            case INTERFACE -> ColorManager.SYNTAX_TYPE;
            case ENUM -> ColorManager.SYNTAX_CONSTANT;
            case RECORD -> ColorManager.SYNTAX_METHOD;
            case METHOD -> ColorManager.SYNTAX_METHOD;
            case FIELD -> ColorManager.SYNTAX_CONSTANT;
            case VARIABLE -> ColorManager.AUTOCOMPLETE_FG;
            case PACKAGE -> ColorManager.SYNTAX_TYPE;
            case SNIPPET -> ColorManager.SYNTAX_ANNOTATION;
            case CONSTRUCTOR -> ColorManager.SYNTAX_CLASS;
        };
    }
}
