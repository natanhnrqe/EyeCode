---
id: java/jdk/java.lang/wrapper
title: Wrappers e Autoboxing
type: api
summary: Classes objeto dos primitivos (Integer, Double, Boolean) — autoboxing, cache de valores, conversões de texto e as armadilhas do == entre objetos.
level: beginner
duration: 8
officialDocs:
  label: API java.lang.Integer
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html
related:
  - java/jdk/fundamentos/variables
  - java/jdk/java.util/list
---

> [!INFO] Wrapper é a **classe objeto** de cada primitivo (`int` → `Integer`). O Java **converte sozinho** entre os dois (autoboxing e unboxing), mas wrappers são objetos — nunca compare com `==`; use `.equals()` ou `Integer.compare`.

## Visão geral

Java separa tipos primitivos (rápidos, guardados por valor) de objetos. Genéricos e coleções só aceitam objetos — `List<Integer>` existe, `List<int>` não. Para isso cada primitivo tem uma classe wrapper, e o compilador insere a conversão automaticamente:

```java
int a = 10;
Integer b = a;                      // autoboxing: int → Integer
List<Integer> nums = new ArrayList<>();
nums.add(42);                       // o compilador boxeia sozinho
int c = b;                          // unboxing: Integer → int
```

Wrappers são **imutáveis** — cada "mudança" gera um objeto novo. Princípio que resume a página: use primitivo no dia a dia e wrapper só onde a linguagem exige objeto (genéricos, coleções, reflexão).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Integer.parseInt(String)` | `int` | texto → primitivo (erro se não for número) |
| `Integer.valueOf(int)` | `Integer` | boxeia (usa cache de -128 a 127) |
| `Integer.valueOf(String)` | `Integer` | texto → wrapper |
| `Integer.toString(int)` | `String` | primitivo → texto |
| `Integer.compare(int, int)` | `int` | compara primitivos (-1, 0 ou 1) |
| `x.compareTo(Integer)` | `int` | ordem do wrapper |
| `Integer.MAX_VALUE` / `MIN_VALUE` | `int` | faixa do `int` |
| `Integer.toHexString(int)` | `String` | hexadecimal (também `toBinaryString`) |
| `Long.parseLong(String)` | `long` | idem para `long` |
| `Double.parseDouble(String)` | `double` | idem para `double` |
| `Boolean.parseBoolean(String)` | `boolean` | `true` só para "true" (ignora caixa) |
| `Character.isLetter(char)` | `boolean` | classe do caractere |
| `x.intValue()` / `doubleValue()` | `int` / `double` | wrapper → primitivo manual |
| `Integer.sum/max/min(int, int)` | `int` | aritmética estática |

## Conversão de texto

```java
int idade = Integer.parseInt("28");           // 28
Integer boxed = Integer.valueOf("28");        // 28 (objeto)
double preco = Double.parseDouble("9,90".replace(',', '.'));  // 9.9
String hex = Integer.toHexString(255);        // "ff"
String txt = String.format("%05d", 42);       // "00042"
```

**`parseInt`/`parseDouble` estouram `NumberFormatException`** quando o texto não é válido — valide a entrada ou capture a exceção antes de confiar no resultado.

## Autoboxing no dia a dia

```java
List<Integer> nums = new ArrayList<>();
nums.add(1);                       // autoboxing automático
int primeiro = nums.get(0);        // unboxing automático

Integer x = 5;
int y = x;                         // desboxeia
x++;                               // desboxeia, soma e boxeia de novo → 6
```

**`x++` em wrapper faz unbox → soma → box**: com `x == null` essa linha vira `NullPointerException` em tempo de execução, sem aviso do compilador.

## Comparação e ordenação

```java
Integer.compare(3, 7);             // -1 — comparação segura para primitivos
Integer a = 3, b = 7;
a.compareTo(b);                    // -1 — ordem natural do wrapper

