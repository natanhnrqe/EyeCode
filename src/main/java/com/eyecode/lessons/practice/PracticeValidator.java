package com.eyecode.lessons.practice;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.editor.v2.language.java.parser.ParserException;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.lessons.content.LessonPractice;

import java.util.List;

public final class PracticeValidator {
    private static final String INTEGER_SCORE = "integer-score";
    private static final String FIRST_PROGRAM_MESSAGE = "first-program-message";
    private static final String FIRST_PROGRAM_SECOND_LINE = "first-program-second-line";
    private static final String EYE_CODE_GREETING = "Olá, EyeCode!";
    private static final String FIRST_PROGRAM_SECOND_MESSAGE = "Meu primeiro programa Java!";
    private static final String VARIABLES_DECLARE_SCORE = "variables-declare-score";
    private static final String VARIABLES_CHANGE_SCORE = "variables-change-score";
    private static final String VARIABLES_PRINT_SCORE = "variables-print-score";
    private static final String STRINGS_TEXT = "strings-text";
    private static final String ARITHMETIC_CALCULATION = "arithmetic-calculation";
    private static final String ASSIGNMENT_UPDATE = "assignment-update";
    private static final String COMPARISON_LOGIC = "comparison-logic";

    private final JavaLexerService lexerService;

    public PracticeValidator() {
        this(new JavaLexerService());
    }

    PracticeValidator(JavaLexerService lexerService) {
        this.lexerService = lexerService;
    }

    public PracticeVerificationResult verify(LessonPractice practice, String source) {
        if (practice == null) {
            throw new IllegalArgumentException("Unsupported practice");
        }

        return switch (practice.id()) {
            case INTEGER_SCORE -> verifyIntegerScore(source);
            case VARIABLES_DECLARE_SCORE -> verifyDeclaration(source, "score", "int", "10");
            case VARIABLES_CHANGE_SCORE -> verifyAssignment(source, "score", "25");
            case VARIABLES_PRINT_SCORE -> verifyPrintVariable(source, "score");
            case STRINGS_TEXT -> verifyStrings(source);
            case ARITHMETIC_CALCULATION -> verifyOperators(source, List.of("+", "-", "*", "/", "%"), PracticeVerificationStatus.MISSING_OPERATOR);
            case ASSIGNMENT_UPDATE -> verifyOperators(source, List.of("=", "+=", "-=", "++", "--"), PracticeVerificationStatus.MISSING_OPERATOR);
            case COMPARISON_LOGIC -> verifyOperators(source, List.of(">=", "&&"), PracticeVerificationStatus.MISSING_COMPARISON);
            case FIRST_PROGRAM_MESSAGE -> verifyProgramOutput(FIRST_PROGRAM_MESSAGE, source, List.of(EYE_CODE_GREETING));
            case FIRST_PROGRAM_SECOND_LINE -> verifyProgramOutput(FIRST_PROGRAM_SECOND_LINE, source, List.of(EYE_CODE_GREETING, FIRST_PROGRAM_SECOND_MESSAGE));
            default -> throw new IllegalArgumentException("Unsupported practice");
        };
    }

    private PracticeVerificationResult verifyDeclaration(String source, String name, String type, String value) {
        JavaFileModel model;
        try { model = parse(source); } catch (ParserException exception) { return result(PracticeVerificationStatus.SYNTAX_ERROR); }
        JavaMethodModel main = mainMethod(model);
        if (main == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);
        JavaVariableModel variable = main.getLocalVariables().stream().filter(v -> name.equals(v.getName())).findFirst().orElse(null);
        if (variable == null) return result(PracticeVerificationStatus.MISSING_DECLARATION);
        if (!type.equals(variable.getType())) return result(PracticeVerificationStatus.WRONG_TYPE);
        return hasLiteral(model.getAstRoot(), variable.getRange(), value) ? result(PracticeVerificationStatus.SUCCESS) : result(PracticeVerificationStatus.WRONG_INITIALIZER);
    }

    private PracticeVerificationResult verifyAssignment(String source, String name, String value) {
        JavaFileModel model;
        try { model = parse(source); } catch (ParserException exception) { return result(PracticeVerificationStatus.SYNTAX_ERROR); }
        JavaMethodModel main = mainMethod(model);
        if (main == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);
        boolean found = AstNodes.descendants(model.getAstRoot()).stream()
                .filter(n -> n.kind() == AstNodeKind.ASSIGNMENT_EXPRESSION && main.getRange().contains(n.range()))
                .anyMatch(n -> n.children().size() == 3 && isName(n.children().get(0), name) && tokenText(n.children().get(1), "=") && tokenText(n.children().get(2), value));
        return result(found ? PracticeVerificationStatus.SUCCESS : PracticeVerificationStatus.MISSING_ASSIGNMENT);
    }

