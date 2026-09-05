---
id: java/jdk/supplier
title: Supplier
concept: supplier
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Supplier
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Supplier.html
related:
  - java/syntax/lambda
  - java/jdk/function
  - java/jdk/consumer
---
Supplier representa uma operação que não recebe entrada e produz um valor do tipo T.

~~~java
Supplier<String> gerarNome = () -> "EyeCode";
System.out.println(gerarNome.get());
~~~

O método get executa a produção quando é chamado. Supplier não recebe o valor produzido antecipadamente.
