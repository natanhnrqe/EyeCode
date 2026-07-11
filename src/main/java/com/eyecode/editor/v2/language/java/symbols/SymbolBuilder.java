package com.eyecode.editor.v2.language.java.symbols;

import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SymbolBuilder {

    public List<ProjectSymbol> build(JavaFileModel model, Path sourceFile) {
        List<ProjectSymbol> symbols = new ArrayList<>();
        for (JavaClassModel type : model.getTypes()) {
            buildType(type, sourceFile, symbols);
        }
        return symbols;
    }

    private void buildType(JavaClassModel type, Path sourceFile, List<ProjectSymbol> symbols) {
        ProjectSymbol symbol = new ProjectSymbol();
        symbol.setName(type.getName());
        symbol.setKind(toKind(type.getKind()));
        symbol.setSuperClassName(type.getSuperClass());
        symbol.setModifiers(type.getModifiers());
        symbol.setSourceFile(sourceFile);
        symbol.setAstReference(type);
        symbols.add(symbol);

        for (JavaFieldModel field : type.getFields()) {
            ProjectSymbol fieldSymbol = new ProjectSymbol();
            fieldSymbol.setName(field.getName());
            fieldSymbol.setKind(SymbolKind.FIELD);
            fieldSymbol.setOwner(type.getName());
            fieldSymbol.setType(field.getType());
            fieldSymbol.setModifiers(field.getModifiers());
            fieldSymbol.setSourceFile(sourceFile);
            fieldSymbol.setAstReference(field);
            symbols.add(fieldSymbol);
        }

        for (JavaConstructorModel constructor : type.getConstructors()) {
            ProjectSymbol constructorSymbol = new ProjectSymbol();
            constructorSymbol.setName(constructor.getName());
            constructorSymbol.setKind(SymbolKind.CONSTRUCTOR);
            constructorSymbol.setOwner(type.getName());
            constructorSymbol.setSignature("(...)");
            constructorSymbol.setModifiers(constructor.getModifiers());
            constructorSymbol.setSourceFile(sourceFile);
            constructorSymbol.setAstReference(constructor);
            symbols.add(constructorSymbol);
        }

        for (JavaMethodModel method : type.getMethods()) {
            ProjectSymbol methodSymbol = new ProjectSymbol();
            methodSymbol.setName(method.getName());
            methodSymbol.setKind(SymbolKind.METHOD);
            methodSymbol.setOwner(type.getName());
            methodSymbol.setType(method.getReturnType());
            methodSymbol.setSignature("(...)");
            methodSymbol.setModifiers(method.getModifiers());
            methodSymbol.setSourceFile(sourceFile);
            methodSymbol.setAstReference(method);
            symbols.add(methodSymbol);

            for (JavaParameterModel parameter : method.getParameters()) {
                ProjectSymbol parameterSymbol = new ProjectSymbol();
                parameterSymbol.setName(parameter.getName());
                parameterSymbol.setKind(SymbolKind.PARAMETER);
                parameterSymbol.setOwner(method.getName());
                parameterSymbol.setType(parameter.getType());
                parameterSymbol.setSourceFile(sourceFile);
                parameterSymbol.setAstReference(parameter);
                symbols.add(parameterSymbol);
            }

            for (JavaVariableModel variable : method.getLocalVariables()) {
                ProjectSymbol variableSymbol = new ProjectSymbol();
                variableSymbol.setName(variable.getName());
                variableSymbol.setKind(SymbolKind.LOCAL_VARIABLE);
                variableSymbol.setOwner(method.getName());
                variableSymbol.setType(variable.getType());
                variableSymbol.setSourceFile(sourceFile);
                variableSymbol.setAstReference(variable);
                symbols.add(variableSymbol);
            }
        }

        for (JavaClassModel nested : type.getNestedTypes()) {
            buildType(nested, sourceFile, symbols);
        }
    }

    private SymbolKind toKind(TypeKind kind) {
        return switch (kind) {
            case CLASS -> SymbolKind.CLASS;
            case INTERFACE -> SymbolKind.INTERFACE;
            case ENUM -> SymbolKind.ENUM;
            case RECORD -> SymbolKind.RECORD;
        };
    }
}
