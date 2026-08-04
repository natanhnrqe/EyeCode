package com.eyecode.javafx.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.javafx.editor.JavaFxEditor;
import com.eyecode.javafx.editor.JavaFxEditorController;

public final class FxEditorContainer extends com.eyecode.javafx.designsystem.FxCard {

    public FxEditorContainer() {
        getStyleClass().add("editor-card");
        getStyleClass().remove("fx-card");

        EditorDocument document = new EditorDocument(
                java.nio.file.Path.of("src/test/resources/javafx/demo/Animal.java"),
                "package com.eyecode.javafx.editor.demo;\n\n" +
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
                "}\n"
        );
        EditorBuffer buffer = new EditorBuffer(document);

        JavaFxEditor editor = new JavaFxEditor();
        JavaFxEditorController controller = new JavaFxEditorController(editor, buffer);
        controller.loadDocument();

        setContent(editor);
    }
}
