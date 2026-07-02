package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaUtilSymbols {

    private JavaUtilSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                // ── Classes / Interfaces ──
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

                CompletionItem.builder("LinkedList", "LinkedList", CompletionItemKind.CLASS)
                        .detail("java.util.LinkedList")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A doubly-linked list implementation of List and Deque, efficient for insertions and removals at both ends.")
                        .build(),

                CompletionItem.builder("Vector", "Vector", CompletionItemKind.CLASS)
                        .detail("java.util.Vector")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A synchronized, growable array of objects implements List. Legacy class from Java 1.0.")
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

                CompletionItem.builder("LinkedHashMap", "LinkedHashMap", CompletionItemKind.CLASS)
                        .detail("java.util.LinkedHashMap")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Hash-table and linked-list implementation of Map with predictable iteration order (insertion-order or access-order).")
                        .build(),

                CompletionItem.builder("TreeMap", "TreeMap", CompletionItemKind.CLASS)
                        .detail("java.util.TreeMap")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A Red-Black tree based NavigableMap implementation sorted by keys (natural order or Comparator).")
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

                CompletionItem.builder("LinkedHashSet", "LinkedHashSet", CompletionItemKind.CLASS)
                        .detail("java.util.LinkedHashSet")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Hash-table and linked-list implementation of Set with predictable iteration order.")
                        .build(),

                CompletionItem.builder("TreeSet", "TreeSet", CompletionItemKind.CLASS)
                        .detail("java.util.TreeSet")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A NavigableSet implementation backed by a TreeMap, elements sorted by natural order or Comparator.")
                        .build(),

                CompletionItem.builder("Queue", "Queue", CompletionItemKind.INTERFACE)
                        .detail("java.util.Queue")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("A collection designed for holding elements prior to processing, typically FIFO order.")
                        .build(),

                CompletionItem.builder("Deque", "Deque", CompletionItemKind.INTERFACE)
                        .detail("java.util.Deque")
                        .owner("java.util")
                        .category("Interface")
                        .documentation("A linear collection that supports element insertion and removal at both ends (double-ended queue).")
                        .build(),

                CompletionItem.builder("PriorityQueue", "PriorityQueue", CompletionItemKind.CLASS)
                        .detail("java.util.PriorityQueue")
                        .owner("java.util")
                        .category("Class")
                        .documentation("An unbounded priority queue based on a priority heap, ordered by natural order or Comparator.")
                        .build(),

                CompletionItem.builder("Stack", "Stack", CompletionItemKind.CLASS)
                        .detail("java.util.Stack")
                        .owner("java.util")
                        .category("Class")
                        .documentation("A LIFO (last-in-first-out) stack of objects, extending Vector with push/pop/peek operations.")
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
                        .build(),

                CompletionItem.builder("Objects", "Objects", CompletionItemKind.CLASS)
                        .detail("java.util.Objects")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Provides static utility methods for objects: null-checking, hashing, comparison, and toString.")
                        .build(),

                CompletionItem.builder("UUID", "UUID", CompletionItemKind.CLASS)
                        .detail("java.util.UUID")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Represents an immutable universally unique identifier (UUID), 128-bit value.")
                        .example("UUID id = UUID.randomUUID();")
                        .build(),

                CompletionItem.builder("Random", "Random", CompletionItemKind.CLASS)
                        .detail("java.util.Random")
                        .owner("java.util")
                        .category("Class")
                        .documentation("Generates pseudo-random numbers with various distributions: int, long, double, Gaussian.")
                        .example("Random rng = new Random();\nint n = rng.nextInt(100);")
                        .build(),

                // ── ArrayList Methods ──
                CompletionItem.builder("add", "add", CompletionItemKind.METHOD)
                        .detail("ArrayList.add")
                        .signature("ArrayList.add(E element)")
                        .returnType("boolean")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Appends the specified element to the end of this list.")
                        .build(),

                CompletionItem.builder("get", "get", CompletionItemKind.METHOD)
                        .detail("ArrayList.get")
                        .signature("ArrayList.get(int index)")
                        .returnType("E")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Returns the element at the specified position in this list.")
                        .build(),

                CompletionItem.builder("remove", "remove", CompletionItemKind.METHOD)
                        .detail("ArrayList.remove")
                        .signature("ArrayList.remove(int index)")
                        .returnType("E")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Removes the element at the specified position from this list.")
                        .build(),

                CompletionItem.builder("size", "size", CompletionItemKind.METHOD)
                        .detail("ArrayList.size")
                        .signature("ArrayList.size()")
                        .returnType("int")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Returns the number of elements in this list.")
                        .build(),

                CompletionItem.builder("clear", "clear", CompletionItemKind.METHOD)
                        .detail("ArrayList.clear")
                        .signature("ArrayList.clear()")
                        .returnType("void")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Removes all elements from this list.")
                        .build(),

                CompletionItem.builder("contains", "contains", CompletionItemKind.METHOD)
                        .detail("ArrayList.contains")
                        .signature("ArrayList.contains(Object o)")
                        .returnType("boolean")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Tests if this list contains the specified element.")
                        .build(),

                CompletionItem.builder("indexOf", "indexOf", CompletionItemKind.METHOD)
                        .detail("ArrayList.indexOf")
                        .signature("ArrayList.indexOf(Object o)")
                        .returnType("int")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Returns the index of the first occurrence of the element, or -1 if not found.")
                        .build(),

                CompletionItem.builder("isEmpty", "isEmpty", CompletionItemKind.METHOD)
                        .detail("ArrayList.isEmpty")
                        .signature("ArrayList.isEmpty()")
                        .returnType("boolean")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Tests if this list contains no elements.")
                        .build(),

                CompletionItem.builder("set", "set", CompletionItemKind.METHOD)
                        .detail("ArrayList.set")
                        .signature("ArrayList.set(int index, E element)")
                        .returnType("E")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Replaces the element at the specified position with the specified element.")
                        .build(),

                CompletionItem.builder("sort", "sort", CompletionItemKind.METHOD)
                        .detail("ArrayList.sort")
                        .signature("ArrayList.sort(Comparator<? super E> c)")
                        .returnType("void")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Sorts the list according to the specified Comparator.")
                        .build(),

                CompletionItem.builder("forEach", "forEach", CompletionItemKind.METHOD)
                        .detail("ArrayList.forEach")
                        .signature("ArrayList.forEach(Consumer<? super E> action)")
                        .returnType("void")
                        .owner("java.util.ArrayList")
                        .category("Method")
                        .documentation("Performs the given action for each element in the list.")
                        .build(),

                // ── HashMap Methods ──
                CompletionItem.builder("put", "put", CompletionItemKind.METHOD)
                        .detail("HashMap.put")
                        .signature("HashMap.put(K key, V value)")
                        .returnType("V")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Associates the specified value with the specified key in the map.")
                        .build(),

                CompletionItem.builder("get", "get", CompletionItemKind.METHOD)
                        .detail("HashMap.get")
                        .signature("HashMap.get(Object key)")
                        .returnType("V")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Returns the value mapped to the key, or null if no mapping exists.")
                        .build(),

                CompletionItem.builder("containsKey", "containsKey", CompletionItemKind.METHOD)
                        .detail("HashMap.containsKey")
                        .signature("HashMap.containsKey(Object key)")
                        .returnType("boolean")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Tests if the map contains a mapping for the specified key.")
                        .build(),

                CompletionItem.builder("containsValue", "containsValue", CompletionItemKind.METHOD)
                        .detail("HashMap.containsValue")
                        .signature("HashMap.containsValue(Object value)")
                        .returnType("boolean")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Tests if the map maps one or more keys to the specified value.")
                        .build(),

                CompletionItem.builder("remove", "remove", CompletionItemKind.METHOD)
                        .detail("HashMap.remove")
                        .signature("HashMap.remove(Object key)")
                        .returnType("V")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Removes the mapping for the specified key if present.")
                        .build(),

                CompletionItem.builder("keySet", "keySet", CompletionItemKind.METHOD)
                        .detail("HashMap.keySet")
                        .signature("HashMap.keySet()")
                        .returnType("Set<K>")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Returns a Set view of the keys contained in this map.")
                        .build(),

                CompletionItem.builder("values", "values", CompletionItemKind.METHOD)
                        .detail("HashMap.values")
                        .signature("HashMap.values()")
                        .returnType("Collection<V>")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Returns a Collection view of the values contained in this map.")
                        .build(),

                CompletionItem.builder("entrySet", "entrySet", CompletionItemKind.METHOD)
                        .detail("HashMap.entrySet")
                        .signature("HashMap.entrySet()")
                        .returnType("Set<Map.Entry<K,V>>")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Returns a Set view of the key-value mappings in this map.")
                        .build(),

                CompletionItem.builder("size", "size", CompletionItemKind.METHOD)
                        .detail("HashMap.size")
                        .signature("HashMap.size()")
                        .returnType("int")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Returns the number of key-value mappings in this map.")
                        .build(),

                CompletionItem.builder("getOrDefault", "getOrDefault", CompletionItemKind.METHOD)
                        .detail("HashMap.getOrDefault")
                        .signature("HashMap.getOrDefault(Object key, V defaultValue)")
                        .returnType("V")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Returns the value mapped to the key, or defaultValue if no mapping exists (Java 8+).")
                        .build(),

                CompletionItem.builder("putIfAbsent", "putIfAbsent", CompletionItemKind.METHOD)
                        .detail("HashMap.putIfAbsent")
                        .signature("HashMap.putIfAbsent(K key, V value)")
                        .returnType("V")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Inserts the mapping only if the key is not already present (Java 8+).")
                        .build(),

                CompletionItem.builder("forEach", "forEach", CompletionItemKind.METHOD)
                        .detail("HashMap.forEach")
                        .signature("HashMap.forEach(BiConsumer<? super K, ? super V> action)")
                        .returnType("void")
                        .owner("java.util.HashMap")
                        .category("Method")
                        .documentation("Performs the given action for each entry in the map (Java 8+).")
                        .build(),

                // ── Optional Methods ──
                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("Optional.of")
                        .signature("Optional.of(T value)")
                        .returnType("Optional<T>")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Returns an Optional describing the given non-null value, or throws NullPointerException if null.")
                        .build(),

                CompletionItem.builder("ofNullable", "ofNullable", CompletionItemKind.METHOD)
                        .detail("Optional.ofNullable")
                        .signature("Optional.ofNullable(T value)")
                        .returnType("Optional<T>")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Returns an Optional describing the value, or an empty Optional if null.")
                        .build(),

                CompletionItem.builder("empty", "empty", CompletionItemKind.METHOD)
                        .detail("Optional.empty")
                        .signature("Optional.empty()")
                        .returnType("Optional<T>")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Returns an empty Optional instance (no value present).")
                        .build(),

                CompletionItem.builder("isPresent", "isPresent", CompletionItemKind.METHOD)
                        .detail("Optional.isPresent")
                        .signature("Optional.isPresent()")
                        .returnType("boolean")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Tests if a value is present in this Optional.")
                        .build(),

                CompletionItem.builder("isEmpty", "isEmpty", CompletionItemKind.METHOD)
                        .detail("Optional.isEmpty")
                        .signature("Optional.isEmpty()")
                        .returnType("boolean")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Tests if no value is present in this Optional (Java 11+).")
                        .build(),

                CompletionItem.builder("ifPresent", "ifPresent", CompletionItemKind.METHOD)
                        .detail("Optional.ifPresent")
                        .signature("Optional.ifPresent(Consumer<? super T> consumer)")
                        .returnType("void")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Performs the action with the value if present, otherwise does nothing.")
                        .build(),

                CompletionItem.builder("orElse", "orElse", CompletionItemKind.METHOD)
                        .detail("Optional.orElse")
                        .signature("Optional.orElse(T other)")
                        .returnType("T")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Returns the value if present, otherwise returns other.")
                        .build(),

                CompletionItem.builder("orElseGet", "orElseGet", CompletionItemKind.METHOD)
                        .detail("Optional.orElseGet")
                        .signature("Optional.orElseGet(Supplier<? extends T> supplier)")
                        .returnType("T")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Returns the value if present, otherwise invokes the supplier and returns the result.")
                        .build(),

                CompletionItem.builder("orElseThrow", "orElseThrow", CompletionItemKind.METHOD)
                        .detail("Optional.orElseThrow")
                        .signature("Optional.orElseThrow()")
                        .returnType("T")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Returns the value if present, otherwise throws NoSuchElementException.")
                        .build(),

                CompletionItem.builder("map", "map", CompletionItemKind.METHOD)
                        .detail("Optional.map")
                        .signature("Optional.map(Function<? super T, ? extends U> mapper)")
                        .returnType("Optional<U>")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Applies the mapper function to the value if present, returning an Optional of the result.")
                        .build(),

                CompletionItem.builder("filter", "filter", CompletionItemKind.METHOD)
                        .detail("Optional.filter")
                        .signature("Optional.filter(Predicate<? super T> predicate)")
                        .returnType("Optional<T>")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("If a value is present and matches the predicate, returns an Optional describing it; otherwise empty.")
                        .build(),

                CompletionItem.builder("flatMap", "flatMap", CompletionItemKind.METHOD)
                        .detail("Optional.flatMap")
                        .signature("Optional.flatMap(Function<? super T, Optional<U>> mapper)")
                        .returnType("Optional<U>")
                        .owner("java.util.Optional")
                        .category("Method")
                        .documentation("Applies the mapper function to the value if present, returning the Optional result directly.")
                        .build(),

                // ── LinkedList Methods ──
                CompletionItem.builder("addFirst", "addFirst", CompletionItemKind.METHOD)
                        .detail("LinkedList.addFirst")
                        .signature("LinkedList.addFirst(E e)")
                        .returnType("void")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Inserts the element at the front of this list.")
                        .build(),

                CompletionItem.builder("addLast", "addLast", CompletionItemKind.METHOD)
                        .detail("LinkedList.addLast")
                        .signature("LinkedList.addLast(E e)")
                        .returnType("void")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Appends the element to the end of this list.")
                        .build(),

                CompletionItem.builder("getFirst", "getFirst", CompletionItemKind.METHOD)
                        .detail("LinkedList.getFirst")
                        .signature("LinkedList.getFirst()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Returns the first element in this list.")
                        .build(),

                CompletionItem.builder("getLast", "getLast", CompletionItemKind.METHOD)
                        .detail("LinkedList.getLast")
                        .signature("LinkedList.getLast()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Returns the last element in this list.")
                        .build(),

                CompletionItem.builder("removeFirst", "removeFirst", CompletionItemKind.METHOD)
                        .detail("LinkedList.removeFirst")
                        .signature("LinkedList.removeFirst()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Removes and returns the first element from this list.")
                        .build(),

                CompletionItem.builder("removeLast", "removeLast", CompletionItemKind.METHOD)
                        .detail("LinkedList.removeLast")
                        .signature("LinkedList.removeLast()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Removes and returns the last element from this list.")
                        .build(),

                CompletionItem.builder("offer", "offer", CompletionItemKind.METHOD)
                        .detail("LinkedList.offer")
                        .signature("LinkedList.offer(E e)")
                        .returnType("boolean")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Adds the element as the tail (last element) of this list.")
                        .build(),

                CompletionItem.builder("poll", "poll", CompletionItemKind.METHOD)
                        .detail("LinkedList.poll")
                        .signature("LinkedList.poll()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Retrieves and removes the head (first element) of this list, or returns null if empty.")
                        .build(),

                CompletionItem.builder("peek", "peek", CompletionItemKind.METHOD)
                        .detail("LinkedList.peek")
                        .signature("LinkedList.peek()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Retrieves but does not remove the head (first element) of this list, or returns null if empty.")
                        .build(),

                CompletionItem.builder("push", "push", CompletionItemKind.METHOD)
                        .detail("LinkedList.push")
                        .signature("LinkedList.push(E e)")
                        .returnType("void")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Pushes an element onto the front of this list (stack operation).")
                        .build(),

                CompletionItem.builder("pop", "pop", CompletionItemKind.METHOD)
                        .detail("LinkedList.pop")
                        .signature("LinkedList.pop()")
                        .returnType("E")
                        .owner("java.util.LinkedList")
                        .category("Method")
                        .documentation("Pops (removes and returns) the first element of this list (stack operation).")
                        .build(),

                // ── TreeMap Methods ──
                CompletionItem.builder("firstKey", "firstKey", CompletionItemKind.METHOD)
                        .detail("TreeMap.firstKey")
                        .signature("TreeMap.firstKey()")
                        .returnType("K")
                        .owner("java.util.TreeMap")
                        .category("Method")
                        .documentation("Returns the first (lowest) key currently in this map.")
                        .build(),

                CompletionItem.builder("lastKey", "lastKey", CompletionItemKind.METHOD)
                        .detail("TreeMap.lastKey")
                        .signature("TreeMap.lastKey()")
                        .returnType("K")
                        .owner("java.util.TreeMap")
                        .category("Method")
                        .documentation("Returns the last (highest) key currently in this map.")
                        .build(),

                CompletionItem.builder("ceilingKey", "ceilingKey", CompletionItemKind.METHOD)
                        .detail("TreeMap.ceilingKey")
                        .signature("TreeMap.ceilingKey(K key)")
                        .returnType("K")
                        .owner("java.util.TreeMap")
                        .category("Method")
                        .documentation("Returns the least key greater than or equal to the given key, or null if none.")
                        .build(),

                CompletionItem.builder("floorKey", "floorKey", CompletionItemKind.METHOD)
                        .detail("TreeMap.floorKey")
                        .signature("TreeMap.floorKey(K key)")
                        .returnType("K")
                        .owner("java.util.TreeMap")
                        .category("Method")
                        .documentation("Returns the greatest key less than or equal to the given key, or null if none.")
                        .build(),

                CompletionItem.builder("subMap", "subMap", CompletionItemKind.METHOD)
                        .detail("TreeMap.subMap")
                        .signature("TreeMap.subMap(K fromKey, K toKey)")
                        .returnType("SortedMap<K,V>")
                        .owner("java.util.TreeMap")
                        .category("Method")
                        .documentation("Returns a view of the portion of this map from fromKey (inclusive) to toKey (exclusive).")
                        .build(),

                // ── TreeSet Methods ──
                CompletionItem.builder("first", "first", CompletionItemKind.METHOD)
                        .detail("TreeSet.first")
                        .signature("TreeSet.first()")
                        .returnType("E")
                        .owner("java.util.TreeSet")
                        .category("Method")
                        .documentation("Returns the first (lowest) element currently in this set.")
                        .build(),

                CompletionItem.builder("last", "last", CompletionItemKind.METHOD)
                        .detail("TreeSet.last")
                        .signature("TreeSet.last()")
                        .returnType("E")
                        .owner("java.util.TreeSet")
                        .category("Method")
                        .documentation("Returns the last (highest) element currently in this set.")
                        .build(),

                CompletionItem.builder("ceiling", "ceiling", CompletionItemKind.METHOD)
                        .detail("TreeSet.ceiling")
                        .signature("TreeSet.ceiling(E e)")
                        .returnType("E")
                        .owner("java.util.TreeSet")
                        .category("Method")
                        .documentation("Returns the least element greater than or equal to the given element, or null if none.")
                        .build(),

                CompletionItem.builder("floor", "floor", CompletionItemKind.METHOD)
                        .detail("TreeSet.floor")
                        .signature("TreeSet.floor(E e)")
                        .returnType("E")
                        .owner("java.util.TreeSet")
                        .category("Method")
                        .documentation("Returns the greatest element less than or equal to the given element, or null if none.")
                        .build(),

                CompletionItem.builder("subSet", "subSet", CompletionItemKind.METHOD)
                        .detail("TreeSet.subSet")
                        .signature("TreeSet.subSet(E fromElement, E toElement)")
                        .returnType("SortedSet<E>")
                        .owner("java.util.TreeSet")
                        .category("Method")
                        .documentation("Returns a view of the portion of this set from fromElement (inclusive) to toElement (exclusive).")
                        .build(),

                // ── Collections Methods ──
                CompletionItem.builder("sort", "sort", CompletionItemKind.METHOD)
                        .detail("Collections.sort")
                        .signature("Collections.sort(List<T> list)")
                        .returnType("void")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Sorts the specified list into ascending natural order.")
                        .build(),

                CompletionItem.builder("binarySearch", "binarySearch", CompletionItemKind.METHOD)
                        .detail("Collections.binarySearch")
                        .signature("Collections.binarySearch(List<? extends Comparable<? super T>> list, T key)")
                        .returnType("int")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Searches a sorted list for a key using binary search.")
                        .build(),

                CompletionItem.builder("reverse", "reverse", CompletionItemKind.METHOD)
                        .detail("Collections.reverse")
                        .signature("Collections.reverse(List<?> list)")
                        .returnType("void")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Reverses the order of elements in the specified list.")
                        .build(),

                CompletionItem.builder("shuffle", "shuffle", CompletionItemKind.METHOD)
                        .detail("Collections.shuffle")
                        .signature("Collections.shuffle(List<?> list)")
                        .returnType("void")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Randomly permutes the elements in the specified list using a default random source.")
                        .build(),

                CompletionItem.builder("fill", "fill", CompletionItemKind.METHOD)
                        .detail("Collections.fill")
                        .signature("Collections.fill(List<? super T> list, T obj)")
                        .returnType("void")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Replaces all elements of the list with the specified element.")
                        .build(),

                CompletionItem.builder("copy", "copy", CompletionItemKind.METHOD)
                        .detail("Collections.copy")
                        .signature("Collections.copy(List<? super T> dest, List<? extends T> src)")
                        .returnType("void")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Copies elements from source list to destination list.")
                        .build(),

                CompletionItem.builder("swap", "swap", CompletionItemKind.METHOD)
                        .detail("Collections.swap")
                        .signature("Collections.swap(List<?> list, int i, int j)")
                        .returnType("void")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Swaps the elements at the specified positions in the list.")
                        .build(),

                CompletionItem.builder("addAll", "addAll", CompletionItemKind.METHOD)
                        .detail("Collections.addAll")
                        .signature("Collections.addAll(Collection<? super T> c, T... elements)")
                        .returnType("boolean")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Adds all of the specified elements to the given collection.")
                        .build(),

                CompletionItem.builder("frequency", "frequency", CompletionItemKind.METHOD)
                        .detail("Collections.frequency")
                        .signature("Collections.frequency(Collection<?> c, Object o)")
                        .returnType("int")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns the number of elements in the collection equal to the specified object.")
                        .build(),

                CompletionItem.builder("disjoint", "disjoint", CompletionItemKind.METHOD)
                        .detail("Collections.disjoint")
                        .signature("Collections.disjoint(Collection<?> c1, Collection<?> c2)")
                        .returnType("boolean")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns true if the two collections have no elements in common.")
                        .build(),

                CompletionItem.builder("max", "max", CompletionItemKind.METHOD)
                        .detail("Collections.max")
                        .signature("Collections.max(Collection<? extends T> coll)")
                        .returnType("T")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns the maximum element of the collection according to natural order.")
                        .build(),

                CompletionItem.builder("min", "min", CompletionItemKind.METHOD)
                        .detail("Collections.min")
                        .signature("Collections.min(Collection<? extends T> coll)")
                        .returnType("T")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns the minimum element of the collection according to natural order.")
                        .build(),

                CompletionItem.builder("unmodifiableList", "unmodifiableList", CompletionItemKind.METHOD)
                        .detail("Collections.unmodifiableList")
                        .signature("Collections.unmodifiableList(List<? extends T> list)")
                        .returnType("List<T>")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns an unmodifiable view of the specified list.")
                        .build(),

                CompletionItem.builder("synchronizedList", "synchronizedList", CompletionItemKind.METHOD)
                        .detail("Collections.synchronizedList")
                        .signature("Collections.synchronizedList(List<T> list)")
                        .returnType("List<T>")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns a synchronized (thread-safe) list backed by the specified list.")
                        .build(),

                CompletionItem.builder("emptyList", "emptyList", CompletionItemKind.METHOD)
                        .detail("Collections.emptyList")
                        .signature("Collections.emptyList()")
                        .returnType("List<T>")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns an empty immutable list.")
                        .build(),

                CompletionItem.builder("singletonList", "singletonList", CompletionItemKind.METHOD)
                        .detail("Collections.singletonList")
                        .signature("Collections.singletonList(T o)")
                        .returnType("List<T>")
                        .owner("java.util.Collections")
                        .category("Method")
                        .documentation("Returns an immutable list containing only the specified object.")
                        .build(),

                // ── Arrays Methods ──
                CompletionItem.builder("asList", "asList", CompletionItemKind.METHOD)
                        .detail("Arrays.asList")
                        .signature("Arrays.asList(T... a)")
                        .returnType("List<T>")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Returns a fixed-size list backed by the specified array.")
                        .example("List<String> list = Arrays.asList(\"a\", \"b\", \"c\");")
                        .build(),

                CompletionItem.builder("sort", "sort", CompletionItemKind.METHOD)
                        .detail("Arrays.sort")
                        .signature("Arrays.sort(int[] a)")
                        .returnType("void")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Sorts the specified array into ascending natural order.")
                        .build(),

                CompletionItem.builder("binarySearch", "binarySearch", CompletionItemKind.METHOD)
                        .detail("Arrays.binarySearch")
                        .signature("Arrays.binarySearch(int[] a, int key)")
                        .returnType("int")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Searches a sorted array for a key using binary search.")
                        .build(),

                CompletionItem.builder("fill", "fill", CompletionItemKind.METHOD)
                        .detail("Arrays.fill")
                        .signature("Arrays.fill(int[] a, int val)")
                        .returnType("void")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Assigns the specified value to each element of the array.")
                        .build(),

                CompletionItem.builder("copyOf", "copyOf", CompletionItemKind.METHOD)
                        .detail("Arrays.copyOf")
                        .signature("Arrays.copyOf(int[] original, int newLength)")
                        .returnType("int[]")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Copies the array, truncating or padding with zeros as needed.")
                        .build(),

                CompletionItem.builder("copyOfRange", "copyOfRange", CompletionItemKind.METHOD)
                        .detail("Arrays.copyOfRange")
                        .signature("Arrays.copyOfRange(int[] original, int from, int to)")
                        .returnType("int[]")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Copies the specified range of the array into a new array.")
                        .build(),

                CompletionItem.builder("toString", "toString", CompletionItemKind.METHOD)
                        .detail("Arrays.toString")
                        .signature("Arrays.toString(int[] a)")
                        .returnType("String")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Returns a string representation of the array contents.")
                        .build(),

                CompletionItem.builder("deepToString", "deepToString", CompletionItemKind.METHOD)
                        .detail("Arrays.deepToString")
                        .signature("Arrays.deepToString(Object[] a)")
                        .returnType("String")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Returns a string representation of the nested array contents.")
                        .build(),

                CompletionItem.builder("equals", "equals", CompletionItemKind.METHOD)
                        .detail("Arrays.equals")
                        .signature("Arrays.equals(int[] a, int[] a2)")
                        .returnType("boolean")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Tests if two arrays are equal element-by-element.")
                        .build(),

                CompletionItem.builder("deepEquals", "deepEquals", CompletionItemKind.METHOD)
                        .detail("Arrays.deepEquals")
                        .signature("Arrays.deepEquals(Object[] a1, Object[] a2)")
                        .returnType("boolean")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Tests if two nested arrays are deeply equal.")
                        .build(),

                CompletionItem.builder("stream", "stream", CompletionItemKind.METHOD)
                        .detail("Arrays.stream")
                        .signature("Arrays.stream(int[] array)")
                        .returnType("IntStream")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Creates a sequential IntStream from the array (Java 8+).")
                        .build(),

                CompletionItem.builder("parallelSort", "parallelSort", CompletionItemKind.METHOD)
                        .detail("Arrays.parallelSort")
                        .signature("Arrays.parallelSort(int[] a)")
                        .returnType("void")
                        .owner("java.util.Arrays")
                        .category("Method")
                        .documentation("Sorts the array using parallel sort for better performance on large arrays (Java 8+).")
                        .build(),

                // ── Objects Methods ──
                CompletionItem.builder("equals", "equals", CompletionItemKind.METHOD)
                        .detail("Objects.equals")
                        .signature("Objects.equals(Object a, Object b)")
                        .returnType("boolean")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Tests if two objects are equal, safely handling nulls.")
                        .build(),

                CompletionItem.builder("deepEquals", "deepEquals", CompletionItemKind.METHOD)
                        .detail("Objects.deepEquals")
                        .signature("Objects.deepEquals(Object a, Object b)")
                        .returnType("boolean")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Tests if two objects are deeply equal, supporting arrays.")
                        .build(),

                CompletionItem.builder("hash", "hash", CompletionItemKind.METHOD)
                        .detail("Objects.hash")
                        .signature("Objects.hash(Object... values)")
                        .returnType("int")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Generates a hash code for a sequence of input values.")
                        .build(),

                CompletionItem.builder("hashCode", "hashCode", CompletionItemKind.METHOD)
                        .detail("Objects.hashCode")
                        .signature("Objects.hashCode(Object o)")
                        .returnType("int")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Returns the hash code of an object, or 0 if null.")
                        .build(),

                CompletionItem.builder("toString", "toString", CompletionItemKind.METHOD)
                        .detail("Objects.toString")
                        .signature("Objects.toString(Object o)")
                        .returnType("String")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Returns the string representation of the object, or \"null\" if null.")
                        .build(),

                CompletionItem.builder("requireNonNull", "requireNonNull", CompletionItemKind.METHOD)
                        .detail("Objects.requireNonNull")
                        .signature("Objects.requireNonNull(T obj)")
                        .returnType("T")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Checks that the specified object reference is not null, throwing NullPointerException if it is.")
                        .build(),

                CompletionItem.builder("isNull", "isNull", CompletionItemKind.METHOD)
                        .detail("Objects.isNull")
                        .signature("Objects.isNull(Object obj)")
                        .returnType("boolean")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Returns true if the provided reference is null, otherwise false.")
                        .build(),

                CompletionItem.builder("nonNull", "nonNull", CompletionItemKind.METHOD)
                        .detail("Objects.nonNull")
                        .signature("Objects.nonNull(Object obj)")
                        .returnType("boolean")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Returns true if the provided reference is non-null, otherwise false.")
                        .build(),

                CompletionItem.builder("compare", "compare", CompletionItemKind.METHOD)
                        .detail("Objects.compare")
                        .signature("Objects.compare(T a, T b, Comparator<? super T> c)")
                        .returnType("int")
                        .owner("java.util.Objects")
                        .category("Method")
                        .documentation("Compares two objects using the provided Comparator, safely handling nulls.")
                        .build(),

                // ── UUID Methods ──
                CompletionItem.builder("randomUUID", "randomUUID", CompletionItemKind.METHOD)
                        .detail("UUID.randomUUID")
                        .signature("UUID.randomUUID()")
                        .returnType("UUID")
                        .owner("java.util.UUID")
                        .category("Method")
                        .documentation("Generates a random UUID (type 4) using a cryptographically strong pseudo-random number generator.")
                        .build(),

                CompletionItem.builder("fromString", "fromString", CompletionItemKind.METHOD)
                        .detail("UUID.fromString")
                        .signature("UUID.fromString(String name)")
                        .returnType("UUID")
                        .owner("java.util.UUID")
                        .category("Method")
                        .documentation("Creates a UUID from the standard string representation (e.g. 550e8400-e29b-41d4-a716-446655440000).")
                        .build(),

                // ── Random Methods ──
                CompletionItem.builder("nextInt", "nextInt", CompletionItemKind.METHOD)
                        .detail("Random.nextInt")
                        .signature("Random.nextInt()")
                        .returnType("int")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Returns a pseudo-random int between Integer.MIN_VALUE and Integer.MAX_VALUE.")
                        .build(),

                CompletionItem.builder("nextInt", "nextInt", CompletionItemKind.METHOD)
                        .detail("Random.nextInt(int bound)")
                        .signature("Random.nextInt(int bound)")
                        .returnType("int")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Returns a pseudo-random int between 0 (inclusive) and the bound (exclusive).")
                        .build(),

                CompletionItem.builder("nextLong", "nextLong", CompletionItemKind.METHOD)
                        .detail("Random.nextLong")
                        .signature("Random.nextLong()")
                        .returnType("long")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Returns a pseudo-random long value.")
                        .build(),

                CompletionItem.builder("nextDouble", "nextDouble", CompletionItemKind.METHOD)
                        .detail("Random.nextDouble")
                        .signature("Random.nextDouble()")
                        .returnType("double")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Returns a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive).")
                        .build(),

                CompletionItem.builder("nextBoolean", "nextBoolean", CompletionItemKind.METHOD)
                        .detail("Random.nextBoolean")
                        .signature("Random.nextBoolean()")
                        .returnType("boolean")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Returns a pseudo-random boolean value.")
                        .build(),

                CompletionItem.builder("nextGaussian", "nextGaussian", CompletionItemKind.METHOD)
                        .detail("Random.nextGaussian")
                        .signature("Random.nextGaussian()")
                        .returnType("double")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Returns a pseudo-random Gaussian-distributed double with mean 0.0 and stddev 1.0.")
                        .build(),

                CompletionItem.builder("setSeed", "setSeed", CompletionItemKind.METHOD)
                        .detail("Random.setSeed")
                        .signature("Random.setSeed(long seed)")
                        .returnType("void")
                        .owner("java.util.Random")
                        .category("Method")
                        .documentation("Sets the seed of this random number generator using a single long seed.")
                        .build(),

                // ── Scanner Methods ──
                CompletionItem.builder("next", "next", CompletionItemKind.METHOD)
                        .detail("Scanner.next")
                        .signature("Scanner.next()")
                        .returnType("String")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Finds and returns the next complete token from this scanner.")
                        .build(),

                CompletionItem.builder("nextLine", "nextLine", CompletionItemKind.METHOD)
                        .detail("Scanner.nextLine")
                        .signature("Scanner.nextLine()")
                        .returnType("String")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Advances this scanner past the current line and returns the skipped input.")
                        .build(),

                CompletionItem.builder("nextInt", "nextInt", CompletionItemKind.METHOD)
                        .detail("Scanner.nextInt")
                        .signature("Scanner.nextInt()")
                        .returnType("int")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Scans the next token as an int.")
                        .build(),

                CompletionItem.builder("nextDouble", "nextDouble", CompletionItemKind.METHOD)
                        .detail("Scanner.nextDouble")
                        .signature("Scanner.nextDouble()")
                        .returnType("double")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Scans the next token as a double.")
                        .build(),

                CompletionItem.builder("hasNext", "hasNext", CompletionItemKind.METHOD)
                        .detail("Scanner.hasNext")
                        .signature("Scanner.hasNext()")
                        .returnType("boolean")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Tests if this scanner has another token in its input.")
                        .build(),

                CompletionItem.builder("hasNextLine", "hasNextLine", CompletionItemKind.METHOD)
                        .detail("Scanner.hasNextLine")
                        .signature("Scanner.hasNextLine()")
                        .returnType("boolean")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Tests if there is another line in the scanner input.")
                        .build(),

                CompletionItem.builder("close", "close", CompletionItemKind.METHOD)
                        .detail("Scanner.close")
                        .signature("Scanner.close()")
                        .returnType("void")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Closes this scanner and releases any associated resources.")
                        .build(),

                CompletionItem.builder("useDelimiter", "useDelimiter", CompletionItemKind.METHOD)
                        .detail("Scanner.useDelimiter")
                        .signature("Scanner.useDelimiter(String pattern)")
                        .returnType("Scanner")
                        .owner("java.util.Scanner")
                        .category("Method")
                        .documentation("Sets the delimiter pattern for token separation.")
                        .build(),

                // ── Stack Methods ──
                CompletionItem.builder("push", "push", CompletionItemKind.METHOD)
                        .detail("Stack.push")
                        .signature("Stack.push(E item)")
                        .returnType("E")
                        .owner("java.util.Stack")
                        .category("Method")
                        .documentation("Pushes an item onto the top of this stack.")
                        .build(),

                CompletionItem.builder("pop", "pop", CompletionItemKind.METHOD)
                        .detail("Stack.pop")
                        .signature("Stack.pop()")
                        .returnType("E")
                        .owner("java.util.Stack")
                        .category("Method")
                        .documentation("Removes and returns the object at the top of this stack.")
                        .build(),

                CompletionItem.builder("peek", "peek", CompletionItemKind.METHOD)
                        .detail("Stack.peek")
                        .signature("Stack.peek()")
                        .returnType("E")
                        .owner("java.util.Stack")
                        .category("Method")
                        .documentation("Looks at the object at the top of this stack without removing it.")
                        .build(),

                CompletionItem.builder("empty", "empty", CompletionItemKind.METHOD)
                        .detail("Stack.empty")
                        .signature("Stack.empty()")
                        .returnType("boolean")
                        .owner("java.util.Stack")
                        .category("Method")
                        .documentation("Tests if this stack is empty.")
                        .build(),

                CompletionItem.builder("search", "search", CompletionItemKind.METHOD)
                        .detail("Stack.search")
                        .signature("Stack.search(Object o)")
                        .returnType("int")
                        .owner("java.util.Stack")
                        .category("Method")
                        .documentation("Returns the 1-based position of the object on the stack, or -1 if not found.")
                        .build()
        );
    }
}
