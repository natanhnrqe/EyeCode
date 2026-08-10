package com.eyecode.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserServiceTest {

    private static DocumentSnapshot sessionSnapshot(String sessionId, long version, String text) {
        return new DocumentSnapshot(version, text, LineMap.of(text), null, sessionId);
    }

    @Test
    void fullParseProducesAstRoot() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot snapshot = service.parse(sessionSnapshot("s1", 1, "class A {}"));
        assertNotNull(snapshot.astRoot());
        assertEquals(1, snapshot.version());
    }

    @Test
    void versionParity() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot snapshot = service.parse(sessionSnapshot("s1", 42, "class A {}"));
        assertEquals(42, snapshot.version());
    }

    @Test
    void emptyDocumentStillProducesAst() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot snapshot = service.parse(sessionSnapshot("s1", 1, ""));
        assertNotNull(snapshot.astRoot());
    }

    @Test
    void invalidDocumentStillProducesAst() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot snapshot = service.parse(sessionSnapshot("s1", 1, "class A {"));
        assertNotNull(snapshot.astRoot());
    }

    @Test
    void nullDocumentRejected() {
        JavaParserService service = new JavaParserService();
        assertThrows(IllegalArgumentException.class, () -> service.parse(null));
    }

    @Test
    void cacheHitReturnsSameSnapshotInstance() {
        JavaParserService service = new JavaParserService();
        DocumentSnapshot doc = sessionSnapshot("s1", 1, "class A {}");
        ParserSnapshot first = service.parse(doc);
        ParserSnapshot second = service.parse(doc);
        assertSame(first, second);
    }

    @Test
    void differentSessionAlwaysFullReparse() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot a = service.parse(sessionSnapshot("s1", 1, "class A {}"));
        ParserSnapshot b = service.parse(sessionSnapshot("s2", 1, "class A {}"));
        assertEquals(a.version(), b.version());
        assertEquals(a.text(), b.text());
    }

    @Test
    void anonymousOneShotAlwaysFullReparse() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot a = service.parse(DocumentSnapshot.oneShot("class A {}"));
        ParserSnapshot b = service.parse(DocumentSnapshot.oneShot("class A {}"));
        assertNotNull(a);
        assertNotNull(b);
    }

    @Test
    void invalidateClearsCache() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot first = service.parse(sessionSnapshot("s1", 1, "class A {}"));
        service.invalidate();
        assertEquals(null, service.cachedSnapshot());
        ParserSnapshot second = service.parse(sessionSnapshot("s1", 1, "class A {}"));
        assertNotNull(second);
        assertEquals(first.version(), second.version());
    }

    @Test
    void newerVersionReplacesCached() {
        JavaParserService service = new JavaParserService();
        service.parse(sessionSnapshot("s1", 1, "class A {}"));
        ParserSnapshot v2 = service.parse(sessionSnapshot("s1", 2, "class AB {}"));
        assertEquals(2, v2.version());
        assertEquals(2, service.cachedSnapshot().version());
    }

    @Test
    void newerVersionReturnsCachedWithoutFullReparse() {
        JavaParserService service = new JavaParserService();
        ParserSnapshot v1 = service.parse(sessionSnapshot("s1", 1, "class A {}"));
        ParserSnapshot v1Again = service.parse(sessionSnapshot("s1", 1, "class A {}"));
        assertSame(v1, v1Again);
    }
}
