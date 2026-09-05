---
id: java/jdk/consumer
title: Consumer
concept: consumer
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Consumer
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Consumer.html
related:
  - java/syntax/lambda
  - java/jdk/supplier
  - java/jdk/function
---
Consumer representa uma operação que recebe um valor do tipo T e não produz resultado.

~~~java
Consumer<String> imprimir = texto -> System.out.println(texto);
imprimir.accept("Java");
~~~

O método accept entrega o valor à operação. Efeitos como impressão ou atualização de estado acontecem no corpo da lambda.
