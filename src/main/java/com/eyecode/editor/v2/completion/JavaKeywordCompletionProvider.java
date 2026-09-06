package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaKeywordCompletionProvider implements CompletionProvider {

    @Override
    public boolean supports(CompletionContextKind contextKind) {
        return contextKind == CompletionContextKind.IDENTIFIER;
    }

    private static final Map<String, String> KEYWORD_DOCS = Map.ofEntries(
            Map.entry("public", "Access modifier visible to all classes."),
            Map.entry("private", "Access modifier visible only within the declaring class."),
            Map.entry("protected", "Access modifier visible to package and subclasses."),
            Map.entry("class", "Declares a Java class."),
            Map.entry("interface", "Declares an interface — a contract of abstract methods."),
            Map.entry("enum", "Declares an enumerated type with fixed constants."),
            Map.entry("record", "Declares a concise data-carrying class."),
            Map.entry("extends", "Indicates inheritance from a parent class."),
            Map.entry("implements", "Indicates implementation of one or more interfaces."),
            Map.entry("static", "Declares a member that belongs to the class, not an instance."),
            Map.entry("final", "Prevents modification, overriding, or subclassing."),
            Map.entry("void", "Return type indicating no value is returned."),
            Map.entry("new", "Creates a new object instance."),
            Map.entry("return", "Returns a value from a method."),
            Map.entry("if", "Conditional branch statement."),
            Map.entry("else", "Alternative branch for an if statement."),
            Map.entry("for", "Loop construct for iteration."),
            Map.entry("while", "Loop construct that repeats while a condition holds."),
            Map.entry("switch", "Multi-branch selection statement."),
            Map.entry("case", "Label inside a switch statement."),
            Map.entry("try", "Begins a block with exception handling."),
            Map.entry("catch", "Catches a specific exception type."),
            Map.entry("finally", "Runs after a try block completes."),
            Map.entry("throw", "Throws an exception."),
            Map.entry("throws", "Declares exceptions a method can throw."),
            Map.entry("package", "Declares the package for a source file."),
            Map.entry("import", "Imports a type or static member."),
            Map.entry("this", "References the current instance."),
            Map.entry("super", "References the direct superclass."),
            Map.entry("true", "Boolean literal true."),
            Map.entry("false", "Boolean literal false."),
            Map.entry("null", "Null reference literal.")
    );

    private static final List<String> KEYWORDS = List.copyOf(KEYWORD_DOCS.keySet());
    private static final Set<String> DECLARATION_ONLY_MODIFIERS = Set.of("public", "private", "protected");

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context, boolean manual) {
        if (CompletionPrefixResolver.isQualifiedContext(context)) {
            return CompletionSnapshot.empty();
        }
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        if (prefix.isEmpty() && !manual) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = KEYWORDS.stream()
                .filter(keyword -> !DECLARATION_ONLY_MODIFIERS.contains(keyword)
                        || !CompletionContextResolver.isMethodBodyExpressionContext(context))
                .map(keyword -> CompletionItem.builder(keyword, keyword, CompletionItemKind.KEYWORD)
                        .detail("Java keyword")
                        .owner("java.lang")
                        .category("Keyword")
                        .documentation(KEYWORD_DOCS.getOrDefault(keyword, ""))
                        .priority(10)
                        .build())
                .toList();

        return new CompletionSnapshot(items);
    }
}
