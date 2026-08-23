---
id: java/jdk/linked-list/get-first
title: LinkedList.getFirst()
concept: linked-list-get-first
kind: method
sourceMember: getFirst
level: beginner
duration: 1
category: COLLECTIONS
depth: quick
related:
  - java/jdk/linked-list
parent: java/jdk/linked-list
---

## What it does

Returns the first element.

```java
LinkedList<String> list = new LinkedList<>();
String first = list.getFirst();
```

It throws when the list is empty; check emptiness when needed.
