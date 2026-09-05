---
id: java/oop/method-overriding
title: sobrescrita de métodos
concept: method-overriding
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/oop/inheritance
  - java/oop/polymorphism
  - java/methods/overloading
  - java/syntax/types/extends
  - java/syntax/types/implements
---
Sobrescrita ocorre quando uma subclasse fornece nova implementação para um método de instância herdado com assinatura compatível.

~~~java
class Animal {
    void emitirSom() {
        System.out.println("Som");
    }
}

class Cachorro extends Animal {
    @Override
    void emitirSom() {
        System.out.println("Au");
    }
}
~~~

A anotação Override ajuda o compilador a verificar a intenção. Sobrescrita é diferente de sobrecarga: sobrecarga muda a lista de parâmetros. Métodos static não participam da sobrescrita polimórfica comum.
