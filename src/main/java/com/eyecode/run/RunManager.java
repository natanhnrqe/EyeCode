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
public class RunManager {

    /**
     * Scanner responsável por localizar
     * todos os arquivos .java do projeto.
     */
    private final ProjectScanner scanner = new ProjectScanner();

    /**
     * Compila e executa um projeto Java inteiro.
     *
     * @param projectRoot raiz do projeto
     *                    Exemplo:
     *                    EyeCode/
     *
     * @param mainClass classe principal completa
     *                  Ex:
     *                  com.eyecode.Main
     *
     * @return saída da compilação + execução
     */
    public String runProject(File projectRoot, String mainClass) {

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
            File srcRoot = projectRoot;


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

            if (javaFiles.isEmpty()) {
                return "No Java files found.";
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

            compileCommand.add("javac");

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
            ProcessBuilder runBuilder = new ProcessBuilder(
                    "java",
                    "-cp",
                    "out",
                    mainClass
            );

            runBuilder.directory(projectRoot);

            Process runProcess = runBuilder.start();

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

}
