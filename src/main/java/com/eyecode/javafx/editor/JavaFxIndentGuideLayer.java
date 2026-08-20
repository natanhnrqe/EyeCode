package com.eyecode.javafx.editor;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.Paragraph;
import org.reactfx.Subscription;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class JavaFxIndentGuideLayer extends Region {

    private static final double GUIDE_TEXT_INSET = 6;
    private static final Font FALLBACK_FONT = Font.font("JetBrains Mono", 13);

    private final CodeArea codeArea;
    private final Canvas canvas;
    private final Line guideSample;
    private final Line activeGuideSample;
    private final IndentGuideModel guideModel;
    private final IndentGuideDescriptorBuilder descriptorBuilder;
    private final Subscription viewportSubscription;
    private final Subscription textSubscription;
    private final ListChangeListener<Paragraph<?, ?, ?>> paragraphListener;
    private final ChangeListener<Number> repaintListener;
    private List<IndentGuideDescriptor> descriptors = List.of();
    private List<RenderedGuide> renderedGuides = List.of();
    private boolean repaintRequested;
    private Scene installedScene;
    private final Runnable postLayoutPainter = this::paintIfRequested;

    public JavaFxIndentGuideLayer(CodeArea codeArea) {
        this(codeArea, new IndentGuideModel(), new IndentGuideDescriptorBuilder());
    }

    JavaFxIndentGuideLayer(CodeArea codeArea,
                           IndentGuideModel guideModel,
                           IndentGuideDescriptorBuilder descriptorBuilder) {
        this.codeArea = codeArea;
        this.guideModel = guideModel == null ? new IndentGuideModel() : guideModel;
        this.descriptorBuilder = descriptorBuilder == null ? new IndentGuideDescriptorBuilder(this.guideModel) : descriptorBuilder;

        getStyleClass().add("indent-guide-layer");
        setMouseTransparent(true);
        setPickOnBounds(false);

        canvas = new Canvas();
        canvas.setManaged(false);
        canvas.setMouseTransparent(true);

        guideSample = new Line();
        guideSample.getStyleClass().add("indent-guide");
        guideSample.setManaged(false);
        guideSample.setVisible(false);

        activeGuideSample = new Line();
        activeGuideSample.getStyleClass().addAll("indent-guide", "indent-guide-active");
        activeGuideSample.setManaged(false);
        activeGuideSample.setVisible(false);

        getChildren().addAll(canvas, guideSample, activeGuideSample);

        viewportSubscription =
            codeArea.viewportDirtyEvents()
                    .subscribe(ignore -> requestGuideRepaint());
        textSubscription = codeArea.multiPlainChanges().subscribe(ignore -> rebuildAndRepaint());
        paragraphListener = change -> rebuildAndRepaint();
        codeArea.getParagraphs().addListener(paragraphListener);
        repaintListener =
            (obs, oldValue, newValue) -> requestGuideRepaint();
        widthProperty().addListener(repaintListener);
        heightProperty().addListener(repaintListener);
        codeArea.currentParagraphProperty().addListener(repaintListener);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removePostLayoutPulseListener(postLayoutPainter);
            }
            if (getScene() != null) {
            installedScene = getScene();
            installedScene.addPostLayoutPulseListener(postLayoutPainter);
        }
            installedScene = newScene;

            if (newScene != null) {
                newScene.addPostLayoutPulseListener(postLayoutPainter);
                requestGuideRepaint();
            }
        });

        rebuildDescriptors();
        repaintRequested = true;
        requestLayout();
    }

    public void dispose() {

        if (installedScene != null) {
        installedScene.removePostLayoutPulseListener(postLayoutPainter);
        installedScene = null;
    }
        viewportSubscription.unsubscribe();
        textSubscription.unsubscribe();
        codeArea.getParagraphs().removeListener(paragraphListener);
        widthProperty().removeListener(repaintListener);
        heightProperty().removeListener(repaintListener);
        codeArea.currentParagraphProperty().removeListener(repaintListener);
    }

    List<IndentGuideDescriptor> descriptorsForTest() {
        return descriptors;
    }

    List<RenderedGuide> renderedGuidesForTest() {
        return renderedGuides;
    }

    void rebuildAndRepaint() {
        rebuildDescriptors();
        requestGuideRepaint();
    }

   @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();

        boolean canvasResized =
                Double.compare(canvas.getWidth(), width) != 0
                        || Double.compare(canvas.getHeight(), height) != 0;

        if (canvasResized) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            requestGuideRepaint();
        }
    }

    private void rebuildDescriptors() {
        descriptors = descriptorBuilder.build(codeArea.getParagraphs());
    }

    private void clearCanvas() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );
    }

    private void repaint() {
        if (getScene() == null
                || canvas.getWidth() <= 0
                || canvas.getHeight() <= 0) {
            return;
        }

        applyCss();

        if (descriptors.isEmpty()) {
            clearCanvas();
            renderedGuides = List.of();
            return;
        }

        List<VisibleParagraphBounds> visible = visibleParagraphBounds();

        if (visible.isEmpty()) {
            return;
        }

        clearCanvas();

        GraphicsContext graphics = canvas.getGraphicsContext2D();

        int activeParagraph = codeArea.getCurrentParagraph();
        int activeColumn = activeColumn(activeParagraph);
        double scaleX = outputScaleX();
        double lineWidth = 1.0 / scaleX;
        double characterWidth = characterWidth();

        List<RenderedGuide> painted = projectVisibleGuides(
                descriptors,
                visible,
                activeParagraph,
                activeColumn,
                characterWidth,
                scaleX
        );

        for (RenderedGuide guide : painted) {
            Paint stroke = guide.active()
                    ? activeGuideSample.getStroke()
                    : guideSample.getStroke();

            double opacity = guide.active()
                    ? activeGuideSample.getOpacity()
                    : guideSample.getOpacity();

            if (stroke == null) {
                continue;
            }

            graphics.setStroke(stroke);
            graphics.setLineWidth(lineWidth);
            graphics.setGlobalAlpha(opacity);
            graphics.strokeLine(
                    guide.x(),
                    guide.startY(),
                    guide.x(),
                    guide.endY()
            );
        }

        graphics.setGlobalAlpha(1.0);
        renderedGuides = List.copyOf(painted);
    }

    private List<VisibleParagraphBounds> visibleParagraphBounds() {
        List<VisibleParagraphBounds> visible = new ArrayList<>();
        int visibleCount = codeArea.getVisibleParagraphs().size();
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            int paragraphIndex = codeArea.visibleParToAllParIndex(visibleIndex);
            Optional<Bounds> screenBounds;
            try {
                screenBounds = codeArea.getParagraphBoundsOnScreen(paragraphIndex);
            } catch (RuntimeException ex) {
                continue;
            }
            if (screenBounds == null || screenBounds.isEmpty()) {
                continue;
            }
            IndentGuideLine line = guideModel.lineFor(codeArea.getParagraph(paragraphIndex).getText());
            visible.add(new VisibleParagraphBounds(paragraphIndex, screenBoundsToLocal(screenBounds.get()), line));
        }
        visible.sort(Comparator.comparingInt(VisibleParagraphBounds::paragraphIndex));
        return visible;
    }

    private Bounds screenBoundsToLocal(Bounds screenBounds) {
        Point2D min = canvas.screenToLocal(screenBounds.getMinX(), screenBounds.getMinY());
        Point2D max = canvas.screenToLocal(screenBounds.getMaxX(), screenBounds.getMaxY());
        return new BoundingBox(min.getX(), min.getY(), max.getX() - min.getX(), max.getY() - min.getY());
    }

    static List<RenderedGuide> projectVisibleGuides(
            List<IndentGuideDescriptor> descriptors,
            List<VisibleParagraphBounds> visibleParagraphs,
            int activeParagraph,
            int activeColumn,
            double characterWidth,
            double scaleX) {

        if (descriptors == null
                || descriptors.isEmpty()
                || visibleParagraphs == null
                || visibleParagraphs.isEmpty()) {
            return List.of();
        }

        int visibleFirst = visibleParagraphs.get(0).paragraphIndex();
        int visibleLast = visibleParagraphs.get(visibleParagraphs.size() - 1).paragraphIndex();

        List<RenderedGuide> painted = new ArrayList<>();

        for (IndentGuideDescriptor descriptor : descriptors) {
            if (!descriptor.intersects(visibleFirst, visibleLast)) {
                continue;
            }

            boolean active =
                    descriptor.column() == activeColumn
                            && descriptor.startParagraph() <= activeParagraph
                            && activeParagraph <= descriptor.endParagraph();

            double x = snappedStrokeCenterX(
                    baseTextX(visibleParagraphs.get(0).bounds())
                            + descriptor.column() * characterWidth,
                    scaleX
            );

            appendVisibleSegments(painted, descriptor, visibleParagraphs, x, active);
        }

        return List.copyOf(painted);
    }

    private static void appendVisibleSegments(List<RenderedGuide> painted,
                                              IndentGuideDescriptor descriptor,
                                              List<VisibleParagraphBounds> visibleParagraphs,
                                              double x,
                                              boolean active) {
        Double segmentStart = null;
        double segmentEnd = 0;
        for (VisibleParagraphBounds paragraph : visibleParagraphs) {
            if (paragraph.paragraphIndex() < descriptor.startParagraph()
                    || paragraph.paragraphIndex() > descriptor.endParagraph()) {
                continue;
            }
            boolean startsGuide = paragraph.paragraphIndex() == descriptor.startParagraph();
            boolean endsGuide = paragraph.paragraphIndex() == descriptor.endParagraph();
            if (startsGuide) {
                segmentStart = paragraph.bounds().getMaxY();
                segmentEnd = segmentStart;
                continue;
            }
            if (endsGuide) {
                if (segmentStart != null) {
                    addSegment(painted, descriptor, x, segmentStart, paragraph.bounds().getMinY(), active);
                }
                segmentStart = null;
                continue;
            }
            if (paragraph.line().containsGuideColumn(descriptor.column())) {
                if (segmentStart == null) {
                    segmentStart = paragraph.bounds().getMinY();
                }
                segmentEnd = paragraph.bounds().getMaxY();
                continue;
            }
            if (segmentStart != null) {
                addSegment(painted, descriptor, x, segmentStart, paragraph.bounds().getMinY(), active);
            }
            segmentStart = null;
        }
        if (segmentStart != null) {
            addSegment(painted, descriptor, x, segmentStart, segmentEnd, active);
        }
    }

    private static void addSegment(List<RenderedGuide> painted,
                                   IndentGuideDescriptor descriptor,
                                   double x,
                                   double startY,
                                   double endY,
                                   boolean active) {
        if (endY > startY) {
            painted.add(new RenderedGuide(descriptor, x, startY, endY, active));
        }
    }

    private void requestGuideRepaint() {
        repaintRequested = true;
        requestLayout();
    }

    private void paintIfRequested() {
        if (!repaintRequested) {
            return;
        }

        repaintRequested = false;
        repaint();
    }

    private int activeColumn(int paragraphIndex) {
        if (paragraphIndex < 0 || paragraphIndex >= codeArea.getParagraphs().size()) {
            return 0;
        }
        return guideModel.lineFor(codeArea.getParagraph(paragraphIndex).getText()).deepestColumn();
    }

    private static double baseTextX(Bounds paragraphBounds) {
    return paragraphBounds.getMinX()
            + JavaFxGutterFactory.GUTTER_WIDTH
            + GUIDE_TEXT_INSET;
    }

    private double characterWidth() {
        Font font = resolvedFont();
        Text probe = new Text("0");
        probe.setFont(font);
        return probe.getLayoutBounds().getWidth();
    }

    private Font resolvedFont() {
        Node textNode = codeArea.lookup(".text");
        if (textNode instanceof Text text && text.getFont() != null) {
            return text.getFont();
        }
        return FALLBACK_FONT;
    }

    private double outputScaleX() {
        Window window = getScene() == null ? null : getScene().getWindow();
        if (window == null) {
            return 1.0;
        }
        Double scale = invokeScale(window, "getOutputScaleX");
        if (scale != null && scale > 0) {
            return scale;
        }
        scale = invokeScale(window, "getRenderScaleX");
        return scale != null && scale > 0 ? scale : 1.0;
    }

    private Double invokeScale(Window window, String methodName) {
        try {
            Method method = window.getClass().getMethod(methodName);
            Object result = method.invoke(window);
            return result instanceof Number number ? number.doubleValue() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static double snappedStrokeCenterX(double x, double scaleX) {
        double safeScale = Math.max(1.0, scaleX);
        return (Math.round(x * safeScale - 0.5) + 0.5) / safeScale;
    }

    static VisibleParagraphBounds visibleParagraphBoundsForTest(int paragraphIndex, Bounds bounds) {
        return new VisibleParagraphBounds(paragraphIndex, bounds, new IndentGuideLine(List.of(), Integer.MAX_VALUE, true));
    }

    static VisibleParagraphBounds visibleParagraphBoundsForTest(int paragraphIndex,
                                                                 Bounds bounds,
                                                                 IndentGuideLine line) {
        return new VisibleParagraphBounds(paragraphIndex, bounds, line);
    }

    static record VisibleParagraphBounds(int paragraphIndex, Bounds bounds, IndentGuideLine line) {
    }

    record RenderedGuide(IndentGuideDescriptor descriptor, double x, double startY, double endY, boolean active) {
    }
}
