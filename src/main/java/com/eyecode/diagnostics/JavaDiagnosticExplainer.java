package com.eyecode.diagnostics;

import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticEducation;
import com.eyecode.language.diagnostics.DiagnosticExplainer;
import com.eyecode.language.diagnostics.DiagnosticRelatedContent;

import java.util.List;
import java.util.Optional;

/**
 * Pedagogical explanations for the diagnostics beginners actually hit in the
 * lessons. Entries are matched first by the structured category produced by
 * the syntax analyzer and then by the real diagnostic message formats emitted
 * by javac (parse phase) and JDT-LS. Related content only references
 * identifiers that exist in the documentation catalog.
 */
public final class JavaDiagnosticExplainer implements DiagnosticExplainer {
    private static final String MISSING_CLOSING_PARENTHESIS = "MISSING_CLOSING_PARENTHESIS";

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public Optional<DiagnosticEducation> explain(Diagnostic diagnostic) {
        if (diagnostic == null) {
            return Optional.empty();
        }
        DiagnosticEducation education = byCategory(diagnostic.category());
        if (education == null) {
            education = byMessage(diagnostic.message());
        }
        return Optional.ofNullable(education);
    }

    private static DiagnosticEducation byCategory(String category) {
        if (MISSING_CLOSING_PARENTHESIS.equals(category)) {
            return missingClosingParenthesis();
        }
        return null;
    }

