package com.eyecode.language.semantic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.junit.jupiter.api.Test;

class JavaTypeMemberResolverTest {

    private final JavaTypeMemberResolver resolver = new JavaTypeMemberResolver();

    @Test
    void javafxUiClassResolvesMembersWithoutInitializingToolkit() {
        String source = """
                import javafx.scene.control.TextField;
                public class Sample {
                    private TextField field;
                    void edit() {
                        field.
                    }
                }
                """;
        int offset = source.indexOf("field.") + "field.".length();

        List<JavaResolvedMember> members = assertDoesNotThrow(() -> resolver.resolveMembers(source, offset));

        assertFalse(members.isEmpty());
        assertTrue(members.stream().anyMatch(member -> "setText".equals(member.name())));
    }

    @Test
    void unresolvedReceiverReturnsEmptyWithoutThrowing() {
        String source = """
                public class Sample {
                    void edit() {
                        mystery.
                    }
                }
                """;
        int offset = source.indexOf("mystery.") + "mystery.".length();

        List<JavaResolvedMember> members = assertDoesNotThrow(() -> resolver.resolveMembers(source, offset));

        assertTrue(members.isEmpty());
    }

    @Test
    void jdkTypeStillResolvesWithDeferredInitialization() {
        String source = """
                public class Sample {
                    void edit() {
                        String.
                    }
                }
                """;
        int offset = source.indexOf("String.") + "String.".length();

        List<JavaResolvedMember> members = assertDoesNotThrow(() -> resolver.resolveMembers(source, offset));

        assertTrue(members.stream().anyMatch(member -> "valueOf".equals(member.name())));
    }
}
