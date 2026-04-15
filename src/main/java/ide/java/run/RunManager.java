package ide.java.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Responsável por compilar e executar arquivos Java.
 *
 * Atua como um "orquestrador de processos":
 * - chama o compilador (javac)
 * - executa a classe (java)
 * - captura saída e erros
 *
 * Não interpreta Java → delega para o próprio JDK.
 */
public class RunManager {

    /**
     * Compila e executa um arquivo Java.
     *
     * @param file Arquivo .java a ser executado
     * @return Saída completa (erros + execução)
     */
    public String runJavaFile(File file){
        StringBuilder output = new StringBuilder();

        try {
            // Diretorio onde o arquivo esta localizado
            File dir = file.getParentFile();

            /**
             * ETAPA 1: COMPILAÇÃO
             *
             * Executa: javac NomeArquivo.java
             */
            ProcessBuilder compile = new ProcessBuilder(
                    "javac",
                    file.getName()
            );

            // Define o diretório de execução
            compile.directory(dir);

            Process compileProcess = compile.start();

            // Captura erros de compilacao (stderr)
            BufferedReader compileError = new BufferedReader(
                    new InputStreamReader(compileProcess.getErrorStream())
            );

            String line;

            // Le todas as mensagens de erro
            while ((line = compileError.readLine()) != null){
                output.append(line).append("\n");
            }

            // Espera a compilacao terminar
            compileProcess.waitFor();

            /**
             * ETAPA 2: EXECUÇÃO
             *
             * Executa: java NomeClasse
             */
            String className = file.getName().replace(".java", "");

            ProcessBuilder run = new ProcessBuilder(
                    "java",
                    className
            );
            run.directory(dir);

            Process runProcess = run.start();

            // Captura a saida normal (stdout)
            BufferedReader runOutput = new BufferedReader(
                    new InputStreamReader(runProcess.getInputStream())
            );

            // Le a saida do programa
            while ((line = runOutput.readLine()) != null){
                output.append(line).append("\n");
            }

            runProcess.waitFor();

        }catch (Exception e){

            // Tratamento generico de erro
            output.append("Error: ").append(e.getMessage());
        }
        return output.toString();
    }
}
