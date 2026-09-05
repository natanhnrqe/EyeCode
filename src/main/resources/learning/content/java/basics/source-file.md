---
id: java/basics/source-file
title: Arquivo-fonte Java
concept: source-file
level: beginner
duration: 4
category: FUNDAMENTOS
officialDocs:
  label: Java Language Specification
  url: https://docs.oracle.com/javase/specs/
related:
  - java/types/class
  - java/syntax/organization/package
  - java/syntax/organization/import
  - java/basics/identifiers
  - java/basics/variables
---
Um arquivo `.java` é um arquivo-fonte que contém código Java. Ele normalmente reúne uma unidade de compilação: a declaração opcional de pacote, os imports e uma ou mais declarações de tipos.

## Estrutura básica

```java
package com.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> names = List.of("Ada");
    }
}
```

A declaração `package`, quando existe, vem antes dos `import`. Os imports vêm antes das classes, interfaces, enums ou records declarados no arquivo.

## Nome do arquivo

Uma classe de nível superior declarada como `public` deve ter o mesmo nome do arquivo. Por exemplo, `public class Main` deve estar em `Main.java`. Um arquivo também pode conter tipos de nível superior não públicos, mas manter um tipo principal por arquivo costuma deixar a organização mais clara.

O arquivo-fonte é a unidade de organização; a classe, seus métodos e suas variáveis são os elementos que formam o programa.

## Cuidados comuns

- O pacote precisa ser a primeira declaração de código do arquivo.
- O caminho das pastas deve corresponder ao pacote em projetos convencionais.
- O nome do arquivo precisa acompanhar o tipo público.
- `import` apenas torna nomes disponíveis; ele não cria objetos nem executa código.