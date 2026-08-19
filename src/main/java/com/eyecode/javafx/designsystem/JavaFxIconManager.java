package com.eyecode.javafx.designsystem;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class JavaFxIconManager {

    private static final String COMPLETION_ICON_DIR = "icons/completion/";
    private static final String SVG_EXTENSION = ".svg";

    private JavaFxIconManager() {}

    private static final Map<EyeCodeIcon, Map<Double, Image>> CACHE = new EnumMap<>(EyeCodeIcon.class);
    private static final Map<CompletionItemKind, Map<Double, Image>> COMPLETION_CACHE =
            new EnumMap<>(CompletionItemKind.class);

    public static ImageView icon(EyeCodeIcon icon, double size) {
        Image img = loadImage(icon.resourcePath(), icon.name(), size, CACHE.computeIfAbsent(icon, key -> new HashMap<>()));
        return imageView(img, size);
    }

    public static ImageView completionIcon(CompletionItemKind kind, double size) {
        Map<Double, Image> sizedCache = COMPLETION_CACHE.computeIfAbsent(kind, key -> new HashMap<>());
        String resourceName = completionResourceName(kind);
        Image img = loadImage(COMPLETION_ICON_DIR + resourceName + SVG_EXTENSION, "completion/" + resourceName, size, sizedCache);
        return imageView(img, size);
    }

    private static ImageView imageView(Image img, double size) {
        ImageView view = new ImageView(img);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private static Image loadImage(String resourcePath, String label, double size, Map<Double, Image> cache) {
        return cache.computeIfAbsent(size, key -> rasterize(resourcePath, label, key));
    }

    private static Image rasterize(String resourcePath, String label, double size) {
        try {
            java.net.URL url = JavaFxIconManager.class.getResource("/" + resourcePath);
            if (url == null) {
                throw new IllegalStateException("Icon resource not found: " + label);
            }

            PNGTranscoder t = new PNGTranscoder();
            t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
            t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);

            TranscoderInput in = new TranscoderInput(url.toString());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            t.transcode(in, new TranscoderOutput(out));

            return new Image(new java.io.ByteArrayInputStream(out.toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to rasterize icon " + label, e);
        }
    }

    private static String completionResourceName(CompletionItemKind kind) {
        return switch (kind) {
            case CLASS, ENUM, RECORD -> "class";
            case INTERFACE -> "interface";
            case METHOD, CONSTRUCTOR -> "method";
            case FIELD -> "field";
            case VARIABLE -> "variable";
            case PACKAGE -> "package";
            case SNIPPET -> "snippet";
            case KEYWORD -> "keyword";
        };
    }
}
