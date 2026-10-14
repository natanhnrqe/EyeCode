package com.eyecode.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaToolchainResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void explicitPropertyWinsOverStagedAndHost() throws Exception {
        Path explicit = fakeJdk(tempDir.resolve("explicit-jdk"));
        Path staged = fakeJdk(tempDir.resolve("target/eyecode-desktop/runtime/jdk"));
        Map<String, String> properties = new HashMap<>();
        properties.put(JavaToolchainResolver.JDK_HOME_PROPERTY, explicit.toString());
        properties.put("java.home", staged.toString());

        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(), properties, tempDir, true);
        JavaToolchain toolchain = resolver.require();

        assertEquals(explicit.toAbsolutePath().normalize(), toolchain.home());
        assertEquals(explicit.resolve("bin/java.exe").toAbsolutePath().normalize(), toolchain.javaExecutable());
        assertEquals(explicit.resolve("bin/javac.exe").toAbsolutePath().normalize(), toolchain.javacExecutable());
    }

    @Test
    void envOverrideWinsWhenPropertyMissing() throws Exception {
        Path explicit = fakeJdk(tempDir.resolve("env-jdk"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, explicit.toString()),
                Map.of("java.home", tempDir.resolve("missing").toString()),
                tempDir, true);
        assertEquals(explicit.toAbsolutePath().normalize(), resolver.require().home());
    }

    @Test
    void stagedJdkIsUsedWhenOverrideMissing() throws Exception {
        Path staged = fakeJdk(tempDir.resolve("target/eyecode-desktop/runtime/jdk"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(), Map.of("java.home", tempDir.resolve("missing").toString()),
                tempDir, true);
        assertEquals(staged.toAbsolutePath().normalize(), resolver.require().home());
    }

    @Test
    void hostJavaHomeUsedWhenNoStagedJdk() throws Exception {
        Path host = fakeJdk(tempDir.resolve("host-jdk"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(), Map.of("java.home", host.toString()),
                tempDir.resolve("elsewhere"), true);
        assertEquals(host.toAbsolutePath().normalize(), resolver.require().home());
    }

    @Test
    void missingJavacIsRejected() throws Exception {
        Path jreLike = tempDir.resolve("jre");
        Files.createDirectories(jreLike.resolve("bin"));
        Files.createFile(jreLike.resolve("bin/java.exe"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jreLike.toString()),
                Map.of(), tempDir, true);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, resolver::require);
        assertTrue(error.getMessage().contains("No usable Java development toolchain"));
        assertTrue(error.getMessage().contains(JavaToolchainResolver.JDK_HOME_PROPERTY));
        assertTrue(error.getMessage().contains("stage-eyecode-jdk.ps1"));
    }

    @Test
    void nonJdkDirectoryIsRejected() throws Exception {
        Files.createDirectories(tempDir.resolve("not-a-jdk"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, tempDir.resolve("not-a-jdk").toString()),
                Map.of(), tempDir, true);
        assertThrows(IllegalArgumentException.class, resolver::require);
    }

    @Test
    void posixUsesJavaWithoutExe() throws Exception {
        Path jdk = tempDir.resolve("posix-jdk");
        Files.createDirectories(jdk.resolve("bin"));
        Files.createFile(jdk.resolve("bin/java"));
        Files.createFile(jdk.resolve("bin/javac"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jdk.toString()),
                Map.of(), tempDir, false);
        JavaToolchain toolchain = resolver.require();
        assertEquals(jdk.resolve("bin/java").toAbsolutePath().normalize(), toolchain.javaExecutable());
        assertEquals(jdk.resolve("bin/javac").toAbsolutePath().normalize(), toolchain.javacExecutable());
    }

    @Test
    void childEnvironmentPropagatesJavaHomeAndPath() throws Exception {
        Path jdk = fakeJdk(tempDir.resolve("env-jdk"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, jdk.toString()),
                Map.of(), tempDir, true);
        Map<String, String> env = resolver.childEnvironment();
        assertEquals(jdk.toAbsolutePath().normalize().toString(), env.get("JAVA_HOME"));
        assertTrue(env.get("PATH").startsWith(jdk.resolve("bin").toAbsolutePath().normalize().toString()));
    }

    @Test
    void emptyWhenNothingResolvable() {
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(), Map.of(), tempDir.resolve("elsewhere"), true);
        assertFalse(resolver.resolve().isPresent());
    }

    @Test
    void blankOverrideIsIgnored() throws Exception {
        Path host = fakeJdk(tempDir.resolve("host-jdk"));
        JavaToolchainResolver resolver = new JavaToolchainResolver(
                Map.of(JavaToolchainResolver.JDK_HOME_ENV, "   "),
                Map.of("java.home", host.toString()),
                tempDir.resolve("elsewhere"), true);
        assertEquals(host.toAbsolutePath().normalize(), resolver.require().home());
    }

    private static Path fakeJdk(Path home) throws Exception {
        Files.createDirectories(home.resolve("bin"));
        Files.createFile(home.resolve("bin/java.exe"));
        Files.createFile(home.resolve("bin/javac.exe"));
        return home;
    }
}
