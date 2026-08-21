---
id: java/jdk/string
title: String
concept: string
level: beginner
duration: 4
category: JAVA API
officialDocs:
  label: String
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html
related:
  - java/jdk/object
---

## What it is

`String` represents text. Strings are immutable, so operations create a new value.

```java
String greeting = "Hello";
String loud = greeting.toUpperCase();
```

Use `equals` to compare text, not `==`.
