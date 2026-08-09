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
            case BLOCK -> visitBlock(node);
            case EMPTY_STATEMENT -> visitEmptyStatement(node);
            case LOCAL_VARIABLE_DECLARATION -> visitLocalVariableDeclaration(node);
            case EXPRESSION_STATEMENT -> visitExpressionStatement(node);
            case IF_STATEMENT -> visitIfStatement(node);
            case CONDITION -> visitCondition(node);
            case THEN -> visitThen(node);
            case ELSE -> visitElse(node);
            case FOR_STATEMENT -> visitForStatement(node);
            case INITIALIZER -> visitInitializer(node);
            case UPDATE -> visitUpdate(node);
            case ENHANCED_FOR_STATEMENT -> visitEnhancedForStatement(node);
            case VARIABLE -> visitVariable(node);
            case ITERABLE -> visitIterable(node);
            case WHILE_STATEMENT -> visitWhileStatement(node);
            case DO_WHILE_STATEMENT -> visitDoWhileStatement(node);
            case RETURN_STATEMENT -> visitReturnStatement(node);
            case BREAK_STATEMENT -> visitBreakStatement(node);
            case CONTINUE_STATEMENT -> visitContinueStatement(node);
            case THROW_STATEMENT -> visitThrowStatement(node);
            case TRY_STATEMENT -> visitTryStatement(node);
            case CATCH_CLAUSE -> visitCatchClause(node);
            case FINALLY_CLAUSE -> visitFinallyClause(node);
            case SWITCH_STATEMENT -> visitSwitchStatement(node);
            case SWITCH_CASE -> visitSwitchCase(node);
            case SWITCH_LABEL -> visitSwitchLabel(node);
            case SYNCHRONIZED_STATEMENT -> visitSynchronizedStatement(node);
            case LABELED_STATEMENT -> visitLabeledStatement(node);
            case DECLARATOR -> visitDeclarator(node);
            case SKIPPED -> visitSkipped(node);
            case IDENTIFIER_EXPRESSION -> visitIdentifierExpression(node);
            case LITERAL_EXPRESSION -> visitLiteralExpression(node);
            case BINARY_EXPRESSION -> visitBinaryExpression(node);
            case UNARY_EXPRESSION -> visitUnaryExpression(node);
            case ASSIGNMENT_EXPRESSION -> visitAssignmentExpression(node);
            case METHOD_CALL_EXPRESSION -> visitMethodCallExpression(node);
            case FIELD_ACCESS_EXPRESSION -> visitFieldAccessExpression(node);
            case PARENTHESIZED_EXPRESSION -> visitParenthesizedExpression(node);
            case NEW_EXPRESSION -> visitNewExpression(node);
            case CONDITIONAL_EXPRESSION -> visitConditionalExpression(node);
            case ARRAY_ACCESS_EXPRESSION -> visitArrayAccessExpression(node);
            case OPERATOR -> visitOperator(node);
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

    default void visitBlock(AstNode node) {
    }

    default void visitEmptyStatement(AstNode node) {
    }

    default void visitLocalVariableDeclaration(AstNode node) {
    }

    default void visitExpressionStatement(AstNode node) {
    }

    default void visitIfStatement(AstNode node) {
    }

    default void visitCondition(AstNode node) {
    }

    default void visitThen(AstNode node) {
    }

    default void visitElse(AstNode node) {
    }

    default void visitForStatement(AstNode node) {
    }

    default void visitInitializer(AstNode node) {
    }

    default void visitUpdate(AstNode node) {
    }

    default void visitEnhancedForStatement(AstNode node) {
    }

    default void visitVariable(AstNode node) {
    }

    default void visitIterable(AstNode node) {
    }

    default void visitWhileStatement(AstNode node) {
    }

    default void visitDoWhileStatement(AstNode node) {
    }

    default void visitReturnStatement(AstNode node) {
    }

    default void visitBreakStatement(AstNode node) {
    }

    default void visitContinueStatement(AstNode node) {
    }

    default void visitThrowStatement(AstNode node) {
    }

    default void visitTryStatement(AstNode node) {
    }

    default void visitCatchClause(AstNode node) {
    }

    default void visitFinallyClause(AstNode node) {
    }

    default void visitSwitchStatement(AstNode node) {
    }

    default void visitSwitchCase(AstNode node) {
    }

    default void visitSwitchLabel(AstNode node) {
    }

    default void visitSynchronizedStatement(AstNode node) {
    }

    default void visitLabeledStatement(AstNode node) {
    }

    default void visitDeclarator(AstNode node) {
    }

    default void visitSkipped(AstNode node) {
    }

    default void visitIdentifierExpression(AstNode node) {
    }

    default void visitLiteralExpression(AstNode node) {
    }

    default void visitBinaryExpression(AstNode node) {
    }

    default void visitUnaryExpression(AstNode node) {
    }

    default void visitAssignmentExpression(AstNode node) {
    }

    default void visitMethodCallExpression(AstNode node) {
    }

    default void visitFieldAccessExpression(AstNode node) {
    }

    default void visitParenthesizedExpression(AstNode node) {
    }

    default void visitNewExpression(AstNode node) {
    }

    default void visitConditionalExpression(AstNode node) {
    }

    default void visitArrayAccessExpression(AstNode node) {
    }

    default void visitOperator(AstNode node) {
    }
}
