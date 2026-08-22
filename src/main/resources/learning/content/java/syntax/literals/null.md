---
id: java/syntax/literals/null
title: null
concept: null
level: beginner
duration: 2
category: LITERALS
depth: quick
related:
  - java/syntax/types/instanceof
  - java/syntax/control-flow/if
---

`null` means an object reference currently points to no object. It is not an object or a type.

```java
if (value == null) { return; }
```

Calling an instance member through `null` causes `NullPointerException`.
