---
id: java/syntax/lambda
title: Expressões lambda
concept: lambda
level: intermediate
duration: 8
category: JAVA MODERNO
depth: full
related:
  - java/syntax/functional-interfaces
  - java/syntax/method-references
  - java/jdk/comparator
---
Uma expressão lambda representa um comportamento compatível com uma interface funcional. Ela precisa de um tipo-alvo; não é uma função independente solta no Java.

~~~java
Runnable tarefa = () -> {
    System.out.println("Executando");
};
tarefa.run();

Comparator<String> porTamanho =
        (a, b) -> Integer.compare(a.length(), b.length());
~~~

Os parâmetros aparecem antes da seta e o corpo depois dela. Uma lambda pode ter nenhum parâmetro, um parâmetro sem parênteses ou vários parâmetros entre parênteses. O corpo pode ser uma expressão ou um bloco de instruções.

O tipo-alvo Comparator<String> informa os tipos dos parâmetros e o contrato que a lambda deve cumprir. Lambdas tornaram-se padrão no Java 8.

~~~java
String prefixo = "Olá, ";
Consumer<String> imprimir =
        nome -> System.out.println(prefixo + nome);
~~~

Uma variável local capturada precisa ser final ou efetivamente final: depois de inicializada, não pode receber outra atribuição.