    private static DiagnosticEducation byMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String text = message.strip();
        if (text.contains("is undefined for the type")) {
            return undefinedMethod();
        }
        if (text.contains("cannot be resolved to a type")) {
            return unknownType();
        }
        if (text.contains("cannot be resolved to a variable") || text.contains("cannot be resolved")) {
            return undeclaredVariable();
        }
        if (text.contains("is already defined") || text.contains("Duplicate local variable")
                || text.contains("Duplicate field") || text.contains("Duplicate variable")) {
            return duplicateVariable();
        }
        if (text.contains("Type mismatch: cannot convert from") || text.contains("incompatible types")) {
            return typeMismatch();
        }
        if (text.contains("reached end of file while parsing")) {
            return unclosedClassBody();
        }
        if (text.contains("unclosed string literal") || text.contains("String literal is not properly closed")) {
            return unclosedString();
        }
        if (text.contains("not a statement")) {
            return notAStatement();
        }
        if (text.contains("illegal start of expression") || text.contains("illegal start of type")) {
            return illegalExpression();
        }
        if (text.contains("')' expected") || text.contains("insert \")\"")) {
            return missingClosingParenthesis();
        }
        if (text.contains("';' expected") || text.contains("insert \";\"")) {
            return missingSemicolon();
        }
        return null;
    }

    private static DiagnosticEducation missingClosingParenthesis() {
        return new DiagnosticEducation(
                "Parece que sua condição está incompleta",
                "Está faltando fechar a condição com `)`.",
                "O compilador encontrou o início do bloco antes de encontrar o `)` que fecha a condição.",
                "if (condição) {\n    // código\n}",
                "if (idade >= 18) {\n    System.out.println(\"Você é maior de idade\");\n}",
                "Depois de escrever a condição, confira se os parênteses e chaves estão fechados.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Declare e use valores no seu programa"),
                        new DiagnosticRelatedContent("java/jdk/java.lang/system", "System", "Entenda o System.out.println das aulas")
                ));
    }

    private static DiagnosticEducation missingSemicolon() {
        return new DiagnosticEducation(
                "Falta o ponto e vírgula",
                "Toda instrução em Java termina com `;`.",
                "O compilador esperava um `;` fechando a instrução desta linha antes de continuar para a próxima.",
                "int idade = 15\nSystem.out.println(idade);",
                "int idade = 15;\nSystem.out.println(idade);",
                "Ao terminar uma instrução, escreva `;` antes de pular de linha.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Veja declarações completas com `;`")
                ));
    }

    private static DiagnosticEducation undeclaredVariable() {
        return new DiagnosticEducation(
                "Variável não declarada",
                "Você usou um nome que o Java não conhece.",
                "Para usar um valor, primeiro é preciso declarar a variável com um tipo, por exemplo `int saldo = 10;`.",
                "System.out.println(saldo);",
                "int saldo = 10;\nSystem.out.println(saldo);",
                "Confira se o nome está escrito exatamente igual à declaração — Java diferencia maiúsculas de minúsculas.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Como declarar e usar variáveis")
                ));
    }

    private static DiagnosticEducation undefinedMethod() {
        return new DiagnosticEducation(
                "Método não encontrado",
                "A chamada não corresponde a nenhum método existente.",
                "O Java procurou um método com esse nome nessa classe e não encontrou — o nome pode estar diferente ou o método não existe.",
                "System.out.printlne(\"Oi\");",
                "System.out.println(\"Oi\");",
                "Confira a grafia do método e os argumentos da chamada.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/classes", "Classes", "Onde os métodos são declarados"),
                        new DiagnosticRelatedContent("java/jdk/java.lang/system", "System", "Métodos de System.out usados nas aulas")
                ));
    }

    private static DiagnosticEducation unknownType() {
        return new DiagnosticEducation(
                "Tipo não reconhecido",
                "O Java não conhece esse tipo de nome.",
                "Isso costuma acontecer quando falta importar uma classe ou quando o nome do tipo está escrito de forma diferente da declaração.",
                "Strig nome = \"Ana\";",
                "String nome = \"Ana\";",
                "Verifique a ortografia do tipo — `String` começa com letra maiúscula.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Tipos usados em declarações"),
                        new DiagnosticRelatedContent("java/jdk/java.lang/string", "String", "O tipo String usado nas aulas")
                ));
    }

    private static DiagnosticEducation duplicateVariable() {
        return new DiagnosticEducation(
                "Nome já usado",
                "Já existe uma variável com esse nome.",
                "No mesmo escopo, cada nome pode ser declarado apenas uma vez — reutilize a variável existente ou escolha outro nome.",
                "int total = 10;\nint total = 15;",
                "int total = 10;\ntotal = 15;",
                "Para mudar o valor depois da declaração, apenas atribua: `total = 15;`.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Declaração, atribuição e escopo")
                ));
    }

    private static DiagnosticEducation typeMismatch() {
        return new DiagnosticEducation(
                "Tipos incompatíveis",
                "O valor não cabe no tipo da variável.",
                "Java não converte automaticamente entre tipos incompatíveis — o valor à direita precisa combinar com o tipo declarado.",
                "int preco = 12.5;",
                "double preco = 12.5;",
                "Use o tipo adequado ao valor: `double` aceita decimais, `int` apenas números inteiros.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Tipos primitivos e seus valores")
                ));
    }

    private static DiagnosticEducation unclosedClassBody() {
        return new DiagnosticEducation(
                "Classe sem fechar",
                "Falta fechar as chaves do programa.",
                "O compilador chegou ao fim do arquivo ainda dentro da classe — provavelmente uma `}` ficou faltando.",
                "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Oi\");\n",
                "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Oi\");\n    }\n}",
                "Confira se cada `{` que você abriu tem uma `}` correspondente.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/classes", "Classes", "Estrutura de uma classe Java")
                ));
    }

    private static DiagnosticEducation unclosedString() {
        return new DiagnosticEducation(
                "Texto sem fechar",
                "Faltou fechar as aspas do texto.",
                "Uma `String` precisa começar e terminar com `\"` na mesma linha — o compilador encontrou o fim da linha sem as aspas de fechamento.",
                "String nome = \"Ana;\nSystem.out.println(nome);",
                "String nome = \"Ana\";\nSystem.out.println(nome);",
                "Reescreva o texto garantindo as duas aspas: `\"Ana\"`.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/java.lang/string", "String", "Como escrever textos em Java")
                ));
    }

    private static DiagnosticEducation notAStatement() {
        return new DiagnosticEducation(
                "Instrução incompleta",
                "Essa linha não forma uma instrução válida.",
                "Java esperava uma instrução completa — verifique se não falta um operador, um nome ou o `;`.",
                "total + 5;",
                "total = total + 5;",
                "Instruções fazem algo: declaram, atribuem, chamam métodos ou controlam fluxo.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Instruções comuns com variáveis")
                ));
    }

    private static DiagnosticEducation illegalExpression() {
        return new DiagnosticEducation(
                "Expressão inesperada",
                "O Java não entendeu essa parte do código.",
                "A ordem ou os símbolos dessa expressão não formam código válido — confira operadores e delimitadores ao redor.",
                "int = 10;",
                "int idade = 10;",
                "Dê uma olhada no trecho apontado: geralmente falta um nome ou sobrou um símbolo.",
                List.of(
                        new DiagnosticRelatedContent("java/jdk/fundamentos/variables", "Variáveis", "Anatomia de uma declaração")
                ));
    }
}
