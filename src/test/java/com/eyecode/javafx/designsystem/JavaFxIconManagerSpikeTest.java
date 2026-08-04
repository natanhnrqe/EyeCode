package com.eyecode.javafx.designsystem;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxIconManagerSpikeTest {

    @Test
    void rasterizesProjectSvgAt16() {
        Image img = JavaFxIconManager.icon(EyeCodeIcon.PROJECT, 16).getImage();
        assertNotNull(img);
        assertTrue(img.getProgress() >= 1.0, "Image não totalmente carregada");
        assertEquals(16.0, img.getWidth(), 1.0, "Largura 16");
        assertEquals(16.0, img.getHeight(), 1.0, "Altura 16");
    }

    @Test
    void rasterizesProjectSvgAt20() {
        Image img = JavaFxIconManager.icon(EyeCodeIcon.PROJECT, 20).getImage();
        assertNotNull(img);
        assertEquals(20.0, img.getWidth(), 1.0);
        assertEquals(20.0, img.getHeight(), 1.0);
    }

    @Test
    void rasterizesProjectSvgAt24() {
        Image img = JavaFxIconManager.icon(EyeCodeIcon.PROJECT, 24).getImage();
        assertNotNull(img);
        assertEquals(24.0, img.getWidth(), 1.0);
        assertEquals(24.0, img.getHeight(), 1.0);
    }

    @Test
    void cacheRetornaMesmaInstanciaPorTamanho() {
        Image a = JavaFxIconManager.icon(EyeCodeIcon.PROJECT, 16).getImage();
        Image b = JavaFxIconManager.icon(EyeCodeIcon.PROJECT, 16).getImage();
        assertTrue(a == b, "Cache de Image não reutilizado");
    }
}
