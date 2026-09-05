---
id: java/jdk/string-builder
title: StringBuilder
concept: string-builder
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: StringBuilder
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html
related:
  - java/jdk/string
  - java/jdk/char-sequence
  - java/basics/reference-types
---
StringBuilder é uma sequência mutável, útil para construir texto em várias etapas.

```java
StringBuilder builder = new StringBuilder();

builder.append("Eye");
builder.append("Code");

String resultado = builder.toString();
System.out.println(resultado);
```

Em construções repetidas, operações como append evitam criar manualmente uma nova String a cada etapa.

```java
StringBuilder nomes = new StringBuilder();

for (int i = 1; i <= 3; i++) {
    nomes.append("Item ").append(i).append('\n');
}

System.out.println(nomes);
```

Também é possível usar insert, delete e replace. String é imutável; StringBuilder é mutável e não é sincronizado para mutações concorrentes compartilhadas.
