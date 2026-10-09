package com.eyecode.challenge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChallengeWorkspaceService {
    public record EnsureResult(Path path, boolean fresh) {
    }

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9-]{1,64}");

    private final Path root;

    public ChallengeWorkspaceService() {
        this(Path.of(System.getProperty("user.home"), ".eyecode", "challenges"));
    }

    public ChallengeWorkspaceService(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public synchronized EnsureResult ensure(String id) {
        Path directory = challengeDirectory(id).toAbsolutePath().normalize();
        boolean fresh = !Files.isRegularFile(entryFile(directory, classNameOf(id)));
        if (fresh) scaffold(directory, id);
        else refreshUnchangedStarter(directory, id);
        return new EnsureResult(directory, fresh);
    }

    public synchronized String mainFilePath(String id) {
        Path directory = challengeDirectory(id).toAbsolutePath().normalize();
        return directory.resolve(mainClassPath(classNameOf(id))).toString();
    }

    public synchronized boolean exists(String id) {
        Path directory = challengeDirectory(id);
        return Files.isRegularFile(entryFile(directory, classNameOf(id)));
    }

    public synchronized void reset(String id) {
        Path directory = challengeDirectory(id);
        String className = classNameOf(id);
        write(directory.resolve(mainClassPath(className)), starterSource(id, className));
        write(directory.resolve(testClassPath(className)), testSource(id, className));
        refreshMavenTestPlugins(directory.resolve("pom.xml"), id);
    }

    public synchronized ChallengeTestRun runTests(String id) {
        Path directory = challengeDirectory(id).toAbsolutePath().normalize();
        ensure(id);
        Path reports = directory.resolve("target/surefire-reports");
        clearTestReports(reports);
        Path output = null;
        try {
            output = Files.createTempFile("eyecode-challenge-", ".log");
            List<String> command = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                    ? List.of("cmd.exe", "/c", "mvn", "-q", "-Dstyle.color=never", "test")
                    : List.of("mvn", "-q", "-Dstyle.color=never", "test");
            Process process = new ProcessBuilder(command).directory(directory.toFile())
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                return new ChallengeTestRun(List.of(failure("runner-timeout", "Execução dos testes", "O Maven excedeu o limite de 180 segundos.", readTail(output))),
                        "A execução dos testes excedeu o tempo limite.");
            }
            List<ChallengeTestRun.TestCase> cases = readTestReports(reports);
            if (cases.isEmpty() || cases.stream().allMatch(test -> "hiddenTestCase".equals(test.name()))) {
                return new ChallengeTestRun(List.of(failure("no-exercise-tests", "Nenhum teste de exercício encontrado.",
                        "Restaure os arquivos iniciais do desafio para criar a suíte JUnit.", readTail(output))),
                        "Nenhum teste de exercício foi encontrado.");
            }
            String message = process.exitValue() == 0 ? "Testes executados." : "Existem testes com falha ou erro de compilação.";
            return new ChallengeTestRun(cases, message);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ChallengeTestRun(List.of(failure("runner-interrupted", "Execução interrompida", "A execução foi interrompida.", "")),
                    "A execução dos testes foi interrompida.");
        } catch (IOException exception) {
            return new ChallengeTestRun(List.of(failure("runner-unavailable", "Maven indisponível", exception.getMessage(), "")),
                    "Não foi possível iniciar o Maven.");
        } finally {
            if (output != null) {
                try { Files.deleteIfExists(output); } catch (IOException ignored) { }
            }
        }
    }

    private Path challengeDirectory(String id) {
        requireValidId(id);
        return root.resolve(id);
    }

    private static void scaffold(Path directory, String id) {
        String className = classNameOf(id);
        write(directory.resolve("pom.xml"), pomSource(id));
        write(directory.resolve(mainClassPath(className)), starterSource(id, className));
        write(directory.resolve(testClassPath(className)), testSource(id, className));
    }

    private static void refreshUnchangedStarter(Path directory, String id) {
        String className = classNameOf(id);
        Path source = directory.resolve(mainClassPath(className));
        Path tests = directory.resolve(testClassPath(className));
        try {
            if (Files.readString(source).equals(starterSource(className))) {
                write(source, starterSource(id, className));
            }
            if (Files.exists(tests) && Files.readString(tests).equals(legacyTestSource(className))) {
                write(tests, testSource(id, className));
            }
        } catch (IOException ignored) { }
        refreshMavenTestPlugins(directory.resolve("pom.xml"), id);
    }

    private static void refreshMavenTestPlugins(Path pom, String id) {
        if (!Files.isRegularFile(pom)) return;
        try {
            String xml = Files.readString(pom, StandardCharsets.UTF_8);
            if (!xml.contains("junit-jupiter") || xml.contains("maven-surefire-plugin")) return;
            Matcher build = Pattern.compile("(?s)  <build>.*?</build>\\R").matcher(pomSource(id));
            if (!build.find()) return;
            int dependencies = xml.indexOf("  <dependencies>");
            if (dependencies >= 0) write(pom, xml.substring(0, dependencies) + build.group() + xml.substring(dependencies));
        } catch (IOException ignored) { }
    }

    private static String legacyTestSource(String className) {
        return """
                package br.com.eyecode.challenge;

                import org.junit.jupiter.api.Test;

                class %sTest {

                    @Test
                    void hiddenTestCase() {
                    }
                }
                """.formatted(className);
    }

    private static Path entryFile(Path directory, String className) {
        return directory.resolve(mainClassPath(className));
    }

    private static String mainClassPath(String className) {
        return "src/main/java/br/com/eyecode/challenge/" + className + ".java";
    }

    private static void clearTestReports(Path reports) {
        if (!Files.isDirectory(reports)) return;
        try (var files = Files.list(reports)) {
            files.filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    private static List<ChallengeTestRun.TestCase> readTestReports(Path reports) {
        if (!Files.isDirectory(reports)) return List.of();
        Pattern testcase = Pattern.compile("<testcase\\b([^>]*?)(?:/\\s*>|>(.*?)</testcase>)", Pattern.DOTALL);
        Pattern attributeName = Pattern.compile("\\bname=\"([^\"]*)\"");
        Pattern attributeClass = Pattern.compile("\\bclassname=\"([^\"]*)\"");
        Pattern failureMessage = Pattern.compile("<(?:failure|error)\\b[^>]*?message=\"([^\"]*)\"");
        List<ChallengeTestRun.TestCase> results = new ArrayList<>();
        try (var files = Files.list(reports)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".xml")).sorted(Comparator.naturalOrder()).toList()) {
                String xml = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matches = testcase.matcher(xml);
                while (matches.find()) {
                    String attributes = matches.group(1);
                    String body = matches.group(2) == null ? "" : matches.group(2);
                    String name = attribute(attributeName, attributes, file.getFileName().toString());
                    String className = attribute(attributeClass, attributes, "test");
                    Matcher failed = failureMessage.matcher(body);
                    boolean isFailure = body.contains("<failure") || body.contains("<error");
                    String message = failed.find() ? decodeXml(failed.group(1)) : "";
                    String stack = isFailure ? stripXml(body) : "";
                    results.add(new ChallengeTestRun.TestCase(className + "#" + name, name,
                            isFailure ? "failure" : "success", message, stack));
                }
            }
        } catch (IOException ignored) { }
        return List.copyOf(results);
    }

    private static String attribute(Pattern pattern, String attributes, String fallback) {
        Matcher matcher = pattern.matcher(attributes);
        return matcher.find() ? decodeXml(matcher.group(1)) : fallback;
    }

    private static String decodeXml(String value) {
        return value.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&amp;", "&");
    }

    private static String stripXml(String body) {
        String plain = body.replaceAll("(?s)<\\!\\[CDATA\\[(.*?)]]>", "$1")
                .replaceAll("<[^>]+>", "").replace("&#10;", "\n");
        return decodeXml(plain).trim();
    }

    private static String readTail(Path file) {
        try (var input = new java.io.RandomAccessFile(file.toFile(), "r")) {
            long length = input.length();
            int size = (int) Math.min(length, 16000);
            byte[] bytes = new byte[size];
            input.seek(length - size);
            input.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            return "";
        }
    }

    private static ChallengeTestRun.TestCase failure(String id, String name, String message, String stack) {
        return new ChallengeTestRun.TestCase(id, name, "failure", message == null ? "" : message,
                stack == null ? "" : stack);
    }

    private static String testClassPath(String className) {
        return "src/test/java/br/com/eyecode/challenge/" + className + "Test.java";
    }

    static void requireValidId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid challenge id: " + id);
        }
    }

    static String classNameOf(String id) {
        StringBuilder name = new StringBuilder();
        for (String part : id.split("-")) {
            if (part.isEmpty()) continue;
            name.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) name.append(part.substring(1));
        }
        return name.toString();
    }

    static String pomSource(String id) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>br.com.eyecode.challenge</groupId>
                  <artifactId>%s</artifactId>
                  <version>1.0.0</version>
                  <packaging>jar</packaging>
                  <properties>
                    <maven.compiler.release>21</maven.compiler.release>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.15.0</version>
                      </plugin>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>3.5.4</version>
                      </plugin>
                    </plugins>
                  </build>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.10.2</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(id);
    }

    static String starterSource(String className) {
        return """
                package br.com.eyecode.challenge;

                public class %s {

                    public boolean solve(String input) {
                        return false;
                    }
                }
                """.formatted(className);
    }

    static String starterSource(String id, String className) {
        return switch (id) {
            case "cnpj-validator" -> source(className, "public boolean isValid(String cnpj) {\n        return false;\n    }");
            case "fizzbuzz" -> source(className, "public java.util.List<String> fizzBuzz(int start, int end) {\n        return java.util.List.of();\n    }");
            case "conversor-temperatura" -> source(className, "public double toFahrenheit(double celsius) {\n        return 0.0;\n    }");
            case "ano-bissexto" -> source(className, "public boolean isLeapYear(int year) {\n        return false;\n    }");
            case "media-aprovacao" -> source(className, "public boolean isApproved(double[] grades) {\n        return false;\n    }");
            case "palindromo" -> source(className, "public boolean isPalindrome(String text) {\n        return false;\n    }");
            case "conta-bancaria" -> """
                    package br.com.eyecode.challenge;

                    public class %s {

                        private double saldo;

                        public %s(double saldoInicial) {
                            // TODO: inicialize o saldo com o valor informado
                        }

                        public void depositar(double valor) {
                            // TODO: some valores positivos ao saldo
                        }

                        public boolean sacar(double valor) {
                            // TODO: devolva false quando o valor for inválido ou maior que o saldo
                            return false;
                        }

                        public double getSaldo() {
                            return saldo;
                        }
                    }
                    """.formatted(className, className);
            case "relogio-digital" -> """
                    package br.com.eyecode.challenge;

                    public class %s {

                        private int hora;
                        private int minuto;

                        public %s() {
                            // TODO: inicie o relógio às 00:00
                        }

                        public %s(int hora, int minuto) {
                            // TODO: valide hora (0..23) e minuto (0..59) e armazene o estado
                        }

                        public void avancarMinutos(int minutos) {
                            // TODO: avance o relógio com retorno à meia-noite (ciclo de 24h)
                        }

                        @Override
                        public String toString() {
                            // TODO: formate o horário como HH:MM
                            return "00:00";
                        }
                    }
                    """.formatted(className, className, className);
            case "formas-geometricas" -> """
                    package br.com.eyecode.challenge;

                    public class %s {

                        public interface Forma {
                            double area();
                            double perimetro();
                        }

                        public static class Circulo implements Forma {

                            private final double raio;

                            public Circulo(double raio) {
                                this.raio = raio;
                            }

                            @Override
                            public double area() {
                                // TODO: use Math.PI * raio * raio
                                return 0.0;
                            }

                            @Override
                            public double perimetro() {
                                // TODO: use 2 * Math.PI * raio
                                return 0.0;
                            }
                        }

                        public static class Retangulo implements Forma {

                            private final double largura;
                            private final double altura;

                            public Retangulo(double largura, double altura) {
                                this.largura = largura;
                                this.altura = altura;
                            }

                            @Override
                            public double area() {
                                // TODO: use largura * altura
                                return 0.0;
                            }

                            @Override
                            public double perimetro() {
                                // TODO: use 2 * (largura + altura)
                                return 0.0;
                            }
                        }
                    }
                    """.formatted(className);
            case "folha-pagamento" -> """
                    package br.com.eyecode.challenge;

                    import java.util.ArrayList;
                    import java.util.List;

                    public class %s {

                        public abstract static class Funcionario {

                            private final String nome;

                            protected Funcionario(String nome) {
                                this.nome = nome;
                            }

                            public String getNome() {
                                return nome;
                            }

                            public abstract double salario();
                        }

                        public static class Horista extends Funcionario {

                            private final int horas;
                            private final double valorHora;

                            public Horista(String nome, int horas, double valorHora) {
                                super(nome);
                                this.horas = horas;
                                this.valorHora = valorHora;
                            }

                            @Override
                            public double salario() {
                                // TODO: devolva horas * valorHora
                                return 0.0;
                            }
                        }

                        public static class Assalariado extends Funcionario {

                            private final double salarioMensal;

                            public Assalariado(String nome, double salarioMensal) {
                                super(nome);
                                this.salarioMensal = salarioMensal;
                            }

                            @Override
                            public double salario() {
                                // TODO: devolva o salário mensal fixo
                                return 0.0;
                            }
                        }

                        private final List<Funcionario> funcionarios = new ArrayList<>();

                        public void contratar(Funcionario funcionario) {
                            funcionarios.add(funcionario);
                        }

                        public double totalDaFolha() {
                            // TODO: some salario() de cada funcionário contratado
                            return 0.0;
                        }
                    }
                    """.formatted(className);
            case "biblioteca" -> """
                    package br.com.eyecode.challenge;

                    import java.util.ArrayList;
                    import java.util.List;

                    public class %s {

                        public static class Livro {

                            private final String titulo;
                            private final int paginas;

                            public Livro(String titulo, int paginas) {
                                this.titulo = titulo;
                                this.paginas = paginas;
                            }

                            public String getTitulo() {
                                return titulo;
                            }

                            public int getPaginas() {
                                return paginas;
                            }
                        }

                        private final List<Livro> livros = new ArrayList<>();

                        public void adicionar(String titulo, int paginas) {
                            // TODO: crie um Livro e guarde na lista interna
                        }

                        public int totalDePaginas() {
                            // TODO: some as páginas de todos os livros
                            return 0;
                        }

                        public boolean possui(String titulo) {
                            // TODO: procure um livro com o título exato
                            return false;
                        }

                        public int quantidadeDeLivros() {
                            return livros.size();
                        }
                    }
                    """.formatted(className);
            case "estoque-produtos" -> """
                    package br.com.eyecode.challenge;

                    import java.util.ArrayList;
                    import java.util.List;

                    public class %s {

                        public static class Produto {

                            private final String nome;
                            private final double preco;
                            private int quantidade;

                            public Produto(String nome, double preco, int quantidade) {
                                this.nome = nome;
                                this.preco = preco;
                                this.quantidade = quantidade;
                            }

                            public String getNome() {
                                return nome;
                            }

                            public double getPreco() {
                                return preco;
                            }

                            public int getQuantidade() {
                                return quantidade;
                            }

                            public void reduzir(int quantidade) {
                                if (quantidade > this.quantidade) {
                                    throw new IllegalArgumentException("quantidade insuficiente");
                                }
                                this.quantidade -= quantidade;
                            }
                        }

                        private final List<Produto> produtos = new ArrayList<>();

                        public void registrar(String nome, double preco, int quantidade) {
                            // TODO: adicione um novo Produto à lista interna
                        }

                        public List<Produto> getProdutos() {
                            // TODO: devolva uma cópia defensiva da lista de produtos
                            return List.of();
                        }

                        public boolean retirar(String nome, int quantidade) {
                            // TODO: encontre o produto, confira o saldo e chame reduzir
                            return false;
                        }

                        public double valorEmEstoque() {
                            // TODO: some preco * quantidade de cada produto
                            return 0.0;
                        }
                    }
                    """.formatted(className);
            default -> starterSource(className);
        };
    }

    private static String source(String className, String method) {
        return "package br.com.eyecode.challenge;\n\npublic class " + className + " {\n    " + method + "\n}\n";
    }

    static String testSource(String id, String className) {
        String tests = switch (id) {
            case "cnpj-validator" -> """
                    @Test void acceptsValidCnpj() { assertTrue(new %s().isValid("04.252.011/0001-10")); }
                    @Test void rejectsInvalidCnpj() { assertFalse(new %s().isValid("04.252.011/0001-11")); }
                    @Test void rejectsRepeatedDigits() { assertFalse(new %s().isValid("00000000000000")); }
                    """.formatted(className, className, className);
            case "fizzbuzz" -> """
                    @Test void processesInclusiveRange() { assertEquals(java.util.List.of("1", "2", "Fizz", "4", "Buzz"), new %s().fizzBuzz(1, 5)); }
                    @Test void handlesMultiplesOfBothDivisors() { assertEquals(java.util.List.of("FizzBuzz"), new %s().fizzBuzz(15, 15)); }
                    @Test void handlesZeroAndNegativeNumbers() { assertEquals(java.util.List.of("Fizz", "-2", "-1", "FizzBuzz"), new %s().fizzBuzz(-3, 0)); }
                    """.formatted(className, className, className);
            case "conversor-temperatura" -> """
                    @Test void convertsFreezingPoint() { assertEquals(32.0, new %s().toFahrenheit(0.0), 0.0001); }
                    @Test void convertsBoilingPoint() { assertEquals(212.0, new %s().toFahrenheit(100.0), 0.0001); }
                    @Test void convertsNegativeValues() { assertEquals(-40.0, new %s().toFahrenheit(-40.0), 0.0001); }
                    """.formatted(className, className, className);
            case "ano-bissexto" -> """
                    @Test void acceptsDivisibleByFour() { assertTrue(new %s().isLeapYear(2024)); }
                    @Test void rejectsCenturyNotDivisibleByFourHundred() { assertFalse(new %s().isLeapYear(1900)); }
                    @Test void acceptsDivisibleByFourHundred() { assertTrue(new %s().isLeapYear(2000)); }
                    """.formatted(className, className, className);
            case "media-aprovacao" -> """
                    @Test void approvesAverageAtThreshold() { assertTrue(new %s().isApproved(new double[]{7.0, 7.0})); }
                    @Test void rejectsAverageBelowThreshold() { assertFalse(new %s().isApproved(new double[]{5.0, 6.0})); }
                    @Test void rejectsEmptyGrades() { assertFalse(new %s().isApproved(new double[]{})); }
                    @Test void rejectsOutOfRangeGrade() { assertFalse(new %s().isApproved(new double[]{11.0})); }
                    """.formatted(className, className, className, className);
            case "palindromo" -> """
                    @Test void acceptsPhraseIgnoringSpacesAndPunctuation() { assertTrue(new %s().isPalindrome("Socorram-me, subi no ônibus em Marrocos")); }
                    @Test void ignoresLetterCase() { assertTrue(new %s().isPalindrome("EyeCode edocEYE")); }
                    @Test void rejectsNonPalindrome() { assertFalse(new %s().isPalindrome("EyeCode")); }
                    @Test void rejectsNullAndNoLetters() { assertFalse(new %s().isPalindrome(null)); assertFalse(new %s().isPalindrome(" !!! ")); }
                    """.formatted(className, className, className, className, className);
            case "conta-bancaria" -> """
                    @Test void startingBalanceIsPreserved() { assertEquals(100.0, new %s(100.0).getSaldo()); }
                    @Test void depositIncreasesBalance() { %s conta = new %s(0.0); conta.depositar(50.0); assertEquals(50.0, conta.getSaldo()); }
                    @Test void withdrawalBelowBalanceSucceeds() { %s conta = new %s(100.0); assertTrue(conta.sacar(30.0)); assertEquals(70.0, conta.getSaldo()); }
                    @Test void withdrawalExceedingBalanceIsRejected() { %s conta = new %s(100.0); assertFalse(conta.sacar(200.0)); assertEquals(100.0, conta.getSaldo()); }
                    @Test void invalidAmountsDoNotChangeBalance() { %s conta = new %s(100.0); conta.depositar(-50.0); assertFalse(conta.sacar(0.0)); assertEquals(100.0, conta.getSaldo()); }
                    """.formatted(className, className, className, className, className, className, className, className, className);
            case "relogio-digital" -> """
                    @Test void defaultStartsAtMidnight() { assertEquals("00:00", new %s().toString()); }
                    @Test void formatsTwoDigits() { assertEquals("09:05", new %s(9, 5).toString()); }
                    @Test void advancesAcrossHourBoundary() { %s relogio = new %s(9, 55); relogio.avancarMinutos(10); assertEquals("10:05", relogio.toString()); }
                    @Test void wrapsAfterMidnight() { %s relogio = new %s(23, 50); relogio.avancarMinutos(20); assertEquals("00:10", relogio.toString()); }
                    @Test void rejectsInvalidTimeAndNegativeAdvance() { assertThrows(IllegalArgumentException.class, () -> new %s(24, 0)); assertThrows(IllegalArgumentException.class, () -> new %s(9, 60)); assertThrows(IllegalArgumentException.class, () -> new %s(9, 0).avancarMinutos(-1)); }
                    """.formatted(className, className, className, className, className, className, className, className, className);
            case "formas-geometricas" -> """
                    @Test void circleMeasuresUsePi() { %s.Circulo circulo = new %s.Circulo(1.0); assertEquals(Math.PI, circulo.area(), 1e-9); assertEquals(2 * Math.PI, circulo.perimetro(), 1e-9); }
                    @Test void rectangleMeasuresAreExact() { %s.Retangulo retangulo = new %s.Retangulo(3.0, 4.0); assertEquals(12.0, retangulo.area(), 1e-9); assertEquals(14.0, retangulo.perimetro(), 1e-9); }
                    @Test void sumsAreasThroughTheInterface() { java.util.List<%s.Forma> formas = java.util.List.of(new %s.Circulo(1.0), new %s.Retangulo(3.0, 4.0)); double total = 0.0; for (%s.Forma forma : formas) { total += forma.area(); } assertEquals(Math.PI + 12.0, total, 1e-9); }
                    @Test void scalesMeasuresWithRadius() { %s.Circulo circulo = new %s.Circulo(2.0); assertEquals(4 * Math.PI, circulo.area(), 1e-9); assertEquals(4 * Math.PI, circulo.perimetro(), 1e-9); }
                    """.formatted(className, className, className, className, className, className, className, className, className, className);
            case "folha-pagamento" -> """
                    @Test void horistaSalaryIsHoursTimesRate() { assertEquals(1000.0, new %s.Horista("Ana", 40, 25.0).salario(), 1e-9); }
                    @Test void assalariadoSalaryIsMonthlyValue() { assertEquals(3500.0, new %s.Assalariado("Bia", 3500.0).salario(), 1e-9); }
                    @Test void totalSumsEveryContractedEmployee() { %s folha = new %s(); folha.contratar(new %s.Horista("Ana", 40, 25.0)); folha.contratar(new %s.Assalariado("Bia", 3500.0)); assertEquals(4500.0, folha.totalDaFolha(), 1e-9); }
                    @Test void emptyPayrollTotalsZero() { assertEquals(0.0, new %s().totalDaFolha(), 1e-9); }
                    """.formatted(className, className, className, className, className, className, className);
            case "biblioteca" -> """
                    @Test void addedBooksAreCounted() { %s biblioteca = new %s(); biblioteca.adicionar("Clean Code", 400); biblioteca.adicionar("Refatoração", 440); assertEquals(2, biblioteca.quantidadeDeLivros()); }
                    @Test void totalPagesSumsEveryBook() { %s biblioteca = new %s(); biblioteca.adicionar("Clean Code", 400); biblioteca.adicionar("Refatoração", 440); assertEquals(840, biblioteca.totalDePaginas()); }
                    @Test void findsBookByExactTitle() { %s biblioteca = new %s(); biblioteca.adicionar("Clean Code", 400); assertTrue(biblioteca.possui("Clean Code")); assertFalse(biblioteca.possui("clean code")); }
                    @Test void emptyLibraryHasNoBooks() { %s biblioteca = new %s(); assertEquals(0, biblioteca.quantidadeDeLivros()); assertEquals(0, biblioteca.totalDePaginas()); assertFalse(biblioteca.possui("Clean Code")); }
                    """.formatted(className, className, className, className, className, className, className, className);
            case "estoque-produtos" -> """
                    @Test void registrarAddsProductToList() { %s estoque = new %s(); estoque.registrar("Arroz", 2.5, 4); assertEquals(1, estoque.getProdutos().size()); assertEquals("Arroz", estoque.getProdutos().get(0).getNome()); }
                    @Test void valorEmEstoqueSumsPriceTimesQuantity() { %s estoque = new %s(); estoque.registrar("Arroz", 2.5, 4); estoque.registrar("Feijão", 4.0, 2); assertEquals(18.0, estoque.valorEmEstoque(), 1e-9); }
                    @Test void retirarReducesStockWhenSufficient() { %s estoque = new %s(); estoque.registrar("Arroz", 2.5, 4); assertTrue(estoque.retirar("Arroz", 1)); assertEquals(3, estoque.getProdutos().get(0).getQuantidade()); }
                    @Test void retirarRejectsInsufficientStockOrUnknownProduct() { %s estoque = new %s(); estoque.registrar("Arroz", 2.5, 4); assertFalse(estoque.retirar("Arroz", 10)); assertFalse(estoque.retirar("Feijão", 1)); assertEquals(4, estoque.getProdutos().get(0).getQuantidade()); }
                    """.formatted(className, className, className, className, className, className, className, className);
            default -> "@Test void starterCompiles() { assertNotNull(new %s()); }".formatted(className);
        };
        return "package br.com.eyecode.challenge;\n\nimport org.junit.jupiter.api.Test;\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass " + className + "Test {\n    " + tests.replace("\n", "\n    ") + "\n}\n";
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write challenge file: " + file, exception);
        }
    }
}
