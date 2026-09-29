---
id: java/jdk/java.lang/stringbuilder
title: StringBuilder
type: api
summary: Buffer de texto mutável para montar strings em etapas — métodos de montagem, leitura e capacidade, com as armadilhas de índice, comparação e uso entre threads.
level: beginner
duration: 7
officialDocs:
  label: API java.lang.StringBuilder
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StringBuilder.html
related:
  - java/jdk/java.lang/string
  - java/jdk/fundamentos/classes
---

> [!INFO] `StringBuilder` é um **buffer mutável de texto**: você acrescenta com `append` quantas vezes quiser e só chama `toString()` no final. Diferente de `String`, ele **muda no lugar** — por isso é a forma certa de montar texto dentro de **laços**.

## Visão geral

`String` é imutável: cada `+` cria um objeto novo. Quando o texto cresce em etapas — relatórios, SQL, mensagens montadas dentro de um laço —, criar centenas de Strings temporárias é desperdício de memória e de CPU. `StringBuilder` resolve isso guardando um **buffer interno reutilizável** que cresce conforme necessário.

```java
StringBuilder sb = new StringBuilder("SELECT ");
sb.append("nome").append(" FROM ");
sb.append("usuarios");
String sql = sb.toString();   // "SELECT nome FROM usuarios"
```

Regra prática: `String` para texto pronto e parado; `StringBuilder` enquanto o texto ainda está sendo montado.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `append(String/char/int/long/double/Object...)` | `StringBuilder` | acrescenta no final |
| `insert(int i, ...)` | `StringBuilder` | insere na posição `i` |
| `delete(int from, int to)` | `StringBuilder` | apaga o trecho `[from, to)` |
| `deleteCharAt(int i)` | `StringBuilder` | apaga um caractere |
| `replace(int from, int to, String)` | `StringBuilder` | substitui o trecho |
| `reverse()` | `StringBuilder` | inverte os caracteres |
| `setCharAt(int i, char c)` | `void` | troca um caractere |
| `charAt(int i)` | `char` | lê o caractere da posição |
| `length()` | `int` | quantidade de caracteres |
| `substring(int from[, int to])` | `String` | trecho (devolve cópia) |
| `indexOf(String[, int from])` | `int` | posição ou `-1` |
| `toString()` | `String` | vira `String` imutável |
| `capacity()` | `int` | espaço já alocado |
| `ensureCapacity(int min)` | `void` | garante espaço mínimo |
| `setLength(int n)` | `void` | corta (ou estende com `\0`) |

## Montagem de texto

```java
StringBuilder sb = new StringBuilder();
sb.append("olá");                  // "olá"
sb.append(' ').append("mundo");    // "olá mundo"
sb.append(42).append('!');         // "olá mundo42!"
sb.insert(0, ">> ");               // ">> olá mundo42!"
String frase = sb.toString();      // ">> olá mundo42!"
```

`append` aceita quase qualquer tipo (`String`, `char`, número, `boolean`, `Object`) e **sempre devolve o próprio builder** — é por isso que a corrente `append().append()` funciona sem criar objetos novos.

## Remoção e substituição

```java
StringBuilder sb = new StringBuilder("olá mundo cruel");
sb.delete(9, 15);                  // apaga " cruel"  → "olá mundo"
sb.replace(4, 9, "terra");         // troca "mundo"   → "olá terra"
sb.setCharAt(0, 'O');              // 1 caractere     → "Olá terra"

StringBuilder pal = new StringBuilder("arara");
pal.reverse();                     // inverte         → "arara"
```

**Os intervalos são `[from, to)` em caracteres** — `delete(9, 15)` vai da posição 9 até a 14 inclusive; errar o fim apaga a letra vizinha sem nenhum aviso.

## Leitura e conversão

```java
StringBuilder sb = new StringBuilder("SELECT nome FROM usuarios");
sb.length();                       // 25
sb.charAt(0);                      // 'S'
sb.substring(7, 11);               // "nome"
sb.indexOf("FROM");                // 12
String sql = sb.toString();        // "SELECT nome FROM usuarios"
```

