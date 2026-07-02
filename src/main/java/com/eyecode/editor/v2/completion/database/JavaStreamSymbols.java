package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaStreamSymbols {

    private JavaStreamSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("Stream", "Stream", CompletionItemKind.INTERFACE)
                        .detail("java.util.stream.Stream")
                        .owner("java.util.stream")
                        .category("Interface")
                        .documentation("A sequence of elements supporting sequential and parallel aggregate operations (Java 8+).")
                        .build(),

                CompletionItem.builder("IntStream", "IntStream", CompletionItemKind.INTERFACE)
                        .detail("java.util.stream.IntStream")
                        .owner("java.util.stream")
                        .category("Interface")
                        .documentation("A sequence of primitive int-valued elements supporting aggregate operations (Java 8+).")
                        .build(),

                CompletionItem.builder("LongStream", "LongStream", CompletionItemKind.INTERFACE)
                        .detail("java.util.stream.LongStream")
                        .owner("java.util.stream")
                        .category("Interface")
                        .documentation("A sequence of primitive long-valued elements supporting aggregate operations (Java 8+).")
                        .build(),

                CompletionItem.builder("DoubleStream", "DoubleStream", CompletionItemKind.INTERFACE)
                        .detail("java.util.stream.DoubleStream")
                        .owner("java.util.stream")
                        .category("Interface")
                        .documentation("A sequence of primitive double-valued elements supporting aggregate operations (Java 8+).")
                        .build(),

                CompletionItem.builder("Collectors", "Collectors", CompletionItemKind.CLASS)
                        .detail("java.util.stream.Collectors")
                        .owner("java.util.stream")
                        .category("Class")
                        .documentation("Provides static factory methods for common Collector implementations: toList, toSet, toMap, joining, groupingBy, and more (Java 8+).")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("Stream.of")
                        .signature("Stream.of(T... values)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Creates a sequential Stream from the given values.")
                        .example("Stream<String> s = Stream.of(\"a\", \"b\", \"c\");")
                        .build(),

                CompletionItem.builder("filter", "filter", CompletionItemKind.METHOD)
                        .detail("Stream.filter")
                        .signature("Stream.filter(Predicate<? super T> predicate)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns a stream consisting of elements that match the given predicate.")
                        .build(),

                CompletionItem.builder("map", "map", CompletionItemKind.METHOD)
                        .detail("Stream.map")
                        .signature("Stream.map(Function<? super T, ? extends R> mapper)")
                        .returnType("Stream<R>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Applies the mapper function to each element and returns a stream of the results.")
                        .build(),

                CompletionItem.builder("flatMap", "flatMap", CompletionItemKind.METHOD)
                        .detail("Stream.flatMap")
                        .signature("Stream.flatMap(Function<? super T, ? extends Stream<? extends R>> mapper)")
                        .returnType("Stream<R>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Flattens nested streams into a single stream by replacing each element with stream content.")
                        .build(),

                CompletionItem.builder("forEach", "forEach", CompletionItemKind.METHOD)
                        .detail("Stream.forEach")
                        .signature("Stream.forEach(Consumer<? super T> action)")
                        .returnType("void")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Performs the given action for each element of the stream.")
                        .build(),

                CompletionItem.builder("collect", "collect", CompletionItemKind.METHOD)
                        .detail("Stream.collect")
                        .signature("Stream.collect(Collector<? super T, A, R> collector)")
                        .returnType("R")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Performs a mutable reduction of stream elements using a Collector.")
                        .example("List<String> list = stream.collect(Collectors.toList());")
                        .build(),

                CompletionItem.builder("reduce", "reduce", CompletionItemKind.METHOD)
                        .detail("Stream.reduce")
                        .signature("Stream.reduce(T identity, BinaryOperator<T> accumulator)")
                        .returnType("T")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Performs a reduction on the elements of the stream using an associative accumulation function.")
                        .build(),

                CompletionItem.builder("sorted", "sorted", CompletionItemKind.METHOD)
                        .detail("Stream.sorted")
                        .signature("Stream.sorted()")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns a stream sorted in natural order.")
                        .build(),

                CompletionItem.builder("distinct", "distinct", CompletionItemKind.METHOD)
                        .detail("Stream.distinct")
                        .signature("Stream.distinct()")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns a stream with duplicate elements removed.")
                        .build(),

                CompletionItem.builder("limit", "limit", CompletionItemKind.METHOD)
                        .detail("Stream.limit")
                        .signature("Stream.limit(long maxSize)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Truncates the stream to not exceed the given maximum size.")
                        .build(),

                CompletionItem.builder("skip", "skip", CompletionItemKind.METHOD)
                        .detail("Stream.skip")
                        .signature("Stream.skip(long n)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Discards the first n elements of the stream.")
                        .build(),

                CompletionItem.builder("peek", "peek", CompletionItemKind.METHOD)
                        .detail("Stream.peek")
                        .signature("Stream.peek(Consumer<? super T> action)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns a stream with the same elements, applying the action to each as elements are consumed.")
                        .build(),

                CompletionItem.builder("findFirst", "findFirst", CompletionItemKind.METHOD)
                        .detail("Stream.findFirst")
                        .signature("Stream.findFirst()")
                        .returnType("Optional<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns an Optional describing the first element of the stream, or empty if the stream is empty.")
                        .build(),

                CompletionItem.builder("findAny", "findAny", CompletionItemKind.METHOD)
                        .detail("Stream.findAny")
                        .signature("Stream.findAny()")
                        .returnType("Optional<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns an Optional describing any element of the stream.")
                        .build(),

                CompletionItem.builder("anyMatch", "anyMatch", CompletionItemKind.METHOD)
                        .detail("Stream.anyMatch")
                        .signature("Stream.anyMatch(Predicate<? super T> predicate)")
                        .returnType("boolean")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Tests if any element of the stream matches the given predicate.")
                        .build(),

                CompletionItem.builder("allMatch", "allMatch", CompletionItemKind.METHOD)
                        .detail("Stream.allMatch")
                        .signature("Stream.allMatch(Predicate<? super T> predicate)")
                        .returnType("boolean")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Tests if all elements of the stream match the given predicate.")
                        .build(),

                CompletionItem.builder("noneMatch", "noneMatch", CompletionItemKind.METHOD)
                        .detail("Stream.noneMatch")
                        .signature("Stream.noneMatch(Predicate<? super T> predicate)")
                        .returnType("boolean")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Tests if no elements of the stream match the given predicate.")
                        .build(),

                CompletionItem.builder("count", "count", CompletionItemKind.METHOD)
                        .detail("Stream.count")
                        .signature("Stream.count()")
                        .returnType("long")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns the count of elements in the stream.")
                        .build(),

                CompletionItem.builder("toList", "toList", CompletionItemKind.METHOD)
                        .detail("Stream.toList")
                        .signature("Stream.toList()")
                        .returnType("List<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Collects stream elements into an unmodifiable List (Java 16+).")
                        .build(),

                CompletionItem.builder("toArray", "toArray", CompletionItemKind.METHOD)
                        .detail("Stream.toArray")
                        .signature("Stream.toArray()")
                        .returnType("Object[]")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns an array containing the elements of this stream.")
                        .build(),

                CompletionItem.builder("iterate", "iterate", CompletionItemKind.METHOD)
                        .detail("Stream.iterate")
                        .signature("Stream.iterate(T seed, UnaryOperator<T> f)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Creates an infinite sequential stream by iteratively applying a function to a seed.")
                        .build(),

                CompletionItem.builder("generate", "generate", CompletionItemKind.METHOD)
                        .detail("Stream.generate")
                        .signature("Stream.generate(Supplier<? extends T> s)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Creates an infinite sequential stream where each element is generated by the Supplier.")
                        .build(),

                CompletionItem.builder("concat", "concat", CompletionItemKind.METHOD)
                        .detail("Stream.concat")
                        .signature("Stream.concat(Stream<? extends T> a, Stream<? extends T> b)")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Creates a lazy concatenated stream whose elements are all elements of first followed by second.")
                        .build(),

                CompletionItem.builder("empty", "empty", CompletionItemKind.METHOD)
                        .detail("Stream.empty")
                        .signature("Stream.empty()")
                        .returnType("Stream<T>")
                        .owner("java.util.stream.Stream")
                        .category("Method")
                        .documentation("Returns an empty sequential stream.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("IntStream.of")
                        .signature("IntStream.of(int... values)")
                        .returnType("IntStream")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Creates a sequential IntStream from the given int values.")
                        .build(),

                CompletionItem.builder("range", "range", CompletionItemKind.METHOD)
                        .detail("IntStream.range")
                        .signature("IntStream.range(int startInclusive, int endExclusive)")
                        .returnType("IntStream")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Creates an IntStream from startInclusive (inclusive) to endExclusive (exclusive).")
                        .build(),

                CompletionItem.builder("rangeClosed", "rangeClosed", CompletionItemKind.METHOD)
                        .detail("IntStream.rangeClosed")
                        .signature("IntStream.rangeClosed(int startInclusive, int endInclusive)")
                        .returnType("IntStream")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Creates an IntStream from startInclusive to endInclusive (both inclusive).")
                        .build(),

                CompletionItem.builder("sum", "sum", CompletionItemKind.METHOD)
                        .detail("IntStream.sum")
                        .signature("IntStream.sum()")
                        .returnType("int")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Returns the sum of elements in this stream.")
                        .build(),

                CompletionItem.builder("average", "average", CompletionItemKind.METHOD)
                        .detail("IntStream.average")
                        .signature("IntStream.average()")
                        .returnType("OptionalDouble")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Returns the arithmetic mean of elements in this stream.")
                        .build(),

                CompletionItem.builder("min", "min", CompletionItemKind.METHOD)
                        .detail("IntStream.min")
                        .signature("IntStream.min()")
                        .returnType("OptionalInt")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Returns the minimum element of this stream.")
                        .build(),

                CompletionItem.builder("max", "max", CompletionItemKind.METHOD)
                        .detail("IntStream.max")
                        .signature("IntStream.max()")
                        .returnType("OptionalInt")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Returns the maximum element of this stream.")
                        .build(),

                CompletionItem.builder("boxed", "boxed", CompletionItemKind.METHOD)
                        .detail("IntStream.boxed")
                        .signature("IntStream.boxed()")
                        .returnType("Stream<Integer>")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Converts the IntStream to a Stream<Integer> by boxing each element.")
                        .build(),

                CompletionItem.builder("mapToObj", "mapToObj", CompletionItemKind.METHOD)
                        .detail("IntStream.mapToObj")
                        .signature("IntStream.mapToObj(IntFunction<? extends R> mapper)")
                        .returnType("Stream<R>")
                        .owner("java.util.stream.IntStream")
                        .category("Method")
                        .documentation("Maps each int element to an object, returning a Stream.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("LongStream.of")
                        .signature("LongStream.of(long... values)")
                        .returnType("LongStream")
                        .owner("java.util.stream.LongStream")
                        .category("Method")
                        .documentation("Creates a sequential LongStream from the given long values.")
                        .build(),

                CompletionItem.builder("range", "range", CompletionItemKind.METHOD)
                        .detail("LongStream.range")
                        .signature("LongStream.range(long startInclusive, long endExclusive)")
                        .returnType("LongStream")
                        .owner("java.util.stream.LongStream")
                        .category("Method")
                        .documentation("Creates a LongStream from startInclusive (inclusive) to endExclusive (exclusive).")
                        .build(),

                CompletionItem.builder("rangeClosed", "rangeClosed", CompletionItemKind.METHOD)
                        .detail("LongStream.rangeClosed")
                        .signature("LongStream.rangeClosed(long startInclusive, long endInclusive)")
                        .returnType("LongStream")
                        .owner("java.util.stream.LongStream")
                        .category("Method")
                        .documentation("Creates a LongStream from startInclusive to endInclusive (both inclusive).")
                        .build(),

                CompletionItem.builder("sum", "sum", CompletionItemKind.METHOD)
                        .detail("LongStream.sum")
                        .signature("LongStream.sum()")
                        .returnType("long")
                        .owner("java.util.stream.LongStream")
                        .category("Method")
                        .documentation("Returns the sum of elements in this stream.")
                        .build(),

                CompletionItem.builder("average", "average", CompletionItemKind.METHOD)
                        .detail("LongStream.average")
                        .signature("LongStream.average()")
                        .returnType("OptionalDouble")
                        .owner("java.util.stream.LongStream")
                        .category("Method")
                        .documentation("Returns the arithmetic mean of elements in this stream.")
                        .build(),

                CompletionItem.builder("boxed", "boxed", CompletionItemKind.METHOD)
                        .detail("LongStream.boxed")
                        .signature("LongStream.boxed()")
                        .returnType("Stream<Long>")
                        .owner("java.util.stream.LongStream")
                        .category("Method")
                        .documentation("Converts the LongStream to a Stream<Long> by boxing each element.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("DoubleStream.of")
                        .signature("DoubleStream.of(double... values)")
                        .returnType("DoubleStream")
                        .owner("java.util.stream.DoubleStream")
                        .category("Method")
                        .documentation("Creates a sequential DoubleStream from the given double values.")
                        .build(),

                CompletionItem.builder("sum", "sum", CompletionItemKind.METHOD)
                        .detail("DoubleStream.sum")
                        .signature("DoubleStream.sum()")
                        .returnType("double")
                        .owner("java.util.stream.DoubleStream")
                        .category("Method")
                        .documentation("Returns the sum of elements in this stream.")
                        .build(),

                CompletionItem.builder("average", "average", CompletionItemKind.METHOD)
                        .detail("DoubleStream.average")
                        .signature("DoubleStream.average()")
                        .returnType("OptionalDouble")
                        .owner("java.util.stream.DoubleStream")
                        .category("Method")
                        .documentation("Returns the arithmetic mean of elements in this stream.")
                        .build(),

                CompletionItem.builder("boxed", "boxed", CompletionItemKind.METHOD)
                        .detail("DoubleStream.boxed")
                        .signature("DoubleStream.boxed()")
                        .returnType("Stream<Double>")
                        .owner("java.util.stream.DoubleStream")
                        .category("Method")
                        .documentation("Converts the DoubleStream to a Stream<Double> by boxing each element.")
                        .build(),

                CompletionItem.builder("toList", "toList", CompletionItemKind.METHOD)
                        .detail("Collectors.toList")
                        .signature("Collectors.toList()")
                        .returnType("Collector<T,?,List<T>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that accumulates elements into a mutable List.")
                        .example("stream.collect(Collectors.toList())")
                        .build(),

                CompletionItem.builder("toSet", "toSet", CompletionItemKind.METHOD)
                        .detail("Collectors.toSet")
                        .signature("Collectors.toSet()")
                        .returnType("Collector<T,?,Set<T>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that accumulates elements into a mutable Set.")
                        .build(),

                CompletionItem.builder("toMap", "toMap", CompletionItemKind.METHOD)
                        .detail("Collectors.toMap")
                        .signature("Collectors.toMap(Function keyMapper, Function valueMapper)")
                        .returnType("Collector<T,?,Map<K,U>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that accumulates elements into a Map by key-value mapping functions.")
                        .build(),

                CompletionItem.builder("joining", "joining", CompletionItemKind.METHOD)
                        .detail("Collectors.joining")
                        .signature("Collectors.joining(CharSequence delimiter)")
                        .returnType("Collector<CharSequence,?,String>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that concatenates CharSequences with the specified delimiter.")
                        .example("stream.collect(Collectors.joining(\", \"))")
                        .build(),

                CompletionItem.builder("groupingBy", "groupingBy", CompletionItemKind.METHOD)
                        .detail("Collectors.groupingBy")
                        .signature("Collectors.groupingBy(Function classifier)")
                        .returnType("Collector<T,?,Map<K,List<T>>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that groups elements by a classifier function into a Map.")
                        .build(),

                CompletionItem.builder("partitioningBy", "partitioningBy", CompletionItemKind.METHOD)
                        .detail("Collectors.partitioningBy")
                        .signature("Collectors.partitioningBy(Predicate predicate)")
                        .returnType("Collector<T,?,Map<Boolean,List<T>>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that partitions elements by a predicate into a Map<Boolean, List<T>>.")
                        .build(),

                CompletionItem.builder("counting", "counting", CompletionItemKind.METHOD)
                        .detail("Collectors.counting")
                        .signature("Collectors.counting()")
                        .returnType("Collector<T,?,Long>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that counts the number of elements.")
                        .build(),

                CompletionItem.builder("summingInt", "summingInt", CompletionItemKind.METHOD)
                        .detail("Collectors.summingInt")
                        .signature("Collectors.summingInt(ToIntFunction mapper)")
                        .returnType("Collector<T,?,Integer>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that sums the result of applying an int-valued mapper function.")
                        .build(),

                CompletionItem.builder("maxBy", "maxBy", CompletionItemKind.METHOD)
                        .detail("Collectors.maxBy")
                        .signature("Collectors.maxBy(Comparator comparator)")
                        .returnType("Collector<T,?,Optional<T>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that finds the maximum element according to a Comparator.")
                        .build(),

                CompletionItem.builder("minBy", "minBy", CompletionItemKind.METHOD)
                        .detail("Collectors.minBy")
                        .signature("Collectors.minBy(Comparator comparator)")
                        .returnType("Collector<T,?,Optional<T>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that finds the minimum element according to a Comparator.")
                        .build(),

                CompletionItem.builder("reducing", "reducing", CompletionItemKind.METHOD)
                        .detail("Collectors.reducing")
                        .signature("Collectors.reducing(BinaryOperator op)")
                        .returnType("Collector<T,?,Optional<T>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that performs a reduction on elements using a BinaryOperator.")
                        .build(),

                CompletionItem.builder("mapping", "mapping", CompletionItemKind.METHOD)
                        .detail("Collectors.mapping")
                        .signature("Collectors.mapping(Function mapper, Collector downstream)")
                        .returnType("Collector<T,?,R>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Adapts a Collector by applying a mapping function before accumulation.")
                        .build(),

                CompletionItem.builder("collectingAndThen", "collectingAndThen", CompletionItemKind.METHOD)
                        .detail("Collectors.collectingAndThen")
                        .signature("Collectors.collectingAndThen(Collector downstream, Function finisher)")
                        .returnType("Collector<T,?,R>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Adapts a Collector by applying a finishing transformation after accumulation.")
                        .build(),

                CompletionItem.builder("toUnmodifiableList", "toUnmodifiableList", CompletionItemKind.METHOD)
                        .detail("Collectors.toUnmodifiableList")
                        .signature("Collectors.toUnmodifiableList()")
                        .returnType("Collector<T,?,List<T>>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Returns a Collector that accumulates elements into an unmodifiable List (Java 10+).")
                        .build(),

                CompletionItem.builder("filtering", "filtering", CompletionItemKind.METHOD)
                        .detail("Collectors.filtering")
                        .signature("Collectors.filtering(Predicate predicate, Collector downstream)")
                        .returnType("Collector<T,?,R>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Adapts a Collector by applying a filter before accumulation (Java 9+).")
                        .build(),

                CompletionItem.builder("flatMapping", "flatMapping", CompletionItemKind.METHOD)
                        .detail("Collectors.flatMapping")
                        .signature("Collectors.flatMapping(Function mapper, Collector downstream)")
                        .returnType("Collector<T,?,R>")
                        .owner("java.util.stream.Collectors")
                        .category("Method")
                        .documentation("Adapts a Collector by applying a flat-mapping function before accumulation (Java 9+).")
                        .build()
        );
    }
}
