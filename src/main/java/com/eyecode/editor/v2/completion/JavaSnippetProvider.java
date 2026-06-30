package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.List;

public final class JavaSnippetProvider implements CompletionProvider {

    private static final List<CompletionItem> SNIPPETS = List.of(
            CompletionItem.builder("psvm", "public static void main(String[] args) {\n    ${0}\n}", CompletionItemKind.SNIPPET)
                    .detail("public static void main")
                    .category("Snippet")
                    .documentation("Generates the main method signature.")
                    .example("public static void main(String[] args) {\n    \n}")
                    .priority(100)
                    .build(),

            CompletionItem.builder("main", "public static void main(String[] args) {\n    ${0}\n}", CompletionItemKind.SNIPPET)
                    .detail("public static void main")
                    .category("Snippet")
                    .documentation("Generates the main method signature.")
                    .example("public static void main(String[] args) {\n    \n}")
                    .priority(100)
                    .build(),

            CompletionItem.builder("sout", "System.out.println(${0});", CompletionItemKind.SNIPPET)
                    .detail("System.out.println")
                    .category("Snippet")
                    .documentation("Prints a line to standard output.")
                    .example("System.out.println(\"Hello\");")
                    .priority(100)
                    .build(),

            CompletionItem.builder("if", "if (condition) {\n    ${0}\n}", CompletionItemKind.SNIPPET)
                    .detail("if statement")
                    .category("Snippet")
                    .documentation("Generates an if statement block.")
                    .example("if (x > 0) {\n    \n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("ifelse", "if (condition) {\n    ${0}\n} else {\n    \n}", CompletionItemKind.SNIPPET)
                    .detail("if-else statement")
                    .category("Snippet")
                    .documentation("Generates an if-else statement block.")
                    .example("if (x > 0) {\n    \n} else {\n    \n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("for", "for (int i = 0; i < ${0}; i++) {\n    \n}", CompletionItemKind.SNIPPET)
                    .detail("for loop")
                    .category("Snippet")
                    .documentation("Generates a classic for loop.")
                    .example("for (int i = 0; i < n; i++) {\n    \n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("foreach", "for (var item : collection) {\n    ${0}\n}", CompletionItemKind.SNIPPET)
                    .detail("enhanced for loop")
                    .category("Snippet")
                    .documentation("Generates an enhanced for-each loop.")
                    .example("for (var item : list) {\n    \n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("while", "while (condition) {\n    ${0}\n}", CompletionItemKind.SNIPPET)
                    .detail("while loop")
                    .category("Snippet")
                    .documentation("Generates a while loop block.")
                    .example("while (x > 0) {\n    \n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("do", "do {\n    ${0}\n} while (condition);", CompletionItemKind.SNIPPET)
                    .detail("do-while loop")
                    .category("Snippet")
                    .documentation("Generates a do-while loop block.")
                    .example("do {\n    \n} while (x > 0);")
                    .priority(90)
                    .build(),

            CompletionItem.builder("switch", "switch (variable) {\n    case value:\n        ${0}\n        break;\n    default:\n        break;\n}", CompletionItemKind.SNIPPET)
                    .detail("switch statement")
                    .category("Snippet")
                    .documentation("Generates a switch statement with a default case.")
                    .example("switch (day) {\n    case 1:\n        break;\n    default:\n        break;\n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("try", "try {\n    ${0}\n}", CompletionItemKind.SNIPPET)
                    .detail("try block")
                    .category("Snippet")
                    .documentation("Generates a try block.")
                    .example("try {\n    \n}")
                    .priority(80)
                    .build(),

            CompletionItem.builder("trycatch", "try {\n    ${0}\n} catch (Exception e) {\n    \n}", CompletionItemKind.SNIPPET)
                    .detail("try-catch block")
                    .category("Snippet")
                    .documentation("Generates a try-catch block.")
                    .example("try {\n    \n} catch (Exception e) {\n    e.printStackTrace();\n}")
                    .priority(90)
                    .build(),

            CompletionItem.builder("tryfinally", "try {\n    ${0}\n} finally {\n    \n}", CompletionItemKind.SNIPPET)
                    .detail("try-finally block")
                    .category("Snippet")
                    .documentation("Generates a try-finally block.")
                    .example("try {\n    \n} finally {\n    \n}")
                    .priority(85)
                    .build()
    );

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return new CompletionSnapshot(SNIPPETS);
    }
}
