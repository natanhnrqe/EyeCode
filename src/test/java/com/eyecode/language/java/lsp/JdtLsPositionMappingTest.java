package com.eyecode.language.java.lsp;

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JdtLsPositionMappingTest {
    @Test
    void positionAfterSurrogatePairUsesTwoUtf16CodeUnits() {
        String source = "String mensagem = \"😀\";\nString texto = mensagem;\ntexto.";
        int offset = source.length();
        Position position = JdtLsProjectCompletion.positionFor(source, offset);
        assertEquals(2, position.getLine());
        assertEquals("texto.".length(), position.getCharacter());
    }

    @Test
    void sameLineEmojiContributesTwoUnitsBeforeCompletionCursor() {
        String source = "String texto = \"😀\" + valor; texto.";
        int offset = source.length();
        Position position = JdtLsProjectCompletion.positionFor(source, offset);
        assertEquals(source.length(), position.getCharacter());
        assertEquals(source.codePointCount(0, source.length()) + 1, position.getCharacter());
    }
}
