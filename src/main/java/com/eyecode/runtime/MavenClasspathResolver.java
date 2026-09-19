package com.eyecode.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class MavenClasspathResolver {
    private final BuildToolExecutableResolver buildToolResolver;

    MavenClasspathResolver(BuildToolExecutableResolver buildToolResolver) {
        this.buildToolResolver = buildToolResolver == null ? new BuildToolExecutableResolver() : buildToolResolver;
    }

    String resolve(Path projectRoot) {
        Path outputFile = null;
        Path logFile = null;
        Process process = null;
        try {
            outputFile = Files.createTempFile("eyecode-maven-classpath-", ".txt");
            logFile = Files.createTempFile("eyecode-maven-classpath-", ".log");
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
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile());
            process = builder.start();
            while (!process.waitFor(100, TimeUnit.MILLISECONDS)) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
            int exitCode = process.exitValue();
            String output = Files.readString(logFile, StandardCharsets.UTF_8);
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
            ProcessTree.destroy(process, true);
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Maven classpath resolution was interrupted", exception);
        } finally {
            ProcessTree.destroy(process, true);
            try {
                if (outputFile != null) Files.deleteIfExists(outputFile);
                if (logFile != null) Files.deleteIfExists(logFile);
            } catch (IOException ignored) {
            }
        }
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
