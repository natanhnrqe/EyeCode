package com.eyecode.language.documentation;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.semantic.DefinitionAtCaretResolver;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.learning.content.DocumentationTarget;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocumentationAtCaretResolver {

    private static final Pattern IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+(?:static\\s+)?([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");
    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "\\b(?:class|interface|enum|record|@interface)\\s+([A-Za-z_$][\\w$]*)");

    private final DefinitionAtCaretResolver definitionResolver;
    private final JavaDocumentationResolver documentationResolver;
    private final JavaLexerService lexerService;

    public DocumentationAtCaretResolver() {
        this(new DefinitionAtCaretResolver(), new JavaDocumentationResolver(), new JavaLexerService());
    }

    DocumentationAtCaretResolver(DefinitionAtCaretResolver definitionResolver,
                                 JavaDocumentationResolver documentationResolver,
                                 JavaLexerService lexerService) {
        this.definitionResolver = definitionResolver;
        this.documentationResolver = documentationResolver;
        this.lexerService = lexerService;
    }

    public Optional<DocumentationTarget> resolve(String source, int caretOffset) {
        return resolve(source, caretOffset, null);
    }

    public Optional<DocumentationTarget> resolve(String source,
                                                 int caretOffset,
                                                 SymbolTable symbolTable) {
        return resolveType(source, caretOffset, symbolTable)
                .map(documentationResolver::target);
    }

    public Optional<JavaJdkType> resolveType(String source, int caretOffset) {
        return resolveType(source, caretOffset, null);
    }

    public Optional<JavaJdkType> resolveType(String source,
                                             int caretOffset,
                                             SymbolTable symbolTable) {
        if (source == null || source.isEmpty()) {
            return Optional.empty();
        }
        int offset = Math.max(0, Math.min(caretOffset, source.length()));
        if (symbolTable != null) {
            Optional<DefinitionLocation> definition = resolveDefinition(source, offset, symbolTable);
            if (definition.isPresent()) {
                return documentationResolver.resolveType(definition.get().symbol());
            }
        }

        LexerSnapshot snapshot = lexerService.lex(DocumentSnapshot.oneShot(source));
        Optional<Token> token = tokenAt(snapshot, offset);
        if (token.isEmpty() || token.get().type() != JavaTokenType.IDENTIFIER) {
            return Optional.empty();
        }

        String identifier = qualifiedIdentifierAt(source, offset);
        Map<String, String> imports = imports(source);
        String simpleName = simpleName(identifier);
        if (isProjectType(source, simpleName)) {
            return Optional.empty();
        }
        String qualifiedName = imports.getOrDefault(simpleName, identifier);
        Optional<JavaJdkType> type = JavaJdkTypeCatalog.findQualified(qualifiedName);
        if (type.isEmpty() && !identifier.contains(".")) {
            type = JavaJdkTypeCatalog.findSimple(identifier);
        }
        return type;
    }

    private Optional<DefinitionLocation> resolveDefinition(String source,
                                                            int offset,
                                                            SymbolTable table) {
        try {
            return definitionResolver.resolve(source, offset, table);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Token> tokenAt(LexerSnapshot snapshot, int offset) {
        return snapshot.tokens().stream()
                .filter(token -> token.type() == JavaTokenType.COMMENT
                        || token.type() == JavaTokenType.STRING
                        || token.type() == JavaTokenType.CHARACTER
                        || (token.type() == JavaTokenType.IDENTIFIER
                        && token.startOffset() <= offset && offset <= token.endOffset()))
                .filter(token -> token.startOffset() <= offset && offset <= token.endOffset())
                .findFirst()
                .filter(token -> token.type() == JavaTokenType.IDENTIFIER);
    }

    private static String qualifiedIdentifierAt(String source, int offset) {
        int start = offset;
        int end = offset;
        if (start == source.length() || !isIdentifierPart(source.charAt(start))) {
            start--;
        }
        while (start >= 0 && isIdentifierPart(source.charAt(start))) {
            start--;
        }
        start++;
        while (end < source.length() && isIdentifierPart(source.charAt(end))) {
            end++;
        }
        String name = source.substring(start, end);
        while (start > 0 && source.charAt(start - 1) == '.') {
            int componentEnd = start - 1;
            int componentStart = componentEnd - 1;
            while (componentStart >= 0 && isIdentifierPart(source.charAt(componentStart))) {
                componentStart--;
            }
            componentStart++;
            name = source.substring(componentStart, componentEnd) + "." + name;
            start = componentStart;
        }
        return name;
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    private static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private static Map<String, String> imports(String source) {
        Map<String, String> imports = new HashMap<>();
        Matcher matcher = IMPORT.matcher(source);
        while (matcher.find()) {
            String qualified = matcher.group(1);
            imports.put(simpleName(qualified), qualified);
        }
        return imports;
    }

    private static boolean isProjectType(String source, String simpleName) {
        Matcher matcher = TYPE_DECLARATION.matcher(source);
        while (matcher.find()) {
            if (matcher.group(1).equals(simpleName)) {
                return true;
            }
        }
        return false;
    }
}
