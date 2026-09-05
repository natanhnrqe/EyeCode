---
id: java/jdk/hash-map
title: HashMap
concept: hash-map
level: beginner
duration: 4
category: COLEÇÕES
officialDocs:
  label: HashMap
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html
related:
  - java/jdk/map
  - java/generics/generics
parent: java/jdk/map
---
## O que é isso?

`HashMap` armazena pares de chave e valor e oferece busca média rápida pela chave. Ele não garante a ordem de iteração.

## Como funciona?

Para uma chave, o Java usa `hashCode()` para escolher um compartimento e depois usa `equals()` para localizar a chave correspondente entre as entradas.

```text
chave -> hashCode() -> compartimento -> equals() -> valor
```

Quando muitas chaves caem no mesmo compartimento, a estrutura ainda mantém a busca eficiente em média, mas a estratégia exata da tabela é um detalhe de implementação.

## Erros comuns

As chaves devem manter um comportamento estável de `hashCode()` e `equals()` enquanto estiverem armazenadas. Use `Map` nas APIs quando os chamadores precisarem da abstração e `HashMap` quando a implementação concreta for necessária.
