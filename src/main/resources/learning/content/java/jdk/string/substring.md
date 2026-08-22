---
id: java/jdk/string/substring
title: String.substring()
concept: string-substring
kind: method
sourceMember: substring
level: beginner
duration: 2
category: JAVA API
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---

## What it does

`substring(begin, end)` returns the characters from `begin`, inclusive, to `end`, exclusive.

```java
String code = "EyeCode".substring(0, 3);
// Eye
```

Indexes must be within the string and in ascending order.

## Common signatures

Use `substring(beginIndex)` to take everything from an index to the end, or
`substring(beginIndex, endIndex)` for an exclusive upper bound.

The returned value is a new String view of the selected text. The original
String remains unchanged.
