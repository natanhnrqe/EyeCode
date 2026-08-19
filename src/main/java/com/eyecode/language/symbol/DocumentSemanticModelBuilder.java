package com.eyecode.language.symbol;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerSnapshot;

import java.util.List;
import java.util.Optional;

public final class DocumentSemanticModelBuilder {

    private final JavaLexerService lexerService;

    public DocumentSemanticModelBuilder() {
        this(new JavaLexerService());
    }

    public DocumentSemanticModelBuilder(JavaLexerService lexerService) {
        this.lexerService = lexerService;
    }

    public Optional<SemanticModelSnapshot> build(EditorDocument document) {
        if (document == null) {
            return Optional.empty();
        }
        try {
            String source = document.getText();
            LexerSnapshot lexerSnapshot = lexerService.lex(document.snapshot());
            List<Token> tokens = lexerSnapshot.tokens();
            JavaTokenStream stream = new JavaTokenStream(tokens, source);
            JavaFileModel model = new JavaParser(stream).parse();
            String sourceFile = document.getSourceFile() != null
                    ? document.getSourceFile().getFileName().toString()
                    : "Untitled.java";
            return Optional.of(new SymbolTableBuilder(
                    model,
                    document.currentVersion(),
                    sourceFile,
                    source
            ).build());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