    private PracticeVerificationResult verifyPrintVariable(String source, String name) {
        JavaFileModel model;
        try { model = parse(source); } catch (ParserException exception) { return result(PracticeVerificationStatus.SYNTAX_ERROR); }
        JavaMethodModel main = mainMethod(model);
        if (main == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);
        boolean found = AstNodes.descendants(model.getAstRoot()).stream().filter(n -> n.kind() == AstNodeKind.METHOD_CALL_EXPRESSION)
                .filter(n -> main.getRange().contains(n.range())).anyMatch(n -> n.children().size() == 2 && isSystemOutPrintln(n.children().getFirst()) && isName(n.children().get(1), name));
        return result(found ? PracticeVerificationStatus.SUCCESS : PracticeVerificationStatus.MISSING_OUTPUT);
    }

    private PracticeVerificationResult verifyStrings(String source) {
        JavaFileModel model;
        try { model = parse(source); } catch (ParserException exception) { return result(PracticeVerificationStatus.SYNTAX_ERROR); }
        JavaMethodModel main = mainMethod(model);
        if (main == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);
        JavaVariableModel letra = main.getLocalVariables().stream().filter(v -> "letra".equals(v.getName())).findFirst().orElse(null);
        JavaVariableModel nome = main.getLocalVariables().stream().filter(v -> "nome".equals(v.getName())).findFirst().orElse(null);
        boolean character = letra != null && "char".equals(letra.getType()) && hasLiteral(model.getAstRoot(), letra.getRange(), "B");
        boolean string = nome != null && "String".equals(nome.getType()) && hasLiteral(model.getAstRoot(), nome.getRange(), "Bia");
        boolean printed = AstNodes.descendants(model.getAstRoot()).stream().filter(n -> n.kind() == AstNodeKind.METHOD_CALL_EXPRESSION)
                .anyMatch(n -> main.getRange().contains(n.range()) && n.children().size() == 2 && isSystemOutPrintln(n.children().getFirst()) && isName(n.children().get(1), "nome"));
        return result(character && string && printed ? PracticeVerificationStatus.SUCCESS : PracticeVerificationStatus.WRONG_INITIALIZER);
    }

    private PracticeVerificationResult verifyOperators(String source, List<String> operators, PracticeVerificationStatus missing) {
        JavaFileModel model;
        try { model = parse(source); } catch (ParserException exception) { return result(PracticeVerificationStatus.SYNTAX_ERROR); }
        JavaMethodModel main = mainMethod(model);
        if (main == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);
        List<String> found = AstNodes.descendants(model.getAstRoot()).stream()
                .filter(n -> n.kind() == AstNodeKind.OPERATOR && main.getRange().contains(n.range()))
                .filter(n -> n.token() != null).map(n -> n.token().text()).toList();
        if (!found.contains("=") && main.getLocalVariables().stream().anyMatch(v -> "pontos".equals(v.getName()))) {
            found = new java.util.ArrayList<>(found);
            found.add("=");
        }
        return result(operators.stream().allMatch(found::contains) ? PracticeVerificationStatus.SUCCESS : missing);
    }

    private static JavaMethodModel mainMethod(JavaFileModel model) {
        return model.getTypes().stream().filter(t -> "Main".equals(t.getName())).findFirst().flatMap(t -> t.getMethods().stream().filter(m -> "main".equals(m.getName())).findFirst()).orElse(null);
    }

    private static boolean hasLiteral(AstNode root, TextRange range, String expected) {
        return AstNodes.descendants(root).stream().filter(n -> n.kind() == AstNodeKind.DECLARATOR && n.range().contains(range))
                .flatMap(n -> AstNodes.descendants(n).stream()).filter(n -> n.kind() == AstNodeKind.LITERAL_EXPRESSION)
                .anyMatch(n -> n.token() != null && expected.equals(n.token().text().replace("\"", "").replace("'", "")));
    }
    private static boolean tokenText(AstNode node, String text) { return node.token() != null && text.equals(node.token().text()); }

