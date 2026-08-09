package com.eyecode.language.ast;

/**
 * Extensible visitor for the declarative AST (Sprint 5.3a).
 * <p>
 * The generic {@link #visit(AstNode)} dispatches to the per-kind
 * {@code visitXxx} method; implementors override only the kinds they care
 * about. Traversal is driven by {@link AstNodes#traverse}.
 * <p>
 * Future services (symbol collection, semantic analysis, diagnostics,
 * completion, references, hover) will implement this interface — no such
 * service is coupled to the AST yet.
 */
public interface AstVisitor {

    /**
     * Dispatches to the per-kind {@code visitXxx} method. Called by
     * {@link AstNodes#traverse} for every node (pre-order).
     */
    default void visit(AstNode node) {
        switch (node.kind()) {
            case COMPILATION_UNIT -> visitCompilationUnit(node);
            case PACKAGE_DECLARATION -> visitPackageDeclaration(node);
            case IMPORT_DECLARATION -> visitImportDeclaration(node);
            case CLASS_DECLARATION -> visitClassDeclaration(node);
            case INTERFACE_DECLARATION -> visitInterfaceDeclaration(node);
            case ENUM_DECLARATION -> visitEnumDeclaration(node);
            case RECORD_DECLARATION -> visitRecordDeclaration(node);
            case ANNOTATION -> visitAnnotation(node);
            case FIELD_DECLARATION -> visitFieldDeclaration(node);
            case METHOD_DECLARATION -> visitMethodDeclaration(node);
            case CONSTRUCTOR_DECLARATION -> visitConstructorDeclaration(node);
            case PARAMETER -> visitParameter(node);
            case TYPE -> visitType(node);
            case MODIFIER -> visitModifier(node);
        }
    }

    default void visitCompilationUnit(AstNode node) {
    }

    default void visitPackageDeclaration(AstNode node) {
    }

    default void visitImportDeclaration(AstNode node) {
    }

    default void visitClassDeclaration(AstNode node) {
    }

    default void visitInterfaceDeclaration(AstNode node) {
    }

    default void visitEnumDeclaration(AstNode node) {
    }

    default void visitRecordDeclaration(AstNode node) {
    }

    default void visitAnnotation(AstNode node) {
    }

    default void visitFieldDeclaration(AstNode node) {
    }

    default void visitMethodDeclaration(AstNode node) {
    }

    default void visitConstructorDeclaration(AstNode node) {
    }

    default void visitParameter(AstNode node) {
    }

    default void visitType(AstNode node) {
    }

    default void visitModifier(AstNode node) {
    }
}
