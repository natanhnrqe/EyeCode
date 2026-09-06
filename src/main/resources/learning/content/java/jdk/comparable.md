---
id: java/jdk/comparable
title: Comparable
concept: comparable
level: beginner
duration: 4
members:
  - compareTo(): java/jdk/comparable/ordering
category: API JAVA
officialDocs:
  label: Comparable
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html
related:
  - java/jdk/integer
  - java/jdk/string
  - java/basics/reference-types
  - java/jdk/tree-set
  - java/jdk/tree-map
  - java/jdk/priority-queue
---
Comparable<T> define a ordenação natural de um tipo por meio de compareTo(T other).

```java
class Produto implements Comparable<Produto> {
    private final int preco;

    Produto(int preco) {
        this.preco = preco;
    }

    @Override
    public int compareTo(Produto outro) {
        return Integer.compare(this.preco, outro.preco);
    }
}
```

Um resultado negativo, zero ou positivo indica a ordem relativa.

```java
Produto a = new Produto(10);
Produto b = new Produto(20);
System.out.println(a.compareTo(b));
```

Não prefira subtrair valores em compareTo: a subtração pode sofrer overflow.
