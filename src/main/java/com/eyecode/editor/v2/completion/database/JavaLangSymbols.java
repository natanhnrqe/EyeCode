package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaLangSymbols {

    private JavaLangSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                // ── Classes ──
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

                CompletionItem.builder("Runtime", "Runtime", CompletionItemKind.CLASS)
                        .detail("java.lang.Runtime")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Allows the application to interface with the JVM environment: memory, processors, and process control.")
                        .example("Runtime rt = Runtime.getRuntime();\nlong mem = rt.freeMemory();")
                        .build(),

                CompletionItem.builder("Process", "Process", CompletionItemKind.CLASS)
                        .detail("java.lang.Process")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Represents a native operating system process spawned by ProcessBuilder or Runtime.exec().")
                        .build(),

                CompletionItem.builder("ThreadLocal", "ThreadLocal", CompletionItemKind.CLASS)
                        .detail("java.lang.ThreadLocal")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Provides per-thread variable isolation. Each thread has its own copy of the value.")
                        .example("ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);")
                        .build(),

                CompletionItem.builder("Throwable", "Throwable", CompletionItemKind.CLASS)
                        .detail("java.lang.Throwable")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("The root class of the exception hierarchy. All exceptions and errors extend Throwable.")
                        .build(),

                CompletionItem.builder("Error", "Error", CompletionItemKind.CLASS)
                        .detail("java.lang.Error")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Represents serious problems that a reasonable application should not try to catch, such as OutOfMemoryError.")
                        .build(),

                CompletionItem.builder("Character", "Character", CompletionItemKind.CLASS)
                        .detail("java.lang.Character")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive char type, providing static methods for character classification and conversion.")
                        .build(),

                CompletionItem.builder("Byte", "Byte", CompletionItemKind.CLASS)
                        .detail("java.lang.Byte")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive byte type.")
                        .build(),

                CompletionItem.builder("Short", "Short", CompletionItemKind.CLASS)
                        .detail("java.lang.Short")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive short type.")
                        .build(),

                CompletionItem.builder("Long", "Long", CompletionItemKind.CLASS)
                        .detail("java.lang.Long")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive long type.")
                        .build(),

                CompletionItem.builder("Float", "Float", CompletionItemKind.CLASS)
                        .detail("java.lang.Float")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Wrapper class for the primitive float type.")
                        .build(),

                CompletionItem.builder("StrictMath", "StrictMath", CompletionItemKind.CLASS)
                        .detail("java.lang.StrictMath")
                        .owner("java.lang")
                        .category("Class")
                        .documentation("Provides mathematical functions with guaranteed cross-platform consistent results.")
                        .build(),

                // ── Fields ──
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

                CompletionItem.builder("TRUE", "TRUE", CompletionItemKind.FIELD)
                        .detail("Boolean.TRUE")
                        .signature("Boolean.TRUE")
                        .returnType("Boolean")
                        .owner("java.lang.Boolean")
                        .category("Field")
                        .documentation("The Boolean object corresponding to the primitive value true.")
                        .build(),

                CompletionItem.builder("FALSE", "FALSE", CompletionItemKind.FIELD)
                        .detail("Boolean.FALSE")
                        .signature("Boolean.FALSE")
                        .returnType("Boolean")
                        .owner("java.lang.Boolean")
                        .category("Field")
                        .documentation("The Boolean object corresponding to the primitive value false.")
                        .build(),

                CompletionItem.builder("BYTES", "BYTES", CompletionItemKind.FIELD)
                        .detail("Byte.BYTES")
                        .signature("Byte.BYTES")
                        .returnType("int")
                        .owner("java.lang.Byte")
                        .category("Field")
                        .documentation("The number of bytes used to represent a byte value: 1.")
                        .build(),

                CompletionItem.builder("SIZE", "SIZE", CompletionItemKind.FIELD)
                        .detail("Byte.SIZE")
                        .signature("Byte.SIZE")
                        .returnType("int")
                        .owner("java.lang.Byte")
                        .category("Field")
                        .documentation("The number of bits used to represent a byte value: 8.")
                        .build(),

                // ── System Methods ──
                CompletionItem.builder("currentTimeMillis", "currentTimeMillis", CompletionItemKind.METHOD)
                        .detail("System.currentTimeMillis")
                        .signature("System.currentTimeMillis()")
                        .returnType("long")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Returns the current time in milliseconds since January 1, 1970 UTC (Unix epoch).")
                        .example("long start = System.currentTimeMillis();")
                        .build(),

                CompletionItem.builder("nanoTime", "nanoTime", CompletionItemKind.METHOD)
                        .detail("System.nanoTime")
                        .signature("System.nanoTime()")
                        .returnType("long")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Returns the current value of the high-resolution time source, in nanoseconds, for elapsed-time measurement.")
                        .example("long elapsed = System.nanoTime() - start;")
                        .build(),

                CompletionItem.builder("arraycopy", "arraycopy", CompletionItemKind.METHOD)
                        .detail("System.arraycopy")
                        .signature("System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length)")
                        .returnType("void")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Copies a subrange of array elements from one array to another.")
                        .build(),

                CompletionItem.builder("getProperty", "getProperty", CompletionItemKind.METHOD)
                        .detail("System.getProperty")
                        .signature("System.getProperty(String key)")
                        .returnType("String")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Retrieves the system property indicated by the specified key.")
                        .example("String home = System.getProperty(\"user.home\");")
                        .build(),

                CompletionItem.builder("getenv", "getenv", CompletionItemKind.METHOD)
                        .detail("System.getenv")
                        .signature("System.getenv(String name)")
                        .returnType("String")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Retrieves the value of an environment variable by name.")
                        .build(),

                CompletionItem.builder("gc", "gc", CompletionItemKind.METHOD)
                        .detail("System.gc")
                        .signature("System.gc()")
                        .returnType("void")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Suggests that the JVM run garbage collection. There is no guarantee it will run immediately.")
                        .build(),

                CompletionItem.builder("exit", "exit", CompletionItemKind.METHOD)
                        .detail("System.exit")
                        .signature("System.exit(int status)")
                        .returnType("void")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Terminates the currently running Java Virtual Machine with the given exit status code.")
                        .build(),

                CompletionItem.builder("identityHashCode", "identityHashCode", CompletionItemKind.METHOD)
                        .detail("System.identityHashCode")
                        .signature("System.identityHashCode(Object x)")
                        .returnType("int")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Returns the same hash code for an object as the default hashCode() method, regardless of override.")
                        .build(),

                CompletionItem.builder("lineSeparator", "lineSeparator", CompletionItemKind.METHOD)
                        .detail("System.lineSeparator")
                        .signature("System.lineSeparator()")
                        .returnType("String")
                        .owner("java.lang.System")
                        .category("Method")
                        .documentation("Returns the system-dependent line separator string (e.g. \\n on Unix, \\r\\n on Windows).")
                        .build(),

                // ── String Methods ──
                CompletionItem.builder("length", "length", CompletionItemKind.METHOD)
                        .detail("String.length")
                        .signature("String.length()")
                        .returnType("int")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the number of characters in this string.")
                        .build(),

                CompletionItem.builder("isEmpty", "isEmpty", CompletionItemKind.METHOD)
                        .detail("String.isEmpty")
                        .signature("String.isEmpty()")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns true if this string contains no characters.")
                        .build(),

                CompletionItem.builder("charAt", "charAt", CompletionItemKind.METHOD)
                        .detail("String.charAt")
                        .signature("String.charAt(int index)")
                        .returnType("char")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the character at the specified index in this string.")
                        .build(),

                CompletionItem.builder("substring", "substring", CompletionItemKind.METHOD)
                        .detail("String.substring")
                        .signature("String.substring(int beginIndex)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a new string that is a substring of this string, starting at the given index.")
                        .build(),

                CompletionItem.builder("indexOf", "indexOf", CompletionItemKind.METHOD)
                        .detail("String.indexOf")
                        .signature("String.indexOf(String str)")
                        .returnType("int")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the index of the first occurrence of the specified substring, or -1 if not found.")
                        .build(),

                CompletionItem.builder("lastIndexOf", "lastIndexOf", CompletionItemKind.METHOD)
                        .detail("String.lastIndexOf")
                        .signature("String.lastIndexOf(String str)")
                        .returnType("int")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the index of the last occurrence of the specified substring, or -1 if not found.")
                        .build(),

                CompletionItem.builder("contains", "contains", CompletionItemKind.METHOD)
                        .detail("String.contains")
                        .signature("String.contains(CharSequence seq)")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Tests whether this string contains the given character sequence.")
                        .build(),

                CompletionItem.builder("startsWith", "startsWith", CompletionItemKind.METHOD)
                        .detail("String.startsWith")
                        .signature("String.startsWith(String prefix)")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Tests if this string starts with the specified prefix.")
                        .build(),

                CompletionItem.builder("endsWith", "endsWith", CompletionItemKind.METHOD)
                        .detail("String.endsWith")
                        .signature("String.endsWith(String suffix)")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Tests if this string ends with the specified suffix.")
                        .build(),

                CompletionItem.builder("equals", "equals", CompletionItemKind.METHOD)
                        .detail("String.equals")
                        .signature("String.equals(Object other)")
                        .returnType("boolean")
                        .owner("java.lang.Object")
                        .category("Method")
                        .documentation("Compares this string to another object for value equality.")
                        .build(),

                CompletionItem.builder("equalsIgnoreCase", "equalsIgnoreCase", CompletionItemKind.METHOD)
                        .detail("String.equalsIgnoreCase")
                        .signature("String.equalsIgnoreCase(String other)")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Compares this string to another string, ignoring case considerations.")
                        .build(),

                CompletionItem.builder("toUpperCase", "toUpperCase", CompletionItemKind.METHOD)
                        .detail("String.toUpperCase")
                        .signature("String.toUpperCase()")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Converts all characters in this string to upper case using the default locale.")
                        .build(),

                CompletionItem.builder("toLowerCase", "toLowerCase", CompletionItemKind.METHOD)
                        .detail("String.toLowerCase")
                        .signature("String.toLowerCase()")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Converts all characters in this string to lower case using the default locale.")
                        .build(),

                CompletionItem.builder("trim", "trim", CompletionItemKind.METHOD)
                        .detail("String.trim")
                        .signature("String.trim()")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a copy of this string with leading and trailing whitespace removed.")
                        .build(),

                CompletionItem.builder("strip", "strip", CompletionItemKind.METHOD)
                        .detail("String.strip")
                        .signature("String.strip()")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a copy of this string with leading and trailing whitespace removed (Java 11+, Unicode-aware).")
                        .build(),

                CompletionItem.builder("replace", "replace", CompletionItemKind.METHOD)
                        .detail("String.replace")
                        .signature("String.replace(char oldChar, char newChar)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a new string with all occurrences of oldChar replaced by newChar.")
                        .build(),

                CompletionItem.builder("replaceAll", "replaceAll", CompletionItemKind.METHOD)
                        .detail("String.replaceAll")
                        .signature("String.replaceAll(String regex, String replacement)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Replaces each substring matching the regex with the given replacement.")
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

                CompletionItem.builder("join", "join", CompletionItemKind.METHOD)
                        .detail("String.join")
                        .signature("String.join(CharSequence delimiter, CharSequence... elements)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Joins the given elements with the specified delimiter.")
                        .example("String csv = String.join(\", \", \"a\", \"b\", \"c\");")
                        .build(),

                CompletionItem.builder("format", "format", CompletionItemKind.METHOD)
                        .detail("String.format")
                        .signature("String.format(String format, Object... args)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a formatted string using printf-style format specifiers.")
                        .example("String msg = String.format(\"Hello %s, age %d\", name, age);")
                        .build(),

                CompletionItem.builder("valueOf", "valueOf", CompletionItemKind.METHOD)
                        .detail("String.valueOf")
                        .signature("String.valueOf(Object obj)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns the string representation of the given object.")
                        .build(),

                CompletionItem.builder("toCharArray", "toCharArray", CompletionItemKind.METHOD)
                        .detail("String.toCharArray")
                        .signature("String.toCharArray()")
                        .returnType("char[]")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Converts this string to a new character array.")
                        .build(),

                CompletionItem.builder("matches", "matches", CompletionItemKind.METHOD)
                        .detail("String.matches")
                        .signature("String.matches(String regex)")
                        .returnType("boolean")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Tests whether this string matches the given regular expression.")
                        .build(),

                CompletionItem.builder("repeat", "repeat", CompletionItemKind.METHOD)
                        .detail("String.repeat")
                        .signature("String.repeat(int count)")
                        .returnType("String")
                        .owner("java.lang.String")
                        .category("Method")
                        .documentation("Returns a string whose value is the concatenation of this string repeated count times (Java 11+).")
                        .build(),

                // ── Integer Methods ──
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
                        .detail("Integer.valueOf")
                        .signature("Integer.valueOf(int i)")
                        .returnType("Integer")
                        .owner("java.lang.Integer")
                        .category("Method")
                        .documentation("Returns an Integer instance representing the specified int value.")
                        .build(),

                CompletionItem.builder("toHexString", "toHexString", CompletionItemKind.METHOD)
                        .detail("Integer.toHexString")
                        .signature("Integer.toHexString(int i)")
                        .returnType("String")
                        .owner("java.lang.Integer")
                        .category("Method")
                        .documentation("Returns a hexadecimal string representation of the int argument.")
                        .build(),

                CompletionItem.builder("toBinaryString", "toBinaryString", CompletionItemKind.METHOD)
                        .detail("Integer.toBinaryString")
                        .signature("Integer.toBinaryString(int i)")
                        .returnType("String")
                        .owner("java.lang.Integer")
                        .category("Method")
                        .documentation("Returns a binary string representation of the int argument.")
                        .build(),

                CompletionItem.builder("bitCount", "bitCount", CompletionItemKind.METHOD)
                        .detail("Integer.bitCount")
                        .signature("Integer.bitCount(int i)")
                        .returnType("int")
                        .owner("java.lang.Integer")
                        .category("Method")
                        .documentation("Returns the number of one-bits in the two's complement binary representation.")
                        .build(),

                CompletionItem.builder("numberOfLeadingZeros", "numberOfLeadingZeros", CompletionItemKind.METHOD)
                        .detail("Integer.numberOfLeadingZeros")
                        .signature("Integer.numberOfLeadingZeros(int i)")
                        .returnType("int")
                        .owner("java.lang.Integer")
                        .category("Method")
                        .documentation("Returns the number of zero bits preceding the highest-order one-bit.")
                        .build(),

                // ── Double Methods ──
                CompletionItem.builder("parseDouble", "parseDouble", CompletionItemKind.METHOD)
                        .detail("Double.parseDouble")
                        .signature("Double.parseDouble(String s)")
                        .returnType("double")
                        .owner("java.lang.Double")
                        .category("Method")
                        .documentation("Parses the given string as a double value. Throws NumberFormatException if invalid.")
                        .build(),

                CompletionItem.builder("isNaN", "isNaN", CompletionItemKind.METHOD)
                        .detail("Double.isNaN")
                        .signature("Double.isNaN(double v)")
                        .returnType("boolean")
                        .owner("java.lang.Double")
                        .category("Method")
                        .documentation("Tests whether the specified double value is Not-a-Number (NaN).")
                        .build(),

                CompletionItem.builder("isInfinite", "isInfinite", CompletionItemKind.METHOD)
                        .detail("Double.isInfinite")
                        .signature("Double.isInfinite(double v)")
                        .returnType("boolean")
                        .owner("java.lang.Double")
                        .category("Method")
                        .documentation("Tests whether the specified double value is infinite (positive or negative).")
                        .build(),

                CompletionItem.builder("doubleToLongBits", "doubleToLongBits", CompletionItemKind.METHOD)
                        .detail("Double.doubleToLongBits")
                        .signature("Double.doubleToLongBits(double value)")
                        .returnType("long")
                        .owner("java.lang.Double")
                        .category("Method")
                        .documentation("Returns the IEEE 754 representation of the double value as a long bit pattern.")
                        .build(),

                // ── Boolean Methods ──
                CompletionItem.builder("parseBoolean", "parseBoolean", CompletionItemKind.METHOD)
                        .detail("Boolean.parseBoolean")
                        .signature("Boolean.parseBoolean(String s)")
                        .returnType("boolean")
                        .owner("java.lang.Boolean")
                        .category("Method")
                        .documentation("Parses the given string as a boolean, returning true only if the string equals \"true\" (case-insensitive).")
                        .build(),

                CompletionItem.builder("logicalAnd", "logicalAnd", CompletionItemKind.METHOD)
                        .detail("Boolean.logicalAnd")
                        .signature("Boolean.logicalAnd(boolean a, boolean b)")
                        .returnType("boolean")
                        .owner("java.lang.Boolean")
                        .category("Method")
                        .documentation("Returns the result of applying the logical AND operator to the given booleans (Java 8+).")
                        .build(),

                CompletionItem.builder("logicalOr", "logicalOr", CompletionItemKind.METHOD)
                        .detail("Boolean.logicalOr")
                        .signature("Boolean.logicalOr(boolean a, boolean b)")
                        .returnType("boolean")
                        .owner("java.lang.Boolean")
                        .category("Method")
                        .documentation("Returns the result of applying the logical OR operator to the given booleans (Java 8+).")
                        .build(),

                // ── Math Methods ──
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
                        .build(),

                CompletionItem.builder("floor", "floor", CompletionItemKind.METHOD)
                        .detail("Math.floor")
                        .signature("Math.floor(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the largest double value that is less than or equal to the argument and is equal to an integer.")
                        .build(),

                CompletionItem.builder("ceil", "ceil", CompletionItemKind.METHOD)
                        .detail("Math.ceil")
                        .signature("Math.ceil(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the smallest double value that is greater than or equal to the argument and is equal to an integer.")
                        .build(),

                CompletionItem.builder("round", "round", CompletionItemKind.METHOD)
                        .detail("Math.round")
                        .signature("Math.round(double a)")
                        .returnType("long")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the closest long to the argument, with ties rounding to positive infinity.")
                        .build(),

                CompletionItem.builder("sin", "sin", CompletionItemKind.METHOD)
                        .detail("Math.sin")
                        .signature("Math.sin(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the trigonometric sine of an angle measured in radians.")
                        .build(),

                CompletionItem.builder("cos", "cos", CompletionItemKind.METHOD)
                        .detail("Math.cos")
                        .signature("Math.cos(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the trigonometric cosine of an angle measured in radians.")
                        .build(),

                CompletionItem.builder("tan", "tan", CompletionItemKind.METHOD)
                        .detail("Math.tan")
                        .signature("Math.tan(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the trigonometric tangent of an angle measured in radians.")
                        .build(),

                CompletionItem.builder("toDegrees", "toDegrees", CompletionItemKind.METHOD)
                        .detail("Math.toDegrees")
                        .signature("Math.toDegrees(double angrad)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Converts an angle measured in radians to an approximately equivalent angle in degrees.")
                        .build(),

                CompletionItem.builder("toRadians", "toRadians", CompletionItemKind.METHOD)
                        .detail("Math.toRadians")
                        .signature("Math.toRadians(double angdeg)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Converts an angle measured in degrees to an approximately equivalent angle in radians.")
                        .build(),

                CompletionItem.builder("exp", "exp", CompletionItemKind.METHOD)
                        .detail("Math.exp")
                        .signature("Math.exp(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns Euler's number e raised to the power of the argument.")
                        .build(),

                CompletionItem.builder("log", "log", CompletionItemKind.METHOD)
                        .detail("Math.log")
                        .signature("Math.log(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the natural logarithm (base e) of the argument.")
                        .build(),

                CompletionItem.builder("log10", "log10", CompletionItemKind.METHOD)
                        .detail("Math.log10")
                        .signature("Math.log10(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the base-10 logarithm of the argument.")
                        .build(),

                CompletionItem.builder("signum", "signum", CompletionItemKind.METHOD)
                        .detail("Math.signum")
                        .signature("Math.signum(double d)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the signum function of the argument: 0 if zero, 1.0 if positive, -1.0 if negative.")
                        .build(),

                CompletionItem.builder("floorMod", "floorMod", CompletionItemKind.METHOD)
                        .detail("Math.floorMod")
                        .signature("Math.floorMod(int x, int y)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the floor modulus of the int arguments, with a result that is non-negative.")
                        .build(),

                CompletionItem.builder("addExact", "addExact", CompletionItemKind.METHOD)
                        .detail("Math.addExact")
                        .signature("Math.addExact(int x, int y)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the sum of its arguments, throwing ArithmeticException on overflow.")
                        .build(),

                CompletionItem.builder("subtractExact", "subtractExact", CompletionItemKind.METHOD)
                        .detail("Math.subtractExact")
                        .signature("Math.subtractExact(int x, int y)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the difference of its arguments, throwing ArithmeticException on overflow.")
                        .build(),

                CompletionItem.builder("multiplyExact", "multiplyExact", CompletionItemKind.METHOD)
                        .detail("Math.multiplyExact")
                        .signature("Math.multiplyExact(int x, int y)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the product of its arguments, throwing ArithmeticException on overflow.")
                        .build(),

                CompletionItem.builder("floorDiv", "floorDiv", CompletionItemKind.METHOD)
                        .detail("Math.floorDiv")
                        .signature("Math.floorDiv(int x, int y)")
                        .returnType("int")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the largest integer less than or equal to the quotient of the arguments.")
                        .build(),

                CompletionItem.builder("hypot", "hypot", CompletionItemKind.METHOD)
                        .detail("Math.hypot")
                        .signature("Math.hypot(double x, double y)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns sqrt(x^2 + y^2) without intermediate overflow or underflow.")
                        .build(),

                CompletionItem.builder("cbrt", "cbrt", CompletionItemKind.METHOD)
                        .detail("Math.cbrt")
                        .signature("Math.cbrt(double a)")
                        .returnType("double")
                        .owner("java.lang.Math")
                        .category("Method")
                        .documentation("Returns the cube root of the argument.")
                        .build(),

                // ── Character Methods ──
                CompletionItem.builder("isDigit", "isDigit", CompletionItemKind.METHOD)
                        .detail("Character.isDigit")
                        .signature("Character.isDigit(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the specified character is a digit.")
                        .build(),

                CompletionItem.builder("isLetter", "isLetter", CompletionItemKind.METHOD)
                        .detail("Character.isLetter")
                        .signature("Character.isLetter(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the specified character is a letter.")
                        .build(),

                CompletionItem.builder("isLetterOrDigit", "isLetterOrDigit", CompletionItemKind.METHOD)
                        .detail("Character.isLetterOrDigit")
                        .signature("Character.isLetterOrDigit(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the specified character is a letter or digit.")
                        .build(),

                CompletionItem.builder("isWhitespace", "isWhitespace", CompletionItemKind.METHOD)
                        .detail("Character.isWhitespace")
                        .signature("Character.isWhitespace(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the specified character is whitespace according to Java.")
                        .build(),

                CompletionItem.builder("isUpperCase", "isUpperCase", CompletionItemKind.METHOD)
                        .detail("Character.isUpperCase")
                        .signature("Character.isUpperCase(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the specified character is an uppercase letter.")
                        .build(),

                CompletionItem.builder("isLowerCase", "isLowerCase", CompletionItemKind.METHOD)
                        .detail("Character.isLowerCase")
                        .signature("Character.isLowerCase(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the specified character is a lowercase letter.")
                        .build(),

                CompletionItem.builder("toUpperCase", "toUpperCase", CompletionItemKind.METHOD)
                        .detail("Character.toUpperCase")
                        .signature("Character.toUpperCase(char ch)")
                        .returnType("char")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Converts the character argument to uppercase using Unicode mapping.")
                        .build(),

                CompletionItem.builder("toLowerCase", "toLowerCase", CompletionItemKind.METHOD)
                        .detail("Character.toLowerCase")
                        .signature("Character.toLowerCase(char ch)")
                        .returnType("char")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Converts the character argument to lowercase using Unicode mapping.")
                        .build(),

                CompletionItem.builder("isJavaIdentifierStart", "isJavaIdentifierStart", CompletionItemKind.METHOD)
                        .detail("Character.isJavaIdentifierStart")
                        .signature("Character.isJavaIdentifierStart(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the character may be the first character of a Java identifier.")
                        .build(),

                CompletionItem.builder("isJavaIdentifierPart", "isJavaIdentifierPart", CompletionItemKind.METHOD)
                        .detail("Character.isJavaIdentifierPart")
                        .signature("Character.isJavaIdentifierPart(char ch)")
                        .returnType("boolean")
                        .owner("java.lang.Character")
                        .category("Method")
                        .documentation("Determines if the character may be part of a Java identifier (after the first character).")
                        .build(),

                // ── Byte Methods ──
                CompletionItem.builder("parseByte", "parseByte", CompletionItemKind.METHOD)
                        .detail("Byte.parseByte")
                        .signature("Byte.parseByte(String s)")
                        .returnType("byte")
                        .owner("java.lang.Byte")
                        .category("Method")
                        .documentation("Parses the given string as a signed decimal byte.")
                        .build(),

                // ── Short Methods ──
                CompletionItem.builder("parseShort", "parseShort", CompletionItemKind.METHOD)
                        .detail("Short.parseShort")
                        .signature("Short.parseShort(String s)")
                        .returnType("short")
                        .owner("java.lang.Short")
                        .category("Method")
                        .documentation("Parses the given string as a signed decimal short.")
                        .build(),

                // ── Long Methods ──
                CompletionItem.builder("parseLong", "parseLong", CompletionItemKind.METHOD)
                        .detail("Long.parseLong")
                        .signature("Long.parseLong(String s)")
                        .returnType("long")
                        .owner("java.lang.Long")
                        .category("Method")
                        .documentation("Parses the given string as a signed decimal long.")
                        .build(),

                CompletionItem.builder("toHexString", "toHexString", CompletionItemKind.METHOD)
                        .detail("Long.toHexString")
                        .signature("Long.toHexString(long i)")
                        .returnType("String")
                        .owner("java.lang.Long")
                        .category("Method")
                        .documentation("Returns a hexadecimal string representation of the long argument.")
                        .build(),

                // ── Float Methods ──
                CompletionItem.builder("parseFloat", "parseFloat", CompletionItemKind.METHOD)
                        .detail("Float.parseFloat")
                        .signature("Float.parseFloat(String s)")
                        .returnType("float")
                        .owner("java.lang.Float")
                        .category("Method")
                        .documentation("Parses the given string as a float value.")
                        .build(),

                CompletionItem.builder("isNaN", "isNaN", CompletionItemKind.METHOD)
                        .detail("Float.isNaN")
                        .signature("Float.isNaN(float v)")
                        .returnType("boolean")
                        .owner("java.lang.Float")
                        .category("Method")
                        .documentation("Tests whether the specified float value is Not-a-Number (NaN).")
                        .build(),

                CompletionItem.builder("isInfinite", "isInfinite", CompletionItemKind.METHOD)
                        .detail("Float.isInfinite")
                        .signature("Float.isInfinite(float v)")
                        .returnType("boolean")
                        .owner("java.lang.Float")
                        .category("Method")
                        .documentation("Tests whether the specified float value is infinite.")
                        .build(),

                // ── Runtime Methods ──
                CompletionItem.builder("getRuntime", "getRuntime", CompletionItemKind.METHOD)
                        .detail("Runtime.getRuntime")
                        .signature("Runtime.getRuntime()")
                        .returnType("Runtime")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Returns the singleton Runtime instance associated with the current Java application.")
                        .build(),

                CompletionItem.builder("availableProcessors", "availableProcessors", CompletionItemKind.METHOD)
                        .detail("Runtime.availableProcessors")
                        .signature("Runtime.availableProcessors()")
                        .returnType("int")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Returns the number of processors available to the Java Virtual Machine.")
                        .build(),

                CompletionItem.builder("freeMemory", "freeMemory", CompletionItemKind.METHOD)
                        .detail("Runtime.freeMemory")
                        .signature("Runtime.freeMemory()")
                        .returnType("long")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Returns the amount of free memory in the JVM, in bytes.")
                        .build(),

                CompletionItem.builder("totalMemory", "totalMemory", CompletionItemKind.METHOD)
                        .detail("Runtime.totalMemory")
                        .signature("Runtime.totalMemory()")
                        .returnType("long")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Returns the total amount of memory in the JVM, in bytes.")
                        .build(),

                CompletionItem.builder("maxMemory", "maxMemory", CompletionItemKind.METHOD)
                        .detail("Runtime.maxMemory")
                        .signature("Runtime.maxMemory()")
                        .returnType("long")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Returns the maximum amount of memory the JVM will attempt to use, in bytes.")
                        .build(),

                CompletionItem.builder("exec", "exec", CompletionItemKind.METHOD)
                        .detail("Runtime.exec")
                        .signature("Runtime.exec(String command)")
                        .returnType("Process")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Executes the specified system command in a separate native process.")
                        .build(),

                CompletionItem.builder("addShutdownHook", "addShutdownHook", CompletionItemKind.METHOD)
                        .detail("Runtime.addShutdownHook")
                        .signature("Runtime.addShutdownHook(Thread hook)")
                        .returnType("void")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Registers a new virtual-machine shutdown hook thread.")
                        .build(),

                CompletionItem.builder("halt", "halt", CompletionItemKind.METHOD)
                        .detail("Runtime.halt")
                        .signature("Runtime.halt(int status)")
                        .returnType("void")
                        .owner("java.lang.Runtime")
                        .category("Method")
                        .documentation("Forcibly terminates the JVM without running shutdown hooks or finalizers.")
                        .build(),

                // ── ThreadLocal Methods ──
                CompletionItem.builder("get", "get", CompletionItemKind.METHOD)
                        .detail("ThreadLocal.get")
                        .signature("ThreadLocal.get()")
                        .returnType("T")
                        .owner("java.lang.ThreadLocal")
                        .category("Method")
                        .documentation("Returns the value in the current thread's copy of this thread-local variable.")
                        .build(),

                CompletionItem.builder("set", "set", CompletionItemKind.METHOD)
                        .detail("ThreadLocal.set")
                        .signature("ThreadLocal.set(T value)")
                        .returnType("void")
                        .owner("java.lang.ThreadLocal")
                        .category("Method")
                        .documentation("Sets the current thread's copy of this thread-local variable to the specified value.")
                        .build(),

                CompletionItem.builder("remove", "remove", CompletionItemKind.METHOD)
                        .detail("ThreadLocal.remove")
                        .signature("ThreadLocal.remove()")
                        .returnType("void")
                        .owner("java.lang.ThreadLocal")
                        .category("Method")
                        .documentation("Removes the current thread's value from this thread-local variable.")
                        .build(),

                CompletionItem.builder("withInitial", "withInitial", CompletionItemKind.METHOD)
                        .detail("ThreadLocal.withInitial")
                        .signature("ThreadLocal.withInitial(Supplier<? extends S> supplier)")
                        .returnType("ThreadLocal<S>")
                        .owner("java.lang.ThreadLocal")
                        .category("Method")
                        .documentation("Creates a thread-local variable with an initial value supplied by the given Supplier.")
                        .build(),

                // ── Throwable / Exception Methods ──
                CompletionItem.builder("getMessage", "getMessage", CompletionItemKind.METHOD)
                        .detail("Throwable.getMessage")
                        .signature("Throwable.getMessage()")
                        .returnType("String")
                        .owner("java.lang.Throwable")
                        .category("Method")
                        .documentation("Returns the detail message string of this throwable.")
                        .build(),

                CompletionItem.builder("getCause", "getCause", CompletionItemKind.METHOD)
                        .detail("Throwable.getCause")
                        .signature("Throwable.getCause()")
                        .returnType("Throwable")
                        .owner("java.lang.Throwable")
                        .category("Method")
                        .documentation("Returns the cause of this throwable, or null if the cause is nonexistent or unknown.")
                        .build(),

                CompletionItem.builder("printStackTrace", "printStackTrace", CompletionItemKind.METHOD)
                        .detail("Throwable.printStackTrace")
                        .signature("Throwable.printStackTrace()")
                        .returnType("void")
                        .owner("java.lang.Throwable")
                        .category("Method")
                        .documentation("Prints this throwable and its backtrace to the standard error stream.")
                        .build(),

                CompletionItem.builder("getStackTrace", "getStackTrace", CompletionItemKind.METHOD)
                        .detail("Throwable.getStackTrace")
                        .signature("Throwable.getStackTrace()")
                        .returnType("StackTraceElement[]")
                        .owner("java.lang.Throwable")
                        .category("Method")
                        .documentation("Provides programmatic access to the stack trace information.")
                        .build(),

                CompletionItem.builder("initCause", "initCause", CompletionItemKind.METHOD)
                        .detail("Throwable.initCause")
                        .signature("Throwable.initCause(Throwable cause)")
                        .returnType("Throwable")
                        .owner("java.lang.Throwable")
                        .category("Method")
                        .documentation("Initializes the cause of this throwable to the specified value.")
                        .build(),

                // ── println / print / printf ──
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
                        .build()
        );
    }
}
