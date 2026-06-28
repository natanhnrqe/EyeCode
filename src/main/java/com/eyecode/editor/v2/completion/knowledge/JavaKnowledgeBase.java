package com.eyecode.editor.v2.completion.knowledge;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JavaKnowledgeBase {

    private static final Map<String, CompletionItem> ENTRIES = new LinkedHashMap<>();

    static {
        registerKeywords();
        registerPrimitiveTypes();
        registerLiterals();
        registerModifiers();
        registerCommonClasses();
    }

    private JavaKnowledgeBase() {}

    private static void registerKeywords() {
        String[][] keywords = {
                {"abstract", "Indicates that a class or method cannot be instantiated or has an incomplete implementation."},
                {"assert", "Tests a boolean expression, throwing AssertionError if false."},
                {"boolean", "Primitive type representing true or false values."},
                {"break", "Terminates the nearest enclosing loop or switch statement."},
                {"byte", "Primitive 8-bit signed integer type."},
                {"case", "Defines a label inside a switch statement."},
                {"catch", "Catches a specific exception type in a try block."},
                {"char", "Primitive 16-bit Unicode character type."},
                {"class", "Declares a Java class."},
                {"const", "Reserved keyword, not used in modern Java."},
                {"continue", "Skips the rest of the current loop iteration."},
                {"default", "Default case in a switch, or default method in an interface."},
                {"do", "Begins a do-while loop."},
                {"double", "Primitive 64-bit floating-point type."},
                {"else", "Alternative branch for an if statement."},
                {"enum", "Declares an enumerated type with fixed constants."},
                {"extends", "Indicates inheritance from a parent class."},
                {"final", "Prevents modification, overriding, or subclassing."},
                {"finally", "Block that always executes after a try-catch, regardless of exceptions."},
                {"float", "Primitive 32-bit floating-point type."},
                {"for", "Loop construct for iteration."},
                {"goto", "Reserved keyword, not used in Java."},
                {"if", "Conditional branch statement."},
                {"implements", "Indicates implementation of one or more interfaces."},
                {"import", "Imports a package or class for use in the current file."},
                {"instanceof", "Tests whether an object is an instance of a specific type."},
                {"int", "Primitive 32-bit signed integer type."},
                {"interface", "Declares an interface — a contract of abstract methods."},
                {"long", "Primitive 64-bit signed integer type."},
                {"native", "Indicates a method is implemented in platform-native code via JNI."},
                {"new", "Creates a new object instance."},
                {"package", "Declares the package that the current file belongs to."},
                {"private", "Access modifier visible only within the declaring class."},
                {"protected", "Access modifier visible to package and subclasses."},
                {"public", "Access modifier visible to all classes."},
                {"return", "Returns a value from a method."},
                {"short", "Primitive 16-bit signed integer type."},
                {"static", "Declares a member that belongs to the class, not an instance."},
                {"strictfp", "Ensures floating-point calculations are consistent across platforms."},
                {"super", "References the parent class instance or constructor."},
                {"switch", "Multi-branch selection statement."},
                {"synchronized", "Restricts access to a method or block to a single thread at a time."},
                {"this", "References the current object instance."},
                {"throw", "Throws an exception."},
                {"throws", "Declares the exceptions a method may throw."},
                {"transient", "Marks a field to be skipped during serialization."},
                {"try", "Begins a block with exception handling."},
                {"void", "Return type indicating no value is returned."},
                {"volatile", "Ensures a field's value is always read from main memory, not cached."},
                {"while", "Loop construct that repeats while a condition holds."},
                {"var", "Declares a local variable with inferred type (Java 10+)."},
                {"yield", "Yields a value from a switch expression (Java 14+)."},
                {"record", "Declares a concise data-carrying class with implicit constructors and accessors (Java 16+)."},
                {"sealed", "Restricts which classes can extend or implement a type (Java 17+)."},
                {"permits", "Lists the classes permitted to extend a sealed type (Java 17+)."},
        };

        for (String[] entry : keywords) {
            register(CompletionItem.builder(entry[0], entry[0], CompletionItemKind.KEYWORD)
                    .detail("Java keyword")
                    .owner("java.lang")
                    .category("Keyword")
                    .documentation(entry[1])
                    .build());
        }
    }

    private static void registerPrimitiveTypes() {
        String[][] types = {
                {"int", "Primitive 32-bit signed integer type.", "0"},
                {"long", "Primitive 64-bit signed integer type.", "0L"},
                {"double", "Primitive 64-bit floating-point type.", "0.0"},
                {"float", "Primitive 32-bit floating-point type.", "0.0f"},
                {"boolean", "Primitive type representing true or false.", "false"},
                {"char", "Primitive 16-bit Unicode character type.", "'\\0'"},
                {"byte", "Primitive 8-bit signed integer type.", "0"},
                {"short", "Primitive 16-bit signed integer type.", "0"},
                {"void", "Return type indicating no value is returned.", "—"},
        };

        for (String[] entry : types) {
            register(CompletionItem.builder(entry[0], entry[0], CompletionItemKind.VARIABLE)
                    .detail("Primitive type")
                    .owner("java.lang")
                    .category("Primitive")
                    .documentation(entry[1])
                    .example("Default value: " + entry[2])
                    .build());
        }
    }

    private static void registerLiterals() {
        String[][] literals = {
                {"true", "Boolean literal representing the value true."},
                {"false", "Boolean literal representing the value false."},
                {"null", "Literal representing a null reference — no object."},
        };

        for (String[] entry : literals) {
            register(CompletionItem.builder(entry[0], entry[0], CompletionItemKind.VARIABLE)
                    .detail("Literal")
                    .owner("java.lang")
                    .category("Literal")
                    .documentation(entry[1])
                    .build());
        }
    }

    private static void registerModifiers() {
        String[][] modifiers = {
                {"public", "Access modifier visible to all classes."},
                {"private", "Access modifier visible only within the declaring class."},
                {"protected", "Access modifier visible to package and subclasses."},
                {"static", "Declares a member that belongs to the class, not an instance."},
                {"final", "Prevents modification, overriding, or subclassing."},
                {"abstract", "Indicates that a class or method cannot be instantiated or has an incomplete implementation."},
                {"synchronized", "Restricts access to a method or block to a single thread at a time."},
                {"volatile", "Ensures a field's value is always read from main memory."},
                {"transient", "Marks a field to be skipped during serialization."},
                {"native", "Indicates a method is implemented in platform-native code via JNI."},
                {"strictfp", "Ensures floating-point calculations are consistent across platforms."},
                {"default", "Default case in a switch, or default method in an interface."},
        };

        for (String[] entry : modifiers) {
            if (!ENTRIES.containsKey(entry[0])) {
                register(CompletionItem.builder(entry[0], entry[0], CompletionItemKind.KEYWORD)
                        .detail("Modifier")
                        .owner("java.lang")
                        .category("Modifier")
                        .documentation(entry[1])
                        .build());
            }
        }
    }

    private static void registerCommonClasses() {
        register(CompletionItem.builder("System", "System", CompletionItemKind.CLASS)
                .detail("java.lang.System")
                .owner("java.lang")
                .category("Class")
                .documentation("Provides access to system resources and environment: standard input, output, error streams, and system properties.")
                .build());

        register(CompletionItem.builder("String", "String", CompletionItemKind.CLASS)
                .detail("java.lang.String")
                .owner("java.lang")
                .category("Class")
                .documentation("Represents an immutable sequence of characters.")
                .build());

        register(CompletionItem.builder("Math", "Math", CompletionItemKind.CLASS)
                .detail("java.lang.Math")
                .owner("java.lang")
                .category("Class")
                .documentation("Provides mathematical functions and constants like E and PI.")
                .build());

        register(CompletionItem.builder("Object", "Object", CompletionItemKind.CLASS)
                .detail("java.lang.Object")
                .owner("java.lang")
                .category("Class")
                .documentation("Root of the Java class hierarchy. Every class extends Object.")
                .build());

        register(CompletionItem.builder("Integer", "Integer", CompletionItemKind.CLASS)
                .detail("java.lang.Integer")
                .owner("java.lang")
                .category("Class")
                .documentation("Wrapper class for the primitive int type, providing utility methods.")
                .build());

        register(CompletionItem.builder("Double", "Double", CompletionItemKind.CLASS)
                .detail("java.lang.Double")
                .owner("java.lang")
                .category("Class")
                .documentation("Wrapper class for the primitive double type.")
                .build());

        register(CompletionItem.builder("Boolean", "Boolean", CompletionItemKind.CLASS)
                .detail("java.lang.Boolean")
                .owner("java.lang")
                .category("Class")
                .documentation("Wrapper class for the primitive boolean type.")
                .build());

        register(CompletionItem.builder("Scanner", "Scanner", CompletionItemKind.CLASS)
                .detail("java.util.Scanner")
                .owner("java.util")
                .category("Class")
                .documentation("Parses primitive types and strings from an input source using delimiters.")
                .example("Scanner sc = new Scanner(System.in);\nint n = sc.nextInt();")
                .build());

        register(CompletionItem.builder("Arrays", "Arrays", CompletionItemKind.CLASS)
                .detail("java.util.Arrays")
                .owner("java.util")
                .category("Class")
                .documentation("Provides static methods for array manipulation: sorting, searching, copying, and conversion.")
                .build());

        register(CompletionItem.builder("Collections", "Collections", CompletionItemKind.CLASS)
                .detail("java.util.Collections")
                .owner("java.util")
                .category("Class")
                .documentation("Provides static utility methods for collections: sorting, searching, synchronization, and immutability.")
                .build());

        register(CompletionItem.builder("List", "List", CompletionItemKind.INTERFACE)
                .detail("java.util.List")
                .owner("java.util")
                .category("Interface")
                .documentation("An ordered collection allowing duplicates with positional access.")
                .build());

        register(CompletionItem.builder("ArrayList", "ArrayList", CompletionItemKind.CLASS)
                .detail("java.util.ArrayList")
                .owner("java.util")
                .category("Class")
                .documentation("A resizable-array implementation of the List interface.")
                .example("List<String> list = new ArrayList<>();\nlist.add(\"item\");")
                .build());

        register(CompletionItem.builder("Map", "Map", CompletionItemKind.INTERFACE)
                .detail("java.util.Map")
                .owner("java.util")
                .category("Interface")
                .documentation("An object that maps keys to values. A map cannot contain duplicate keys.")
                .build());

        register(CompletionItem.builder("HashMap", "HashMap", CompletionItemKind.CLASS)
                .detail("java.util.HashMap")
                .owner("java.util")
                .category("Class")
                .documentation("Hash-table based implementation of the Map interface, allowing null keys and values.")
                .example("Map<String, Integer> map = new HashMap<>();\nmap.put(\"key\", 1);")
                .build());

        register(CompletionItem.builder("Set", "Set", CompletionItemKind.INTERFACE)
                .detail("java.util.Set")
                .owner("java.util")
                .category("Interface")
                .documentation("A collection that cannot contain duplicate elements.")
                .build());

        register(CompletionItem.builder("HashSet", "HashSet", CompletionItemKind.CLASS)
                .detail("java.util.HashSet")
                .owner("java.util")
                .category("Class")
                .documentation("Hash-table based implementation of the Set interface.")
                .build());

        register(CompletionItem.builder("Exception", "Exception", CompletionItemKind.CLASS)
                .detail("java.lang.Exception")
                .owner("java.lang")
                .category("Class")
                .documentation("Base class for checked exceptions that must be caught or declared.")
                .build());

        register(CompletionItem.builder("RuntimeException", "RuntimeException", CompletionItemKind.CLASS)
                .detail("java.lang.RuntimeException")
                .owner("java.lang")
                .category("Class")
                .documentation("Base class for unchecked exceptions. Common ones include NullPointerException and IllegalArgumentException.")
                .build());

        register(CompletionItem.builder("Thread", "Thread", CompletionItemKind.CLASS)
                .detail("java.lang.Thread")
                .owner("java.lang")
                .category("Class")
                .documentation("Represents a thread of execution in a Java program.")
                .build());

        register(CompletionItem.builder("Runnable", "Runnable", CompletionItemKind.INTERFACE)
                .detail("java.lang.Runnable")
                .owner("java.lang")
                .category("Interface")
                .documentation("Functional interface representing a unit of work that can be executed by a Thread.")
                .example("Runnable r = () -> System.out.println(\"Hello\");\nnew Thread(r).start();")
                .build());

        register(CompletionItem.builder("Optional", "Optional", CompletionItemKind.CLASS)
                .detail("java.util.Optional")
                .owner("java.util")
                .category("Class")
                .documentation("A container that may or may not contain a non-null value, used to avoid null checks and NullPointerException.")
                .example("Optional<String> opt = Optional.of(\"value\");\nopt.ifPresent(System.out::println);")
                .build());

        register(CompletionItem.builder("StringBuilder", "StringBuilder", CompletionItemKind.CLASS)
                .detail("java.lang.StringBuilder")
                .owner("java.lang")
                .category("Class")
                .documentation("A mutable sequence of characters, efficient for building strings through concatenation.")
                .example("StringBuilder sb = new StringBuilder();\nsb.append(\"Hello\").append(\" World\");")
                .build());

        register(CompletionItem.builder("StringBuffer", "StringBuffer", CompletionItemKind.CLASS)
                .detail("java.lang.StringBuffer")
                .owner("java.lang")
                .category("Class")
                .documentation("A thread-safe mutable sequence of characters.")
                .build());
    }

    private static void register(CompletionItem item) {
        if (item != null && item.getLabel() != null) {
            ENTRIES.put(item.getLabel(), item);
        }
    }

    public static List<CompletionItem> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES.values()));
    }

    public static CompletionItem get(String label) {
        return ENTRIES.get(label);
    }

    public static List<CompletionItem> findByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompletionItem> result = new ArrayList<>();
        for (CompletionItem item : ENTRIES.values()) {
            if (item.getLabel().startsWith(prefix)) {
                result.add(item);
            }
        }
        return result;
    }

    public static int size() {
        return ENTRIES.size();
    }

    public static void contribute(CompletionItem item) {
        register(item);
    }

    public static void contributeAll(List<CompletionItem> items) {
        if (items != null) {
            for (CompletionItem item : items) {
                register(item);
            }
        }
    }
}
