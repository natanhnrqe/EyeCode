---
id: java/types/object
title: Objetos em Java
concept: object
level: beginner
duration: 8
category: CONCEITO JAVA
officialDocs:
  label: A classe Object
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html
related:
  - java/types/class
next: java/types/class
---
## O que é isso?

Um objeto é uma instância de uma classe em tempo de execução. Ele combina estado concreto com o comportamento definido pela classe.

```java
Counter counter = new Counter();
counter.increment();
```

## Como funciona?

A classe define a estrutura; cada objeto criado a partir dela mantém seus próprios valores de instância. Métodos podem ler ou alterar esse estado respeitando as regras da classe.

Todo objeto possui identidade, estado e comportamento. Uma variável de referência aponta para o objeto, mas não é o objeto em si.

## Relação com classes

A classe é o molde e o objeto é uma instância concreta desse molde. A mesma classe pode criar muitos objetos independentes.

## Cuidados comuns

Não confunda duas referências com dois objetos: duas variáveis podem apontar para a mesma instância. Para comparar estado, use `equals` quando o tipo o definir; `==` compara identidade de referência.
