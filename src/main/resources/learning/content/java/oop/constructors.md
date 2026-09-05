---
id: java/oop/constructors
title: construtores
concept: constructors
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/syntax/objects/new
  - java/syntax/objects/this
  - java/methods/parameters
  - java/methods/overloading
  - java/oop/fields
  - java/oop/access-modifiers
---
Um construtor inicializa uma nova instância. Ele tem o mesmo nome da classe e não possui tipo de retorno.

~~~java
class Pessoa {
    private String nome;

    Pessoa(String nome) {
        this.nome = nome;
    }
}
~~~

A criação usa new e escolhe o construtor compatível com os argumentos. Uma classe sem construtor declarado pode receber um construtor padrão sem argumentos, sujeito às regras de acesso. Construtores também podem ser sobrecarregados.
