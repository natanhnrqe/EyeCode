package com.eyecode.javafx;

import com.eyecode.javafx.ui.FxRootLayout;
import com.eyecode.workbench.toolwindow.ToolWindowPosition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sprint32IntegrationTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void rootLayoutSwapsToolWindowContentDynamically() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        final StringBuilder report = new StringBuilder();

        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        System.setErr(new PrintStream(errBuf, true, StandardCharsets.UTF_8));

        Platform.runLater(() -> {
            try {
                FxRootLayout root = new FxRootLayout(() -> {});
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(
                        getClass().getResource("/javafx/style/eyecode.css").toExternalForm());
                root.applyCss();
                root.layout();

                assertNotNull(root.getToolWindowManager(), "manager ausente");
                assertEquals("project",
                        root.getToolWindowManager().getActive(ToolWindowPosition.LEFT).getId());
                assertEquals("terminal",
                        root.getToolWindowManager().getActive(ToolWindowPosition.BOTTOM).getId());

                Node projectContent = root.getLeftToolWindow().getCurrentContent();
                assertNotNull(projectContent, "conteúdo inicial da LeftToolWindow ausente");

                root.getToolWindowManager().activate("learn");
                Node learnContent = root.getLeftToolWindow().getCurrentContent();
                assertNotNull(learnContent, "conteúdo após troca ausente");
                assertTrue(projectContent != learnContent,
                        "LeftToolWindow deve trocar o conteúdo dinamicamente");

                root.getToolWindowManager().activate("problems");
                Node problemsContent = root.getBottomToolWindow().getCurrentContent();
                assertNotNull(problemsContent, "conteúdo do BottomToolWindow ausente");

                assertEquals("problems",
                        root.getToolWindowManager().getActive(ToolWindowPosition.BOTTOM).getId());
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                System.setErr(oldErr);
                report.append(errBuf.toString(StandardCharsets.UTF_8));
                done.countDown();
            }
        });

        assertTrue(done.await(20, TimeUnit.SECONDS), "tempo esgotado no FX thread");
        System.out.println(report);
        if (error[0] != null) {
            throw new AssertionError("Falha na integração Sprint 3.2", error[0]);
        }
        String err = report.toString().toLowerCase();
        assertTrue(!err.contains("css error") && !err.contains("error parsing"),
                "Erros de parse de CSS:\n" + report);
    }
}
