---
id: java/oop/encapsulation
title: encapsulamento
concept: encapsulation
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/oop/access-modifiers
  - java/oop/fields
  - java/oop/instance-members
  - java/syntax/visibility/private
  - java/syntax/visibility/public
---
Encapsulamento é controlar o acesso ao estado e à implementação de um objeto. O objetivo é proteger invariantes e expor operações significativas, não apenas criar getters e setters para tudo.

~~~java
class Conta {
    private double saldo;

    public void depositar(double valor) {
        if (valor > 0) {
            saldo += valor;
        }
    }

    public double getSaldo() {
        return saldo;
    }
}
~~~

Campos privados e métodos públicos podem formar uma API controlada. Um getter ou setter faz sentido quando representa uma operação válida para o domínio.
