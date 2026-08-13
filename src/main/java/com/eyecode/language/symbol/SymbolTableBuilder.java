package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;

import java.util.List;
import java.util.Optional;

/**
 * Builds a {@link ProjectSymbolTable} from a {@link JavaFileModel} (Sprint 5.4a).
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
 */
public final class SymbolTableBuilder {

    private final ProjectSymbolTable symbolTable;
    private final JavaFileModel fileModel;
    private final long version;
    private final String sourceFile;

    public SymbolTableBuilder(JavaFileModel fileModel, long version, String sourceFile) {
        this.fileModel = fileModel;
        this.version = version;
        this.sourceFile = sourceFile;
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

        // Build the semantic model snapshot
        SymbolTable snapshotTable = symbolTable.snapshotTable(version, sourceFile);
        return new SemanticModelSnapshot(version, snapshotTable, sourceFile);
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
}
