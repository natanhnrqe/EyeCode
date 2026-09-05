---
id: java/oop/fields
title: campos
concept: fields
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/basics/variables
  - java/syntax/modifiers/static
  - java/oop/encapsulation
  - java/oop/instance-members
  - java/syntax/objects/this
---
Campo é uma variável declarada como membro de uma classe. Campos de instância representam estado de cada objeto; campos static pertencem à classe.

~~~java
class Conta {
    private double saldo;
}
~~~

Diferencie campo de variável local e parâmetro: o campo pertence ao objeto ou à classe e pode ter valor padrão; a variável local e o parâmetro vivem no escopo do método e precisam ser usados conforme suas regras de inicialização.