    private PracticeVerificationResult verifyIntegerScore(String source) {
        JavaFileModel model;
        try {
            model = parse(source);
        } catch (ParserException exception) {
            return result(PracticeVerificationStatus.SYNTAX_ERROR);
        }

        JavaClassModel mainClass = model.getTypes().stream()
                .filter(type -> "Main".equals(type.getName()))
                .findFirst().orElse(null);
        if (mainClass == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);

        JavaMethodModel main = mainClass.getMethods().stream()
                .filter(method -> "main".equals(method.getName()))
                .findFirst().orElse(null);
        if (main == null) return result(PracticeVerificationStatus.INVALID_CONTEXT);

        JavaVariableModel score = main.getLocalVariables().stream()
                .filter(variable -> "score".equals(variable.getName()))
                .findFirst().orElse(null);
        if (score != null) {
            if (!"int".equals(score.getType())) return result(PracticeVerificationStatus.WRONG_TYPE);
            if (!hasIntegerLiteralInitializer(model.getAstRoot(), score.getRange(), "100")) {
                return result(PracticeVerificationStatus.WRONG_INITIALIZER);
            }
            return result(PracticeVerificationStatus.SUCCESS);
        }

        if (hasScoreOutsideMain(model, mainClass, main)) return result(PracticeVerificationStatus.INVALID_CONTEXT);
        if (main.getLocalVariables().stream().anyMatch(variable -> "int".equals(variable.getType()))) {
            return result(PracticeVerificationStatus.WRONG_NAME);
        }
        return result(PracticeVerificationStatus.MISSING_DECLARATION);
    }

    private PracticeVerificationResult verifyProgramOutput(String practiceId, String source, List<String> expectedMessages) {
        JavaFileModel model;
        try {
            model = parse(source);
        } catch (ParserException exception) {
            return result(practiceId, PracticeVerificationStatus.SYNTAX_ERROR);
        }

        JavaClassModel mainClass = model.getTypes().stream()
                .filter(type -> "Main".equals(type.getName()))
                .findFirst().orElse(null);
        if (mainClass == null) return result(practiceId, PracticeVerificationStatus.INVALID_CONTEXT);

        JavaMethodModel main = mainClass.getMethods().stream()
                .filter(method -> "main".equals(method.getName()))
                .findFirst().orElse(null);
        if (main == null) return result(practiceId, PracticeVerificationStatus.INVALID_CONTEXT);

        List<String> printedMessages = AstNodes.descendants(model.getAstRoot()).stream()
                .filter(node -> node.kind() == AstNodeKind.METHOD_CALL_EXPRESSION)
                .filter(node -> main.getRange().contains(node.range()))
                .map(PracticeValidator::printlnLiteral)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (printedMessages.isEmpty()) return result(practiceId, PracticeVerificationStatus.MISSING_OUTPUT);
        if (expectedMessages.size() == 2 && printedMessages.size() < 2) {
            return result(practiceId, PracticeVerificationStatus.MISSING_SECOND_OUTPUT);
        }
        if (!printedMessages.subList(0, expectedMessages.size()).equals(expectedMessages)) {
            return result(practiceId, PracticeVerificationStatus.WRONG_OUTPUT);
        }
        return result(practiceId, PracticeVerificationStatus.SUCCESS);
    }

    private JavaFileModel parse(String source) {
        String safeSource = source == null ? "" : source;
        return new JavaParser(new JavaTokenStream(
                lexerService.lex(DocumentSnapshot.oneShot(safeSource)).tokens(), safeSource)).parse();
    }

    private static boolean hasScoreOutsideMain(JavaFileModel model, JavaClassModel mainClass,
                                               JavaMethodModel main) {
        if (mainClass.getFields().stream().anyMatch(field -> "score".equals(field.getName()))) return true;
        return model.getTypes().stream()
                .flatMap(type -> type.getMethods().stream())
                .filter(method -> method != main)
                .flatMap(method -> method.getLocalVariables().stream())
                .anyMatch(variable -> "score".equals(variable.getName()));
    }