**`substring` e `toString` devolvem `String` nova** — o builder pode continuar mudando depois, mas o texto já extraído fica congelado no valor da cópia.

## Capacidade e reaproveitamento

```java
StringBuilder sb = new StringBuilder(32);   // capacidade inicial
sb.capacity();                       // 32
sb.append("x".repeat(30));
sb.length();                         // 30 — capacity segue 32
sb.append("y".repeat(10));           // buffer cresce sozinho (na prática, dobra)
sb.ensureCapacity(1000);             // reserva antes de um lote grande
sb.setLength(0);                     // esvazia o texto, mantém o buffer
```

**`capacity()` não é o tamanho** — `length()` conta o que tem texto; capacity é o espaço já alocado. `setLength(0)` zera o conteúdo sem liberar a memória, ideal para reaproveitar o mesmo builder.

## Armadilhas comuns

> [!WARNING] `StringBuilder` **não é thread-safe**: duas threads montando texto no mesmo buffer corrompem o conteúdo. Para texto compartilhado use `StringBuffer` (mesma API, métodos `synchronized`, mais lento) — ou, melhor, um builder por thread.

**Comparar builders com `equals`:** `StringBuilder` **não** sobrescreve `equals`, então compara referência, igual a `==`:

```java
StringBuilder a = new StringBuilder("oi");
StringBuilder b = new StringBuilder("oi");
a.equals(b);                          // false — dois objetos diferentes
a.toString().equals(b.toString());    // true — compare convertendo em String
```

**Posição que não existe:**

```java
StringBuilder sb = new StringBuilder("abc");
sb.charAt(10);              // StringIndexOutOfBoundsException
sb.setCharAt(9, 'x');       // idem
sb.insert(99, "x");         // idem
```

`delete` e `replace` limitam o fim ao tamanho atual, mas `charAt`, `setCharAt` e `insert` estouram exceção quando a posição está fora da faixa.

**`+` vale mais que builder em pedaços poucos:** o compilador transforma concatenações em uma operação única e otimizada. Builder compensa quando o texto cresce em **vários passos ou dentro de laço**:

```java
String msg = "a" + "b" + "c";          // 3 pedaços — use +, é mais legível
StringBuilder sb = new StringBuilder(); // crescimento em etapas — use builder
sb.append("a").append("b").append("c");
```

## Profundidade

**Buffer reutilizável:** `StringBuilder` mantém um array interno (compacto desde o Java 9) que só realoca quando o conteúdo não cabe mais — daí `append` ser O(1) amortizado, enquanto `s += x` em laço é O(n) a cada volta.

**`AbstractStringBuilder` e `StringBuffer`:** as duas classes herdam exatamente a mesma implementação; a única diferença real é que `StringBuffer` declara os métodos como `synchronized`. Escolha `StringBuffer` apenas quando o mesmo buffer for escrito por mais de uma thread.

**Concatenação otimizada (JLS §15.18.1):** `a + b + c` é compilado para uma chamada de concatenação dinâmica (`StringConcatFactory`), não para uma sequência de `append` — por isso poucos `+` são rápidos e legíveis. O guia é: 2 a 3 pedaços use `+`, crescimento iterativo use `StringBuilder`.

**Crescimento por dobra:** quando o buffer enche, a capacidade é ampliada (na prática, dobrada) e o conteúdo é copiado. Lotes grandes e previsíveis pedem `new StringBuilder(capacidadeEsperada)` ou `ensureCapacity` antes de começar, evitando realocações no meio.

**`CharSequence` e serialização:** o builder implementa `CharSequence`, então pode ser passado direto para métodos que só leem texto (ex.: `String.format`, `subSequence`), e `Serializable` — mas ele continua **mutável**: quem recebe a referência pode alterar o seu conteúdo.

**Por que não tem `equals`/`hashCode`:** o desenho original (JDK 1.0) trata builder como objeto de trabalho, não como valor — igualdade nele não faria sentido enquanto o texto muda. Se dois builders precisam ser "iguais", eles já são `String`s.
