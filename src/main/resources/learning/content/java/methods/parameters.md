---
id: java/methods/parameters
title: parâmetros e argumentos
concept: method-parameters
level: beginner
duration: 3
category: MÉTODOS
depth: full
related:
  - java/methods/declaration
  - java/methods/overloading
  - java/basics/variables
---
Parâmetro é a variável declarada na assinatura do método. Argumento é o valor ou expressão fornecido no momento da chamada.

```java
void alterar(int valor) {
    System.out.println(valor);
}

alterar(10);
```

`valor` é parâmetro; `10` é argumento. Um método pode receber vários parâmetros, cada um com seu tipo.

Java sempre passa argumentos por valor. Para uma referência de objeto, o valor copiado é a própria referência: o método pode alterar o objeto apontado, mas reatribuir a referência local não troca a variável do chamador.
