package com.eyecode.javafx.ui;

import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.javafx.editor.view.JavaFxEditorViewFactory;
import com.eyecode.javafx.learning.JavaFxLearningWorkspace;
import com.eyecode.javafx.ui.editor.FxEditorWorkspacePane;
import com.eyecode.javafx.ui.editor.JavaFxDocumentationWorkspace;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorViewFactory;

import java.nio.file.Path;

public final class FxEditorContainer extends com.eyecode.javafx.designsystem.FxCard {

    private final JavaFxLearningWorkspace learningWorkspace;
    private final JavaFxDocumentationWorkspace documentationWorkspace;

    public FxEditorContainer() {
        getStyleClass().add("editor-card");
        getStyleClass().remove("fx-card");

        EventBus eventBus = new EventBus();
        documentationWorkspace = new JavaFxDocumentationWorkspace();
        learningWorkspace = new JavaFxLearningWorkspace(documentationWorkspace::open);
        EditorViewFactory viewFactory = new JavaFxEditorViewFactory(learningWorkspace);
        EditorManager manager = new EditorManager(
                eventBus, new DefaultFileSystemService(), viewFactory);

        openDemoDocuments(manager);

        FxEditorWorkspacePane workspacePane = new FxEditorWorkspacePane(manager, documentationWorkspace);
        setContent(workspacePane);
    }

    public void dispose() {
        learningWorkspace.dispose();
        documentationWorkspace.dispose();
    }

    private void openDemoDocuments(EditorManager manager) {
        manager.openDocument(Path.of("src/demo/Animal.java"), ANIMAL_SOURCE);
        manager.openDocument(Path.of("src/demo/Shape.java"), SHAPE_SOURCE);
        manager.openDocument(Path.of("src/demo/HelloWorld.java"), HELLO_SOURCE);
    }

    private static final String ANIMAL_SOURCE =
            "package com.eyecode.demo;\n\n" +
            "import java.util.Objects;\n\n" +
            "public class Animal {\n\n" +
            "    private String name;\n\n" +
            "    public Animal(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n\n" +
            "    public String getName() {\n" +
            "        if (name != null) {\n" +
            "            return name;\n" +
            "        }\n" +
            "        return \"Unknown\";\n" +
            "    }\n\n" +
            "    public void feed(String food) {\n" +
            "        System.out.println(\"Feeding with \" + Objects.requireNonNull(food));\n" +
            "    }\n" +
            "}\n";

    private static final String SHAPE_SOURCE =
            "package com.eyecode.demo;\n\n" +
            "public sealed interface Shape permits Circle, Square {\n\n" +
            "    double area();\n" +
            "}\n\n" +
            "record Circle(double radius) implements Shape {\n\n" +
            "    @Override\n" +
            "    public double area() {\n" +
            "        return Math.PI * radius * radius;\n" +
            "    }\n" +
            "}\n\n" +
            "record Square(double side) implements Shape {\n\n" +
            "    @Override\n" +
            "    public double area() {\n" +
            "        return side * side;\n" +
            "    }\n" +
            "}\n";

    private static final String HELLO_SOURCE =
            "package com.eyecode.demo;\n\n" +
            "public final class HelloWorld {\n\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, EyeCode!\");\n" +
            "    }\n" +
            "}\n";
}
