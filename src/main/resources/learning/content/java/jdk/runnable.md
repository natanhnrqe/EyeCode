---
id: java/jdk/runnable
title: Runnable
concept: runnable
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Runnable
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html
related:
  - java/syntax/lambda
  - java/syntax/functional-interfaces
---
Runnable representa uma tarefa sem parâmetros e sem valor de retorno. Por ser uma interface funcional, pode receber uma lambda.

~~~java
Runnable tarefa = () -> System.out.println("Executando");
tarefa.run();
~~~

Chamar run executa a tarefa no fluxo atual. Runnable descreve o trabalho; ele não cria uma thread por si só.
