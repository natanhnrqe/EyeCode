package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementExpressionAstTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private static List<AstNode> ofKind(AstNode parent, AstNodeKind kind) {
        return parent.children().stream().filter(c -> c.kind() == kind).toList();
    }

    private static AstNode onlyChild(AstNode parent, AstNodeKind kind) {
        List<AstNode> matches = ofKind(parent, kind);
        assertEquals(1, matches.size(), "expected exactly one " + kind + " child");
        return matches.get(0);
    }

    private static AstNode methodBlock(String source) {
        JavaFileModel model = new StatementExpressionAstTest().parse(source);
        AstNode clazz = model.getAstRoot().children().get(0);
        AstNode method = onlyChild(clazz, AstNodeKind.METHOD_DECLARATION);
        return onlyChild(method, AstNodeKind.BLOCK);
    }

    private static String slice(String source, AstNode node) {
        return source.substring(node.range().startOffset(), node.range().endOffset());
    }

    @Test
    void blockContainsEmptyStatements() {
        String source = """
                class A {
                    void m() {
                        ;
                        ;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        assertEquals(2, block.children().size());
        assertEquals(AstNodeKind.EMPTY_STATEMENT, block.children().get(0).kind());
        assertEquals(AstNodeKind.EMPTY_STATEMENT, block.children().get(1).kind());
        assertEquals(";", slice(source, block.children().get(0)));
    }

    @Test
    void methodBodyBlockIsChildOfMethod() {
        String source = """
                class A {
                    int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
        JavaFileModel model = parse(source);
        AstNode clazz = model.getAstRoot().children().get(0);
        AstNode method = onlyChild(clazz, AstNodeKind.METHOD_DECLARATION);
        AstNode block = onlyChild(method, AstNodeKind.BLOCK);
        assertEquals(AstNodeKind.RETURN_STATEMENT, onlyChild(block, AstNodeKind.RETURN_STATEMENT).kind());
        assertEquals(1, ofKind(block, AstNodeKind.RETURN_STATEMENT).size());
    }

    @Test
    void localVariableDeclarationShape() {
        String source = """
                class A {
                    void m() {
                        int x = 1;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode declaration = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(2, declaration.children().size());
        AstNode type = declaration.children().get(0);
        AstNode declarator = declaration.children().get(1);
        assertEquals(AstNodeKind.TYPE, type.kind());
        assertEquals(AstNodeKind.DECLARATOR, declarator.kind());
        assertEquals("int", slice(source, type));
        assertEquals("x = 1", slice(source, declarator));
        assertEquals(1, declarator.children().size());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, declarator.children().get(0).kind());
    }

    @Test
    void declaratorWithoutInitializerHasNoChildren() {
        String source = """
                class A {
                    void m() {
                        String name;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode declarator = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION)
                .children().get(1);
        assertEquals(0, declarator.children().size());
        assertEquals("name", slice(source, declarator));
    }

    @Test
    void multipleDeclaratorsShareOneType() {
        String source = """
                class A {
                    void m() {
                        int a = 1, b = 2, c;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode declaration = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        List<AstNode> declarators = ofKind(declaration, AstNodeKind.DECLARATOR);
        assertEquals(3, declarators.size());
        assertEquals("a = 1", slice(source, declarators.get(0)));
        assertEquals("b = 2", slice(source, declarators.get(1)));
        assertEquals("c", slice(source, declarators.get(2)));
        assertEquals(1, ofKind(declaration, AstNodeKind.TYPE).size());
    }

    @Test
    void varKeywordDeclarationsSupported() {
        String source = """
                class A {
                    void m() {
                        var list = new ArrayList<String>();
                    }
                }
                """;
        JavaFileModel model = parse(source);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(1, method.getLocalVariables().size());
        assertEquals("var", method.getLocalVariables().get(0).getType());
        assertEquals("list", method.getLocalVariables().get(0).getName());

        AstNode block = onlyChild(onlyChild(model.getAstRoot().children().get(0),
                AstNodeKind.METHOD_DECLARATION), AstNodeKind.BLOCK);
        AstNode declaration = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals("var", slice(source, ofKind(declaration, AstNodeKind.TYPE).get(0)));
    }

    @Test
    void ifStatementWithElseAndElseIf() {
        String source = """
                class A {
                    void m() {
                        if (x > 0) {
                            use(x);
                        } else if (x == 0) {
                            zero();
                        } else {
                            neg();
                        }
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode ifStatement = onlyChild(block, AstNodeKind.IF_STATEMENT);
        assertEquals(3, ifStatement.children().size());
        assertEquals(AstNodeKind.CONDITION, ifStatement.children().get(0).kind());
        assertEquals(AstNodeKind.THEN, ifStatement.children().get(1).kind());
        AstNode elseWrapper = ifStatement.children().get(2);
        assertEquals(AstNodeKind.ELSE, elseWrapper.kind());
        assertEquals(AstNodeKind.IF_STATEMENT, elseWrapper.children().get(0).kind());
    }

    @Test
    void ifWithoutElseHasTwoChildren() {
        String source = """
                class A {
                    void m() {
                        if (flag) return;
                    }
                }
                """;
        AstNode ifStatement = onlyChild(methodBlock(source), AstNodeKind.IF_STATEMENT);
        assertEquals(2, ifStatement.children().size());
        assertEquals(AstNodeKind.CONDITION, ifStatement.children().get(0).kind());
        assertEquals(AstNodeKind.THEN, ifStatement.children().get(1).kind());
        assertEquals(AstNodeKind.RETURN_STATEMENT, ifStatement.children().get(1).children().get(0).kind());
    }

    @Test
    void whileLoopWrapsConditionAndBody() {
        String source = """
                class A {
                    void m() {
                        while (i < 10) {
                            i++;
                        }
                    }
                }
                """;
        AstNode whileStatement = onlyChild(methodBlock(source), AstNodeKind.WHILE_STATEMENT);
        assertEquals(2, whileStatement.children().size());
        AstNode condition = whileStatement.children().get(0);
        AstNode body = whileStatement.children().get(1);
        assertEquals(AstNodeKind.CONDITION, condition.kind());
        assertEquals(AstNodeKind.THEN, body.kind());
        assertEquals(AstNodeKind.BLOCK, body.children().get(0).kind());
        assertEquals(AstNodeKind.BINARY_EXPRESSION, condition.children().get(0).kind());
    }

    @Test
    void doWhileLoopOrdersBodyThenCondition() {
        String source = """
                class A {
                    void m() {
                        do {
                            i--;
                        } while (i > 0);
                    }
                }
                """;
        AstNode doWhile = onlyChild(methodBlock(source), AstNodeKind.DO_WHILE_STATEMENT);
        assertEquals(2, doWhile.children().size());
        assertEquals(AstNodeKind.THEN, doWhile.children().get(0).kind());
        assertEquals(AstNodeKind.CONDITION, doWhile.children().get(1).kind());
        assertTrue(slice(source, doWhile).endsWith(";"));
    }

    @Test
    void classicForWrapsInitConditionUpdateBody() {
        String source = """
                class A {
                    void m() {
                        for (int i = 0; i < n; i++) {
                            use(i);
                        }
                    }
                }
                """;
        AstNode forStatement = onlyChild(methodBlock(source), AstNodeKind.FOR_STATEMENT);
        assertEquals(4, forStatement.children().size());
        AstNode init = forStatement.children().get(0);
        AstNode condition = forStatement.children().get(1);
        AstNode update = forStatement.children().get(2);
        AstNode body = forStatement.children().get(3);
        assertEquals(AstNodeKind.INITIALIZER, init.kind());
        assertEquals(AstNodeKind.CONDITION, condition.kind());
        assertEquals(AstNodeKind.UPDATE, update.kind());
        assertEquals(AstNodeKind.THEN, body.kind());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION, init.children().get(0).kind());
        assertEquals(AstNodeKind.UNARY_EXPRESSION, update.children().get(0).kind());
    }

    @Test
    void forLoopWithEmptyPartsOnlyHasBody() {
        String source = """
                class A {
                    void m() {
                        for (;;) {
                            break;
                        }
                    }
                }
                """;
        AstNode forStatement = onlyChild(methodBlock(source), AstNodeKind.FOR_STATEMENT);
        assertEquals(1, forStatement.children().size());
        assertEquals(AstNodeKind.THEN, forStatement.children().get(0).kind());
        assertEquals(0, ofKind(forStatement, AstNodeKind.CONDITION).size());
        assertEquals(0, ofKind(forStatement, AstNodeKind.INITIALIZER).size());
        assertEquals(0, ofKind(forStatement, AstNodeKind.UPDATE).size());
    }

    @Test
    void enhancedForWrapsVariableIterableBody() {
        String source = """
                class A {
                    void m() {
                        for (String s : names) {
                            out(s);
                        }
                    }
                }
                """;
        AstNode forStatement = onlyChild(methodBlock(source), AstNodeKind.ENHANCED_FOR_STATEMENT);
        assertEquals(3, forStatement.children().size());
        AstNode variable = forStatement.children().get(0);
        AstNode iterable = forStatement.children().get(1);
        AstNode body = forStatement.children().get(2);
        assertEquals(AstNodeKind.VARIABLE, variable.kind());
        assertEquals(AstNodeKind.ITERABLE, iterable.kind());
        assertEquals(AstNodeKind.THEN, body.kind());
        assertEquals(AstNodeKind.TYPE, variable.children().get(0).kind());
        assertEquals(AstNodeKind.IDENTIFIER_EXPRESSION, iterable.children().get(0).kind());
    }

    @Test
    void returnStatementWithAndWithoutValue() {
        String source = """
                class A {
                    int m() {
                        if (flag) {
                            return;
                        }
                        return 42;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> returns = ofKind(block, AstNodeKind.RETURN_STATEMENT);
        assertEquals(1, returns.size());
        AstNode outerReturn = returns.get(0);
        assertEquals(1, outerReturn.children().size());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, outerReturn.children().get(0).kind());

        AstNode innerIf = onlyChild(block, AstNodeKind.IF_STATEMENT);
        AstNode innerReturn = innerIf.children().get(1).children().get(0).children().get(0);
        assertEquals(0, innerReturn.children().size());
    }

    @Test
    void breakAndContinueCarryOptionalLabels() {
        String source = """
                class A {
                    void m() {
                        outer: for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 10; j++) {
                                if (j > 5) break outer;
                                if (j == 3) continue;
                            }
                        }
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> all = AstNodes.descendants(modelRoot(block));
        List<AstNode> breaks = all.stream().filter(n -> n.kind() == AstNodeKind.BREAK_STATEMENT).toList();
        assertEquals(1, breaks.size());
        assertEquals(1, breaks.get(0).children().size());
        assertEquals(AstNodeKind.IDENTIFIER_EXPRESSION, breaks.get(0).children().get(0).kind());

        List<AstNode> continues = all.stream().filter(n -> n.kind() == AstNodeKind.CONTINUE_STATEMENT).toList();
        assertEquals(1, continues.size());
        assertEquals(0, continues.get(0).children().size());
    }

    private static AstNode modelRoot(AstNode node) {
        AstNode current = node;
        while (current.parent() != null) {
            current = current.parent();
        }
        return current;
    }

    @Test
    void throwStatementWrapsExpression() {
        String source = """
                class A {
                    void m() {
                        throw new IllegalStateException("bad");
                    }
                }
                """;
        AstNode throwStatement = onlyChild(methodBlock(source), AstNodeKind.THROW_STATEMENT);
        assertEquals(1, throwStatement.children().size());
        assertEquals(AstNodeKind.NEW_EXPRESSION, throwStatement.children().get(0).kind());
    }

    @Test
    void tryCatchFinallyStructure() {
        String source = """
                class A {
                    void m() {
                        try {
                            open();
                        } catch (IOException e) {
                            log(e);
                        } finally {
                            close();
                        }
                    }
                }
                """;
        AstNode tryStatement = onlyChild(methodBlock(source), AstNodeKind.TRY_STATEMENT);
        assertEquals(3, tryStatement.children().size());
        assertEquals(AstNodeKind.BLOCK, tryStatement.children().get(0).kind());
        AstNode catchClause = tryStatement.children().get(1);
        assertEquals(AstNodeKind.CATCH_CLAUSE, catchClause.kind());
        assertEquals(AstNodeKind.BLOCK, catchClause.children().get(0).kind());
        AstNode finallyClause = tryStatement.children().get(2);
        assertEquals(AstNodeKind.FINALLY_CLAUSE, finallyClause.kind());
        assertEquals(AstNodeKind.BLOCK, finallyClause.children().get(0).kind());
    }

    @Test
    void tryWithResourcesParses() {
        String source = """
                class A {
                    void m() {
                        try (InputStream in = open()) {
                            read(in);
                        } catch (Exception e) {
                            log(e);
                        }
                    }
                }
                """;
        AstNode tryStatement = onlyChild(methodBlock(source), AstNodeKind.TRY_STATEMENT);
        assertEquals(2, tryStatement.children().size());
        assertEquals(AstNodeKind.BLOCK, tryStatement.children().get(0).kind());
        assertEquals(AstNodeKind.CATCH_CLAUSE, tryStatement.children().get(1).kind());
    }

    @Test
    void switchWithColonCasesAndDefault() {
        String source = """
                class A {
                    void m() {
                        switch (value) {
                            case 1:
                                one();
                                break;
                            case 2:
                                two();
                                break;
                            default:
                                other();
                        }
                    }
                }
                """;
        AstNode switchStatement = onlyChild(methodBlock(source), AstNodeKind.SWITCH_STATEMENT);
        assertEquals(4, switchStatement.children().size());
        assertEquals(AstNodeKind.CONDITION, switchStatement.children().get(0).kind());
        for (int i = 1; i < 4; i++) {
            assertEquals(AstNodeKind.SWITCH_CASE, switchStatement.children().get(i).kind());
        }
        AstNode firstCase = switchStatement.children().get(1);
        assertEquals(AstNodeKind.SWITCH_LABEL, firstCase.children().get(0).kind());
        assertEquals(AstNodeKind.EXPRESSION_STATEMENT, firstCase.children().get(1).kind());
        assertEquals(AstNodeKind.BREAK_STATEMENT, firstCase.children().get(2).kind());
    }

    @Test
    void switchWithMultipleLabelsAndArrow() {
        String source = """
                class A {
                    void m() {
                        switch (value) {
                            case 1, 2 -> handleLow();
                            case 3 -> handleHigh();
                        }
                    }
                }
                """;
        AstNode switchStatement = onlyChild(methodBlock(source), AstNodeKind.SWITCH_STATEMENT);
        assertEquals(3, switchStatement.children().size());
        AstNode firstCase = switchStatement.children().get(1);
        AstNode label = firstCase.children().get(0);
        assertEquals(AstNodeKind.SWITCH_LABEL, label.kind());
        assertEquals(2, label.children().size());
        assertEquals(AstNodeKind.EXPRESSION_STATEMENT, firstCase.children().get(1).kind());
    }

    @Test
    void synchronizedStatementWrapsLockAndBody() {
        String source = """
                class A {
                    void m() {
                        synchronized (lock) {
                            counter++;
                        }
                    }
                }
                """;
        AstNode sync = onlyChild(methodBlock(source), AstNodeKind.SYNCHRONIZED_STATEMENT);
        assertEquals(2, sync.children().size());
        assertEquals(AstNodeKind.CONDITION, sync.children().get(0).kind());
        assertEquals(AstNodeKind.THEN, sync.children().get(1).kind());
    }

    @Test
    void labeledStatementWrapsLabelAndTarget() {
        String source = """
                class A {
                    void m() {
                        outer: while (true) {
                            break outer;
                        }
                    }
                }
                """;
        AstNode labeled = onlyChild(methodBlock(source), AstNodeKind.LABELED_STATEMENT);
        assertEquals(2, labeled.children().size());
        assertEquals(AstNodeKind.IDENTIFIER_EXPRESSION, labeled.children().get(0).kind());
        assertEquals("outer", slice(source, labeled.children().get(0)));
        assertEquals(AstNodeKind.WHILE_STATEMENT, labeled.children().get(1).kind());
    }

    @Test
    void unknownConstructsBecomeSkippedNodes() {
        String source = """
                class A {
                    void m() {
                        assert x == 1;
                        Runnable r = () -> System.out.println("x");
                        Object o = (int) value;
                        yield 5;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        assertEquals(4, block.children().size());
        for (AstNode child : block.children()) {
            assertEquals(AstNodeKind.SKIPPED, child.kind(), child + " should be SKIPPED");
        }
    }

    @Test
    void expressionStatementWrapsExpressionAndSemicolon() {
        String source = """
                class A {
                    void m() {
                        process(item, 5);
                    }
                }
                """;
        AstNode statement = onlyChild(methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT);
        assertEquals(1, statement.children().size());
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, statement.children().get(0).kind());
        assertTrue(slice(source, statement).endsWith(";"));
    }

    @Test
    void binaryPrecedenceGroupsMultiplicativeFirst() {
        String source = """
                class A {
                    void m() {
                        int r = a + b * c;
                    }
                }
                """;
        AstNode declaration = onlyChild(methodBlock(source), AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode declarator = onlyChild(declaration, AstNodeKind.DECLARATOR);
        AstNode initializer = declarator.children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, initializer.kind());
        assertEquals(AstNodeKind.IDENTIFIER_EXPRESSION, initializer.children().get(0).kind());
        AstNode right = initializer.children().get(2);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, right.kind());
        assertEquals("b * c", slice(source, right));
        assertEquals("a + b * c", slice(source, initializer));
    }

    @Test
    void binaryLeftAssociative() {
        String source = """
                class A {
                    void m() {
                        int r = a - b - c;
                    }
                }
                """;
        AstNode initializer = onlyChild(onlyChild(
                onlyChild(methodBlock(source), AstNodeKind.LOCAL_VARIABLE_DECLARATION),
                AstNodeKind.DECLARATOR), AstNodeKind.BINARY_EXPRESSION);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, initializer.children().get(0).kind());
        assertEquals("a - b", slice(source, initializer.children().get(0)));
    }

    @Test
    void parenthesizedExpressionOverridesPrecedence() {
        String source = """
                class A {
                    void m() {
                        int r = (a + b) * c;
                    }
                }
                """;
        AstNode initializer = onlyChild(onlyChild(
                onlyChild(methodBlock(source), AstNodeKind.LOCAL_VARIABLE_DECLARATION),
                AstNodeKind.DECLARATOR), AstNodeKind.BINARY_EXPRESSION);
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, initializer.children().get(0).kind());
        AstNode inner = initializer.children().get(0).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, inner.kind());
        assertEquals("(a + b)", slice(source, initializer.children().get(0)));
    }

    @Test
    void methodCallChainBuildsNestedCalls() {
        String source = """
                class A {
                    void m() {
                        a.b().c(1, "x");
                    }
                }
                """;
        AstNode statement = onlyChild(methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT);
        AstNode call = statement.children().get(0);
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, call.kind());
        assertEquals(3, call.children().size());
        AstNode fieldAccess = call.children().get(0);
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, fieldAccess.kind());
        AstNode innerCall = fieldAccess.children().get(0);
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, innerCall.kind());
        assertEquals(1, innerCall.children().size());
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, innerCall.children().get(0).kind());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, call.children().get(1).kind());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, call.children().get(2).kind());
    }

    @Test
    void genericMethodCallViaFieldAccess() {
        String source = """
                class A {
                    void m() {
                        list.<String>of(x);
                    }
                }
                """;
        AstNode statement = onlyChild(methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT);
        AstNode call = statement.children().get(0);
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, call.kind());
        AstNode fieldAccess = call.children().get(0);
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, fieldAccess.kind());
        assertEquals("list.<String>of", slice(source, fieldAccess));
    }

    @Test
    void arrayAccessAndAssignment() {
        String source = """
                class A {
                    void m() {
                        arr[0] = 5;
                    }
                }
                """;
        AstNode statement = onlyChild(methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT);
        AstNode assignment = statement.children().get(0);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, assignment.kind());
        assertEquals(3, assignment.children().size());
        assertEquals(AstNodeKind.ARRAY_ACCESS_EXPRESSION, assignment.children().get(0).kind());
        assertEquals(AstNodeKind.OPERATOR, assignment.children().get(1).kind());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, assignment.children().get(2).kind());
    }

    @Test
    void assignmentIsRightAssociative() {
        String source = """
                class A {
                    void m() {
                        x = y = 5;
                    }
                }
                """;
        AstNode statement = onlyChild(methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT);
        AstNode outer = statement.children().get(0);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, outer.kind());
        AstNode inner = outer.children().get(2);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, inner.kind());
    }

    @Test
    void compoundAssignmentKeepsOperator() {
        String source = """
                class A {
                    void m() {
                        x += 2;
                    }
                }
                """;
        AstNode assignment = onlyChild(onlyChild(
                methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT), AstNodeKind.ASSIGNMENT_EXPRESSION);
        assertEquals("+=", slice(source, assignment.children().get(1)));
    }

    @Test
    void fieldAccessAssignmentIsLvalue() {
        String source = """
                class A {
                    void m() {
                        config.timeout = 100;
                    }
                }
                """;
        AstNode assignment = onlyChild(onlyChild(
                methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT), AstNodeKind.ASSIGNMENT_EXPRESSION);
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, assignment.children().get(0).kind());
    }

    @Test
    void newExpressionForms() {
        String source = """
                class A {
                    void m() {
                        Foo f1 = new Foo();
                        Foo f2 = new Foo(1, 2);
                        int[] a1 = new int[5];
                        int[] a2 = new int[] { 1, 2 };
                        Foo f3 = new Foo() {
                            @Override
                            void run() {}
                        };
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(5, declarations.size());
        for (AstNode declaration : declarations) {
            AstNode initializer = onlyChild(declaration, AstNodeKind.DECLARATOR).children().get(0);
            assertEquals(AstNodeKind.NEW_EXPRESSION, initializer.kind(), slice(source, initializer));
        }
        AstNode anonymous = declarations.get(4).children().get(1).children().get(0);
        assertEquals(AstNodeKind.NEW_EXPRESSION, anonymous.kind());
        assertTrue(slice(source, anonymous).startsWith("new Foo() {"));
        assertTrue(slice(source, anonymous).endsWith("}"));
    }

    @Test
    void ternaryConditionalExpression() {
        String source = """
                class A {
                    void m() {
                        int r = flag ? a : b;
                    }
                }
                """;
        AstNode initializer = onlyChild(onlyChild(
                onlyChild(methodBlock(source), AstNodeKind.LOCAL_VARIABLE_DECLARATION),
                AstNodeKind.DECLARATOR), AstNodeKind.CONDITIONAL_EXPRESSION);
        assertEquals(3, initializer.children().size());
        assertEquals("flag ? a : b", slice(source, initializer));
    }

    @Test
    void ternaryNestsRightAssociatively() {
        String source = """
                class A {
                    void m() {
                        int r = a ? b : c ? d : e;
                    }
                }
                """;
        AstNode conditional = onlyChild(onlyChild(
                onlyChild(methodBlock(source), AstNodeKind.LOCAL_VARIABLE_DECLARATION),
                AstNodeKind.DECLARATOR), AstNodeKind.CONDITIONAL_EXPRESSION);
        AstNode elseBranch = conditional.children().get(2);
        assertEquals(AstNodeKind.CONDITIONAL_EXPRESSION, elseBranch.kind());
    }

    @Test
    void unaryPrefixAndPostfix() {
        String source = """
                class A {
                    void m() {
                        int r = -x;
                        boolean b = !flag;
                        i++;
                        ++i;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode first = block.children().get(0).children().get(1).children().get(0);
        assertEquals(AstNodeKind.UNARY_EXPRESSION, first.kind());
        assertEquals(2, first.children().size());
        assertEquals(AstNodeKind.OPERATOR, first.children().get(0).kind());
        assertEquals(AstNodeKind.IDENTIFIER_EXPRESSION, first.children().get(1).kind());

        List<AstNode> all = AstNodes.descendants(modelRoot(block));
        List<AstNode> unary = all.stream().filter(n -> n.kind() == AstNodeKind.UNARY_EXPRESSION).toList();
        assertEquals(4, unary.size());
        assertEquals(AstNodeKind.OPERATOR, unary.get(2).children().get(1).kind());
        assertEquals(AstNodeKind.OPERATOR, unary.get(3).children().get(0).kind());
    }

    @Test
    void instanceofIsBinaryOperator() {
        String source = """
                class A {
                    void m() {
                        boolean b = obj instanceof Foo;
                    }
                }
                """;
        AstNode initializer = onlyChild(onlyChild(
                onlyChild(methodBlock(source), AstNodeKind.LOCAL_VARIABLE_DECLARATION),
                AstNodeKind.DECLARATOR), AstNodeKind.BINARY_EXPRESSION);
        assertEquals("instanceof", slice(source, initializer.children().get(1)));
    }

    @Test
    void literalsAndKeywords() {
        String source = """
                class A {
                    void m() {
                        boolean t = true;
                        Object n = null;
                        String s = "hi";
                        char c = 'x';
                        double d = 3.14;
                        this.value = t;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        for (AstNode declaration : ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION)) {
            AstNode initializer = onlyChild(declaration, AstNodeKind.DECLARATOR).children().get(0);
            assertEquals(AstNodeKind.LITERAL_EXPRESSION, initializer.kind(), slice(source, initializer));
        }
        AstNode assignment = onlyChild(block, AstNodeKind.EXPRESSION_STATEMENT).children().get(0);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, assignment.kind());
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, assignment.children().get(0).kind());
        assertEquals("this", slice(source, assignment.children().get(0).children().get(0)));
    }

    @Test
    void localVariablesRegisteredAcrossStatements() {
        String source = """
                class A {
                    void m() {
                        int a = 1;
                        String b = "x";
                        double c;
                        for (String s : list) {
                            use(s);
                        }
                    }
                }
                """;
        JavaFileModel model = parse(source);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(4, method.getLocalVariables().size());
        assertEquals("a", method.getLocalVariables().get(0).getName());
        assertEquals("int", method.getLocalVariables().get(0).getType());
        assertEquals("b", method.getLocalVariables().get(1).getName());
        assertEquals("c", method.getLocalVariables().get(2).getName());
        assertEquals("s", method.getLocalVariables().get(3).getName());
        assertEquals("m", method.getLocalVariables().get(3).getOwnerMethod());
    }

    @Test
    void statementRangesSliceSourceExactly() {
        String source = """
                class A {
                    void m() {
                        if (x < 0) {
                            x = -x;
                        }
                        return x;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode ifStatement = onlyChild(block, AstNodeKind.IF_STATEMENT);
        assertEquals("if (x < 0) {\n            x = -x;\n        }",
                slice(source, ifStatement));
        AstNode returnStatement = onlyChild(block, AstNodeKind.RETURN_STATEMENT);
        assertEquals("return x;", slice(source, returnStatement));
    }

    @Test
    void constructorBodyParsesStatements() {
        String source = """
                class A {
                    A(String name) {
                        this.name = name;
                    }
                }
                """;
        JavaFileModel model = parse(source);
        AstNode clazz = model.getAstRoot().children().get(0);
        AstNode constructor = onlyChild(clazz, AstNodeKind.CONSTRUCTOR_DECLARATION);
        AstNode block = onlyChild(constructor, AstNodeKind.BLOCK);
        AstNode assignment = onlyChild(block, AstNodeKind.EXPRESSION_STATEMENT)
                .children().get(0);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, assignment.kind());
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, assignment.children().get(0).kind());
        assertEquals(AstNodeKind.IDENTIFIER_EXPRESSION, assignment.children().get(0).children().get(0).kind());
    }

    @Test
    void superAndThisConstructorCallsParse() {
        String source = """
                class B extends A {
                    B() {
                        super(1);
                    }
                    B(int x) {
                        this();
                    }
                }
                """;
        JavaFileModel model = parse(source);
        AstNode clazz = model.getAstRoot().children().get(0);
        List<AstNode> constructors = ofKind(clazz, AstNodeKind.CONSTRUCTOR_DECLARATION);
        assertEquals(2, constructors.size());
        for (AstNode constructor : constructors) {
            AstNode block = onlyChild(constructor, AstNodeKind.BLOCK);
            AstNode call = onlyChild(block, AstNodeKind.EXPRESSION_STATEMENT)
                    .children().get(0);
            assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, call.kind());
        }
    }

    @Test
    void brokenBodyDoesNotPropagateToWholeFile() {
        String source = """
                class A {
                    void m() {
                        Runnable r = () -> System.out.println("x");
                        int a = 1;
                        fine();
                    }
                }
                """;
        JavaFileModel model = parse(source);
        AstNode block = methodBlock(source);
        assertEquals(3, block.children().size());
        assertEquals(AstNodeKind.SKIPPED, block.children().get(0).kind());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION, block.children().get(1).kind());
        assertEquals(AstNodeKind.EXPRESSION_STATEMENT, block.children().get(2).kind());
        assertEquals(1, model.getTypes().get(0).getMethods().get(0).getLocalVariables().size());
        assertEquals("a", model.getTypes().get(0).getMethods().get(0).getLocalVariables().get(0).getName());
    }

    @Test
    void heavilyBrokenFileStillParsesWithoutThrowing() {
        String source = """
                class A {
                    void m() {
                        if (x) {
                            broken(
                    }
                    void ok() {
                        fine();
                    }
                }
                """;
        JavaFileModel model = parse(source);
        assertTrue(model.getTypes().get(0).getMethods().size() >= 1);
    }
}
