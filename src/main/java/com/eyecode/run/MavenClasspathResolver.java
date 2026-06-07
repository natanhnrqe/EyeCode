package com.eyecode.run;

import java.io.File;
import java.util.List;

public class MavenClasspathResolver {

    private final CommandExecutor executor = new CommandExecutor();

    public String resolve(File projectRoot){

        return executor.execute(
                List.of(
                        "cmd",
                        "/c",
                        "mvn",
                        "dependency:build-classpath"
                ),
                projectRoot
        );

    }
}
