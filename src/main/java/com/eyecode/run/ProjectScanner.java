package com.eyecode.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectScanner {

    public List<File> findJavaFiles(File root) {
        List<File> javaFiles = new ArrayList<>();

        scan(root, javaFiles);

        return javaFiles;
    }

    private void scan(File file, List<File> javaFiles) {

        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    scan(child, javaFiles);
                }
            }

        } else if (file.getName().endsWith(".java")) {

            javaFiles.add(file);
        }
    }
}