    private static boolean hasIntegerLiteralInitializer(AstNode root, TextRange variableRange, String expected) {
        if (root == null) return false;
        return AstNodes.descendants(root).stream()
                .filter(node -> node.kind() == AstNodeKind.LOCAL_VARIABLE_DECLARATION)
                .filter(node -> node.range().contains(variableRange))
                .flatMap(node -> node.children().stream())
                .filter(node -> node.kind() == AstNodeKind.DECLARATOR && node.range().contains(variableRange))
                .flatMap(node -> node.children().stream())
                .anyMatch(node -> node.kind() == AstNodeKind.LITERAL_EXPRESSION
                        && node.token() != null
                        && node.token().type() == JavaTokenType.NUMBER
                        && expected.equals(node.token().text()));
    }

    private static java.util.Optional<String> printlnLiteral(AstNode call) {
        if (call.children().size() != 2 || !isSystemOutPrintln(call.children().getFirst())) {
            return java.util.Optional.empty();
        }
        AstNode argument = call.children().get(1);
        if (argument.kind() != AstNodeKind.LITERAL_EXPRESSION || argument.token() == null
                || argument.token().type() != JavaTokenType.STRING) {
            return java.util.Optional.empty();
        }
        String literal = argument.token().text();
        return literal.length() >= 2
                ? java.util.Optional.of(literal.substring(1, literal.length() - 1))
                : java.util.Optional.empty();
    }

    private static boolean isSystemOutPrintln(AstNode target) {
        return isFieldAccess(target, "println")
                && isFieldAccess(target.children().getFirst(), "out")
                && isName(target.children().getFirst().children().getFirst(), "System");
    }

    private static boolean isFieldAccess(AstNode node, String member) {
        return node.kind() == AstNodeKind.FIELD_ACCESS_EXPRESSION
                && node.children().size() == 2
                && isName(node.children().get(1), member);
    }

    private static boolean isName(AstNode node, String expected) {
        return node.kind() == AstNodeKind.NAME_EXPRESSION
                && node.token() != null
                && expected.equals(node.token().text());
    }

    private static PracticeVerificationResult result(PracticeVerificationStatus status) {
        return new PracticeVerificationResult(status, switch (status) {
            case SUCCESS -> "Correto. Você declarou `score` como `int` e inicializou com `100`.";
            case SYNTAX_ERROR -> "O código Java ainda está incompleto ou possui um erro de sintaxe.";
            case INVALID_CONTEXT -> "Mantenha a declaração dentro do método `main`.";
            case MISSING_DECLARATION -> "Não encontrei a variável pedida dentro de `main`.";
            case WRONG_TYPE -> "Use o tipo `int` para esta variável.";
            case WRONG_NAME -> "A variável precisa se chamar `score`.";
            case WRONG_INITIALIZER -> "Inicialize `score` com o valor inteiro `100`.";
            case MISSING_OUTPUT -> "Adicione uma instrução `System.out.println` dentro de `main`.";
            case WRONG_OUTPUT -> "Revise o texto exibido e a ordem das instruções `println`.";
            case MISSING_SECOND_OUTPUT -> "Adicione uma segunda instrução `System.out.println` dentro de `main`.";
            case MISSING_ASSIGNMENT -> "Faça a atribuição solicitada dentro de `main`.";
            case WRONG_ASSIGNMENT -> "Revise o valor atribuído à variável.";
            case MISSING_OPERATOR -> "Use todos os operadores pedidos na expressão.";
            case MISSING_COMPARISON -> "Adicione a comparação solicitada e combine seu resultado lógico.";
            case MISSING_LOGICAL_OPERATOR -> "Adicione um operador lógico à condição.";
        });
    }

    private static PracticeVerificationResult result(String practiceId, PracticeVerificationStatus status) {
        if (INTEGER_SCORE.equals(practiceId)) return result(status);
        String message = switch (status) {
            case SUCCESS -> FIRST_PROGRAM_MESSAGE.equals(practiceId)
                    ? "Correto. A mensagem foi alterada para `Olá, EyeCode!`."
                    : "Correto. A segunda mensagem foi adicionada na ordem esperada.";
            case SYNTAX_ERROR -> "O código Java ainda está incompleto ou possui um erro de sintaxe.";
            case INVALID_CONTEXT -> "Mantenha as instruções dentro do método `main`.";
            case MISSING_OUTPUT -> "Adicione uma instrução `System.out.println` dentro de `main`.";
            case WRONG_OUTPUT -> "O método `main` ainda não imprime a mensagem esperada.";
            case MISSING_SECOND_OUTPUT -> "Adicione uma segunda instrução `System.out.println` dentro de `main`.";
            default -> "Revise a instrução solicitada para esta prática.";
        };
        return new PracticeVerificationResult(status, message);
    }
}
