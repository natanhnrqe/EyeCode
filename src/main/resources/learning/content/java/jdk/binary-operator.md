---
id: java/jdk/binary-operator
title: BinaryOperator
concept: binary-operator
level: intermediate
duration: 4
category: API JAVA
officialDocs:
  label: BinaryOperator
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/BinaryOperator.html
related:
  - java/jdk/function
  - java/jdk/comparator
  - java/syntax/lambda
---
BinaryOperator é uma especialização de BiFunction em que os dois operandos e o resultado têm o mesmo tipo T.

~~~java
BinaryOperator<Integer> somar = (a, b) -> a + b;
System.out.println(somar.apply(2, 3));
~~~

Use BinaryOperator para combinar dois valores homogêneos em outro valor do mesmo tipo.
