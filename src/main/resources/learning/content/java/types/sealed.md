---
id: java/types/sealed
title: Classes e interfaces seladas
concept: sealed
level: intermediate
duration: 8
category: JAVA MODERNO
depth: full
related:
  - java/oop/inheritance
  - java/oop/polymorphism
  - java/syntax/modifiers/final
  - java/syntax/pattern-matching
---
Uma classe ou interface sealed limita quais tipos podem ser seus subtipos diretos. A cláusula permits declara esse conjunto.

~~~java
sealed interface Forma permits Circulo, Retangulo {
}

final class Circulo implements Forma {
}

final class Retangulo implements Forma {
}
~~~

Cada subtipo permitido precisa declarar uma continuação: final encerra a hierarquia, sealed restringe a próxima camada e non-sealed reabre a possibilidade de extensão.

~~~java
sealed class Documento permits Fatura {
}

non-sealed class Fatura extends Documento {
}

class FaturaEspecial extends Fatura {
}
~~~

Final, sealed e non-sealed têm efeitos diferentes. Tipos selados tornaram-se padrão no Java 17.
