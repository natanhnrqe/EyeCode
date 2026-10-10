package com.eyecode.challenge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
        Path tests = first.path().resolve("src/test/java/br/com/eyecode/challenge/CnpjValidatorTest.java");
        assertTrue(Files.isRegularFile(tests));
        assertTrue(Files.readString(tests).contains("acceptsValidCnpj"));
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
        assertTrue(restored.contains("public boolean isValid(String cnpj)"),
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

    @Test
    void fundamentalsChallengesCreateTaskSpecificStarterAndJUnitExamples() throws IOException {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        Map<String, String> contracts = Map.of(
                "fizzbuzz", "List<String> fizzBuzz(int start, int end)",
                "conversor-temperatura", "double toFahrenheit(double celsius)",
                "ano-bissexto", "boolean isLeapYear(int year)",
                "media-aprovacao", "boolean isApproved(double[] grades)",
                "palindromo", "boolean isPalindrome(String text)");

        for (Map.Entry<String, String> entry : contracts.entrySet()) {
            Path project = service.ensure(entry.getKey()).path();
            String className = ChallengeWorkspaceService.classNameOf(entry.getKey());
            String source = Files.readString(project.resolve("src/main/java/br/com/eyecode/challenge/" + className + ".java"));
            String tests = Files.readString(project.resolve("src/test/java/br/com/eyecode/challenge/" + className + "Test.java"));
            assertTrue(source.contains(entry.getValue()), entry.getKey() + " should have its documented method contract");
            assertTrue(tests.contains("org.junit.jupiter.api.Test"), entry.getKey() + " should include JUnit examples");
            assertFalse(tests.contains("hiddenTestCase"), entry.getKey() + " should not use the empty placeholder test");
        }
    }

    @Test
    void runTestsExecutesEveryFundamentalsJUnitSuite() {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        Map<String, Integer> suites = Map.of(
                "cnpj-validator", 3,
                "fizzbuzz", 3,
                "conversor-temperatura", 3,
                "ano-bissexto", 3,
                "media-aprovacao", 4,
                "palindromo", 4);

        for (Map.Entry<String, Integer> suite : suites.entrySet()) {
            ChallengeTestRun result = service.runTests(suite.getKey());
            assertEquals(suite.getValue(), result.tests().size(), suite.getKey() + " should execute its JUnit examples");
            assertTrue(result.tests().stream().noneMatch(test -> "test-build".equals(test.id())),
                    suite.getKey() + " should compile its starter and tests");
        }
    }

    @Test
    void objectOrientedChallengesCreateTaskSpecificStarterAndJUnitExamples() throws IOException {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        Map<String, String> contracts = Map.of(
                "conta-bancaria", "public boolean sacar(double valor)",
                "relogio-digital", "public void avancarMinutos(int minutos)",
                "formas-geometricas", "public interface Forma",
                "folha-pagamento", "abstract double salario()",
                "biblioteca", "public boolean possui(String titulo)",
                "estoque-produtos", "public boolean retirar(String nome, int quantidade)");

        for (Map.Entry<String, String> entry : contracts.entrySet()) {
            Path project = service.ensure(entry.getKey()).path();
            String className = ChallengeWorkspaceService.classNameOf(entry.getKey());
            String source = Files.readString(project.resolve("src/main/java/br/com/eyecode/challenge/" + className + ".java"));
            String tests = Files.readString(project.resolve("src/test/java/br/com/eyecode/challenge/" + className + "Test.java"));
            assertTrue(source.contains(entry.getValue()), entry.getKey() + " should have its documented method contract");
            assertTrue(source.contains("TODO"), entry.getKey() + " should guide the learner with TODO markers");
            assertTrue(tests.contains("org.junit.jupiter.api.Test"), entry.getKey() + " should include JUnit examples");
            assertFalse(tests.contains("hiddenTestCase"), entry.getKey() + " should not use the empty placeholder test");
        }
    }

    @Test
    void runTestsExecutesEveryObjectOrientedJUnitSuite() {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        Map<String, Integer> suites = Map.of(
                "conta-bancaria", 5,
                "relogio-digital", 5,
                "formas-geometricas", 4,
                "folha-pagamento", 4,
                "biblioteca", 4,
                "estoque-produtos", 4);

        for (Map.Entry<String, Integer> suite : suites.entrySet()) {
            ChallengeTestRun result = service.runTests(suite.getKey());
            assertEquals(suite.getValue(), result.tests().size(), suite.getKey() + " should execute its JUnit examples");
            assertTrue(result.tests().stream().noneMatch(test -> "test-build".equals(test.id())),
                    suite.getKey() + " should compile its starter and tests");
        }
    }

    @Test
    void dataStructureChallengesCreateTaskSpecificStarterAndJUnitExamples() throws IOException {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        Map<String, String> contracts = Map.ofEntries(
                Map.entry("busca-binaria", "public int buscaBinaria(int[] numeros, int alvo)"),
                Map.entry("ordenacao-bolha", "public int[] ordenarCrescente(int[] numeros)"),
                Map.entry("remover-duplicados", "public List<Integer> removerDuplicados(List<Integer> numeros)"),
                Map.entry("interseccao-listas", "public List<Integer> interseccao(List<Integer> a, List<Integer> b)"),
                Map.entry("contagem-caracteres", "public Map<Character, Integer> contarCaracteres(String texto)"),
                Map.entry("anagramas", "public List<List<String>> agruparAnagramas(List<String> palavras)"),
                Map.entry("par-com-soma", "public int[] parComSoma(int[] numerosOrdenados, int alvo)"),
                Map.entry("janela-maior-soma", "public int maiorSomaJanela(int[] numeros, int tamanho)"),
                Map.entry("fibonacci", "public long fibonacci(int n)"),
                Map.entry("caminhos-escada", "public long caminhosEscada(int degraus)"),
                Map.entry("busca-largura", "public int distancia(int vertices, int[][] arestas, int origem, int destino)"),
                Map.entry("menor-caminho", "public List<Integer> menorCaminho(int vertices, int[][] arestas, int origem, int destino)"));

        for (Map.Entry<String, String> entry : contracts.entrySet()) {
            Path project = service.ensure(entry.getKey()).path();
            String className = ChallengeWorkspaceService.classNameOf(entry.getKey());
            String source = Files.readString(project.resolve("src/main/java/br/com/eyecode/challenge/" + className + ".java"));
            String tests = Files.readString(project.resolve("src/test/java/br/com/eyecode/challenge/" + className + "Test.java"));
            assertTrue(source.contains(entry.getValue()), entry.getKey() + " should have its documented method contract");
            assertTrue(source.contains("TODO"), entry.getKey() + " should guide the learner with TODO markers");
            assertTrue(tests.contains("org.junit.jupiter.api.Test"), entry.getKey() + " should include JUnit examples");
            assertFalse(tests.contains("hiddenTestCase"), entry.getKey() + " should not use the empty placeholder test");
        }
    }

    @Test
    void runTestsExecutesEveryDataStructureJUnitSuite() {
        ChallengeWorkspaceService service = new ChallengeWorkspaceService(temporary);
        Map<String, Integer> suites = Map.ofEntries(
                Map.entry("busca-binaria", 4),
                Map.entry("ordenacao-bolha", 4),
                Map.entry("remover-duplicados", 4),
                Map.entry("interseccao-listas", 4),
                Map.entry("contagem-caracteres", 4),
                Map.entry("anagramas", 4),
                Map.entry("par-com-soma", 4),
                Map.entry("janela-maior-soma", 4),
                Map.entry("fibonacci", 4),
                Map.entry("caminhos-escada", 5),
                Map.entry("busca-largura", 4),
                Map.entry("menor-caminho", 4));

        for (Map.Entry<String, Integer> suite : suites.entrySet()) {
            ChallengeTestRun result = service.runTests(suite.getKey());
            assertEquals(suite.getValue(), result.tests().size(), suite.getKey() + " should execute its JUnit examples");
            assertTrue(result.tests().stream().noneMatch(test -> "test-build".equals(test.id())),
                    suite.getKey() + " should compile its starter and tests");
        }
    }
}
