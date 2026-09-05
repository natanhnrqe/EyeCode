---
id: java/syntax/types/instanceof
title: instanceof
concept: instanceof
level: beginner
duration: 1
category: SISTEMA DE TIPOS
depth: quick
related:
  - java/syntax/literals/null
  - java/types/class
---
`instanceof` verifica se uma referência é compatível com um tipo. O resultado é falso para `null`.

O Java moderno pode vincular uma variável correspondente no próprio teste.


~~~java
if (objeto instanceof String texto) {
    System.out.println(texto.length());
}
~~~

A variável de padrão só fica disponível nos pontos do fluxo em que o teste garante o tipo. Pattern matching para instanceof tornou-se padrão no Java 16.