---
id: java/oop/polymorphism
title: polimorfismo
concept: polymorphism
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/oop/inheritance
  - java/oop/method-overriding
  - java/types/interface
  - java/syntax/types/instanceof
  - java/jdk/object
---
Polimorfismo permite usar uma referência de supertipo ou interface para trabalhar com objetos de tipos concretos diferentes.

~~~java
Animal animal = new Cachorro();
animal.emitirSom();
~~~

A referência determina os membros acessíveis em compilação, mas a implementação sobrescrita do objeto real é escolhida em tempo de execução. Isso permite código dependente de abstrações, não de cada classe concreta.
