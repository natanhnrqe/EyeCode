---
id: java/jdk/linked-list
title: LinkedList
concept: linked-list
level: beginner
duration: 8
category: COLLECTIONS
officialDocs:
  label: LinkedList
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html
related:
  - java/jdk/array-list
  - java/jdk/list
members:
  - add(): java/jdk/linked-list/add
  - addFirst(): java/jdk/linked-list/add-first
  - addLast(): java/jdk/linked-list/add-last
  - getFirst(): java/jdk/linked-list/get-first
  - getLast(): java/jdk/linked-list/get-last
  - removeFirst(): java/jdk/linked-list/remove-first
  - removeLast(): java/jdk/linked-list/remove-last
  - get(): java/jdk/linked-list/get
  - size(): java/jdk/linked-list/size
---

## The mental model

A linked list stores a chain of elements rather than one contiguous array. Java
`LinkedList` is doubly linked: each conceptual node keeps an item, a reference
to the previous node, and a reference to the next node.

```text
first                         last
  |                             |
  v                             v
[A] <-> [B] <-> [C] <-> [D]
```

The list keeps references to its first and last nodes. Traversal follows links
until it reaches the requested position.

## Operations and trade-offs

Adding or removing at an already-known end is natural with `addFirst()`,
`addLast()`, `removeFirst()`, and `removeLast()`. Finding an arbitrary index
still requires traversal, so `get(index)` is not an array-style constant-time
operation. The implementation may choose the nearer end, but it still walks
links.

Each element carries link overhead, and separately allocated nodes usually have
less favorable cache locality than an `ArrayList`'s contiguous storage. That
means `LinkedList` is not automatically better for insertion or removal: if
your code must first search for the position, traversal may dominate. Prefer
the structure whose access pattern matches the real workload.

## Useful members

`add()`, `addFirst()`, `addLast()`, `getFirst()`, `getLast()`, `removeFirst()`,
`removeLast()`, `get()`, and `size()` cover common list usage. The type also
implements the `List` and `Deque` abstractions.
