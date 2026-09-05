---
id: java/jdk/functional-interface
title: FunctionalInterface
concept: functional-interface
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: FunctionalInterface
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/FunctionalInterface.html
related:
  - java/syntax/functional-interfaces
  - java/syntax/lambda
---
A anotação FunctionalInterface documenta a intenção de que uma interface tenha um único método abstrato. Ela também faz o compilador validar essa regra.

~~~java
@FunctionalInterface
interface Conversor {
    String converter(int valor);
}
~~~

A anotação é opcional para a compatibilidade com lambdas. Métodos default e static não contam como métodos abstratos.
