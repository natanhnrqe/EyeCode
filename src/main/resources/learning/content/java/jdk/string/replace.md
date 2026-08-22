---
id: java/jdk/string/replace
title: String.replace()
concept: string-replace
kind: method
sourceMember: replace
level: beginner
duration: 1
category: JAVA API
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---

## What it does

`replace()` returns a new string with matching characters or sequences replaced.

```java
String normalized = "Eye Code".replace(" ", "");
```

The original String is unchanged.

For regular-expression replacement, use `replaceAll()` instead. For a single
literal first match, compare the behavior with `replaceFirst()`.
