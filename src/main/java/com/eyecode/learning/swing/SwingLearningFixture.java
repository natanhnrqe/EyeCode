package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardHeaderData;
import com.eyecode.learning.model.LearningCardFooterData;

public final class SwingLearningFixture {

    private SwingLearningFixture() {}

    public static LearningCardDocument createDocumentFixture() {
        LearningCardDocument document = new LearningCardDocument(
                new LearningCardHeaderData("java", "Class", "Class • Inheritance"),
                new LearningCardFooterData("Updated:", "Today")
        );

        document.addHeading("Class");
        document.addParagraph("A class defines the structure and behavior of an object.");

        document.addHeading("Inheritance");
        document.addParagraph("Inheritance allows a class to reuse and extend behavior from another class.");

        document.addHeading("Java");
        document.addCodeBlock("Java",
                "public class Animal {\n" +
                "    public void speak() {\n" +
                "        System.out.println(\"Hello\");\n" +
                "    }\n" +
                "}\n"
        );

        document.addHeading("Related concepts");
        document.addBullet("Object");
        document.addBullet("Inheritance");
        document.addBullet("Polymorphism");

        return document;
    }
}
