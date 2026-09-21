package com.eyecode.language.java.lsp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record JdtLsToolingJava(Path executable, int majorVersion) {
    private static final Pattern VERSION = Pattern.compile("(?:version|openjdk)\\s+\\\"?(\\d+)");

    public JdtLsToolingJava {
        if (executable == null || !Files.isRegularFile(executable)) {
            throw new IllegalArgumentException("JDT LS tooling Java executable does not exist: " + executable);
        }
        if (majorVersion < 21) {
            throw new IllegalArgumentException("JDT LS requires Java 21 or newer, found Java " + majorVersion);
        }
    }

    public static JdtLsToolingJava currentRuntime(Duration timeout) {
        String executableName = isWindows() ? "java.exe" : "java";
        return verify(Path.of(System.getProperty("java.home"), "bin", executableName), timeout);
    }

    public static JdtLsToolingJava verify(Path executable, Duration timeout) {
        if (executable == null || !Files.isRegularFile(executable)) {
            throw new IllegalArgumentException("JDT LS tooling Java executable does not exist: " + executable);
        }
        Duration bounded = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(5) : timeout;
        Process process = null;
        try {
            process = new ProcessBuilder(executable.toString(), "-version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(bounded.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("Timed out verifying JDT LS tooling Java: " + executable);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException("Unable to verify JDT LS tooling Java: " + executable);
            }
            return new JdtLsToolingJava(executable.toAbsolutePath().normalize(), parseMajorVersion(output));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to start JDT LS tooling Java: " + executable, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Interrupted while verifying JDT LS tooling Java: " + executable, exception);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    static int parseMajorVersion(String output) {
        Matcher matcher = VERSION.matcher(output == null ? "" : output);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to determine JDT LS tooling Java version");
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
