---
id: java/syntax/types/sealed
title: sealed
concept: sealed
level: intermediate
duration: 3
category: SISTEMA DE TIPOS
depth: quick
related:
  - java/types/sealed
  - java/syntax/types/permits
---
sealed restringe os subtipos diretos permitidos para uma classe ou interface.

~~~java
sealed interface Forma permits Circulo {
}
final class Circulo implements Forma {
}
~~~

A palavra-chave faz parte dos tipos selados, que se tornaram padrão no Java 17.