List<Integer> nums = new ArrayList<>(List.of(3, 1, 2));
nums.sort(Integer::compareTo);     // [1, 2, 3] — ordena pela ordem natural
```

**`compareTo` devolve a ordem (-1, 0, 1), não `boolean`** — para `if (a > b)` continue usando primitivos; wrapper só entra em jogo quando a linguagem obriga (coleções genéricas).

## Constantes e utilidades

```java
Integer.MAX_VALUE;                 // 2147483647
Integer.MIN_VALUE;                 // -2147483648
Integer.MAX_VALUE + 1;             // -2147483648 — estouro silencioso
Math.abs(Integer.MIN_VALUE);       // continua negativo (não existe positivo)
Long.parseLong("9000000000");      // 9000000000 — cabe no long
Integer.bitCount(0b1111);          // 4 — bits ligados
```

**O estouro de faixa é silencioso**: `int` não lança exceção, ele "deforma" o valor. Para números maiores use `long`, e para cálculo exato de dinheiro use `BigDecimal`.

## Armadilhas comuns

> [!WARNING] Comparar wrappers com `==`: o Java guarda em memória (cache) os `Integer` de **-128 a 127** — abaixo funciona por acaso, acima falha. Esse é um dos bugs de produção mais comuns em Java.

```java
Integer a = 100, b = 100;
a == b;                   // true — ambos vêm do cache
Integer c = 1000, d = 1000;
c == d;                   // false — dois objetos distintos na memória
c.equals(d);              // true — sempre compare com equals (ou compare)
```

**NPE escondida no unboxing:**

```java
Integer n = null;
int valor = n;            // NullPointerException em tempo de execução
int dobro = n * 2;        // idem — o compilador não avisa
```

Campos de objeto e elementos de coleção nascem `null`; quem lê precisa tratar antes de virar primitivo.

**`equals` de `double` compara bits:**

```java
Double p = 0.0, n = -0.0;
p.equals(n);                        // false — representações diferentes
Double.valueOf(Double.NaN).equals(Double.valueOf(Double.NaN));  // true
Double.NaN == Double.NaN;           // false — nunca compare NaN com ==
```

**Genérico não aceita primitivo:**

```java
// List<int> nums;        // erro de compilação
List<Integer> nums;       // correto — wrapper obrigatório
int[] vetor = new int[5]; // array primitivo continua mais enxuto
```

Arrays primitivos (`int[]`) são contínuos na memória e mais baratos; use wrapper só onde o tipo genérico exigir.

## Profundidade

**Boxing e unboxing (JLS §5.1.7 e §5.1.8):** conversões *estreitas* que o compilador insere automaticamente. Boxing cria um objeto (na prática, chama `valueOf`), unboxing chama `intValue()`/`doubleValue()` — por isso nulo vira NPE. Não existe conversão implícita entre wrappers diferentes (`Integer` → `Long` é erro de compilação).

**Cache de `Integer` (JLS §5.1.7):** a API exige cache garantido dos valores -128 a 127; a partir daí `valueOf` pode criar objeto novo. O efeito visível é `==` funcionando "às vezes" — a regra permanece: **nunca compare objeto com `==`**.

**Imutabilidade:** `value` dos wrappers é `final`. Isso torna wrappers seguros para compartilhar e úteis como chaves de mapa, mas impede "mudança no lugar" — toda operação devolve instância nova (no boxing, também aloca).

**`Number` e conversões:** `Byte`, `Short`, `Integer`, `Long`, `Float` e `Double` estendem `Number` (`intValue()`, `doubleValue()`...); `Boolean` e `Character` não. Os métodos `*Exact` (`Math.addExact`) lançam `ArithmeticException` em estouro — alternativa segura aos `+` silenciosos.

**Desempenho e representação:** `Integer[]` é um array de ponteiros (um objeto por célula, com cabeçalho de 16 bytes); `int[]` guarda os valores soltos. Em laços intensivos e estruturas grandes, primitivo rende mais — wrapper é custo de compatibilidade com genéricos.

**`OptionalInt`/`OptionalLong`:** quando "int ou nada" não pode ser `null`, essas variantes evitam boxe (`OptionalInt.of(7)`, `isPresent()`, `getAsInt()`) e são mais expressivas que `Optional<Integer>`.

**Por que `parse*` e `valueOf` são diferentes:** `parseInt` devolve primitivo (nada de objeto), `valueOf(String)` parseia **e** boxeia — nas duas, texto inválido é `NumberFormatException`.
