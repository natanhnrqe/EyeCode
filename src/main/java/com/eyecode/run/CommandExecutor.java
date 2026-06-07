package com.eyecode.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class CommandExecutor {

    public String execute(List<String> command, File workingDirectory) {

        StringBuilder output = new StringBuilder();

        try {

            ProcessBuilder builder = new ProcessBuilder(command);

            builder.directory(workingDirectory);

            builder.redirectErrorStream(true);

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

        } catch (Exception e) {
            output.append(e.getMessage());
        }

        return output.toString();
    }
}
