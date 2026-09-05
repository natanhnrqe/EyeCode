---
id: java/methods/recursion
title: recursão
concept: recursion
level: beginner
duration: 3
category: MÉTODOS
depth: full
related:
  - java/methods/declaration
  - java/methods/parameters
  - java/syntax/control-flow/return
  - java/syntax/control-flow/if
---
Recursão acontece quando um método chama a si mesmo. Toda recursão precisa de um caso base, que encerra as chamadas, e de um passo que aproxima o problema desse caso.

```java
int fatorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * fatorial(n - 1);
}
```

Sem um caso base correto, as chamadas podem continuar até esgotar a pilha. Recursão pode deixar uma solução clara, mas um laço costuma ser mais simples ou econômico em alguns problemas.
