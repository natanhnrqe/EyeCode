---
id: java/jdk/string/is-blank
title: String.isBlank()
concept: string-is-blank
kind: method
sourceMember: isBlank
level: beginner
duration: 1
category: JAVA API
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---

## What it does

`isBlank()` is true when a string is empty or contains only whitespace.

```java
boolean missing = input.isBlank();
```

Use it when whitespace-only input should count as missing.

Unlike `isEmpty()`, this method treats whitespace-only text as blank.
