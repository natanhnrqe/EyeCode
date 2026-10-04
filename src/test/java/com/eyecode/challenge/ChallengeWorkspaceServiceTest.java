package com.eyecode.challenge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeWorkspaceServiceTest {
    @TempDir
    Path temporary;

    @Test
    void ensureScaffoldsTheChallengeWorkspaceOnce() throws IOException {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);

        ChallengeWorkspaceService.EnsureResult first = service.ensure("cnpj-validator");
        assertTrue(first.fresh());
        assertTrue(Files.isRegularFile(first.path().resolve("pom.xml")));
        assertTrue(Files.isRegularFile(first.path().resolve("src/main/java/br/com/eyecode/challenge/CnpjValidator.java")));
        assertTrue(Files.isRegularFile(first.path().resolve("src/test/java/br/com/eyecode/challenge/CnpjValidatorTest.java")));
        assertEquals(temporary.resolve("cnpj-validator"), first.path());

        ChallengeWorkspaceService.EnsureResult second = service.ensure("cnpj-validator");
        assertFalse(second.fresh(), "an existing challenge workspace must not be re-scaffolded");
    }

    @Test
    void ensurePreservesUserEditsAcrossSessions() throws IOException {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        service.ensure("cnpj-validator");
        Path entry = temporary.resolve("cnpj-validator/src/main/java/br/com/eyecode/challenge/CnpjValidator.java");
        Files.writeString(entry, "package br.com.eyecode.challenge;\nclass CnpjValidator { boolean progress = true; }\n");

        ChallengeWorkspaceService.EnsureResult again = service.ensure("cnpj-validator");

        assertFalse(again.fresh());
        assertTrue(Files.readString(entry).contains("progress = true"),
                "user progress must survive re-entering the challenge");
    }

    @Test
    void resetRestoresTheStarterCode() throws IOException {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        service.ensure("cnpj-validator");
        Path entry = temporary.resolve("cnpj-validator/src/main/java/br/com/eyecode/challenge/CnpjValidator.java");
        Files.writeString(entry, "package br.com.eyecode.challenge;\nclass CnpjValidator { broken }\n");

        service.reset("cnpj-validator");

        String restored = Files.readString(entry);
        assertTrue(restored.contains("public boolean solve(String input)"),
                "reset must restore the starter source");
    }

    @Test
    void existsReflectsScaffoldedState() {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);

        assertFalse(service.exists("cnpj-validator"));
        service.ensure("cnpj-validator");
        assertTrue(service.exists("cnpj-validator"));
    }

    @Test
    void invalidIdsAreRejected() {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);

        assertThrows(IllegalArgumentException.class, () -> service.ensure("../escape"));
        assertThrows(IllegalArgumentException.class, () -> service.ensure(""));
        assertThrows(IllegalArgumentException.class, () -> service.ensure(null));
        assertThrows(IllegalArgumentException.class, () -> service.ensure("CNPJ Validator"));
    }

    @Test
    void classNameIsDerivedFromTheChallengeId() {
        assertEquals("CnpjValidator", ChallengeWorkspaceService.classNameOf("cnpj-validator"));
        assertEquals("Fizzbuzz", ChallengeWorkspaceService.classNameOf("fizzbuzz"));
        assertEquals("ApiPedidos", ChallengeWorkspaceService.classNameOf("api-pedidos"));
    }
}
