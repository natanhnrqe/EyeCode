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

    public static Icon javaFile() { return new VectorIcon("javaFile"); }
    public static Icon textFile() { return new VectorIcon("textFile"); }
    public static Icon modifiedDot() { return new VectorIcon("modifiedDot"); }

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

    public static Icon commit() { return new VectorIcon("commit"); }
    public static Icon pr() { return new VectorIcon("pr"); }
    public static Icon structure() { return new VectorIcon("structure"); }
    public static Icon services() { return new VectorIcon("services"); }
    public static Icon problem() { return new VectorIcon("problem"); }
    public static Icon search() { return new VectorIcon("search"); }
    public static Icon git() { return new VectorIcon("git"); }

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
                case "commit" -> paintCommit(g2);
                case "pr" -> paintPr(g2);
                case "structure" -> paintStructure(g2);
                case "services" -> paintServices(g2);
                case "problem" -> paintProblem(g2);
                case "git" -> paintGit(g2);
                case "search" -> paintSearch(g2);
                case "javaFile" -> paintJavaFile(g2);
                case "textFile" -> paintFile(g2);
                case "modifiedDot" -> paintModifiedDot(g2);
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
            g2.drawRoundRect(3, 2, 12, 14, 2, 2);
            g2.drawLine(9, 5, 9, 13);
            g2.drawLine(5, 9, 13, 9);
        }

        private void paintFolder(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(2, 5);
            path.lineTo(6, 5);
            path.quadTo(7, 5, 8, 7);
            path.lineTo(16, 7);
            path.lineTo(14, 15);
            path.lineTo(2, 15);
            path.closePath();
            g2.draw(path);
            g2.drawLine(2, 5, 2, 4);
            g2.drawLine(2, 4, 7, 4);
            g2.drawLine(7, 4, 8, 7);
        }

        private void paintSave(Graphics2D g2) {
            g2.drawRoundRect(3, 2, 12, 14, 2, 2);
            g2.drawLine(7, 2, 7, 7);
            g2.drawLine(11, 2, 11, 7);
            g2.drawRoundRect(5, 9, 8, 6, 1, 1);
            g2.drawLine(9, 11, 9, 14);
        }

        private void paintRun(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(5, 3);
            path.lineTo(15, 9);
            path.lineTo(5, 15);
            path.closePath();
            g2.draw(path);
        }

        private void paintClose(Graphics2D g2) {
            g2.drawLine(4, 4, 14, 14);
            g2.drawLine(14, 4, 4, 14);
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
            g2.drawOval(4, 4, 10, 10);
            g2.drawOval(7, 7, 4, 4);
            g2.drawLine(9, 1, 9, 3);
            g2.drawLine(9, 15, 9, 17);
            g2.drawLine(1, 9, 3, 9);
            g2.drawLine(15, 9, 17, 9);
            g2.drawLine(4, 4, 5, 5);
            g2.drawLine(14, 14, 13, 13);
            g2.drawLine(14, 4, 13, 5);
            g2.drawLine(4, 14, 5, 13);
        }

        private void paintMinimize(Graphics2D g2) {
            g2.drawLine(5, 12, 13, 12);
        }

        private void paintMaximize(Graphics2D g2) {
            g2.drawRoundRect(5, 5, 8, 8, 1, 1);
        }

        private void paintCommit(Graphics2D g2) {
            g2.drawOval(8, 2, 4, 4);
            g2.drawLine(10, 6, 10, 14);
            g2.drawOval(8, 14, 4, 4);
        }

        private void paintPr(Graphics2D g2) {
            g2.drawLine(6, 4, 6, 14);
            g2.drawLine(6, 4, 12, 4);
            g2.drawOval(10, 2, 4, 4);
            g2.drawLine(12, 6, 12, 9);
            g2.drawLine(12, 9, 6, 12);
        }

        private void paintStructure(Graphics2D g2) {
            g2.drawRoundRect(3, 3, 5, 4, 1, 1);
            g2.drawRoundRect(10, 3, 5, 4, 1, 1);
            g2.drawRoundRect(6, 11, 6, 4, 1, 1);
            g2.drawLine(5, 7, 5, 9);
            g2.drawLine(5, 9, 9, 9);
            g2.drawLine(9, 9, 9, 11);
            g2.drawLine(12, 7, 12, 9);
            g2.drawLine(12, 9, 9, 9);
        }

        private void paintServices(Graphics2D g2) {
            g2.drawRoundRect(4, 4, 10, 10, 2, 2);
            g2.drawLine(4, 8, 14, 8);
        }

        private void paintProblem(Graphics2D g2) {
            g2.drawOval(3, 3, 12, 12);
            g2.drawLine(9, 6, 9, 10);
            g2.drawLine(9, 12, 9, 12);
        }

        private void paintGit(Graphics2D g2) {
            g2.drawLine(9, 3, 5, 9);
            g2.drawLine(5, 9, 9, 15);
            g2.drawLine(9, 15, 13, 9);
            g2.drawLine(13, 9, 9, 3);
            g2.drawOval(7, 7, 4, 4);
        }

        private void paintJavaFile(Graphics2D g2) {
            g2.drawRoundRect(4, 2, 10, 14, 2, 2);
            g2.drawLine(7, 6, 11, 6);
            g2.drawLine(7, 9, 11, 9);
            g2.drawRoundRect(7, 11, 4, 3, 1, 1);
        }

        private void paintModifiedDot(Graphics2D g2) {
            g2.fillOval(6, 6, 6, 6);
        }

        private void paintSearch(Graphics2D g2) {
            g2.drawOval(3, 3, 10, 10);
            g2.drawLine(11, 11, 15, 15);
        }
    }
}
