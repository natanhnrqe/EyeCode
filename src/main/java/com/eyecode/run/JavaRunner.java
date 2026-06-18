package com.eyecode.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsável por:
 *
 * - escanear o projeto
 * - compilar todos os arquivos Java
 * - executar a classe principal
 *
 * Diferente da primeira versão:
 * esta classe agora entende PROJETOS,
 * e não apenas arquivos isolados.
 */
public class JavaRunner implements ProjectRunner {

    private volatile Process currentProcess;

    @Override
    public void stop() {
        Process p = currentProcess;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
    }

    @Override
    public String run(File projectRoot) {

            /**
             * Scanner responsável por localizar
             * todos os arquivos .java do projeto.
             */
            final ProjectScanner scanner = new ProjectScanner();
            MainClassFinder finder = new MainClassFinder();

            /**
             * Compila e executa um projeto Java inteiro.
             *
             * @param projectRoot raiz do projeto
             *                    Exemplo:
             *                    EyeCode/
             *
             * @return saída da compilação + execução
             */

                StringBuilder output = new StringBuilder();

                try {

                    /**
                     * =========================
                     * SOURCE ROOT
                     * =========================
                     *
                     * Agora trabalhamos com:
                     *
                     * projeto inteiro
                     *
                     * e NÃO mais com arquivo solto.
                     *
                     * Então a source root fica:
                     *
                     * EyeCode/src/main/java
                     */
                    File srcRoot =
                            new File(
                                    projectRoot,
                                    "src/main/java"
                            );

                    if (!srcRoot.exists()) {

                        return """
                           Invalid project structure.
                
                           Expected:
                           src/main/java
                
                           Selected root:
                           """ + projectRoot.getAbsolutePath();
                    }


                    /**
                     * =========================
                     * OUT DIRECTORY
                     * =========================
                     *
                     * Diretório onde os .class
                     * compilados serão gerados.
                     *
                     * Ex:
                     *
                     * out/com/eyecode/Main.class
                     */
                    File outDir = new File(projectRoot, "out");

                    if (!outDir.exists()) {
                        outDir.mkdirs();
                    }

                    /**
                     * =========================
                     * SCAN PROJECT
                     * =========================
                     *
                     * Procura TODOS os .java
                     * do projeto recursivamente.
                     */
                    List<File> javaFiles = scanner.findJavaFiles(srcRoot);

                    String mainClass = finder.findMainClass(srcRoot);


                    if (mainClass == null) {
                        return "Main class not found.";
                    }

                    /**
                     * =========================
                     * BUILD COMPILE COMMAND
                     * =========================
                     *
                     * Monta dinamicamente:
                     *
                     * javac -d out arquivo1.java arquivo2.java ...
                     *
                     * Isso substitui o antigo:
                     *
                     * javac Main.java
                     */
                    List<String> compileCommand = new ArrayList<>();

                    String classPath = buildLibraryClasspath(projectRoot);

                    compileCommand.add("javac");

                    compileCommand.add("-cp");
                    compileCommand.add(classPath);

                    /**
                     * -d
                     *
                     * Define pasta de saída
                     * dos arquivos compilados.
                     */
                    compileCommand.add("-d");
                    compileCommand.add("out");

                    /**
                     * Adiciona TODOS os arquivos Java
                     * encontrados no projeto.
                     */
                    for (File file : javaFiles) {
                        compileCommand.add(file.getAbsolutePath());
                    }

                    /**
                     * =========================
                     * COMPILE PROCESS
                     * =========================
                     */
                    ProcessBuilder compileBuilder = new ProcessBuilder(compileCommand);
                    compileBuilder.directory(projectRoot);

                    Process compileProcess = compileBuilder.start();
                    currentProcess = compileProcess;

                    /**
                     * Captura erros de compilação.
                     */
                    BufferedReader compileError = new BufferedReader(
                            new InputStreamReader(compileProcess.getErrorStream()
                            )
                    );

                    String line;

                    while ((line = compileError.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    compileProcess.waitFor();

                    /**
                     * Copia resources para o runtime.
                     *
                     * Ex:
                     * icons/
                     * themes/
                     * fonts/
                     */
                    File resourcesDir = new File(
                            projectRoot,
                            "src/main/resources"
                    );

                    copyResources(resourcesDir, outDir);

                    int compileExit = compileProcess.exitValue();

                    output.append(
                            "\nCompile Exit Code: "
                    ).append(compileExit).append("\n");

                    /**
                     * Se houve erro de compilacao,
                     * interrompe a execucao.
                     */
                    if (compileExit != 0) {
                        return output.toString();
                    }

                    /**
                     * =========================
                     * RUN COMMAND
                     * =========================
                     *
                     * Executa:
                     *
                     * java -cp out com.eyecode.Main
                     *
                     * Agora usamos classpath.
                     *
                     * Isso permite:
                     *
                     * - imports
                     * - packages
                     * - múltiplas classes
                     */

                    System.out.println("opa" + compileCommand);


                    ProcessBuilder runBuilder = new ProcessBuilder(
                            "java",
                            "-cp",
                            classPath,
                            mainClass
                    );

                    runBuilder.directory(projectRoot);

                    Process runProcess = runBuilder.start();
                    currentProcess = runProcess;

                    /**
                     * Captura saída normal.
                     */
                    BufferedReader runOutput = new BufferedReader(
                            new InputStreamReader(runProcess.getInputStream())
                    );

                    while ((line = runOutput.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    /**
                     * Captura erros da JVM.
                     *
                     * MUITO importante para debug.
                     */
                    BufferedReader runErrors = new BufferedReader(
                            new InputStreamReader(runProcess.getErrorStream())
                    );

                    while ((line = runErrors.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    runProcess.waitFor();

                } catch (Exception e) {
                    output.append("Error: ")
                            .append(e.getMessage());
                }
                return output.toString();
            }

    private void copyResources(File source, File target) {

        // Se a pasta não existir, não faz nada
        if (!source.exists()) {
            return;
        }

        /**
         * Se for diretório:
         * cria no destino
         * e copia filhos recursivamente.
         */
        if (source.isDirectory()) {

            if (!target.exists()) {
                target.mkdirs();
            }

            File[] files = source.listFiles();

            if (files != null) {
                for (File file : files) {

                    copyResources(
                            file,
                            new File(target, file.getName())
                    );
                }
            }

            return;
        }

        /**
         * Se for arquivo:
         * copia para destino.
         */
        try {

            Files.copy(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private String buildLibraryClasspath(File projectRoot) {

        File libsDir = new File(projectRoot, "libs");



        String separator = System.getProperty("os.name")
                .toLowerCase()
                .contains("win")
                ? ";" : ":";

        StringBuilder classPath = new StringBuilder("out");

        if (!libsDir.exists()) {
            return classPath.toString();
        }

        File[] files = libsDir.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file.getName().endsWith(".jar")) {

                    classPath.append(separator).append(file.getAbsolutePath());
                }
            }
        }

        return classPath.toString();
    }
}







