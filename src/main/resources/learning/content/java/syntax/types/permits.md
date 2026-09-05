---
id: java/syntax/types/permits
title: permits
concept: permits
level: intermediate
duration: 3
category: SISTEMA DE TIPOS
depth: quick
related:
  - java/syntax/types/sealed
  - java/types/sealed
---
permits declara os subtipos diretos autorizados por uma classe ou interface sealed.

~~~java
sealed interface Forma permits Circulo, Retangulo {
}
~~~

Cada tipo listado deve seguir a regra final, sealed ou non-sealed.
