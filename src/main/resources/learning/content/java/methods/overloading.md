---
id: java/methods/overloading
title: sobrecarga de métodos
concept: method-overloading
level: beginner
duration: 2
category: MÉTODOS
depth: full
related:
  - java/methods/declaration
  - java/methods/parameters
  - java/syntax/modifiers/static
---
Sobrecarga permite usar o mesmo nome para métodos com listas de parâmetros diferentes. A escolha do método ocorre em tempo de compilação.

```java
void imprimir(String texto) { }
void imprimir(int numero) { }
void imprimir(String texto, int vezes) { }
```

O tipo e a quantidade dos parâmetros diferenciam as sobrecargas. O tipo de retorno sozinho não cria uma sobrecarga válida. Construtores também podem ser sobrecarregados, mas não são métodos comuns.
