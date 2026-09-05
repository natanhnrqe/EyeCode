---
id: java/basics/reference-types
title: Tipos de referência em Java
concept: reference-types
level: beginner
duration: 5
category: FUNDAMENTOS
related:
  - java/basics/primitive-types
  - java/types/object
  - java/types/class
  - java/types/interface
  - java/syntax/literals/null
---
Uma variável de tipo de referência guarda uma referência para um objeto ou array. Classes, interfaces e arrays são exemplos de tipos de referência.

```java
String first = new String("Java");
String second = first;

System.out.println(first == second); // true: as referências apontam para o mesmo objeto
```

A atribuição copia o valor da referência; ela não cria automaticamente outro objeto. Por isso, duas variáveis podem apontar para a mesma instância. Alterar um objeto por uma referência pode ser observado por outra referência que aponta para ele.

Uma referência também pode ser `null`, o que significa que ela não aponta para um objeto naquele momento:

```java
String name = null;
if (name != null) {
    System.out.println(name.length());
}
```

Tipos de referência e tipos primitivos têm regras diferentes de atribuição, comparação e conversão. Não é necessário assumir uma divisão fixa entre stack e heap para entender esse comportamento; concentre-se na diferença entre valor primitivo e referência.