---
id: java/jdk/string/split
title: String.split()
concept: string-split
kind: method
sourceMember: split
level: beginner
duration: 2
category: JAVA API
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---

## What it does

`split()` divides a string around a regular-expression delimiter.

```java
String[] words = "learn java".split(" ");
```

Remember that the delimiter is a regular expression, not always a literal character.

The result is a new array. Use the overload with a limit when trailing empty
fields or the number of splits matters.
