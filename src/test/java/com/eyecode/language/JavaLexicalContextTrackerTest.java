package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.incremental.JavaLexicalContextTracker;
import com.eyecode.language.java.incremental.LexicalCheckpoint;
import com.eyecode.language.java.incremental.LexicalState;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLexicalContextTrackerTest {

    private static final long VERSION = 5;

    private final JavaLexicalContextTracker tracker = new JavaLexicalContextTracker();

    private static List<Token> tokens(String source) {
        return new JavaLexerService().lex(DocumentSnapshot.oneShot(source)).tokens();
    }

    private static Token tokenOf(List<Token> tokens, String text, int atLeast) {
        for (Token token : tokens) {
            if (token.text().equals(text) && token.startOffset() >= atLeast) {
                return token;
            }
        }
        throw new AssertionError("token not found: " + text);
    }

    private static Token first(List<Token> tokens, String text) {
        return tokenOf(tokens, text, 0);
    }

    @Test
    void tokenIndexAtFindsTokenContainingOffset() {
        List<Token> tokens = tokens("int x = 42;");

        assertEquals(0, tracker.tokenIndexAt(tokens, 0));
        assertEquals(0, tracker.tokenIndexAt(tokens, 1));
        assertEquals(1, tracker.tokenIndexAt(tokens, 3));
        assertEquals(2, tracker.tokenIndexAt(tokens, 4));
        assertEquals(7, tracker.tokenIndexAt(tokens, 10));
        assertEquals(tokens.size() - 1, tracker.tokenIndexAt(tokens, 11));
        assertEquals(tokens.size() - 1, tracker.tokenIndexAt(tokens, 100));
    }

    @Test
    void tokenIndexAtToleratesEmptyOrNullLists() {
        assertEquals(-1, tracker.tokenIndexAt(null, 3));
        assertEquals(-1, tracker.tokenIndexAt(List.of(), 3));
    }

    @Test
    void stateAtIsNormalOnTokenBoundariesAndInsidePlainTokens() {
        List<Token> tokens = tokens("int x = 42;");

        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, 0));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, 1));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, 10));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, 11));
    }

    @Test
    void stateAtIsStringInsideAndAtTheStartOfStringTokens() {
        List<Token> tokens = tokens("String s = \"hi\";");
        Token string = first(tokens, "\"hi\"");

        assertEquals(LexicalState.STRING, tracker.stateAt(tokens, string.startOffset()));
        assertEquals(LexicalState.STRING, tracker.stateAt(tokens, string.startOffset() + 1));
        assertEquals(LexicalState.STRING, tracker.stateAt(tokens, string.endOffset() - 1));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, string.endOffset()));
    }

    @Test
    void stateAtIsCharacterInsideCharacterLiterals() {
        List<Token> tokens = tokens("char c = 'a';");
        Token character = first(tokens, "'a'");

        assertEquals(LexicalState.CHARACTER, tracker.stateAt(tokens, character.startOffset()));
        assertEquals(LexicalState.CHARACTER, tracker.stateAt(tokens, character.startOffset() + 1));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, character.endOffset()));
    }

    @Test
    void stateAtIsLineCommentInsideLineCommentsOnly() {
        List<Token> tokens = tokens("// comment\nint x = 1;");
        Token comment = first(tokens, "// comment");

        assertEquals(LexicalState.LINE_COMMENT, tracker.stateAt(tokens, comment.startOffset()));
        assertEquals(LexicalState.LINE_COMMENT, tracker.stateAt(tokens, comment.startOffset() + 5));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, comment.endOffset()));
    }

    @Test
    void stateAtIsBlockCommentInsideBlockComments() {
        List<Token> tokens = tokens("/* block */\nint x = 1;");
        Token comment = first(tokens, "/* block */");

        assertEquals(LexicalState.BLOCK_COMMENT, tracker.stateAt(tokens, comment.startOffset()));
        assertEquals(LexicalState.BLOCK_COMMENT, tracker.stateAt(tokens, comment.startOffset() + 3));
        assertEquals(LexicalState.NORMAL, tracker.stateAt(tokens, comment.endOffset()));
    }

    @Test
    void stateAtIsStringForUnterminatedStrings() {
        List<Token> tokens = tokens("String s = \"abc");
        Token string = first(tokens, "\"abc");

        assertEquals(LexicalState.STRING, tracker.stateAt(tokens, string.startOffset()));
        assertEquals(LexicalState.STRING, tracker.stateAt(tokens, string.endOffset() - 1));
    }

    @Test
    void stateAtIsBlockCommentForUnterminatedBlockComments() {
        List<Token> tokens = tokens("/* abc");
        Token comment = first(tokens, "/* abc");

        assertEquals(LexicalState.BLOCK_COMMENT, tracker.stateAt(tokens, comment.startOffset()));
        assertEquals(LexicalState.BLOCK_COMMENT, tracker.stateAt(tokens, comment.endOffset() - 1));
    }

    @Test
    void safeCheckpointOnTokenBoundaryMovesBackToPreviousTokenStart() {
        List<Token> tokens = tokens("int x = 42;");
        Token equals = first(tokens, "=");
        Token whitespaceBefore = tokenOf(tokens, " ", equals.startOffset() - 1);

        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(tokens, equals.startOffset(), VERSION);

        assertEquals(VERSION, checkpoint.version());
        assertEquals(whitespaceBefore.startOffset(), checkpoint.offset());
        assertEquals(whitespaceBefore.startOffset(), tokens.get(checkpoint.tokenIndex()).startOffset());
        assertEquals(LexicalState.NORMAL, checkpoint.state());
    }

    @Test
    void safeCheckpointInsideTokenMovesBackToTokenStart() {
        List<Token> tokens = tokens("int x = 42;");
        Token number = first(tokens, "42");
        int inside = number.startOffset() + 1;

        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(tokens, inside, VERSION);

        assertEquals(number.startOffset(), checkpoint.offset());
        assertEquals(number.startOffset(), tokens.get(checkpoint.tokenIndex()).startOffset());
    }

    @Test
    void safeCheckpointInsideStringMovesBackToStringStart() {
        List<Token> tokens = tokens("String s = \"hello\";");
        Token string = first(tokens, "\"hello\"");
        int inside = string.startOffset() + 3;

        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(tokens, inside, VERSION);

        assertEquals(string.startOffset(), checkpoint.offset());
        assertEquals(LexicalState.STRING, checkpoint.state());
    }

    @Test
    void safeCheckpointInsideBlockCommentMovesBackToCommentStart() {
        List<Token> tokens = tokens("/* a */\nint x = 1;");
        Token comment = first(tokens, "/* a */");

        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(tokens, comment.startOffset() + 2, VERSION);

        assertEquals(comment.startOffset(), checkpoint.offset());
        assertEquals(LexicalState.BLOCK_COMMENT, checkpoint.state());
    }

    @Test
    void safeCheckpointAtDocumentEndMovesBackBeforeEof() {
        List<Token> tokens = tokens("int x = 42;");
        int documentEnd = tokens.get(tokens.size() - 1).startOffset();
        Token lastReal = tokens.get(tokens.size() - 2);

        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(tokens, documentEnd, VERSION);

        assertEquals(VERSION, checkpoint.version());
        assertEquals(lastReal.startOffset(), checkpoint.offset());
        assertEquals(lastReal.startOffset(), tokens.get(checkpoint.tokenIndex()).startOffset());
        assertEquals(LexicalState.NORMAL, checkpoint.state());
    }

    @Test
    void safeCheckpointOnEmptyStreamDefaultsToEditStart() {
        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(List.of(), 7, VERSION);

        assertEquals(7, checkpoint.offset());
        assertEquals(0, checkpoint.tokenIndex());
        assertEquals(LexicalState.NORMAL, checkpoint.state());
    }

    @Test
    void openContextQueries() {
        List<Token> stringTokens = tokens("String s = \"abc");
        Token string = first(stringTokens, "\"abc");
        assertTrue(tracker.isOpenStringBefore(stringTokens, string.startOffset() + 1));
        assertFalse(tracker.isOpenStringBefore(stringTokens, 0));

        List<Token> commentTokens = tokens("/* abc");
        Token comment = first(commentTokens, "/* abc");
        assertTrue(tracker.isOpenBlockCommentBefore(commentTokens, comment.startOffset() + 2));
        assertFalse(tracker.isOpenBlockCommentBefore(commentTokens, comment.endOffset()));
    }

    @Test
    void checkpointRejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> new LexicalCheckpoint(VERSION, -1, 0, LexicalState.NORMAL));
        assertThrows(IllegalArgumentException.class,
                () -> new LexicalCheckpoint(VERSION, 0, -1, LexicalState.NORMAL));
        assertThrows(IllegalArgumentException.class,
                () -> new LexicalCheckpoint(VERSION, 0, 0, null));
    }

    @Test
    void lexicalStateOpenContextSemantics() {
        assertFalse(LexicalState.NORMAL.isOpenContext());
        assertTrue(LexicalState.STRING.isOpenContext());
        assertTrue(LexicalState.CHARACTER.isOpenContext());
        assertTrue(LexicalState.LINE_COMMENT.isOpenContext());
        assertTrue(LexicalState.BLOCK_COMMENT.isOpenContext());
    }
}
