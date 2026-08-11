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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds a {@link ProjectSymbolTable} from a {@link JavaFileModel} (Sprint 5.4a).
 * <p>
 * This builder walks the {@link JavaFileModel} and populates a
 * {@link ProjectSymbolTable} with symbols for all declarations found in the
 * AST. It creates the scope hierarchy and registers symbols with their
 * correct owners and scopes.
 * <p>
 * The builder does NOT perform type resolution, overload resolution,
 * inheritance analysis, or import resolution. It only indexes
 * declarations present in the AST.
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

        // Create package scope if package exists
        SymbolScope packageScope = rootScope;
        if (fileModel.getPackageName() != null && !fileModel.getPackageName().isEmpty()) {
            SymbolScope packageScopeCreated = symbolTable.createChildScope(rootScope, ScopeKind.PACKAGE);
            packageScope = packageScopeCreated;
        }

        // Process all top-level types
        for (JavaClassModel typeModel : fileModel.getTypes()) {
            processType(typeModel, packageScope);
        }

        // Build the semantic model snapshot
        // Get the symbol table snapshot first, then create the semantic model snapshot
        SymbolTable snapshotTable = symbolTable.snapshot(version, sourceFile).symbolTable();
        return new SemanticModelSnapshot(version, snapshotTable, sourceFile);
    }

    private void processType(JavaClassModel typeModel, SymbolScope ownerScope) {
        SymbolKind kind = mapTypeKind(typeModel.getKind());
        TextRange range = typeModel.getRange();

        // Create type scope first
        SymbolScope typeScope = symbolTable.createChildScope(symbolTable.rootScope(), ScopeKind.TYPE);

        // Create type symbol with its own scope ID
        SymbolId typeId = new SymbolId(ownerScope.id(), typeModel.getRange(), mapTypeKind(typeModel.getKind()));
        String qualifiedName = buildQualifiedName(typeModel);
        Symbol typeSymbol = new Symbol(
                new SymbolId(ownerScope.id(), typeModel.getRange(), mapTypeKind(typeModel.getKind())),
                kind,
                typeModel.getName(),
                typeModel.getRange(),
                ownerScope.id(),
                typeScope.id(), // scopeId = type's own scope
                qualifiedName
        );

        symbolTable.declareSymbol(typeScope, typeSymbol);

        // Register the type symbol in the owner scope (package/root)
        symbolTable.declareSymbol(symbolTable.rootScope(), new Symbol(
                new SymbolId(ownerScope.id(), typeModel.getRange(), mapTypeKind(typeModel.getKind())),
                kind,
                typeModel.getName(),
                range,
                ownerScope.id(),
                typeScope.id(),
                qualifiedName
        ));

        // Store the type symbol id for children to reference
        SymbolId typeSymbolId = typeSymbol.id();

        // Process fields
        for (JavaFieldModel fieldModel : typeModel.getFields()) {
            processField(fieldModel, typeScope, typeSymbol.id().ownerScopeId());
        }

        // Process constructors
        for (JavaConstructorModel constructorModel : typeModel.getConstructors()) {
            processConstructor(constructorModel, typeScope, typeSymbol.id().ownerScopeId());
        }

        // Process methods
        for (JavaMethodModel methodModel : typeModel.getMethods()) {
            processMethod(methodModel, typeScope, typeSymbol.id().ownerScopeId());
        }

        // Process nested types
        for (JavaClassModel nestedType : typeModel.getNestedTypes()) {
            processType(nestedType, typeScope);
        }
    }

    private void processField(JavaFieldModel fieldModel, SymbolScope typeScope, long ownerScopeId) {
        TextRange range = fieldModel.getRange();
        Symbol fieldSymbol = new Symbol(
                new SymbolId(ownerScopeId, range, SymbolKind.FIELD),
                SymbolKind.FIELD,
                fieldModel.getName(),
                range,
                ownerScopeId,
                ownerScopeId, // scopeId = ownerScopeId for fields
                buildQualifiedName(fieldModel)
        );
        symbolTable.declareSymbol(typeScope, fieldSymbol);
    }

    private void processConstructor(JavaConstructorModel constructorModel, SymbolScope typeScope, long ownerScopeId) {
        TextRange range = constructorModel.getRange();
        String qualifiedName = constructorModel.getOwner() + "." + constructorModel.getName();
        Symbol constructorSymbol = new Symbol(
                new SymbolId(ownerScopeId, range, SymbolKind.CONSTRUCTOR),
                SymbolKind.CONSTRUCTOR,
                constructorModel.getName(),
                range,
                ownerScopeId,
                ownerScopeId, // scopeId = ownerScopeId for constructors
                qualifiedName
        );

        // Create constructor scope (methods use METHOD scope kind)
        SymbolScope constructorScope = symbolTable.createChildScope(symbolTable.rootScope(), ScopeKind.METHOD);
        symbolTable.declareSymbol(constructorScope, constructorSymbol);

        // Process parameters
        SymbolScope parameterScope = symbolTable.createChildScope(constructorScope, ScopeKind.BLOCK);
        for (JavaParameterModel param : constructorModel.getParameters()) {
            processParameter(param, parameterScope, constructorSymbol.id().ownerScopeId());
        }
    }

    private void processMethod(JavaMethodModel methodModel, SymbolScope typeScope, long ownerScopeId) {
        TextRange range = methodModel.getRange();
        String qualifiedName = methodModel.getOwner() + "." + methodModel.getName();
        Symbol methodSymbol = new Symbol(
                new SymbolId(ownerScopeId, range, SymbolKind.METHOD),
                SymbolKind.METHOD,
                methodModel.getName(),
                range,
                ownerScopeId,
                ownerScopeId, // scopeId = ownerScopeId for methods (they have their own scope created below)
                qualifiedName
        );

        // Create method scope
        SymbolScope methodScope = symbolTable.createChildScope(symbolTable.rootScope(), ScopeKind.METHOD);
        symbolTable.declareSymbol(methodScope, methodSymbol);

        // Process parameters
        SymbolScope parameterScope = symbolTable.createChildScope(methodScope, ScopeKind.BLOCK);
        for (JavaParameterModel param : methodModel.getParameters()) {
            processParameter(param, parameterScope, methodSymbol.id().ownerScopeId());
        }

        // Process local variables
        SymbolScope bodyScope = symbolTable.createChildScope(methodScope, ScopeKind.BLOCK);
        for (JavaVariableModel local : methodModel.getLocalVariables()) {
            processLocalVariable(local, bodyScope, methodSymbol.id().ownerScopeId());
        }
    }

    private void processParameter(JavaParameterModel param, SymbolScope parameterScope, long ownerScopeId) {
        TextRange range = param.getRange();
        Symbol paramSymbol = new Symbol(
                new SymbolId(ownerScopeId, range, SymbolKind.PARAMETER),
                SymbolKind.PARAMETER,
                param.getName(),
                range,
                ownerScopeId,
                ownerScopeId, // scopeId = ownerScopeId for parameters
                buildQualifiedName(param)
        );
        symbolTable.declareSymbol(parameterScope, paramSymbol);
    }

    private void processLocalVariable(JavaVariableModel local, SymbolScope bodyScope, long ownerScopeId) {
        TextRange range = local.getRange();
        Symbol localSymbol = new Symbol(
                new SymbolId(ownerScopeId, range, SymbolKind.LOCAL_VARIABLE),
                SymbolKind.LOCAL_VARIABLE,
                local.getName(),
                range,
                ownerScopeId,
                ownerScopeId, // scopeId = ownerScopeId for local variables
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

    private String buildQualifiedName(JavaFieldModel field) {
        return field.getOwner() + "." + field.getName();
    }

    private String buildQualifiedName(JavaParameterModel param) {
        return param.getName(); // Simplified
    }

    private String buildQualifiedName(JavaVariableModel local) {
        return local.getName(); // Simplified
    }

    private String buildQualifiedName(JavaClassModel type) {
        return type.getName(); // Simplified - would need package context
    }
}