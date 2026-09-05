---
id: java/oop/inheritance
title: herança
concept: inheritance
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/syntax/types/extends
  - java/syntax/objects/super
  - java/oop/method-overriding
  - java/oop/polymorphism
  - java/syntax/visibility/protected
  - java/jdk/object
---
Herança relaciona uma subclasse a uma superclasse. A subclasse recebe membros acessíveis e representa uma relação is-a útil quando há uma especialização clara.

~~~java
class Animal {
    void emitirSom() { }
}

class Cachorro extends Animal {
}
~~~

Uma classe Java estende diretamente no máximo uma classe. Interfaces oferecem outra forma de abstração. Herança não é sempre melhor que composição; escolha a relação que representa o domínio com clareza.
