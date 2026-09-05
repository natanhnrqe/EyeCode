---
id: java/jdk/unary-operator
title: UnaryOperator
concept: unary-operator
level: intermediate
duration: 4
category: API JAVA
officialDocs:
  label: UnaryOperator
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/UnaryOperator.html
related:
  - java/jdk/function
  - java/syntax/lambda
---
UnaryOperator é uma especialização de Function em que a entrada e o resultado têm o mesmo tipo T.

~~~java
UnaryOperator<Integer> dobro = numero -> numero * 2;
System.out.println(dobro.apply(4));
~~~

Use UnaryOperator quando a operação transforma um valor sem mudar seu tipo.
