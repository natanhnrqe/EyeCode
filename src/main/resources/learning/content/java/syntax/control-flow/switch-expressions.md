---
id: java/syntax/control-flow/switch-expressions
title: Expressões switch
concept: switch-expression
level: intermediate
duration: 8
category: JAVA MODERNO
depth: full
related:
  - java/syntax/control-flow/switch
  - java/syntax/control-flow/case
  - java/syntax/control-flow/yield
  - java/syntax/pattern-switch
---
Uma expressão switch produz um valor. Casos com seta evitam o fall-through implícito dos casos tradicionais.

~~~java
int dia = 2;
String nome = switch (dia) {
    case 1 -> "Segunda";
    case 2 -> "Terça";
    default -> "Outro";
};
~~~

A expressão precisa cobrir os valores possíveis, normalmente com default. Um ramo pode usar um bloco e yield para entregar o resultado:

~~~java
String resultado = switch (codigo) {
    case 200 -> "OK";
    case 404 -> "Não encontrado";
    default -> {
        System.out.println("Código: " + codigo);
        yield "Outro";
    }
};
~~~

Yield produz o valor da expressão switch; não retorna do método. Expressões switch tornaram-se padrão no Java 14.
