package com.eyecode.javafx.designsystem;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

public final class JavaFxIconManager {

    private JavaFxIconManager() {}

    private static final Map<EyeCodeIcon, Map<Double, Image>> CACHE = new EnumMap<>(EyeCodeIcon.class);

    public static ImageView icon(EyeCodeIcon icon, double size) {
        Image img = loadImage(icon, size);
        ImageView view = new ImageView(img);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private static Image loadImage(EyeCodeIcon icon, double size) {
        return CACHE
                .computeIfAbsent(icon, k -> new java.util.HashMap<>())
                .computeIfAbsent(size, s -> rasterize(icon, s));
    }

    private static Image rasterize(EyeCodeIcon icon, double size) {
        try {
            java.net.URL url = JavaFxIconManager.class.getResource(
                    "/" + icon.resourcePath());
            if (url == null) {
                throw new IllegalStateException("Ícone SVG não encontrado: " + icon);
            }

            PNGTranscoder t = new PNGTranscoder();
            t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
            t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);

            TranscoderInput in = new TranscoderInput(url.toString());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            t.transcode(in, new TranscoderOutput(out));

            return new Image(new java.io.ByteArrayInputStream(out.toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("Falha rasterizando ícone " + icon, e);
        }
    }
}
