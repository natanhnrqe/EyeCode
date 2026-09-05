---
id: java/jdk/byte
title: Byte
concept: byte-wrapper
level: beginner
duration: 3
category: API JAVA
officialDocs:
  label: Byte
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Byte.html
related:
  - java/jdk/number
  - java/jdk/short
  - java/basics/primitive-types
---
Byte é o wrapper de byte, um inteiro com sinal de 8 bits. Use-o quando uma API exige objeto ou quando null tem significado.

```java
byte nivel = 3;
Byte objeto = Byte.valueOf(nivel);

byte lido = Byte.parseByte("12");
System.out.println(objeto);
System.out.println(Byte.MAX_VALUE);
```

parseByte retorna byte. valueOf retorna Byte. Boxing e unboxing fazem a conversão automática quando apropriado.
