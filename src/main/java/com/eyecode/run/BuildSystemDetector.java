package com.eyecode.run;

import java.io.File;

public class BuildSystemDetector {

    public static BuildSystem detected(File projectRoot) {

        if (new File(projectRoot, "pom.xml").exists()) {
            return BuildSystem.MAVEN;
        }

        return BuildSystem.PLAIN_JAVA;

    }
}
