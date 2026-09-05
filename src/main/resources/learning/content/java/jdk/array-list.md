---
id: java/jdk/array-list
title: ArrayList
concept: array-list
level: beginner
duration: 4
category: COLEÇÕES
officialDocs:
  label: ArrayList
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html
related:
  - java/jdk/list
parent: java/jdk/list
---
## O que é isso?

`ArrayList` é uma implementação redimensionável de `List`. É uma boa escolha padrão quando o acesso por índice é frequente.

## Como funciona?

A lista mantém um `size` lógico separado da capacidade de armazenamento. Adicionar ao final costuma ser barato, enquanto inserir ou remover no início desloca os elementos seguintes. Leituras por índice são rápidas porque a posição corresponde diretamente ao array interno.

```java
ArrayList<String> names = new ArrayList<>();
names.add("Ada");
String first = names.get(0);
```

O armazenamento contíguo também costuma ter boa localidade de cache. Não presuma que toda operação é tempo constante nem que a capacidade faz parte do contrato público.

Em comparação com `LinkedList`, `ArrayList` normalmente usa menos memória por elemento e é melhor para acesso indexado. Escolha de acordo com as operações reais do código.
