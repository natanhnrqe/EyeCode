---
id: java/jdk/linked-list/get
title: LinkedList.get()
concept: linked-list-get
kind: method
sourceMember: get
level: beginner
duration: 1
category: COLLECTIONS
depth: quick
related:
  - java/jdk/linked-list
parent: java/jdk/linked-list
---

## What it does

Returns the element at an index.

```java
LinkedList<String> list = new LinkedList<>();
String value = list.get(2);
```

Indexed access requires traversal, unlike an ArrayList lookup.
