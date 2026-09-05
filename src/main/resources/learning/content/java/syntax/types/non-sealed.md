---
id: java/syntax/types/non-sealed
title: non-sealed
concept: non-sealed
level: intermediate
duration: 3
category: SISTEMA DE TIPOS
depth: quick
related:
  - java/syntax/types/sealed
  - java/syntax/modifiers/final
  - java/types/sealed
---
non-sealed reabre a extensão de um subtipo que participa de uma hierarquia sealed.

~~~java
sealed class Documento permits Fatura {
}
non-sealed class Fatura extends Documento {
}
class FaturaEspecial extends Fatura {
}
~~~

Ao contrário de final, non-sealed permite novos subtipos.
