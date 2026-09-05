---
id: java/syntax/text-blocks
title: Blocos de texto
concept: text-block
level: beginner
duration: 5
category: JAVA MODERNO
depth: full
related:
  - java/jdk/string
  - java/basics/literals
---
Um bloco de texto é um literal de String escrito entre três aspas. Ele facilita conteúdo com várias linhas, como JSON, SQL e HTML.

~~~java
String json = """
    {
      "nome": "EyeCode",
      "linguagem": "Java"
    }
    """;
~~~

O resultado continua sendo uma String. A indentação comum é tratada para preservar o formato pretendido sem exigir várias sequências de nova linha e concatenações.

~~~java
String sql = """
    SELECT nome
    FROM usuarios
    WHERE ativo = true
    """;
~~~

Blocos de texto tornaram-se padrão no Java 15; eles não transformam o conteúdo em JSON, SQL ou HTML automaticamente.
