package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContextAwareCompletionProvider implements CompletionProvider {

    private static final Map<String, List<CompletionItem>> CONTEXT_MAP = buildContextMap();

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        String objectExpr = resolveObjectExpressionBeforeDot(context);
        if (objectExpr == null) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> methods = CONTEXT_MAP.get(objectExpr);
        if (methods == null) {
            return CompletionSnapshot.empty();
        }

        return new CompletionSnapshot(new ArrayList<>(methods));
    }

    private String resolveObjectExpressionBeforeDot(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = offsetForPosition(context, context.getCaret());
        int safeOffset = Math.max(0, Math.min(offset, text.length()));

        int prefixStart = safeOffset;
        while (prefixStart > 0 && Character.isJavaIdentifierPart(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        String prefix = text.substring(prefixStart, safeOffset);

        if (prefixStart == 0) return null;

        int dotPos = prefixStart - 1;
        if (dotPos < 0 || dotPos >= text.length() || text.charAt(dotPos) != '.') {
            return null;
        }

        int exprEnd = dotPos;
        int exprStart = exprEnd;
        while (exprStart > 0 && Character.isJavaIdentifierPart(text.charAt(exprStart - 1))) {
            exprStart--;
        }

        String expr = text.substring(exprStart, exprEnd);

        int beforeExpr = exprStart - 1;
        if (beforeExpr >= 0 && text.charAt(beforeExpr) == '.') {
            int ownerEnd = exprStart;
            int ownerStart = ownerEnd;
            while (ownerStart > 0 && Character.isJavaIdentifierPart(text.charAt(ownerStart - 1))) {
                ownerStart--;
            }
            String owner = text.substring(ownerStart, ownerEnd);
            return owner + "." + expr;
        }

        return expr.isEmpty() ? null : expr;
    }

    private static int offsetForPosition(LanguageContext context, EditorPosition position) {
        String text = context.getDocument().getText();
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == position.line() && column == position.column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }

    private static Map<String, List<CompletionItem>> buildContextMap() {
        Map<String, List<CompletionItem>> map = new LinkedHashMap<>();

        map.put("String", List.of(
                CompletionItem.builder("length", "length", CompletionItemKind.METHOD)
                        .detail("String.length()").signature("String.length()")
                        .returnType("int").owner("java.lang.String").category("Method")
                        .documentation("Returns the number of characters in this string.").build(),
                CompletionItem.builder("substring", "substring", CompletionItemKind.METHOD)
                        .detail("String.substring()").signature("String.substring(int beginIndex)")
                        .returnType("String").owner("java.lang.String").category("Method")
                        .documentation("Returns a substring starting at the given index.").build(),
                CompletionItem.builder("charAt", "charAt", CompletionItemKind.METHOD)
                        .detail("String.charAt()").signature("String.charAt(int index)")
                        .returnType("char").owner("java.lang.String").category("Method")
                        .documentation("Returns the character at the specified index.").build(),
                CompletionItem.builder("trim", "trim", CompletionItemKind.METHOD)
                        .detail("String.trim()").signature("String.trim()")
                        .returnType("String").owner("java.lang.String").category("Method")
                        .documentation("Removes leading and trailing whitespace.").build(),
                CompletionItem.builder("repeat", "repeat", CompletionItemKind.METHOD)
                        .detail("String.repeat()").signature("String.repeat(int count)")
                        .returnType("String").owner("java.lang.String").category("Method")
                        .documentation("Returns a string repeated count times.").build(),
                CompletionItem.builder("contains", "contains", CompletionItemKind.METHOD)
                        .detail("String.contains()").signature("String.contains(CharSequence)")
                        .returnType("boolean").owner("java.lang.String").category("Method")
                        .documentation("Tests if this string contains the given sequence.").build(),
                CompletionItem.builder("equals", "equals", CompletionItemKind.METHOD)
                        .detail("String.equals()").signature("String.equals(Object)")
                        .returnType("boolean").owner("java.lang.String").category("Method")
                        .documentation("Compares this string to another for value equality.").build(),
                CompletionItem.builder("replace", "replace", CompletionItemKind.METHOD)
                        .detail("String.replace()").signature("String.replace(char, char)")
                        .returnType("String").owner("java.lang.String").category("Method")
                        .documentation("Replaces all occurrences of a character.").build(),
                CompletionItem.builder("split", "split", CompletionItemKind.METHOD)
                        .detail("String.split()").signature("String.split(String regex)")
                        .returnType("String[]").owner("java.lang.String").category("Method")
                        .documentation("Splits this string around matches of the regex.").build(),
                CompletionItem.builder("indexOf", "indexOf", CompletionItemKind.METHOD)
                        .detail("String.indexOf()").signature("String.indexOf(String)")
                        .returnType("int").owner("java.lang.String").category("Method")
                        .documentation("Returns the index of the first occurrence of the substring.").build(),
                CompletionItem.builder("toUpperCase", "toUpperCase", CompletionItemKind.METHOD)
                        .detail("String.toUpperCase()").signature("String.toUpperCase()")
                        .returnType("String").owner("java.lang.String").category("Method")
                        .documentation("Converts all characters to upper case.").build(),
                CompletionItem.builder("toLowerCase", "toLowerCase", CompletionItemKind.METHOD)
                        .detail("String.toLowerCase()").signature("String.toLowerCase()")
                        .returnType("String").owner("java.lang.String").category("Method")
                        .documentation("Converts all characters to lower case.").build(),
                CompletionItem.builder("isEmpty", "isEmpty", CompletionItemKind.METHOD)
                        .detail("String.isEmpty()").signature("String.isEmpty()")
                        .returnType("boolean").owner("java.lang.String").category("Method")
                        .documentation("Returns true if the string has length 0.").build()
        ));

        map.put("list", List.of(
                CompletionItem.builder("add", "add", CompletionItemKind.METHOD)
                        .detail("List.add()").signature("List.add(E element)")
                        .returnType("boolean").owner("java.util.List").category("Method")
                        .documentation("Appends the element to the end of the list.").build(),
                CompletionItem.builder("remove", "remove", CompletionItemKind.METHOD)
                        .detail("List.remove()").signature("List.remove(int index)")
                        .returnType("E").owner("java.util.List").category("Method")
                        .documentation("Removes the element at the given index.").build(),
                CompletionItem.builder("size", "size", CompletionItemKind.METHOD)
                        .detail("List.size()").signature("List.size()")
                        .returnType("int").owner("java.util.List").category("Method")
                        .documentation("Returns the number of elements in the list.").build(),
                CompletionItem.builder("contains", "contains", CompletionItemKind.METHOD)
                        .detail("List.contains()").signature("List.contains(Object)")
                        .returnType("boolean").owner("java.util.List").category("Method")
                        .documentation("Returns true if the list contains the element.").build(),
                CompletionItem.builder("clear", "clear", CompletionItemKind.METHOD)
                        .detail("List.clear()").signature("List.clear()")
                        .returnType("void").owner("java.util.List").category("Method")
                        .documentation("Removes all elements from the list.").build(),
                CompletionItem.builder("get", "get", CompletionItemKind.METHOD)
                        .detail("List.get()").signature("List.get(int index)")
                        .returnType("E").owner("java.util.List").category("Method")
                        .documentation("Returns the element at the given index.").build(),
                CompletionItem.builder("indexOf", "indexOf", CompletionItemKind.METHOD)
                        .detail("List.indexOf()").signature("List.indexOf(Object)")
                        .returnType("int").owner("java.util.List").category("Method")
                        .documentation("Returns the index of the first occurrence of the element.").build(),
                CompletionItem.builder("isEmpty", "isEmpty", CompletionItemKind.METHOD)
                        .detail("List.isEmpty()").signature("List.isEmpty()")
                        .returnType("boolean").owner("java.util.List").category("Method")
                        .documentation("Returns true if the list has no elements.").build()
        ));

        map.put("Math", List.of(
                CompletionItem.builder("abs", "abs", CompletionItemKind.METHOD)
                        .detail("Math.abs()").signature("Math.abs(int a)")
                        .returnType("int").owner("java.lang.Math").category("Method")
                        .documentation("Returns the absolute value.").build(),
                CompletionItem.builder("min", "min", CompletionItemKind.METHOD)
                        .detail("Math.min()").signature("Math.min(int a, int b)")
                        .returnType("int").owner("java.lang.Math").category("Method")
                        .documentation("Returns the smaller of two values.").build(),
                CompletionItem.builder("max", "max", CompletionItemKind.METHOD)
                        .detail("Math.max()").signature("Math.max(int a, int b)")
                        .returnType("int").owner("java.lang.Math").category("Method")
                        .documentation("Returns the greater of two values.").build(),
                CompletionItem.builder("pow", "pow", CompletionItemKind.METHOD)
                        .detail("Math.pow()").signature("Math.pow(double a, double b)")
                        .returnType("double").owner("java.lang.Math").category("Method")
                        .documentation("Returns a raised to the power of b.").build(),
                CompletionItem.builder("sqrt", "sqrt", CompletionItemKind.METHOD)
                        .detail("Math.sqrt()").signature("Math.sqrt(double a)")
                        .returnType("double").owner("java.lang.Math").category("Method")
                        .documentation("Returns the square root of a.").build(),
                CompletionItem.builder("random", "random", CompletionItemKind.METHOD)
                        .detail("Math.random()").signature("Math.random()")
                        .returnType("double").owner("java.lang.Math").category("Method")
                        .documentation("Returns a random double between 0.0 and 1.0.").build(),
                CompletionItem.builder("floor", "floor", CompletionItemKind.METHOD)
                        .detail("Math.floor()").signature("Math.floor(double a)")
                        .returnType("double").owner("java.lang.Math").category("Method")
                        .documentation("Returns the largest integer less than or equal to a.").build(),
                CompletionItem.builder("ceil", "ceil", CompletionItemKind.METHOD)
                        .detail("Math.ceil()").signature("Math.ceil(double a)")
                        .returnType("double").owner("java.lang.Math").category("Method")
                        .documentation("Returns the smallest integer greater than or equal to a.").build(),
                CompletionItem.builder("round", "round", CompletionItemKind.METHOD)
                        .detail("Math.round()").signature("Math.round(float a)")
                        .returnType("int").owner("java.lang.Math").category("Method")
                        .documentation("Returns the closest int to the argument.").build()
        ));

        map.put("System.out", List.of(
                CompletionItem.builder("println", "println", CompletionItemKind.METHOD)
                        .detail("PrintStream.println()").signature("println(String x)")
                        .returnType("void").owner("java.io.PrintStream").category("Method")
                        .documentation("Prints a string and terminates the line.").build(),
                CompletionItem.builder("print", "print", CompletionItemKind.METHOD)
                        .detail("PrintStream.print()").signature("print(String x)")
                        .returnType("void").owner("java.io.PrintStream").category("Method")
                        .documentation("Prints a string without a newline.").build(),
                CompletionItem.builder("printf", "printf", CompletionItemKind.METHOD)
                        .detail("PrintStream.printf()").signature("printf(String format, Object... args)")
                        .returnType("PrintStream").owner("java.io.PrintStream").category("Method")
                        .documentation("Prints a formatted string.").build(),
                CompletionItem.builder("format", "format", CompletionItemKind.METHOD)
                        .detail("PrintStream.format()").signature("format(String format, Object... args)")
                        .returnType("PrintStream").owner("java.io.PrintStream").category("Method")
                        .documentation("Writes a formatted string to this output stream.").build(),
                CompletionItem.builder("flush", "flush", CompletionItemKind.METHOD)
                        .detail("PrintStream.flush()").signature("flush()")
                        .returnType("void").owner("java.io.PrintStream").category("Method")
                        .documentation("Flushes the stream.").build()
        ));

        return map;
    }
}
