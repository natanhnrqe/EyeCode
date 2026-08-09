package com.eyecode.language.ast;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstNodeTest {

    @Test
    void ofCreatesNodeWithKindRangeChildren() {
        AstNode child = AstNode.of(AstNodeKind.TYPE, TextRange.of(4, 8), List.of());
        AstNode node = AstNode.of(AstNodeKind.METHOD_DECLARATION, TextRange.of(0, 20),
                List.of(child));

        assertEquals(AstNodeKind.METHOD_DECLARATION, node.kind());
        assertEquals(TextRange.of(0, 20), node.range());
        assertEquals(1, node.children().size());
        assertSame(child, node.children().get(0));
        assertNull(node.parent());
    }

    @Test
    void childrenListIsDefensivelyCopiedAndImmutable() {
        List<AstNode> source = new ArrayList<>();
        source.add(AstNode.of(AstNodeKind.TYPE, TextRange.of(0, 1), List.of()));
        AstNode node = AstNode.of(AstNodeKind.FIELD_DECLARATION, TextRange.of(0, 5), source);

        source.add(AstNode.of(AstNodeKind.TYPE, TextRange.of(1, 2), List.of()));
        assertEquals(1, node.children().size());

        assertThrows(UnsupportedOperationException.class,
                () -> node.children().add(AstNode.of(AstNodeKind.TYPE, TextRange.of(0, 0), List.of())));
    }

    @Test
    void ofWithTokenCarriesOptionalTokenPayload() {
        AstNode plain = AstNode.of(AstNodeKind.NAME_EXPRESSION, TextRange.of(0, 3), List.of());
        assertNull(plain.token());

        Token token = new Token(com.eyecode.language.java.JavaTokenType.IDENTIFIER,
                TextRange.of(0, 3), "foo");
        AstNode withToken = AstNode.of(AstNodeKind.NAME_EXPRESSION, TextRange.of(0, 3),
                List.of(), token);
        assertSame(token, withToken.token());
        assertEquals("foo", withToken.token().text());
        assertEquals(AstNodeKind.NAME_EXPRESSION, withToken.kind());
    }

    @Test
    void nullArgumentsRejected() {
        assertThrows(NullPointerException.class,
                () -> AstNode.of(null, TextRange.of(0, 1), List.of()));
        assertThrows(NullPointerException.class,
                () -> AstNode.of(AstNodeKind.TYPE, null, List.of()));
        assertThrows(NullPointerException.class,
                () -> AstNode.of(AstNodeKind.TYPE, TextRange.of(0, 1), null));
    }

    @Test
    void toStringReportsKindAndRange() {
        AstNode node = AstNode.of(AstNodeKind.CLASS_DECLARATION, TextRange.of(10, 30), List.of());
        String s = node.toString();
        assertTrue(s.startsWith("AstNode[CLASS_DECLARATION "));
        assertTrue(s.contains("TextRange"));
        assertTrue(s.endsWith("]"));
    }

    @Test
    void linkParentsAssignsParentLinks() {
        AstNode field = AstNode.of(AstNodeKind.FIELD_DECLARATION, TextRange.of(6, 12), List.of());
        AstNode typeNode = AstNode.of(AstNodeKind.TYPE, TextRange.of(8, 12), List.of());
        AstNode clazz = AstNode.of(AstNodeKind.CLASS_DECLARATION, TextRange.of(0, 20),
                List.of(AstNode.of(AstNodeKind.MODIFIER, TextRange.of(0, 7), List.of()), field));
        AstNode cu = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 21), List.of(clazz));

        assertSame(cu, AstNodes.linkParents(cu));

        assertSame(cu, cu.children().get(0).parent());
        assertEquals(2, cu.children().get(0).children().size());
        assertSame(clazz, cu.children().get(0).children().get(1).parent());
        assertSame(clazz, field.parent());
        assertNull(cu.parent());
    }

    @Test
    void linkParentsRejectsNullAndForeignImplementations() {
        assertThrows(IllegalArgumentException.class, () -> AstNodes.linkParents(null));

        AstNode rogue = new AstNode() {
            @Override
            public TextRange range() {
                return TextRange.of(0, 1);
            }

            @Override
            public AstNode parent() {
                return null;
            }

            @Override
            public List<AstNode> children() {
                return List.of();
            }

            @Override
            public AstNodeKind kind() {
                return AstNodeKind.TYPE;
            }
        };
        assertThrows(IllegalStateException.class, () -> AstNodes.linkParents(rogue));
    }
}
