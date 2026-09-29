---
id: java/jdk/fundamentos/variables
title: Variáveis em Java
summary: Declaração, inicialização e escopo — a base de qualquer programa, com as regras oficiais da JLS traduzidas para linguagem simples.
level: beginner
duration: 8
officialDocs:
  label: JLS 4 — Variables
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html
related:
  - java/jdk/java.lang/string
  - java/jdk/fundamentos/classes
---

> [!INFO] Variável é uma caixinha com nome onde o programa guarda um valor. Em Java você sempre diz **do que tipo** é a caixinha antes de usá-la — o compilador garante que só entre ali o tipo combinado.

## Por que existe

Toda informação que um programa precisa ler ou alterar — uma idade, um preço, o nome de um usuário — precisa morar em algum lugar na memória enquanto o programa roda. A variável é esse "lugar com nome": você escolhe o nome, o tipo define o que cabe ali, e o valor pode mudar durante a execução.

Sem tipos, você escreveria `idade = "dez"` e descobriria o erro só em produção. Em Java, o compilador sabe que `idade` é número e te avisa na hora — essa é a principal razão de Java ser uma linguagem **estática e fortemente tipada**.

## Anatomia da sintaxe

```java
tipo nome = valor;
```

| Parte | O que é | Exemplo |
|-------|---------|---------|
| `tipo` | O que a caixinha guarda | `int`, `double`, `String`, `boolean` |
| `nome` | O identificador que você usará | `idade`, `totalConta`, `_tempo2` |
| `valor` | O conteúdo inicial (opcional) | `18`, `9.9`, `"Ana"`, `true` |

Declarar sem valor inicial também é válido — a caixinha fica "vazia" até você guardar algo:

```java
int pontuacao;      // declarada, ainda sem valor
pontuacao = 100;    // inicializada depois
```

## Como funciona

Pense na memória do programa como um quadro de endereços. Quando você escreve `int idade = 18;`, o Java:

1. **Reserva** um espaço do tamanho de um `int` (4 bytes);
2. **Liga** o nome `idade` aquele espaço;
3. **Grava** o valor `18` ali dentro.

A partir daí, toda vez que o código diz `idade`, o Java lê o valor atual do espaço. Se você escrever `idade = 19;`, o valor antigo é **substituído** — o espaço é o mesmo, só o conteúdo muda.

O escopo define **onde o nome existe**: uma variável declarada dentro de um bloco `{ ... }` nasce no `{` e morre no `}` correspondente. Fora daquele bloco, o nome simplesmente não é reconhecido pelo compilador.

## Exemplos

O básico — declarar, inicializar e imprimir:

```java
String nome = "Ana";
int idade = 28;
double altura = 1.67;
boolean ativo = true;

System.out.println(nome + " tem " + idade + " anos");
// Saída: Ana tem 28 anos
```

Declarar primeiro, atribuir depois (útil quando o valor vem do usuário):

```java
int quantidade;
double precoUnitario;

quantidade = 3;
precoUnitario = 4.50;

double total = quantidade * precoUnitario;
System.out.println(total);   // Saída: 13.5
```

Variáveis como contadores e acumuladores:

```java
int soma = 0;
for (int i = 1; i <= 10; i++) {
    soma = soma + i;
}
System.out.println(soma);   // Saída: 55
```

## Armadilhas comuns

> [!WARNING] Java é sensível a maiúsculas/minúsculas: `idade` e `Idade` são duas variáveis diferentes. Erro clássico de iniciante — o compilador acusa "não encontrado" e você não vê a diferença.

**Usar uma variável antes de dar valor:**

```java
int x;
System.out.println(x);   // erro: x pode não ter sido inicializada
```

Compiladores Java rejeitam leitura de variável local não inicializada. Atribua antes de ler.

**Confundir `=` (atribuição) com `==` (comparação):**

```java
int a = 5;
if (a = 5) { }   // erro de compilação
if (a == 5) { }  // correto: pergunta se a é igual a 5
```

**Atribuir texto a variável numérica (sem conversão):**

```java
int idade = "18";          // erro
int idade = Integer.parseInt("18");   // correto: converte o texto
```

## Profundidade

Aqui está a teoria completa, sem cortes — do jeito que a especificação define.

**Tipos primitivos (8):**

| Tipo | Tamanho | Faixa / valores |
|------|---------|-----------------|
| `byte` | 1 byte | -128 a 127 |
| `short` | 2 bytes | -32.768 a 32.767 |
| `int` | 4 bytes | ≈ -2 bilhões a 2 bilhões |
| `long` | 8 bytes | enorme (sufixo `L`) |
| `float` | 4 bytes | ponto flutuante (sufixo `f`) |
| `double` | 8 bytes | ponto flutuante dupla precisão |
| `boolean` | 1 bit* | `true` ou `false` |
| `char` | 2 bytes | um caractere Unicode (`'a'`) |

**Regras de nomes (JLS §3.8):** identificadores devem começar com letra, `_` ou `$`; depois aceitam também dígitos; não podem conter espaços nem ser palavra reservada (`class`, `int`, `if`...). Convenção da comunidade: `camelCase` para variáveis e métodos, `PascalCase` para classes, `CONSTANTE_EM_CAIXA_ALTA` para campos `static final`.

**Escopo (JLS §6.3):** o escopo de uma declaração é a região do código onde o nome pode ser usado. Variáveis locais têm escopo de bloco; parâmetros, do ponto de declaração até o fim do método; campos, enquanto a instância/classe existir. Uma declaração no bloco interno **sombra** (esconde) a externa com o mesmo nome dentro daquele bloco.

**Finalidade (`final`):** `final int TOTAL = 100;` cria uma variável de atribuição única — qualquer tentativa de reatribuir depois é erro de compilação. É a base de constantes e de segurança de dados.

**Conversões numéricas (JLS §5.1):** conversão *estreita* (`long` → `int`) pode perder dados e exige cast explícito (`int x = (int) longValue;`); conversão *ampla* (`int` → `double`) acontece automaticamente, sem perda.

**Var (JLS §14.4, Java 10+):** `var total = 10;` — o compilador **infere** o tipo pela inicialização. É só "sugar" de sintaxe: a variável continua fortemente tipada, continua com tipo fixo após a declaração, e o nome permanece explícito. Não funciona sem inicialização (`var x;` é erro).
