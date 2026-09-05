---
id: java/syntax/literals/null
title: null
concept: null
level: beginner
duration: 2
category: LITERAIS
depth: quick
related:
  - java/syntax/types/instanceof
  - java/syntax/control-flow/if
---
`null` significa que uma referência de objeto não aponta atualmente para nenhum objeto. Ele não é um objeto nem um tipo.

```java
if (value == null) { return; }
```

Chamar um membro de instância por meio de `null` causa `NullPointerException`.
