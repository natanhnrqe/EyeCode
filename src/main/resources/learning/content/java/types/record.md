---
id: java/types/record
title: Records
concept: record
level: intermediate
duration: 10
category: JAVA MODERNO
depth: full
officialDocs:
  label: Java Record Classes
  url: https://docs.oracle.com/en/java/javase/21/language/records.html
related:
  - java/types/class
  - java/syntax/modifiers/final
  - java/syntax/pattern-matching
---
Um record é uma forma concisa de declarar um tipo para transportar dados. Seus componentes são valores ou referências finais, mas um objeto referenciado ainda pode ser mutável.

O compilador gera o construtor canônico, accessors com o nome dos componentes, equals, hashCode e toString.

~~~java
record Pessoa(String nome, int idade) {
}

Pessoa pessoa = new Pessoa("Ana", 30);
System.out.println(pessoa.nome());
System.out.println(pessoa.idade());
~~~

O accessor usa o nome do componente, como nome(), e não o padrão JavaBeans getNome(). Um record não pode estender outra classe, embora possa implementar interfaces.

Um construtor compacto permite validar componentes antes da criação:

~~~java
record Pessoa(String nome, int idade) {
    Pessoa {
        if (idade < 0) {
            throw new IllegalArgumentException("Idade inválida");
        }
    }
}
~~~

Records tornaram-se padrão no Java 16. Eles são finais, mas não devem ser descritos simplesmente como classes imutáveis: a referência não muda, porém o objeto referenciado pode mudar.