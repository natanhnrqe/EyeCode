---
id: java/syntax/method-references
title: Referências de método
concept: method-reference
level: intermediate
duration: 7
category: JAVA MODERNO
depth: full
related:
  - java/syntax/lambda
  - java/syntax/functional-interfaces
  - java/jdk/comparator
---
Uma referência de método reaproveita um método existente como comportamento compatível com uma interface funcional. Ela não invoca o método imediatamente.

~~~java
Consumer<String> porLambda =
        texto -> System.out.println(texto);
Consumer<String> porReferencia = System.out::println;

Supplier<List<String>> criarLista = ArrayList::new;
~~~

As formas mais comuns são Type::staticMethod, objeto::instanceMethod, Type::instanceMethod e Type::new. O tipo-alvo determina os parâmetros e o resultado compatíveis.

Em System.out::println, Consumer<String> fornece o contexto funcional. Em ArrayList::new, Supplier<List<String>> espera um construtor sem argumentos.

Referências de método são uma forma curta de certas lambdas e fazem parte do recurso introduzido no Java 8.
