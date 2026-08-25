package com.eyecode.javafx.ui.editor;

import com.eyecode.language.documentation.JdkSourceTarget;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxJdkSourceTabTest {

    @BeforeAll
    static void startToolkit() {
    }

    @Test
    void sourceTabsKeepIndependentReadOnlyContentAndRevealDeclaration() {
            JdkSourceTarget stringTarget = new JdkSourceTarget(
                    "java.lang.String", "java.base", "java.base/java/lang/String.java", "String.java");
            JdkSourceTarget objectTarget = new JdkSourceTarget(
                    "java.lang.Object", "java.base", "java.base/java/lang/Object.java", "Object.java");
            String stringSource = "package java.lang;\npublic final class String {}";
            String objectSource = "package java.lang;\npublic class Object {}";

            JavaFxJdkSourceTab stringTab = new JavaFxJdkSourceTab(stringTarget, stringSource);
            JavaFxJdkSourceTab objectTab = new JavaFxJdkSourceTab(objectTarget, objectSource);
            try {
                assertEquals(stringSource, stringTab.source());
                assertEquals(objectSource, objectTab.source());
                assertTrue(stringTab.isReadOnly());
                assertTrue(objectTab.isReadOnly());
                assertEquals("jdk://java.base/java.base/java/lang/String.java", stringTab.sourceIdentity());
                assertEquals(stringSource.indexOf("class String"), stringTab.revealedOffset());
            } finally {
                stringTab.dispose();
                objectTab.dispose();
            }
    }

    @Test
    void memberRevealMovesTheExistingSourceEditorWithoutChangingItsContent() {
            JdkSourceTarget type = new JdkSourceTarget(
                    "java.lang.String", "java.base", "java.base/java/lang/String.java", "String.java");
            JdkSourceTarget member = type.withMember("contains");
            String source = "public final class String {\n"
                    + "  public boolean contains(CharSequence value) { return true; }\n"
                    + "}";
            JavaFxJdkSourceTab tab = new JavaFxJdkSourceTab(type, source);
            tab.reveal(member);
            assertEquals(source.indexOf("contains(CharSequence"), tab.revealedOffset());
            assertEquals(source, tab.source());
            tab.dispose();
    }
}
