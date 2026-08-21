---
id: java/basics/variables
title: Java Variables
concept: variable
level: beginner
duration: 5
category: BASICS
officialDocs:
  label: Java Language Specification
  url: https://docs.oracle.com/javase/specs/
related:
  - java/types/class
next: java/types/class
---

Variables give a name to data that a Java program needs to remember. A variable has a **type**, a name, and a value.

## Declaring a variable

Use a type such as `int`, followed by a descriptive name:

```java
int score = 10;
String playerName = "Ada";
```

The value of `score` can change later in the program.

### Useful habits

- Choose names that explain the data.
- Initialise a variable before reading it.
- Keep the type aligned with the value you need to store.

## Steps

1. Choose the value you need to store.
2. Choose its Java type.
3. Give the variable a clear name.

> A variable is a labelled place in memory. Its type tells Java which values are valid there.

| Type | Example value | Typical use |
| --- | --- | --- |
| `int` | `10` | Whole numbers |
| `String` | `"Ada"` | Text |
| `boolean` | `true` | Yes or no |

Read more in the [Java language specification](https://docs.oracle.com/javase/specs/).
