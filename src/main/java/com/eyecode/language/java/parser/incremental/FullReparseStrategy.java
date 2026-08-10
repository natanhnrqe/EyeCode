package com.eyecode.language.java.parser.incremental;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.java.JavaLexerService;

/**
 * Fallback path for the incremental parser: rebuild the AST from scratch.
 * <p>
 * Always produces a valid AST. Used by the parser service whenever
 * {@link ParserChangeRegion#fallbackRequired()} is {@code true} or when no
 * previous snapshot exists. The resulting AST is rooted on
 * {@code COMPILATION_UNIT} with parent links established via
 * {@link AstNodes#linkParents(AstNode)}.
 */
public final class FullReparseStrategy {

    public AstNode reparse(DocumentSnapshot snapshot) {
        JavaLexerService lexer = new JavaLexerService();
        var lexSnapshot = lexer.lex(snapshot);
        String text = snapshot.getText();
        JavaTokenStream stream = new JavaTokenStream(lexSnapshot.tokens(), text);
        JavaParser parser = new JavaParser(stream);
        AstNode root = parser.parse().getAstRoot();
        return root;
    }
}
