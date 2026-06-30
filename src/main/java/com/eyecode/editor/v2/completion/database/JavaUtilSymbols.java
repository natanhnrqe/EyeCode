package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaUtilSymbols {

    private JavaUtilSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("Scanner", "Scanner", CompletionItemKind.CLASS)
                        .detail("java.util.Scanner")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Parses primitive types and strings from an input source using delimiters.")
                        .example("Scanner sc = new Scanner(System.in);\nint n = sc.nextInt();")
                        .build(),

                CompletionItem.builder("Arrays", "Arrays", CompletionItemKind.CLASS)
                        .detail("java.util.Arrays")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Provides static methods for array manipulation: sorting, searching, copying, and conversion.")
                        .build(),

                CompletionItem.builder("Collections", "Collections", CompletionItemKind.CLASS)
                        .detail("java.util.Collections")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Provides static utility methods for collections: sorting, searching, synchronization, and immutability.")
                        .build(),

                CompletionItem.builder("List", "List", CompletionItemKind.INTERFACE)
                        .detail("java.util.List")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("An ordered collection allowing duplicates with positional access.")
                        .build(),

                CompletionItem.builder("ArrayList", "ArrayList", CompletionItemKind.CLASS)
                        .detail("java.util.ArrayList")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A resizable-array implementation of the List interface.")
                        .example("List<String> list = new ArrayList<>();\nlist.add(\"item\");")
                        .build(),

                CompletionItem.builder("Map", "Map", CompletionItemKind.INTERFACE)
                        .detail("java.util.Map")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("An object that maps keys to values. A map cannot contain duplicate keys.")
                        .build(),

                CompletionItem.builder("HashMap", "HashMap", CompletionItemKind.CLASS)
                        .detail("java.util.HashMap")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Hash-table based implementation of the Map interface, allowing null keys and values.")
                        .example("Map<String, Integer> map = new HashMap<>();\nmap.put(\"key\", 1);")
                        .build(),

                CompletionItem.builder("Set", "Set", CompletionItemKind.INTERFACE)
                        .detail("java.util.Set")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("A collection that cannot contain duplicate elements.")
                        .build(),

                CompletionItem.builder("HashSet", "HashSet", CompletionItemKind.CLASS)
                        .detail("java.util.HashSet")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Hash-table based implementation of the Set interface.")
                        .build(),

                CompletionItem.builder("Optional", "Optional", CompletionItemKind.CLASS)
                        .detail("java.util.Optional")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A container that may or may not contain a non-null value, used to avoid null checks and NullPointerException.")
                        .example("Optional<String> opt = Optional.of(\"value\");\nopt.ifPresent(System.out::println);")
                        .build(),

                CompletionItem.builder("Iterator", "Iterator", CompletionItemKind.INTERFACE)
                        .detail("java.util.Iterator")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("An interface for iterating over a collection of elements.")
                        .build(),

                CompletionItem.builder("Comparator", "Comparator", CompletionItemKind.INTERFACE)
                        .detail("java.util.Comparator")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("A comparison function for ordering objects, usable with Collections.sort().")
                        .build()
        );
    }
}
