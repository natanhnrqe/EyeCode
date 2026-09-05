---
id: java/jdk/char-sequence
title: CharSequence
concept: char-sequence
level: beginner
duration: 3
category: API JAVA
officialDocs:
  label: CharSequence
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/CharSequence.html
related:
  - java/jdk/string
  - java/jdk/string-builder
  - java/basics/reference-types
---
CharSequence é uma interface para sequências legíveis de caracteres. String e StringBuilder podem ser usados como CharSequence.

```java
static void imprimir(CharSequence texto) {
    System.out.println(texto);
}

imprimir("EyeCode");
imprimir(new StringBuilder("Java"));
```

A interface oferece length(), charAt() e subSequence(), permitindo que o método aceite diferentes representações de texto.

```java
CharSequence texto = "Java";
System.out.println(texto.length());
System.out.println(texto.charAt(0));
System.out.println(texto.subSequence(0, 2));
```
