---
id: java/jdk/string/length
title: String.length()
concept: string-length
kind: method
sourceMember: length
level: beginner
duration: 1
category: JAVA API
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---

## What it does

`length()` returns the number of UTF-16 code units in a string.

```java
int count = "EyeCode".length();
```

The result is zero for an empty string.

`length()` returns an `int`. For Unicode supplementary characters, remember
that the count is UTF-16 code units rather than user-perceived grapheme count.
