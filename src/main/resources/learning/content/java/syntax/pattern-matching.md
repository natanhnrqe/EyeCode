---
id: java/syntax/pattern-matching
title: Pattern matching
concept: pattern-matching
level: intermediate
duration: 7
category: JAVA MODERNO
depth: full
related:
  - java/syntax/types/instanceof
  - java/syntax/pattern-switch
  - java/types/sealed
---
Pattern matching combina um teste de tipo com uma variável já convertida para esse tipo.

~~~java
if (objeto instanceof String texto) {
    System.out.println(texto.length());
}

if (objeto instanceof String texto && !texto.isBlank()) {
    System.out.println(texto);
}
~~~

A variável texto só existe nos pontos do fluxo de controle em que o teste garante que ela é String. A mesma ideia aparece em switch, com padrões de tipos e variáveis de padrão.

Pattern matching para instanceof tornou-se padrão no Java 16. Pattern matching no switch tornou-se padrão no Java 21.
