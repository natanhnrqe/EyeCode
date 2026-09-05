---
id: java/syntax/types/var
title: Inferência de tipo local com var
concept: var
level: intermediate
duration: 6
category: JAVA MODERNO
depth: full
related:
  - java/basics/variables
  - java/generics/generics
---
var permite que o compilador infira o tipo estático de uma variável local a partir do inicializador. Java não se torna dinamicamente tipado.

~~~java
var nome = "EyeCode";
var quantidade = 10;
var lista = new ArrayList<String>();
~~~

O inicializador é obrigatório e determina um tipo específico. var não significa qualquer tipo e não pode ser usado para campos, parâmetros ou variáveis sem inicialização.

~~~java
var valor;
var nulo = null;
~~~

A inferência de tipo local tornou-se padrão no Java 10. Use var quando o tipo for evidente; prefira a declaração explícita quando ela tornar a intenção mais clara.