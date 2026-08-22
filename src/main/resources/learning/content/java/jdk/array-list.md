---
id: java/jdk/array-list
title: ArrayList
concept: array-list
level: beginner
duration: 4
category: COLLECTIONS
officialDocs:
  label: ArrayList
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html
related:
  - java/jdk/list
parent: java/jdk/list
---

## What it is

`ArrayList` is a resizable array implementation of `List`. It is a good default when indexed access is common.

## How it behaves

The list tracks a logical `size` separately from its backing storage
`capacity`. Appending is usually cheap, while inserting or removing near the
front shifts later elements. Indexed reads are fast because the position maps
directly to an array slot.

That contiguous storage also tends to have good cache locality. `ArrayList`
is often a sensible default, but it is not a promise that every operation is
constant time or that capacity is part of the public API contract.

Compared with `LinkedList`, it usually uses less per-element overhead and is
better for indexed access. Choose based on the operations your code performs.
