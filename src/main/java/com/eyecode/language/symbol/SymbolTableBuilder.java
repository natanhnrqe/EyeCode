package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;
import com.eyecode.language.Token;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.semantic.JavaNameResolver;
import com.eyecode.language.semantic.ResolvedSymbolReference;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link ProjectSymbolTable} from a {@link JavaFileModel} (Sprint 5.4a;
 * reference population added in 5.4d.2; type-position reference population
 * added in 5.4d.3).
 * <p>
 * This builder walks the {@link JavaFileModel} and populates a
 * {@link ProjectSymbolTable} with symbols for all declarations found in the
 * AST. It creates a lexically-nested scope hierarchy:
 * <pre>
 *   ROOT
 *     PACKAGE (only if a package is declared)
 *       TYPE         (top-level type, owned by package/root)
 *         FIELD*     (declared in the type scope)
 *         METHOD     (declared in the type scope; owns a METHOD scope)
 *           BLOCK    (parameter scope, owns PARAMETER symbols)
 *           BLOCK    (body scope, owns LOCAL_VARIABLE symbols)
 *         CONSTRUCTOR
 *           BLOCK    (parameter scope)
 *         TYPE       (nested type, owned by enclosing type)
 *           ...
 * </pre>
 * <p>
 * The builder does NOT perform type resolution, overload resolution,
 * inheritance analysis, or import resolution. It only indexes declarations
 * present in the model.
 * <p>
 * <b>Sprint 5.4d.2 — reference population:</b> after the symbol-declaration
 * phase, the builder walks the AST a second time via {@link ReferenceCollector}.
 * For every {@code NAME_EXPRESSION} leaf, it builds a tentative
 * {@link SymbolReference}, runs the existing {@link JavaNameResolver} to
 * obtain the resolved target, and — when the resolution succeeds — registers
 * the bound reference on the table. Unresolved names never produce an
 * indexed reference (no fabricated {@link Symbol}). Identity is structural
 * ({@code target + range + kind}); duplicates are suppressed by a
 * {@link LinkedHashSet}.
 * <p>
 * <b>Sprint 5.4d.3 — type-position reference population:</b> when an optional
 * source text is supplied at construction time, the {@link ReferenceCollector}
 * additionally indexes every {@code TYPE} AST leaf whose leading identifier
 * resolves to a {@link SymbolKind#TYPE}/{@link SymbolKind#INTERFACE}/
 * {@link SymbolKind#ENUM}/{@link SymbolKind#ANNOTATION} symbol. Primitive
 * keyword types ({@code int}, {@code long}, {@code var}, …) are skipped
 * automatically (they have no IDENTIFIER token at the TYPE range start).
 * Field / method return / parameter / local-variable type positions, plus
 * the type child of {@code new Foo()} / {@code (Foo) x} / {@code x instanceof Foo},
 * are all covered. Non-type matches (e.g. a local variable shadowing a class
 * of the same name) are filtered out — the reference is only registered when
 * the resolved symbol is a TYPE-compatible kind. Qualified types
 * ({@code java.util.List}, {@code pkg.Foo}) and generic / array suffixes are
 * not supported at this layer (the leading identifier resolves via the
 * simple-name chain only). No new lookup rule is added — the existing
 * {@link JavaNameResolver} chain is reused opaquely.
 */
public final class SymbolTableBuilder {

    private final ProjectSymbolTable symbolTable;
    private final JavaFileModel fileModel;
    private final long version;
    private final String sourceFile;
    private final String sourceText;

    public SymbolTableBuilder(JavaFileModel fileModel, long version, String sourceFile) {
        this(fileModel, version, sourceFile, null);
    }

    public SymbolTableBuilder(JavaFileModel fileModel, long version, String sourceFile, String sourceText) {
        this.fileModel = fileModel;
        this.version = version;
        this.sourceFile = sourceFile;
        this.sourceText = sourceText;
        this.symbolTable = new ProjectSymbolTable();
    }

    public SemanticModelSnapshot build() {
        SymbolScope rootScope = symbolTable.rootScope();

        // Create package scope if package exists. Use the file/CU range as the
        // package scope range — the package declaration and all top-level type
        // declarations live within the CU.
        SymbolScope ownerScope = rootScope;
        if (fileModel.getPackageName() != null && !fileModel.getPackageName().isEmpty()) {
            TextRange cuRange = fileModel.getRange();
            ownerScope = symbolTable.createChildScope(rootScope, ScopeKind.PACKAGE, cuRange);
        }

        // Process all top-level types in the package/root scope
        for (JavaClassModel typeModel : fileModel.getTypes()) {
            processType(typeModel, ownerScope);
        }

        // Sprint 5.4d.2 — populate SymbolReferences from the AST (simple-name
        // occurrences only — see ReferenceCollector for what is and isn't
        // emitted). Uses the existing JavaNameResolver lookup so shadowing
        // semantics are preserved verbatim.
        collectReferences();

        // Build the semantic model snapshot
        SymbolTable snapshotTable = symbolTable.snapshotTable(version, sourceFile);
        return new SemanticModelSnapshot(version, snapshotTable, sourceFile);
    }

    private void collectReferences() {
        AstNode astRoot = fileModel.getAstRoot();
        if (astRoot == null || astRoot.kind() != AstNodeKind.COMPILATION_UNIT) {
            return;
        }
        JavaNameResolver resolver = new JavaNameResolver();
        ReferenceCollector collector = new ReferenceCollector(symbolTable, resolver, sourceText);
        collector.walk(astRoot);
        for (SymbolReference ref : collector.references()) {
            symbolTable.addReference(ref);
        }
    }

    private void processType(JavaClassModel typeModel, SymbolScope ownerScope) {
        SymbolKind kind = mapTypeKind(typeModel.getKind());
        TextRange range = typeModel.getRange();

        // Create a TYPE child scope nested inside the lexical owner (package/root/nesting type)
        // The scope range covers the whole type declaration so the resolver can match it.
        SymbolScope typeScope = symbolTable.createChildScope(ownerScope, ScopeKind.TYPE, range);

        SymbolId typeId = SymbolId.of(ownerScope.id(), range, kind);
        String qualifiedName = buildQualifiedName(typeModel);
        Symbol typeSymbol = new Symbol(
                typeId,
                kind,
                typeModel.getName(),
                range,
                ownerScope.id(),
                typeScope.id(), // scopeId = type's own scope (where its members live)
                qualifiedName
        );
        // Declare the type in the owner scope (package/root/enclosing type scope).
        // The type is NOT declared in its own typeScope to avoid colliding with a member
        // of the same name (e.g. constructor A() inside class A). Body references to the
        // type's own name resolve through the parent chain: typeScope -> ownerScope -> hit.
        symbolTable.declareSymbol(ownerScope, typeSymbol);

        // Process fields — declared in the TYPE scope
        for (JavaFieldModel fieldModel : typeModel.getFields()) {
            processField(fieldModel, typeScope);
        }

        // Process constructors — declared in the TYPE scope, own a METHOD scope
        for (JavaConstructorModel constructorModel : typeModel.getConstructors()) {
            processConstructor(constructorModel, typeScope);
        }

        // Process methods — declared in the TYPE scope, own a METHOD scope
        for (JavaMethodModel methodModel : typeModel.getMethods()) {
            processMethod(methodModel, typeScope);
        }

        // Process nested types — declared in the enclosing TYPE scope, own their own TYPE scope
        for (JavaClassModel nestedType : typeModel.getNestedTypes()) {
            processType(nestedType, typeScope);
        }
    }

    private void processField(JavaFieldModel fieldModel, SymbolScope typeScope) {
        TextRange range = fieldModel.getRange();
        Symbol fieldSymbol = new Symbol(
                SymbolId.of(typeScope.id(), range, SymbolKind.FIELD),
                SymbolKind.FIELD,
                fieldModel.getName(),
                range,
                typeScope.id(),
                typeScope.id(),
                buildQualifiedName(fieldModel, typeScope)
        );
        symbolTable.declareSymbol(typeScope, fieldSymbol);
    }

    private void processConstructor(JavaConstructorModel constructorModel, SymbolScope typeScope) {
        TextRange range = constructorModel.getRange();
        // Constructor scope is nested inside the type scope (so the constructor has a proper lexical parent)
        SymbolScope constructorScope = symbolTable.createChildScope(typeScope, ScopeKind.METHOD, range);
        Symbol constructorSymbol = new Symbol(
                SymbolId.of(typeScope.id(), range, SymbolKind.CONSTRUCTOR),
                SymbolKind.CONSTRUCTOR,
                constructorModel.getName(),
                range,
                typeScope.id(),
                constructorScope.id(),
                buildQualifiedName(constructorModel, typeScope)
        );
        symbolTable.declareSymbol(typeScope, constructorSymbol);

        // Parameters live inside a BLOCK scope nested inside the constructor scope.
        // (Constructor bodies currently do NOT register locals with the model — see the
        // JavaParser currentMethod guard — so only parameters appear here.)
        SymbolScope constructorInnerScope = symbolTable.createChildScope(constructorScope, ScopeKind.BLOCK, range);
        for (JavaParameterModel param : constructorModel.getParameters()) {
            processParameter(param, constructorInnerScope);
        }
    }

    private void processMethod(JavaMethodModel methodModel, SymbolScope typeScope) {
        TextRange range = methodModel.getRange();
        // Method scope is nested inside the type scope
        SymbolScope methodScope = symbolTable.createChildScope(typeScope, ScopeKind.METHOD, range);
        Symbol methodSymbol = new Symbol(
                SymbolId.of(typeScope.id(), range, SymbolKind.METHOD),
                SymbolKind.METHOD,
                methodModel.getName(),
                range,
                typeScope.id(),
                methodScope.id(),
                buildQualifiedName(methodModel, typeScope)
        );
        symbolTable.declareSymbol(typeScope, methodSymbol);

        // Parameters and local variables share one BLOCK scope nested inside the
        // method scope so the resolution chain follows BLOCK -> METHOD -> TYPE:
        // any reference inside the method body looks up BLOCK first (where params
        // and locals are declared), then falls back to METHOD, then TYPE, etc.
        // This matches the Sprint 5.4b.1 spec hierarchy. The scope range covers
        // the whole method (header + body) so nested block-matching still works
        // when the resolver ascends from inner BLOCK siblings.
        SymbolScope methodInnerScope = symbolTable.createChildScope(methodScope, ScopeKind.BLOCK, range);
        for (JavaParameterModel param : methodModel.getParameters()) {
            processParameter(param, methodInnerScope);
        }
        for (JavaVariableModel local : methodModel.getLocalVariables()) {
            processLocalVariable(local, methodInnerScope);
        }
    }

    private void processParameter(JavaParameterModel param, SymbolScope parameterScope) {
        TextRange range = param.getRange();
        Symbol paramSymbol = new Symbol(
                SymbolId.of(parameterScope.id(), range, SymbolKind.PARAMETER),
                SymbolKind.PARAMETER,
                param.getName(),
                range,
                parameterScope.id(),
                parameterScope.id(),
                buildQualifiedName(param)
        );
        symbolTable.declareSymbol(parameterScope, paramSymbol);
    }

    private void processLocalVariable(JavaVariableModel local, SymbolScope bodyScope) {
        TextRange range = local.getRange();
        Symbol localSymbol = new Symbol(
                SymbolId.of(bodyScope.id(), range, SymbolKind.LOCAL_VARIABLE),
                SymbolKind.LOCAL_VARIABLE,
                local.getName(),
                range,
                bodyScope.id(),
                bodyScope.id(),
                buildQualifiedName(local)
        );
        symbolTable.declareSymbol(bodyScope, localSymbol);
    }

    private SymbolKind mapTypeKind(TypeKind kind) {
        return switch (kind) {
            case CLASS -> SymbolKind.TYPE;
            case INTERFACE -> SymbolKind.INTERFACE;
            case ENUM -> SymbolKind.ENUM;
            case RECORD -> SymbolKind.TYPE;
        };
    }

    private String buildQualifiedName(JavaClassModel type) {
        String pkg = fileModel.getPackageName();
        return (pkg != null && !pkg.isEmpty()) ? pkg + "." + type.getName() : type.getName();
    }

    private String buildQualifiedName(JavaFieldModel field, SymbolScope typeScope) {
        return typeScope.kind() + "." + field.getName();
    }

    private String buildQualifiedName(JavaMethodModel method, SymbolScope typeScope) {
        return method.getOwner() + "." + method.getName();
    }

    private String buildQualifiedName(JavaConstructorModel constructor, SymbolScope typeScope) {
        return constructor.getOwner() + "." + constructor.getName();
    }

    private String buildQualifiedName(JavaParameterModel param) {
        return param.getName();
    }

    private String buildQualifiedName(JavaVariableModel local) {
        return local.getName();
    }

    // ----------------------------------------------------------------------
    // Reference collection (Sprint 5.4d.2; type-position expansion in 5.4d.3)
    //
    // Walks the AST once after the symbol-declaration phase. For every
    // NAME_EXPRESSION leaf, builds a SymbolReference, runs the existing
    // JavaNameResolver to obtain the resolved target, and — when the
    // resolution succeeds — registers it on the table.
    //
    // When an optional source text was supplied at construction time, the
    // collector additionally indexes every TYPE AST leaf whose leading
    // IDENTIFIER token resolves to a TYPE-compatible symbol (TYPE,
    // INTERFACE, ENUM, ANNOTATION). Primitive keyword types (int, long,
    // var, ...) carry no IDENTIFIER at the TYPE range start and are
    // skipped automatically.
    //
    // Identity is structural (target + range + kind); duplicates are
    // suppressed by a LinkedHashSet. Unresolved names never produce an
    // indexed reference (no fabricated Symbol).
    // ----------------------------------------------------------------------

    private static final class ReferenceCollector {

        private final SymbolTable table;
        private final JavaNameResolver resolver;
        private final String sourceText;
        private final Deque<SymbolScope> scopeStack = new ArrayDeque<>();
        private final Set<SymbolReference> references = new LinkedHashSet<>();
        private List<Token> sourceTokens;
        private JavaLexerService lexerService;

        ReferenceCollector(SymbolTable table, JavaNameResolver resolver, String sourceText) {
            this.table = Objects.requireNonNull(table, "table must not be null");
            this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
            this.sourceText = sourceText;
            this.scopeStack.push(table.rootScope());
            if (sourceText != null && !sourceText.isEmpty()) {
                this.lexerService = new JavaLexerService();
            }
        }

        Set<SymbolReference> references() {
            return references;
        }

        void walk(AstNode node) {
            switch (node.kind()) {
                case COMPILATION_UNIT -> visitChildren(node);
                case PACKAGE_DECLARATION -> {
                    pushMatching(node, ScopeKind.PACKAGE);
                    visitChildren(node);
                    pop();
                }
                case CLASS_DECLARATION, INTERFACE_DECLARATION, ENUM_DECLARATION, RECORD_DECLARATION -> {
                    pushMatching(node, ScopeKind.TYPE);
                    visitChildren(node);
                    pop();
                }
                case METHOD_DECLARATION, CONSTRUCTOR_DECLARATION -> {
                    pushMatching(node, ScopeKind.METHOD);
                    visitChildren(node);
                    pop();
                }
                case BLOCK -> {
                    pushMatching(node, ScopeKind.BLOCK, true);
                    visitChildren(node);
                    pop();
                }
                case NAME_EXPRESSION -> resolveSimpleName(node);
                case TYPE -> resolveTypeName(node);
                default -> visitChildren(node);
            }
        }

        private void visitChildren(AstNode node) {
            for (AstNode child : node.children()) {
                walk(child);
            }
        }

        private void pushMatching(AstNode node, ScopeKind target) {
            pushMatching(node, target, false);
        }

        private void pushMatching(AstNode node, ScopeKind target, boolean preferSmallest) {
            SymbolScope current = scopeStack.peek();
            if (current == null) {
                return;
            }
            SymbolScope best = null;
            for (SymbolScope child : current.children()) {
                if (child.kind() != target) {
                    continue;
                }
                if (!rangeContains(child.range(), node.range())) {
                    continue;
                }
                if (best == null || area(child.range()) < area(best.range())) {
                    best = child;
                }
                if (!preferSmallest) {
                    break;
                }
            }
            scopeStack.push(best != null ? best : current);
        }

        private void pop() {
            if (scopeStack.size() > 1) {
                scopeStack.pop();
            }
        }

        private void resolveSimpleName(AstNode node) {
            String name = nameOf(node);
            if (name == null || name.isEmpty()) {
                return;
            }
            SymbolScope scope = scopeStack.peek();
            if (scope == null) {
                return;
            }
            SymbolReference tentative = SymbolReference.simple(name, scope.id(), node.range());
            ResolvedSymbolReference resolved = resolver.resolve(tentative, table);
            if (!resolved.isResolved()) {
                return;
            }
            register(resolved.resolvedSymbolId(), tentative);
        }

        /**
         * Sprint 5.4d.3 — resolve a {@code TYPE} leaf via the leading
         * IDENTIFIER token. The collector was constructed with a source
         * text (otherwise type-position indexing is disabled — the
         * {@code walk} switch simply no-ops on TYPE leaves when the
         * lexer hasn't been initialized).
         * <p>
         * The resolved symbol must be a TYPE-compatible kind
         * ({@link SymbolKind#TYPE}, {@link SymbolKind#INTERFACE},
         * {@link SymbolKind#ENUM}, {@link SymbolKind#ANNOTATION}). A
         * non-type match (e.g. a local variable shadowing a class of the
         * same name) is filtered out — no fabricated type reference.
         */
        private void resolveTypeName(AstNode node) {
            if (sourceText == null) {
                visitChildren(node);
                return;
            }
            String name = leadingIdentifierName(node);
            if (name == null || name.isEmpty()) {
                visitChildren(node);
                return;
            }
            SymbolScope scope = scopeStack.peek();
            if (scope == null) {
                visitChildren(node);
                return;
            }
            SymbolReference tentative = SymbolReference.simple(name, scope.id(), node.range());
            ResolvedSymbolReference resolved = resolver.resolve(tentative, table);
            if (!resolved.isResolved()) {
                visitChildren(node);
                return;
            }
            SymbolId target = resolved.resolvedSymbolId();
            if (!isTypeCompatible(target)) {
                visitChildren(node);
                return;
            }
            register(target, tentative);
            visitChildren(node);
        }

        private void register(SymbolId target, SymbolReference tentative) {
            SymbolReference bound = new SymbolReference(
                    target,
                    tentative.range(),
                    tentative.name(),
                    tentative.scopeId(),
                    tentative.kind());
            references.add(bound);
        }

        private boolean isTypeCompatible(SymbolId target) {
            Symbol symbol = table.find(target).orElse(null);
            if (symbol == null) {
                return false;
            }
            return switch (symbol.kind()) {
                case TYPE, INTERFACE, ENUM, ANNOTATION -> true;
                default -> false;
            };
        }

        /**
         * Lazily lexes the source text and finds the IDENTIFIER token
         * whose {@code startOffset} equals the TYPE leaf's
         * {@code startOffset}. Returns {@code null} for primitive
         * keyword types (no IDENTIFIER at that offset) or when the
         * lexer has not been initialized.
         */
        private String leadingIdentifierName(AstNode typeNode) {
            if (lexerService == null) {
                return null;
            }
            if (sourceTokens == null) {
                sourceTokens = lexerService
                        .lex(com.eyecode.editor.intelligence.document.DocumentSnapshot.oneShot(sourceText))
                        .tokens();
            }
            int start = typeNode.range().startOffset();
            for (Token t : sourceTokens) {
                if (t.range().startOffset() == start
                        && t.type() == JavaTokenType.IDENTIFIER) {
                    return t.text();
                }
            }
            return null;
        }

        private static String nameOf(AstNode node) {
            if (node.token() != null) {
                String text = node.token().text();
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
            return null;
        }

        private static boolean rangeContains(TextRange outer, TextRange inner) {
            return outer.startOffset() <= inner.startOffset()
                    && inner.endOffset() <= outer.endOffset();
        }

        private static int area(TextRange r) {
            int size = r.endOffset() - r.startOffset();
            return Math.max(size, 0);
        }
    }
}
