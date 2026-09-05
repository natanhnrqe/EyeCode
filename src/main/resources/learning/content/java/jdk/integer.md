---
id: java/jdk/integer
title: Integer
concept: integer
level: beginner
duration: 3
category: JAVA API
officialDocs:
  label: Integer
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html
related:
  - java/jdk/string
---

## O que é isso?

`Integer` é a classe que representa um valor `int` como objeto. Ela é útil quando uma API espera uma referência, por exemplo em coleções ou genéricos, e também oferece conversões e constantes comuns.

## int e Integer

`int` é um tipo primitivo: guarda diretamente um número inteiro. `Integer` é um objeto: pode ser usado onde tipos primitivos não são aceitos, mas também pode ser `null`.

```java
int primitive = 42;
Integer boxed = 42;
```

## Autoboxing e unboxing

O Java converte automaticamente entre `int` e `Integer` em atribuições e expressões. Essa conversão é chamada autoboxing quando cria um objeto e unboxing quando extrai o valor primitivo.

```java
Integer boxed = 7;
int primitive = boxed;
```

Se `boxed` for `null`, o unboxing causa `NullPointerException`. Use `Integer` anulável somente quando essa ausência tiver significado.

## Constantes e conversões

`Integer.MAX_VALUE` e `Integer.MIN_VALUE` representam os limites de um `int`. Para converter texto, use `Integer.parseInt`, que retorna `int`, ou `Integer.valueOf`, que retorna `Integer`.

```java
int count = Integer.parseInt("42");
Integer value = Integer.valueOf("42");
String text = Integer.toString(count);
```

Entradas que não representam um inteiro válido causam `NumberFormatException`.

## Comparação

Ao comparar objetos `Integer`, prefira `equals` para comparar valores. O operador `==` compara referências quando os dois lados são objetos; ele pode parecer funcionar para alguns números por causa do cache de wrappers.

```java
Integer first = 1000;
Integer second = 1000;
boolean sameValue = first.equals(second);
```

## Quando usar

Use `int` como padrão para cálculos. Escolha `Integer` quando precisar de `null`, de uma coleção genérica como `List<Integer>` ou de uma API que recebe objetos.
