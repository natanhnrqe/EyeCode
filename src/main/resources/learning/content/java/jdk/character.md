---
id: java/jdk/character
title: Character
concept: character-wrapper
level: beginner
duration: 4
members:
- isLetter(): java/jdk/character/is-letter
- isDigit(): java/jdk/character/is-digit
- isWhitespace(): java/jdk/character/is-whitespace
- isUpperCase(): java/jdk/character/is-upper-case
- isLowerCase(): java/jdk/character/is-lower-case
- toUpperCase(): java/jdk/character/to-upper-case
- toLowerCase(): java/jdk/character/to-lower-case
- getNumericValue(): java/jdk/character/get-numeric-value
- compare(): java/jdk/character/compare
- charValue(): java/jdk/character/char-value
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
