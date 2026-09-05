---
id: java/generics/generic-classes
title: Classes genéricas
concept: generic-classes
level: beginner
duration: 4
category: FUNDAMENTOS
related:
  - java/generics/generics
  - java/generics/generic-methods
  - java/generics/type-bounds
---
Uma classe genérica recebe um parâmetro de tipo. T é um nome escolhido pelo autor, não um tipo especial do Java.

```java
class Caixa<T> {
    private T valor;

    void guardar(T valor) {
        this.valor = valor;
    }

    T obter() {
        return valor;
    }
}

Caixa<String> texto = new Caixa<>();
texto.guardar("EyeCode");

Caixa<Integer> numero = new Caixa<>();
numero.guardar(42);
```

Cada uso escolhe o tipo que a caixa armazenará.
