---
id: java/jdk/function
title: Function
concept: function
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Function
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html
related:
  - java/jdk/predicate
  - java/jdk/consumer
  - java/jdk/supplier
  - java/generics/generics
---
Function representa uma transformação de T para R.

~~~java
Function<String, Integer> tamanho = texto -> texto.length();
int quantidade = tamanho.apply("EyeCode");
~~~

O método apply recebe a entrada e devolve o resultado. Os tipos T e R podem ser diferentes.
