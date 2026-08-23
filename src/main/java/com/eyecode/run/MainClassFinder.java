package com.eyecode.run;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class MainClassFinder {

    public String findMainClass(File srcRoot) {
        List<String> mainClasses = findMainClasses(srcRoot);
        return mainClasses.isEmpty() ? null : mainClasses.getFirst();
    }

    public List<String> findMainClasses(File srcRoot) {
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(srcRoot, javaFiles);
        List<String> mainClasses = new ArrayList<>();
        for (File file : javaFiles) {
            try {
                String content = Files.readString(file.toPath());
                if (content.contains("public static void main")) {
                    mainClasses.add(buildQualifiedClassName(file));
                }
            } catch (Exception ignored) {
            }
        }
        Collections.sort(mainClasses);
        return List.copyOf(mainClasses);
    }

    private void collectJavaFiles(
            File directory,
            List<File> javaFiles
    ) {

        File[] files = directory.listFiles();

        if (files == null) return;

        for (File file : files) {

            if (file.isDirectory()) {

                collectJavaFiles(file, javaFiles);

            } else if (file.getName().endsWith(".java")) {

                javaFiles.add(file);
            }
        }
    }

    private String findMainClass(List<File> javaFiles) {

        for (File file : javaFiles) {

            try {

                String content =
                        Files.readString(file.toPath());

                if (content.contains("public static void main")) {

                    return buildQualifiedClassName(file);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private String buildQualifiedClassName(File file) {

        try {

            String content =
                    Files.readString(file.toPath());

            String packageName = "";

            for (String line : content.split("\n")) {

                line = line.trim();

                if (line.startsWith("package ")) {

                    packageName =
                            line.replace("package ", "")
                                    .replace(";", "");

                    break;
                }
            }

            String className =
                    file.getName()
                            .replace(".java", "");

            if (!packageName.isEmpty()) {

                return packageName + "." + className;
            }

            return className;

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }
}
