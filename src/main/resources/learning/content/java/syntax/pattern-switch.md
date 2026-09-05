---
id: java/syntax/pattern-switch
title: Pattern matching no switch
concept: pattern-switch
level: advanced
duration: 8
category: JAVA MODERNO
depth: full
related:
  - java/syntax/pattern-matching
  - java/syntax/types/instanceof
  - java/syntax/control-flow/switch-expressions
  - java/types/sealed
---
Um switch pode testar padrões de tipo e declarar uma variável para cada ramo.

~~~java
static String descrever(Object valor) {
    return switch (valor) {
        case Integer numero -> "Inteiro: " + numero;
        case String texto -> "Texto: " + texto;
        case null -> "Nulo";
        default -> "Outro";
    };
}
~~~

Os padrões são avaliados na ordem. Um padrão amplo antes de um padrão específico torna o caso específico inalcançável, por isso os tipos específicos devem vir primeiro. O caso null explicita o tratamento do valor nulo.

O recurso combina bem com hierarquias sealed. Pattern matching no switch tornou-se padrão no Java 21.
