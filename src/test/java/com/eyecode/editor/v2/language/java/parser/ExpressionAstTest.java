package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionAstTest {

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
        JavaFileModel model = new ExpressionAstTest().parse(source);
        AstNode clazz = model.getAstRoot().children().get(0);
        AstNode method = onlyChild(clazz, AstNodeKind.METHOD_DECLARATION);
        return onlyChild(method, AstNodeKind.BLOCK);
    }

    private static String slice(String source, AstNode node) {
        return source.substring(node.range().startOffset(), node.range().endOffset());
    }

    private static AstNode initializerOfFirstDeclarator(String source) {
        AstNode block = methodBlock(source);
        AstNode declaration = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        return onlyChild(declaration, AstNodeKind.DECLARATOR).children().get(0);
    }

    private static List<AstNode> allDescendants(AstNode node) {
        return AstNodes.descendants(node);
    }

    @Test
    void literalExpressionsCarryTheirToken() {
        String source = """
                class A {
                    void m() {
                        int a = 42;
                        double b = 3.14;
                        String c = "hi";
                        char d = 'x';
                        boolean e = true;
                        Object f = null;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(6, declarations.size());
        String[] expected = {"42", "3.14", "\"hi\"", "'x'", "true", "null"};
        for (int i = 0; i < declarations.size(); i++) {
            AstNode literal = onlyChild(declarations.get(i), AstNodeKind.DECLARATOR).children().get(0);
            assertEquals(AstNodeKind.LITERAL_EXPRESSION, literal.kind());
            assertEquals(expected[i], slice(source, literal));
            assertEquals(expected[i], literal.token().text());
            assertEquals(slice(source, literal), literal.token().text());
        }
    }

    @Test
    void nameExpressionsCarryTheirToken() {
        String source = """
                class A {
                    void m() {
                        int r = value;
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.NAME_EXPRESSION, initializer.kind());
        assertEquals("value", initializer.token().text());
        assertEquals("value", slice(source, initializer));
        assertEquals(0, initializer.children().size());
    }

    @Test
    void thisAndSuperExpressions() {
        String source = """
                class A {
                    void m() {
                        Object self = this;
                        this.value = 1;
                        super.run();
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode thisDecl = onlyChild(ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION).get(0),
                AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.THIS_EXPRESSION, thisDecl.kind());
        assertEquals("this", slice(source, thisDecl));

        AstNode assignment = ofKind(block, AstNodeKind.EXPRESSION_STATEMENT).get(0)
                .children().get(0);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, assignment.kind());
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, assignment.children().get(0).kind());
        assertEquals(AstNodeKind.THIS_EXPRESSION, assignment.children().get(0).children().get(0).kind());

        AstNode call = ofKind(block, AstNodeKind.EXPRESSION_STATEMENT).get(1)
                .children().get(0);
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, call.kind());
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, call.children().get(0).kind());
        assertEquals(AstNodeKind.SUPER_EXPRESSION, call.children().get(0).children().get(0).kind());
    }

    @Test
    void unaryOperatorsPrefixAndPostfix() {
        String source = """
                class A {
                    void m() {
                        int r = -x;
                        int s = - -y;
                        boolean b = !!flag;
                        int t = ++i;
                        i--;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode neg = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.UNARY_EXPRESSION, neg.kind());
        assertEquals("-", slice(source, neg.children().get(0)));

        AstNode doubleNeg = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.UNARY_EXPRESSION, doubleNeg.kind());
        assertEquals(AstNodeKind.UNARY_EXPRESSION, doubleNeg.children().get(1).kind());

        AstNode doubleNot = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.UNARY_EXPRESSION, doubleNot.kind());
        assertEquals("!!flag", slice(source, doubleNot));

        AstNode prefix = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.UNARY_EXPRESSION, prefix.kind());
        assertEquals(AstNodeKind.OPERATOR, prefix.children().get(0).kind());
        assertEquals("++", prefix.children().get(0).token().text());

        AstNode postfix = onlyChild(block, AstNodeKind.EXPRESSION_STATEMENT).children().get(0);
        assertEquals(AstNodeKind.UNARY_EXPRESSION, postfix.kind());
        assertEquals(AstNodeKind.OPERATOR, postfix.children().get(1).kind());
        assertEquals("--", postfix.children().get(1).token().text());
    }

    @Test
    void binaryPrecedenceLadder() {
        String source = """
                class A {
                    void m() {
                        boolean r = a || b && c | d ^ e & f == g < h << i + j * k;
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, initializer.kind());
        assertEquals("||", initializer.children().get(1).token().text());
        AstNode right = initializer.children().get(2);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, right.kind());
        assertEquals("&&", right.children().get(1).token().text());
        assertEquals("a || b && c | d ^ e & f == g < h << i + j * k", slice(source, initializer));
    }

    @Test
    void binaryOperatorsCarryOperatorTokens() {
        String source = """
                class A {
                    void m() {
                        int r = a + b;
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        AstNode operator = initializer.children().get(1);
        assertEquals(AstNodeKind.OPERATOR, operator.kind());
        assertEquals("+", operator.token().text());
        assertEquals(2, initializer.children().size() - 1);
    }

    @Test
    void parenthesizedExpressionOverridesPrecedence() {
        String source = """
                class A {
                    void m() {
                        int r = (a + b) * (c - d);
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, initializer.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, initializer.children().get(0).kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, initializer.children().get(2).kind());
        assertEquals(AstNodeKind.BINARY_EXPRESSION,
                initializer.children().get(0).children().get(0).kind());
    }

    @Test
    void assignmentIsRightAssociativeAndOperatorsCarryText() {
        String source = """
                class A {
                    void m() {
                        x = y = z += 2;
                    }
                }
                """;
        AstNode statement = onlyChild(methodBlock(source), AstNodeKind.EXPRESSION_STATEMENT);
        AstNode outer = statement.children().get(0);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, outer.kind());
        assertEquals("=", outer.children().get(1).token().text());
        AstNode inner = outer.children().get(2);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, inner.kind());
        assertEquals("=", inner.children().get(1).token().text());
        AstNode innermost = inner.children().get(2);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, innermost.kind());
        assertEquals("+=", innermost.children().get(1).token().text());
    }

    @Test
    void ternaryIsRightAssociative() {
        String source = """
                class A {
                    void m() {
                        int r = a ? b : c ? d : e;
                    }
                }
                """;
        AstNode ternary = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.TERNARY_EXPRESSION, ternary.kind());
        assertEquals(AstNodeKind.TERNARY_EXPRESSION, ternary.children().get(2).kind());
        assertEquals("a ? b : c ? d : e", slice(source, ternary));
    }

    @Test
    void fieldAndArrayAccessChains() {
        String source = """
                class A {
                    void m() {
                        Object v = a.b.c.d;
                        int w = arr[i][j + 1];
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode chain = onlyChild(ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION).get(0),
                AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, chain.kind());
        assertEquals("a.b.c.d", slice(source, chain));

        AstNode access = onlyChild(ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION).get(1),
                AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.ARRAY_ACCESS_EXPRESSION, access.kind());
        assertEquals(AstNodeKind.ARRAY_ACCESS_EXPRESSION, access.children().get(0).kind());
        assertEquals("arr[i][j + 1]", slice(source, access));
    }

    @Test
    void methodCallArgumentsAndChains() {
        String source = """
                class A {
                    void m() {
                        int r = a.b(1, x).c(2).d(3, 4);
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, initializer.kind());
        assertEquals("a.b(1, x).c(2).d(3, 4)", slice(source, initializer));
        AstNode target = initializer.children().get(0);
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, target.kind());
        assertEquals(3, initializer.children().size());
    }

    @Test
    void objectCreationWithArgumentsAnonymousAndGenerics() {
        String source = """
                class A {
                    void m() {
                        Foo f1 = new Foo();
                        Foo f2 = new Foo(1, 2);
                        List<String> l = new ArrayList<String>();
                        Foo f3 = new Foo() {
                            void run() {}
                        };
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(4, declarations.size());

        AstNode plain = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.OBJECT_CREATION_EXPRESSION, plain.kind());
        assertEquals(AstNodeKind.TYPE, plain.children().get(0).kind());
        assertEquals("Foo", slice(source, plain.children().get(0)));
        assertEquals(1, plain.children().size());

        AstNode withArgs = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(3, withArgs.children().size());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, withArgs.children().get(1).kind());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, withArgs.children().get(2).kind());

        AstNode generic = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals("ArrayList<String>", slice(source, generic.children().get(0)));

        AstNode anonymous = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertTrue(slice(source, anonymous).startsWith("new Foo() {"));
        assertTrue(slice(source, anonymous).endsWith("}"));
    }

    @Test
    void arrayCreationWithDimsAndInitializer() {
        String source = """
                class A {
                    void m() {
                        int[] a1 = new int[5];
                        int[][] a2 = new int[2][3];
                        int[] a3 = new int[] { 1, 2, 3 };
                        int[][][] a4 = new int[][][] { { 1 } };
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(4, declarations.size());

        AstNode dims1 = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.ARRAY_CREATION_EXPRESSION, dims1.kind());
        assertEquals(2, dims1.children().size());
        assertEquals(AstNodeKind.TYPE, dims1.children().get(0).kind());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, dims1.children().get(1).kind());
        assertEquals("5", slice(source, dims1.children().get(1)));

        AstNode dims2 = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(3, dims2.children().size());

        AstNode init = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.ARRAY_CREATION_EXPRESSION, init.kind());
        assertEquals(2, init.children().size());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, init.children().get(1).kind());
        assertEquals("{ 1, 2, 3 }", slice(source, init.children().get(1)));

        AstNode emptyDims = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(2, emptyDims.children().size());
        assertEquals(AstNodeKind.TYPE, emptyDims.children().get(0).kind());
    }

    @Test
    void castPrimitiveTypeAlwaysCast() {
        String source = """
                class A {
                    void m() {
                        int r1 = (int) x;
                        int r2 = (int) -x;
                        long r3 = (long) (x + 1);
                        double r4 = (double) (a) + b;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(4, declarations.size());

        AstNode cast1 = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast1.kind());
        assertEquals(AstNodeKind.TYPE, cast1.children().get(0).kind());
        assertEquals("int", slice(source, cast1.children().get(0)));
        assertEquals(AstNodeKind.NAME_EXPRESSION, cast1.children().get(1).kind());
        assertEquals("(int) x", slice(source, cast1));

        AstNode cast2 = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast2.kind());
        assertEquals(AstNodeKind.UNARY_EXPRESSION, cast2.children().get(1).kind());
        assertEquals("(int) -x", slice(source, cast2));

        AstNode cast3 = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast3.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, cast3.children().get(1).kind());

        AstNode cast4 = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, cast4.kind());
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast4.children().get(0).kind());
    }

    @Test
    void castReferenceTypeWhenOperandCanStart() {
        String source = """
                class A {
                    void m() {
                        Object o1 = (Foo) bar;
                        Object o2 = (Object) new Object();
                        Object o3 = (Foo) (bar);
                        Object o4 = (Foo) !flag;
                        Object o5 = (Foo) -x + y;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(5, declarations.size());

        AstNode cast1 = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast1.kind());
        assertEquals("(Foo) bar", slice(source, cast1));

        AstNode cast2 = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast2.kind());
        assertEquals(AstNodeKind.OBJECT_CREATION_EXPRESSION, cast2.children().get(1).kind());

        AstNode cast3 = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast3.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, cast3.children().get(1).kind());

        AstNode cast4 = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast4.kind());
        assertEquals(AstNodeKind.UNARY_EXPRESSION, cast4.children().get(1).kind());

        AstNode binary = onlyChild(declarations.get(4), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, binary.kind());
        assertEquals(AstNodeKind.BINARY_EXPRESSION, binary.children().get(0).kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION,
                binary.children().get(0).children().get(0).kind());
        assertEquals("(Foo) -x + y", slice(source, binary));
    }

    @Test
    void castDisambiguationKeepsParenthesizedForms() {
        String source = """
                class A {
                    void m() {
                        int r1 = (a) - b;
                        int r2 = (a) + b;
                        boolean r3 = (a) instanceof String;
                        int r4 = (a) == b;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(4, declarations.size());

        AstNode minus = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, minus.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, minus.children().get(0).kind());
        assertEquals("(a) - b", slice(source, minus));

        AstNode plus = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, plus.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, plus.children().get(0).kind());

        AstNode instanceofExpr = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.INSTANCEOF_EXPRESSION, instanceofExpr.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, instanceofExpr.children().get(0).kind());

        AstNode equality = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, equality.kind());
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, equality.children().get(0).kind());
    }

    @Test
    void castWithArrayTypes() {
        String source = """
                class A {
                    void m() {
                        Object o1 = (int[]) x;
                        Object o2 = (String[][]) y;
                        Object o3 = (Foo[]) z;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode cast1 = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CAST_EXPRESSION, cast1.kind());
        assertEquals("int[]", slice(source, cast1.children().get(0)));
        AstNode cast2 = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals("String[][]", slice(source, cast2.children().get(0)));
        AstNode cast3 = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals("Foo[]", slice(source, cast3.children().get(0)));
    }

    @Test
    void instanceofParsesTypeChildrenAndChains() {
        String source = """
                class A {
                    void m() {
                        boolean r1 = x instanceof Foo;
                        boolean r2 = x instanceof Foo[];
                        boolean r3 = x instanceof List<String>;
                        boolean r4 = x instanceof Foo && y instanceof Bar;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);

        AstNode simple = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.INSTANCEOF_EXPRESSION, simple.kind());
        assertEquals(3, simple.children().size());
        assertEquals("x", slice(source, simple.children().get(0)));
        assertEquals("instanceof", simple.children().get(1).token().text());
        assertEquals(AstNodeKind.TYPE, simple.children().get(2).kind());
        assertEquals("Foo", slice(source, simple.children().get(2)));

        AstNode arrayType = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals("Foo[]", slice(source, arrayType.children().get(2)));

        AstNode generic = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals("List<String>", slice(source, generic.children().get(2)));

        AstNode chained = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.BINARY_EXPRESSION, chained.kind());
        assertEquals("&&", chained.children().get(1).token().text());
        assertEquals(AstNodeKind.INSTANCEOF_EXPRESSION, chained.children().get(0).kind());
        assertEquals(AstNodeKind.INSTANCEOF_EXPRESSION, chained.children().get(2).kind());
    }

    @Test
    void lambdaEmptyParensWithExpressionBody() {
        String source = """
                class A {
                    void m() {
                        Supplier<Integer> s = () -> 42;
                    }
                }
                """;
        AstNode lambda = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, lambda.kind());
        assertEquals(2, lambda.children().size());
        assertEquals(AstNodeKind.OPERATOR, lambda.children().get(0).kind());
        assertEquals("->", lambda.children().get(0).token().text());
        assertEquals(AstNodeKind.LITERAL_EXPRESSION, lambda.children().get(1).kind());
        assertEquals("() -> 42", slice(source, lambda));
    }

    @Test
    void lambdaParenParamsAreUntypedParameters() {
        String source = """
                class A {
                    void m() {
                        BiFunction<Integer, Integer, Integer> f = (a, b) -> a + b;
                    }
                }
                """;
        AstNode lambda = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, lambda.kind());
        assertEquals(4, lambda.children().size());
        AstNode firstParam = lambda.children().get(0);
        assertEquals(AstNodeKind.PARAMETER, firstParam.kind());
        assertEquals("a", firstParam.token().text());
        assertEquals("a", slice(source, firstParam));
        assertEquals(0, firstParam.children().size());
        AstNode secondParam = lambda.children().get(1);
        assertEquals(AstNodeKind.PARAMETER, secondParam.kind());
        assertEquals("b", secondParam.token().text());
        assertEquals(AstNodeKind.OPERATOR, lambda.children().get(2).kind());
        assertEquals(AstNodeKind.BINARY_EXPRESSION, lambda.children().get(3).kind());
        assertEquals("(a, b) -> a + b", slice(source, lambda));
    }

    @Test
    void lambdaSingleParamWithoutParens() {
        String source = """
                class A {
                    void m() {
                        Function<Integer, Integer> f = x -> x * 2;
                    }
                }
                """;
        AstNode lambda = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, lambda.kind());
        assertEquals(3, lambda.children().size());
        assertEquals(AstNodeKind.NAME_EXPRESSION, lambda.children().get(0).kind());
        assertEquals("x", lambda.children().get(0).token().text());
        assertEquals(AstNodeKind.OPERATOR, lambda.children().get(1).kind());
        assertEquals(AstNodeKind.BINARY_EXPRESSION, lambda.children().get(2).kind());
        assertEquals("x -> x * 2", slice(source, lambda));
    }

    @Test
    void lambdaBlockBodyAndNesting() {
        String source = """
                class A {
                    void m() {
                        Runnable r = () -> {
                            int local = 1;
                            use(local);
                        };
                        int sum = apply((a, b) -> a + b);
                        Function<Integer, Integer> nested = x -> y -> x + y;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(3, declarations.size());

        AstNode blockLambda = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, blockLambda.kind());
        assertEquals(AstNodeKind.BLOCK, blockLambda.children().get(1).kind());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION,
                blockLambda.children().get(1).children().get(0).kind());

        AstNode call = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.METHOD_CALL_EXPRESSION, call.kind());
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, call.children().get(1).kind());

        AstNode nested = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, nested.kind());
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, nested.children().get(2).kind());
        assertEquals("x -> y -> x + y", slice(source, nested));
    }

    @Test
    void lambdaParenthesized() {
        String source = """
                class A {
                    void m() {
                        Function<Integer, Integer> f = (x -> x + 1);
                    }
                }
                """;
        AstNode paren = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.PARENTHESIZED_EXPRESSION, paren.kind());
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, paren.children().get(0).kind());
    }

    @Test
    void methodReferencesParse() {
        String source = """
                class A {
                    void m() {
                        Runnable r = this::run;
                        Supplier<Foo> s = Foo::new;
                        Consumer<Object> c = System.out::println;
                        Comparator<Foo> cmp = Foo::<T>compare;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        assertEquals(4, declarations.size());

        AstNode bound = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.METHOD_REFERENCE_EXPRESSION, bound.kind());
        assertEquals(AstNodeKind.THIS_EXPRESSION, bound.children().get(0).kind());
        assertEquals("run", bound.token().text());
        assertEquals("this::run", slice(source, bound));

        AstNode ctor = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.METHOD_REFERENCE_EXPRESSION, ctor.kind());
        assertEquals("new", ctor.token().text());

        AstNode chain = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.METHOD_REFERENCE_EXPRESSION, chain.kind());
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, chain.children().get(0).kind());
        assertEquals("System.out::println", slice(source, chain));

        AstNode generic = onlyChild(declarations.get(3), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.METHOD_REFERENCE_EXPRESSION, generic.kind());
        assertEquals("compare", generic.token().text());
    }

    @Test
    void classLiterals() {
        String source = """
                class A {
                    void m() {
                        Object c1 = Foo.class;
                        Object c2 = int.class;
                        Object c3 = String.class;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> declarations = ofKind(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode c1 = onlyChild(declarations.get(0), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CLASS_LITERAL_EXPRESSION, c1.kind());
        assertEquals(AstNodeKind.NAME_EXPRESSION, c1.children().get(0).kind());
        assertEquals("Foo.class", slice(source, c1));
        AstNode c2 = onlyChild(declarations.get(1), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CLASS_LITERAL_EXPRESSION, c2.kind());
        assertEquals("int.class", slice(source, c2));
        AstNode c3 = onlyChild(declarations.get(2), AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.CLASS_LITERAL_EXPRESSION, c3.kind());
    }

    @Test
    void switchArrowLabelsAreNotLambdas() {
        String source = """
                class A {
                    void m() {
                        switch (value) {
                            case x -> handle(x);
                            case 1, 2 -> {
                                Runnable r = y -> y;
                            }
                        }
                    }
                }
                """;
        AstNode block = methodBlock(source);
        AstNode switchStatement = onlyChild(block, AstNodeKind.SWITCH_STATEMENT);
        AstNode firstCase = switchStatement.children().get(1);
        AstNode label = firstCase.children().get(0);
        assertEquals(AstNodeKind.SWITCH_LABEL, label.kind());
        assertEquals(AstNodeKind.NAME_EXPRESSION, label.children().get(0).kind());
        assertEquals("x", slice(source, label.children().get(0)));
        AstNode bodyStatement = firstCase.children().get(1);
        assertEquals(AstNodeKind.EXPRESSION_STATEMENT, bodyStatement.kind());

        AstNode secondCase = switchStatement.children().get(2);
        AstNode secondLabel = secondCase.children().get(0);
        assertEquals(2, secondLabel.children().size());
        AstNode blockBody = secondCase.children().get(1);
        assertEquals(AstNodeKind.BLOCK, blockBody.kind());
        AstNode lambdaDecl = onlyChild(blockBody, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode lambda = onlyChild(lambdaDecl, AstNodeKind.DECLARATOR).children().get(0);
        assertEquals(AstNodeKind.LAMBDA_EXPRESSION, lambda.kind());
    }

    @Test
    void expressionRangesSliceSourceExactly() {
        String source = """
                class A {
                    void m() {
                        int r = (a + b) * 2 - (c / d);
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        assertEquals("(a + b) * 2 - (c / d)", slice(source, initializer));
        assertEquals("(a + b) * 2", slice(source, initializer.children().get(0)));
        assertEquals("(c / d)", slice(source, initializer.children().get(2)));
    }

    @Test
    void parentLinksEstablishedThroughExpressions() {
        String source = """
                class A {
                    void m() {
                        int r = a.b().c[0] + 1;
                    }
                }
                """;
        JavaFileModel model = parse(source);
        AstNode root = model.getAstRoot();
        AstVisitorAdapter adapter = new AstVisitorAdapter();
        AstNodes.traverse(root, adapter);
        List<AstNode> all = adapter.collected;
        for (AstNode node : all) {
            if (node == root) {
                continue;
            }
            assertNotNull(node.parent(), node + " must have a parent");
            assertTrue(node.parent().children().contains(node));
        }
    }

    private static final class AstVisitorAdapter implements com.eyecode.language.ast.AstVisitor {
        final List<AstNode> collected = new ArrayList<>();

        @Override
        public void visit(AstNode node) {
            collected.add(node);
        }
    }

    @Test
    void brokenExpressionsBecomeSkippedButFileSurvives() {
        String source = """
                class A {
                    void m() {
                        if (x) foo(
                    }
                    void ok() {
                        fine();
                    }
                }
                """;
        JavaFileModel model = parse(source);
        assertEquals(2, model.getTypes().get(0).getMethods().size());
        assertEquals(AstNodeKind.METHOD_DECLARATION,
                model.getAstRoot().children().get(0).children().get(0).kind());
        assertEquals(AstNodeKind.METHOD_DECLARATION,
                model.getAstRoot().children().get(0).children().get(1).kind());
    }

    @Test
    void danglingBinaryOperatorBecomesSkipped() {
        String source = """
                class A {
                    void m() {
                        int r = a +;
                        int ok = 1;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        assertEquals(AstNodeKind.SKIPPED, block.children().get(0).kind());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION, block.children().get(1).kind());
    }

    @Test
    void typedLambdaParametersFallBackToSkipped() {
        String source = """
                class A {
                    void m() {
                        BiFunction<Integer, Integer, Integer> f = (int a, int b) -> a + b;
                        int ok = 1;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        assertEquals(AstNodeKind.SKIPPED, block.children().get(0).kind());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION, block.children().get(1).kind());
    }

    @Test
    void arrayInitializerLiteralInExpressionPosition() {
        String source = """
                class A {
                    void m() {
                        int r = new int[] { 1, 2 }.length;
                    }
                }
                """;
        AstNode initializer = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.FIELD_ACCESS_EXPRESSION, initializer.kind());
        AstNode creation = initializer.children().get(0);
        assertEquals(AstNodeKind.ARRAY_CREATION_EXPRESSION, creation.kind());
        assertEquals("new int[] { 1, 2 }.length", slice(source, initializer));
    }

    @Test
    void operatorTokensCarryExactText() {
        String source = """
                class A {
                    void m() {
                        int r = a <<= b;
                    }
                }
                """;
        AstNode assignment = initializerOfFirstDeclarator(source);
        assertEquals(AstNodeKind.ASSIGNMENT_EXPRESSION, assignment.kind());
        assertEquals("<<=", assignment.children().get(1).token().text());
    }

    @Test
    void visitorDispatchesPerExpressionKind() {
        String source = """
                class A {
                    void m() {
                        Object r = (Foo) obj instanceof Foo ? ((x -> x)) : this.field;
                    }
                }
                """;
        AstNode block = methodBlock(source);
        List<AstNode> all = allDescendants(modelRoot(block));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.CAST_EXPRESSION));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.INSTANCEOF_EXPRESSION));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.TERNARY_EXPRESSION));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.LAMBDA_EXPRESSION));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.THIS_EXPRESSION));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.PARENTHESIZED_EXPRESSION));
        assertTrue(all.stream().anyMatch(n -> n.kind() == AstNodeKind.FIELD_ACCESS_EXPRESSION));
    }

    private static AstNode modelRoot(AstNode node) {
        AstNode current = node;
        while (current.parent() != null) {
            current = current.parent();
        }
        return current;
    }
}
