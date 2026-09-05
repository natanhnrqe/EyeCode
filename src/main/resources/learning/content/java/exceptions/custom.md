---
id: java/exceptions/custom
title: Exceções personalizadas
concept: custom-exceptions
level: beginner
duration: 4
category: EXCEÇÕES
related:
  - java/syntax/exceptions/throw
  - java/syntax/exceptions/throws
  - java/jdk/exception
  - java/jdk/runtime-exception
---
Uma exceção personalizada comunica uma condição específica do domínio.

```java
class SaldoInsuficienteException extends Exception {
    SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}

static void sacar(double saldo, double valor)
        throws SaldoInsuficienteException {
    if (valor > saldo) {
        throw new SaldoInsuficienteException("Saldo insuficiente");
    }
}
```

Estender Exception cria uma checked exception. Em alguns contratos, estender RuntimeException é apropriado. Escolha conforme a forma de tratamento esperada.
