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

    private final ProjectRunner javaRunner = new JavaRunner();

    private final ProjectRunner mavenRunner = new MavenRunner();


    public String runProject(File projectRoot) {

        BuildSystem buildSystem = BuildSystemDetector.detected(projectRoot);

        System.out.println(buildSystem);

        return switch (buildSystem) {

            case MAVEN -> mavenRunner.run(projectRoot);

            case PLAIN_JAVA -> javaRunner.run(projectRoot);
        };

    }

}