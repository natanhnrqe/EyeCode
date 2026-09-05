---
id: java/generics/generic-methods
title: Métodos genéricos
concept: generic-methods
level: beginner
duration: 3
category: FUNDAMENTOS
related:
  - java/generics/generics
  - java/generics/generic-classes
  - java/generics/type-bounds
---
Um método genérico declara seu parâmetro de tipo antes do retorno.

```java
static <T> T primeiro(T[] valores) {
    return valores[0];
}

String nome = primeiro(new String[]{"Ana", "Bruno"});
Integer numero = primeiro(new Integer[]{10, 20});

System.out.println(nome);
System.out.println(numero);
```

O compilador infere T nas chamadas quando os argumentos fornecem informação suficiente.
