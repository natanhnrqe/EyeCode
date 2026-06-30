package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaLangSymbols {

    private JavaLangSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("System", "System", CompletionItemKind.CLASS)
                        .detail("java.lang.System")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Provides access to system resources and environment: standard input, output, error streams, and system properties.")
                        .build(),

                CompletionItem.builder("String", "String", CompletionItemKind.CLASS)
                        .detail("java.lang.String")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Represents an immutable sequence of characters.")
                        .build(),

                CompletionItem.builder("Math", "Math", CompletionItemKind.CLASS)
                        .detail("java.lang.Math")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Provides mathematical functions and constants like E and PI.")
                        .build(),

                CompletionItem.builder("Object", "Object", CompletionItemKind.CLASS)
                        .detail("java.lang.Object")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Root of the Java class hierarchy. Every class extends Object.")
                        .build(),

                CompletionItem.builder("Integer", "Integer", CompletionItemKind.CLASS)
                        .detail("java.lang.Integer")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive int type, providing utility methods.")
                        .build(),

                CompletionItem.builder("Double", "Double", CompletionItemKind.CLASS)
                        .detail("java.lang.Double")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive double type.")
                        .build(),

                CompletionItem.builder("Boolean", "Boolean", CompletionItemKind.CLASS)
                        .detail("java.lang.Boolean")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive boolean type.")
                        .build(),

                CompletionItem.builder("StringBuilder", "StringBuilder", CompletionItemKind.CLASS)
                        .detail("java.lang.StringBuilder")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("A mutable sequence of characters, efficient for building strings through concatenation.")
                        .example("StringBuilder sb = new StringBuilder();\nsb.append(\"Hello\").append(\" World\");")
                        .build(),

                CompletionItem.builder("StringBuffer", "StringBuffer", CompletionItemKind.CLASS)
                        .detail("java.lang.StringBuffer")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("A thread-safe mutable sequence of characters.")
                        .build(),

                CompletionItem.builder("Exception", "Exception", CompletionItemKind.CLASS)
                        .detail("java.lang.Exception")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Base class for checked exceptions that must be caught or declared.")
                        .build(),

                CompletionItem.builder("RuntimeException", "RuntimeException", CompletionItemKind.CLASS)
                        .detail("java.lang.RuntimeException")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Base class for unchecked exceptions. Common ones include NullPointerException and IllegalArgumentException.")
                        .build(),

                CompletionItem.builder("Thread", "Thread", CompletionItemKind.CLASS)
                        .detail("java.lang.Thread")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Represents a thread of execution in a Java program.")
                        .build(),

                CompletionItem.builder("Runnable", "Runnable", CompletionItemKind.INTERFACE)
                        .detail("java.lang.Runnable")
                        .owner("java.lang")
                        .category("Interface")
                        .documentation("Functional interface representing a unit of work that can be executed by a Thread.")
                        .example("Runnable r = () -> System.out.println(\"Hello\");\nnew Thread(r).start();")
                        .build(),

                // Fields
                CompletionItem.builder("out", "out", CompletionItemKind.FIELD)
                        .detail("System.out")
                        .signature("System.out")
                        .returnType("PrintStream")
                        .owner("java.lang.System")
                        .category("Field")
                        .documentation("The standard output stream, typically used for console output.")
                        .build(),

                CompletionItem.builder("in", "in", CompletionItemKind.FIELD)
                        .detail("System.in")
                        .signature("System.in")
                        .returnType("InputStream")
                        .owner("java.lang.System")
                        .category("Field")
                        .documentation("The standard input stream, typically used for reading keyboard input.")
                        .build(),

                CompletionItem.builder("err", "err", CompletionItemKind.FIELD)
                        .detail("System.err")
                        .signature("System.err")
                        .returnType("PrintStream")
                        .owner("java.lang.System")
                        .category("Field")
                        .documentation("The standard error output stream, used for printing error messages.")
                        .build(),

                CompletionItem.builder("PI", "PI", CompletionItemKind.FIELD)
                        .detail("Math.PI")
                        .signature("Math.PI")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Field")
                        .documentation("The ratio of the circumference of a circle to its diameter, approximately 3.14159.")
                        .build(),

                CompletionItem.builder("E", "E", CompletionItemKind.FIELD)
                        .detail("Math.E")
                        .signature("Math.E")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Field")
                        .documentation("The base of the natural logarithms, approximately 2.71828.")
                        .build(),

                CompletionItem.builder("MAX_VALUE", "MAX_VALUE", CompletionItemKind.FIELD)
                        .detail("Integer.MAX_VALUE")
                        .signature("Integer.MAX_VALUE")
                        .returnType("int")
                        .owner("java.lang.Integer")
                        .category("Field")
                        .documentation("A constant holding the maximum value an int can have: 2^31 - 1 = 2147483647.")
                        .build(),

                CompletionItem.builder("MIN_VALUE", "MIN_VALUE", CompletionItemKind.FIELD)
                        .detail("Integer.MIN_VALUE")
                        .signature("Integer.MIN_VALUE")
                        .returnType("int")
                        .owner("java.lang.Integer")
                        .category("Field")
                        .documentation("A constant holding the minimum value an int can have: -2^31 = -2147483648.")
                        .build(),

                // Methods
                CompletionItem.builder("println", "println", CompletionItemKind.METHOD)
                        .detail("System.out.println")
                        .signature("System.out.println(String x)")
                        .returnType("void")
                        .owner("java.io.PrintStream")
                        .category("Method")
                        .documentation("Prints a string and terminates the current line.")
                        .example("System.out.println(\"Hello\");")
                        .build(),

                CompletionItem.builder("print", "print", CompletionItemKind.METHOD)
                        .detail("System.out.print")
                        .signature("System.out.print(String x)")
                        .returnType("void")
                        .owner("java.io.PrintStream")
                        .category("Method")
                        .documentation("Prints text to standard output without a trailing newline.")
                        .build(),

                CompletionItem.builder("printf", "printf", CompletionItemKind.METHOD)
                        .detail("System.out.printf")
                        .signature("System.out.printf(String format, Object... args)")
                        .returnType("PrintStream")
                        .owner("java.io.PrintStream")
                        .category("Method")
                        .documentation("Prints a formatted string to standard output using format specifiers like %s and %d.")
                        .example("System.out.printf(\"Name: %s, Age: %d\", name, age);")
                        .build(),

                CompletionItem.builder("length", "length", CompletionItemKind.METHOD)
                        .detail("String.length")
                        .signature("String.length()")
                        .returnType("int")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the number of characters in this string.")
                        .build(),

                CompletionItem.builder("substring", "substring", CompletionItemKind.METHOD)
                        .detail("String.substring")
                        .signature("String.substring(int beginIndex)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a new string that is a substring of this string, starting at the given index.")
                        .build(),

                CompletionItem.builder("charAt", "charAt", CompletionItemKind.METHOD)
                        .detail("String.charAt")
                        .signature("String.charAt(int index)")
                        .returnType("char")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the character at the specified index in this string.")
                        .build(),

                CompletionItem.builder("equals", "equals", CompletionItemKind.METHOD)
                        .detail("String.equals")
                        .signature("String.equals(Object other)")
                        .returnType("boolean")
                        .owner("java.lang.Object")
                        .category("Method")
                        .documentation("Compares this string to another object for value equality.")
                        .build(),

                CompletionItem.builder("contains", "contains", CompletionItemKind.METHOD)
                        .detail("String.contains")
                        .signature("String.contains(CharSequence seq)")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Tests whether this string contains the given character sequence.")
                        .build(),

                CompletionItem.builder("replace", "replace", CompletionItemKind.METHOD)
                        .detail("String.replace")
                        .signature("String.replace(char oldChar, char newChar)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a new string with all occurrences of oldChar replaced by newChar.")
                        .build(),

                CompletionItem.builder("split", "split", CompletionItemKind.METHOD)
                        .detail("String.split")
                        .signature("String.split(String regex)")
                        .returnType("String[]")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Splits this string around matches of the given regular expression.")
                        .example("String[] parts = \"a,b,c\".split(\",\");")
                        .build(),

                CompletionItem.builder("trim", "trim", CompletionItemKind.METHOD)
                        .detail("String.trim")
                        .signature("String.trim()")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a copy of this string with leading and trailing whitespace removed.")
                        .build(),

                CompletionItem.builder("parseInt", "parseInt", CompletionItemKind.METHOD)
                        .detail("Integer.parseInt")
                        .signature("Integer.parseInt(String s)")
                        .returnType("int")
                        .owner("java.lang.Integer")
                        .category("Method")
                        .documentation("Parses the given string as a signed decimal integer. Throws NumberFormatException if invalid.")
                        .example("int n = Integer.parseInt(\"42\");")
                        .build(),

                CompletionItem.builder("valueOf", "valueOf", CompletionItemKind.METHOD)
                        .detail("String.valueOf")
                        .signature("String.valueOf(Object obj)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the string representation of the given object.")
                        .build(),

                CompletionItem.builder("abs", "abs", CompletionItemKind.METHOD)
                        .detail("Math.abs")
                        .signature("Math.abs(int a)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the absolute value of the argument.")
                        .build(),

                CompletionItem.builder("max", "max", CompletionItemKind.METHOD)
                        .detail("Math.max")
                        .signature("Math.max(int a, int b)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the greater of two values.")
                        .build(),

                CompletionItem.builder("min", "min", CompletionItemKind.METHOD)
                        .detail("Math.min")
                        .signature("Math.min(int a, int b)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the smaller of two values.")
                        .build(),

                CompletionItem.builder("pow", "pow", CompletionItemKind.METHOD)
                        .detail("Math.pow")
                        .signature("Math.pow(double a, double b)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the value of the first argument raised to the power of the second.")
                        .example("double result = Math.pow(2, 10);")
                        .build(),

                CompletionItem.builder("sqrt", "sqrt", CompletionItemKind.METHOD)
                        .detail("Math.sqrt")
                        .signature("Math.sqrt(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the correctly rounded positive square root of a double value.")
                        .build(),

                CompletionItem.builder("random", "random", CompletionItemKind.METHOD)
                        .detail("Math.random")
                        .signature("Math.random()")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns a double value greater than or equal to 0.0 and less than 1.0.")
                        .example("double r = Math.random();")
                        .build()
        );
    }
}
