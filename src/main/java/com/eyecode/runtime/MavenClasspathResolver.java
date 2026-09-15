package com.eyecode.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class MavenClasspathResolver {
    private final BuildToolExecutableResolver buildToolResolver;

    MavenClasspathResolver(BuildToolExecutableResolver buildToolResolver) {
        this.buildToolResolver = buildToolResolver == null ? new BuildToolExecutableResolver() : buildToolResolver;
    }

    String resolve(Path projectRoot) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("eyecode-maven-classpath-", ".txt");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to prepare Maven classpath resolution", exception);
        }
        try {
            List<String> command = new ArrayList<>(buildToolResolver.mavenCommand(projectRoot,
                    "dependency:build-classpath",
                    "-Dmdep.outputFile=" + outputFile,
                    "-Dmdep.includeScope=runtime",
                    "-q"));
            quoteOutputFileArgumentForWindowsCmd(command);
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true);
            Process process = builder.start();
            String output = read(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalArgumentException("Unable to resolve Maven runtime classpath (exit code "
                        + exitCode + "): " + summarize(output));
            }
            if (!Files.isRegularFile(outputFile)) {
                throw new IllegalArgumentException("Maven did not produce a runtime classpath");
            }
            return Files.readString(outputFile, StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to resolve Maven runtime classpath", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Maven classpath resolution was interrupted", exception);
        } finally {
            try {
                if (outputFile != null) Files.deleteIfExists(outputFile);
            } catch (IOException ignored) {
            }
        }
    }

    private String read(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String summarize(String output) {
        String value = output == null ? "" : output.trim();
        if (value.length() <= 800) return value;
        return value.substring(value.length() - 800);
    }

    private void quoteOutputFileArgumentForWindowsCmd(List<String> command) {
        if (command.size() < 3 || !"cmd".equalsIgnoreCase(command.getFirst())
                || !"/c".equalsIgnoreCase(command.get(1))) {
            return;
        }
        for (int index = 2; index < command.size(); index++) {
            String argument = command.get(index);
            if (argument.startsWith("-Dmdep.outputFile=") && !argument.startsWith("\"")) {
                command.set(index, '"' + argument + '"');
            }
        }
    }
}
