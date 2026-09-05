---
id: java/basics/variables
title: Variáveis em Java
concept: variable
level: beginner
duration: 6
category: FUNDAMENTOS
officialDocs:
  label: Java Language Specification
  url: https://docs.oracle.com/javase/specs/
related:
  - java/basics/identifiers
  - java/basics/primitive-types
  - java/basics/reference-types
  - java/basics/literals
  - java/syntax/modifiers/final
  - java/types/class
next: java/types/class
---
Variáveis dão nome aos dados que um programa precisa ler ou alterar. A declaração combina um tipo, um identificador e, opcionalmente, uma inicialização.

```java
int age = 20;
age = 21;
String playerName = "Ada";
```

A declaração reserva uma variável com um tipo definido. A inicialização atribui o primeiro valor. Depois, uma variável não final pode receber outro valor compatível com seu tipo.

## Variáveis locais e campos

Uma variável local existe dentro de um método ou bloco. Um campo pertence a uma classe e representa um dado associado a cada objeto ou à própria classe, conforme sua declaração.

```java
class User {
    private String name; // campo

    void rename(String newName) {
        String normalized = newName.trim(); // variável local
        name = normalized;
    }
}
```

Variáveis locais precisam ser inicializadas antes de serem lidas. Campos recebem um valor padrão quando o objeto é criado, mas depender desses padrões nem sempre deixa a intenção clara.

## Constantes e escopo

Use `final` quando uma variável não deve receber uma nova atribuição:

```java
final double PI = 3.14159;
```

Uma variável só pode ser usada dentro do escopo em que foi declarada. Um bloco pode esconder uma variável externa com o mesmo nome, mas evitar essa sobreposição normalmente melhora a leitura.

## Boas escolhas

- Escolha identificadores que descrevam o valor.
- Use o tipo mais adequado ao domínio do dado.
- Inicialize variáveis locais antes de lê-las.
- Separe a ideia de variável, tipo e objeto: uma referência pode apontar para um objeto, enquanto um primitivo guarda seu próprio valor.