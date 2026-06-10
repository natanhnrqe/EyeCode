package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public final class UiIcons {

    private UiIcons() {
    }

    public static Icon newFile() {
        return new VectorIcon("newFile");
    }

    public static Icon folder() {
        return new VectorIcon("folder");
    }

    public static Icon save() {
        return new VectorIcon("save");
    }

    public static Icon run() {
        return new VectorIcon("run");
    }

    public static Icon close() {
        return new VectorIcon("close");
    }

    public static Icon terminal() {
        return new VectorIcon("terminal");
    }

    public static Icon project() {
        return new VectorIcon("project");
    }

    public static Icon clear() {
        return new VectorIcon("clear");
    }

    public static Icon settings() {
        return new VectorIcon("settings");
    }

    public static Icon minimize() {
        return new VectorIcon("minimize");
    }

    public static Icon maximize() {
        return new VectorIcon("maximize");
    }

    private static class VectorIcon implements Icon {

        private final String name;
        private final int size;

        VectorIcon(String name) {
            this.name = name;
            this.size = 18;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(c != null && c.isEnabled() ? c.getForeground() : new Color(105, 105, 105));

            switch (name) {
                case "newFile" -> paintNewFile(g2);
                case "folder" -> paintFolder(g2);
                case "save" -> paintSave(g2);
                case "run" -> paintRun(g2);
                case "close" -> paintClose(g2);
                case "terminal" -> paintTerminal(g2);
                case "project" -> paintProject(g2);
                case "clear" -> paintClear(g2);
                case "settings" -> paintSettings(g2);
                case "minimize" -> paintMinimize(g2);
                case "maximize" -> paintMaximize(g2);
                default -> paintFile(g2);
            }

            g2.dispose();
        }

        private void paintFile(Graphics2D g2) {
            g2.drawRoundRect(4, 2, 10, 14, 2, 2);
            g2.drawLine(7, 6, 12, 6);
            g2.drawLine(7, 10, 12, 10);
        }

        private void paintNewFile(Graphics2D g2) {
            paintFile(g2);
            g2.drawLine(9, 8, 9, 14);
            g2.drawLine(6, 11, 12, 11);
        }

        private void paintFolder(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(2, 6);
            path.lineTo(7, 6);
            path.lineTo(8.7, 8);
            path.lineTo(16, 8);
            path.lineTo(16, 15);
            path.lineTo(2, 15);
            path.closePath();
            g2.draw(path);
        }

        private void paintSave(Graphics2D g2) {
            g2.drawRoundRect(3, 3, 12, 12, 2, 2);
            g2.drawLine(6, 3, 6, 7);
            g2.drawLine(11, 3, 11, 7);
            g2.drawLine(6, 12, 12, 12);
        }

        private void paintRun(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(6, 4);
            path.lineTo(14, 9);
            path.lineTo(6, 14);
            path.closePath();
            g2.draw(path);
        }

        private void paintClose(Graphics2D g2) {
            g2.drawLine(5, 5, 13, 13);
            g2.drawLine(13, 5, 5, 13);
        }

        private void paintTerminal(Graphics2D g2) {
            g2.drawRoundRect(2, 3, 14, 12, 2, 2);
            g2.drawLine(5, 7, 8, 9);
            g2.drawLine(5, 11, 8, 9);
            g2.drawLine(10, 12, 13, 12);
        }

        private void paintProject(Graphics2D g2) {
            g2.drawRoundRect(4, 2, 10, 14, 2, 2);
            g2.drawLine(7, 6, 11, 6);
            g2.drawLine(7, 9, 11, 9);
            g2.drawLine(7, 12, 11, 12);
        }

        private void paintClear(Graphics2D g2) {
            g2.drawRoundRect(4, 6, 10, 9, 2, 2);
            g2.drawLine(6, 6, 6, 4);
            g2.drawLine(12, 6, 12, 4);
            g2.drawLine(5, 4, 13, 4);
            g2.drawLine(7, 9, 7, 12);
            g2.drawLine(11, 9, 11, 12);
        }

        private void paintSettings(Graphics2D g2) {
            g2.drawOval(6, 6, 6, 6);
            g2.drawLine(9, 2, 9, 4);
            g2.drawLine(9, 14, 9, 16);
            g2.drawLine(2, 9, 4, 9);
            g2.drawLine(14, 9, 16, 9);
            g2.drawLine(5, 5, 6, 6);
            g2.drawLine(12, 12, 13, 13);
            g2.drawLine(13, 5, 12, 6);
            g2.drawLine(6, 12, 5, 13);
        }

        private void paintMinimize(Graphics2D g2) {
            g2.drawLine(5, 12, 13, 12);
        }

        private void paintMaximize(Graphics2D g2) {
            g2.drawRoundRect(5, 5, 8, 8, 1, 1);
        }
    }
}
