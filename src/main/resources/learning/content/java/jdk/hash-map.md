---
id: java/jdk/hash-map
title: HashMap
concept: hash-map
level: beginner
duration: 4
category: COLLECTIONS
officialDocs:
  label: HashMap
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html
related:
  - java/jdk/map
parent: java/jdk/map
---

## What it is

`HashMap` stores key-value pairs and provides fast average lookup by key. It does not guarantee iteration order.

## The mental model

For a key, Java uses `hashCode()` to choose a bucket and then uses `equals()`
to find the matching key among entries in that bucket.

```text
key -> hashCode() -> bucket -> equals() -> value
```

Different keys can collide in one bucket. The map resizes as its load grows so
average lookup remains efficient, but the exact table strategy is an
implementation detail rather than a contract to program against.

## Common mistakes

Keys should have stable `hashCode()` and `equals()` behavior while stored. A
mutable key that changes after insertion can become difficult to find. Also,
use `Map` in APIs when callers need the abstraction, and `HashMap` when the
concrete implementation is an intentional choice.
