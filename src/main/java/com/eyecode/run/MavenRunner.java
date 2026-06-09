package com.eyecode.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class MavenRunner implements ProjectRunner{
    @Override
    public String run(File projectRoot) {

        StringBuilder output = new StringBuilder();

        try {

            ProcessBuilder builder = new ProcessBuilder(
                    "cmd",
                    "/c",
                    "mvn",
                    "clean",
                    "compile",
                    "exec:java",
                    "-Dexec.mainClass=com.eyecode.Main"
            );


            builder.directory(projectRoot);

            Process process = builder.start();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()
                            )
                    );

            String line;

            while ((line = reader.readLine()) != null) {

                output.append(line).append("\n");
            }
            process.waitFor();

        }catch (Exception e) {

            output.append(e.getMessage());
        }

        return output.toString();
    }
}
