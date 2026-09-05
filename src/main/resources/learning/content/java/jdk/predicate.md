---
id: java/jdk/predicate
title: Predicate
concept: predicate
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Predicate
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Predicate.html
related:
  - java/jdk/function
  - java/jdk/consumer
  - java/syntax/lambda
---
Predicate representa um teste que recebe T e devolve boolean.

~~~java
Predicate<Integer> positivo = numero -> numero > 0;
System.out.println(positivo.test(10));
~~~

O método test responde à condição. Predicados podem ser combinados para formar filtros mais expressivos.
