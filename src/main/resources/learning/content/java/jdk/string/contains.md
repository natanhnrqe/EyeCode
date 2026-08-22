---
id: java/jdk/string/contains
title: String.contains()
concept: string-contains
kind: method
sourceMember: contains
level: beginner
duration: 1
category: JAVA API
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---

## What it does

`contains()` checks whether one string occurs inside another.

```java
boolean hasCode = "EyeCode".contains("Code");
```

It returns a boolean and is case-sensitive.

For a case-insensitive check, normalize both values deliberately rather than
assuming `contains()` ignores case.
