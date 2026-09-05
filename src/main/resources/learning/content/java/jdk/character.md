---
id: java/jdk/character
title: Character
concept: character-wrapper
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Character
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Character.html
related:
  - java/basics/primitive-types
  - java/jdk/string
  - java/jdk/char-sequence
---
Character é o wrapper de char e oferece operações de classificação e conversão.

```java
char caractere = 'A';

System.out.println(Character.isLetter(caractere));
System.out.println(Character.isDigit(caractere));
System.out.println(Character.toLowerCase(caractere));
```

char representa uma unidade de código UTF-16. Alguns pontos de código Unicode exigem um par substituto, então nem todo caractere visual cabe em um único char.
