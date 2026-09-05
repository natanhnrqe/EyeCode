---
id: java/oop/instance-members
title: membros de instância
concept: instance-members
level: beginner
duration: 2
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/oop/fields
  - java/methods/declaration
  - java/syntax/modifiers/static
  - java/syntax/objects/this
  - java/types/object
---
Membros de instância são campos e métodos associados a um objeto. Eles operam no contexto daquela instância e podem acessar seu estado por meio de this.

~~~java
class Conta {
    private double saldo;

    void depositar(double valor) {
        saldo += valor;
    }
}
~~~

Cada objeto pode ter valores de instância diferentes. Membros static pertencem à classe e não dependem de um objeto específico.
