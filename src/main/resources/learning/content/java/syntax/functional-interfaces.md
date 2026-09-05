---
id: java/syntax/functional-interfaces
title: Interfaces funcionais
concept: functional-interface
level: intermediate
duration: 8
category: JAVA MODERNO
depth: full
related:
  - java/syntax/lambda
  - java/syntax/method-references
  - java/jdk/function
  - java/jdk/consumer
  - java/jdk/supplier
---
Uma interface funcional tem exatamente um método abstrato. Esse contrato permite usar uma lambda ou referência de método como implementação.

~~~java
@FunctionalInterface
interface Operacao {
    int executar(int a, int b);
}

Operacao soma = (a, b) -> a + b;
System.out.println(soma.executar(2, 3));
~~~

Métodos default e static não contam como métodos abstratos. A anotação FunctionalInterface não é obrigatória, mas pede ao compilador que valide a intenção.

Supplier produz um valor, Consumer recebe um valor, Function transforma um valor e Predicate responde uma condição. UnaryOperator e BinaryOperator são especializações para operandos do mesmo tipo.

Interfaces funcionais e lambdas tornaram-se padrão no Java 8.
