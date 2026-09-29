---
id: java/jdk/java.lang/string
title: String
type: api
summary: Texto em Java: visão geral da classe, tabela dos métodos que você usa no dia a dia, exemplos por grupo e as armadilhas de imutabilidade e comparação.
level: beginner
duration: 8
officialDocs:
  label: API java.lang.String
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html
related:
  - java/jdk/fundamentos/variables
  - java/jdk/fundamentos/classes
---

> [!INFO] `String` guarda texto e é **imutável**: cada operação ("tudo maiúsculo", "juntar com") cria uma String **nova** — a original nunca muda. E nunca compare texto com `==`; use `.equals()`.

## Visão geral

`String` é a classe do Java para texto e uma das mais usadas da linguagem — nomes, mensagens, chaves de configuração, URLs. O compilador oferece atalhos para ela: literais `"..."` (com pool em tempo de compilação) e concatenação com `+`.

```java
String nome = "Ana";                 // literal — a forma usual
String outro = new String("Ana");    // objeto novo (evite isto)
String vazio = "";                   // String vazia, não nula
```

A regra que resume a classe: **texto nunca muda no lugar** — métodos que "modificam" devolvem uma String nova. Por isso Strings são seguras para compartilhar entre threads e como chaves de mapa.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `length()` | `int` | quantidade de caracteres (UTF-16) |
| `charAt(int i)` | `char` | caractere na posição `i` |
| `substring(int from[, int to])` | `String` | trecho `[from, to)` |
| `indexOf(String s[, int from])` | `int` | posição de `s` ou `-1` |
| `contains(CharSequence s)` | `boolean` | se contém `s` |
| `startsWith(String)` / `endsWith(String)` | `boolean` | prefixo / sufixo |
| `equals(Object)` / `equalsIgnoreCase(String)` | `boolean` | compara conteúdo |
| `compareTo(String)` | `int` | ordem lexicográfica (0 = igual) |
| `toLowerCase()` / `toUpperCase()` | `String` | muda a caixa |
| `trim()` / `strip()` | `String` | remove espaços das pontas |
| `replace(char, char)` / `replace(CharSequence, CharSequence)` | `String` | troca literal |
| `replaceAll(String regex, String repl)` | `String` | troca por regex |
| `split(String regex)` | `String[]` | quebra em array |
| `concat(String)` | `String` | junta (igual a `+`) |
| `format(String fmt, Object... args)` | `String` | formatação com `%s`/`%d` |
| `isBlank()` (Java 11) | `boolean` | vazio **ou** só espaços |
| `lines()` (Java 11) | `Stream<String>` | stream de linhas |
| `codePoints()` | `IntStream` | código points Unicode |

## Leitura e acesso

```java
String texto = "  Bem-vindo ao EyeCode!  ";

texto.length();                // 25
texto.charAt(4);               // 'B'
texto.substring(2, 12);        // "Bem-vindo"
texto.indexOf("vindo");        // 7 (ou -1 se não existe)
texto.substring(texto.indexOf("ao"));  // "ao EyeCode!  "
```

Armadilha: posições são em **chars UTF-16** — emoji e símbolos fora do BMP ocupam 2 posições (ver Profundidade).

## Comparação e busca

```java
String a = "oi";
String b = "oi";

a.equals(b);             // true — compara conteúdo (sempre use este)
a.equalsIgnoreCase("OI");// true — ignora maiúsculas
a.compareTo(b);          // 0 — ordem lexicográfica (para ordenar)
a.compareTo("ou");       // número negativo — "oi" vem antes de "ou"
"Ana".contains("n");     // true
"arquivo.txt".endsWith(".txt");  // true
```

> [!WARNING] `a == b` compara **endereços**, não texto. `"oi" == "oi"` pode dar `true` por causa do pool de literais, mas `new String("oi") == "oi"` é `false` — comportamento instável. Em bugs reais, esse é o erro nº 1 com Strings.

## Transformação

```java
String s = "  Bem-vindo  ";

s.trim();                          // "Bem-vindo" (só ASCII <= U+0020)
s.strip();                         // "Bem-vindo" (Unicode, Java 11+)
s.toUpperCase();                   // "  BEM-VINDO  "
s.replace('o', '0');               // troca caractere
s.replace("Bem", "Ola");           // trecho literal
s.replaceAll("\\s+", " ");         // regex: colapsa espaços
s.split(" ");                      // array: ["", "", "Bem-vindo", "", ""]
"Ana".toUpperCase().toLowerCase(); // cadeia de transforms — cada passo é novo
```

O resultado de cada linha é uma String **nova**; `s` continua igual.

## Montagem e formatação

```java
String nome = "Ana";
int idade = 28;

String msg = nome + " tem " + idade + " anos";          // concatenação com +
String msg2 = String.format("%s tem %d anos", nome, idade);  // formatação explícita
String linha = String.join(", ", "a", "b", "c");        // "a, b, c"
```

Para valores, `String.format` é mais legível que `+` quando há muitos pedaços. Em laços, concatenação repetida vira `StringBuilder` otimizado pelo compilador — mas laços longos pedem `StringBuilder` explícito (ver Armadilhas).

Texto multilinha com text blocks (Java 15+) — sem `\n` manuais:

```java
String sql = """
        SELECT *
        FROM usuarios
        WHERE ativo = true
        """;
```

## Armadilhas comuns

**String nula (`null`) — `.equals` estoura exceção:**

```java
String nome = null;
nome.equals("Ana");          // NullPointerException!
"Ana".equals(nome);          // false — seguro (literal primeiro)
java.util.Objects.equals(nome, "Ana");   // alternativa moderna
```

**Imutabilidade desperdiçada em laços:**

```java
// ruim: cria uma String nova por volta
String total = "";
for (int i = 0; i < 1000; i++) total += i;

// bom: um único buffer reutilizável
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) sb.append(i);
String total2 = sb.toString();
```

**`split` aceita regex:** `split(".")` não divide por ponto (`.` é curinga) — use `split("\\.")`. Para separador literal simples, `split(Pattern.quote("."))` ou `replace` + `split`.

**`charAt` em texto com emoji:** `"👍".charAt(0)` devolve metade do par surrogate (`\uD83D`) — para percorrer por código-point use `codePoints()`.

## Profundidade

**Pool de literais e constant folding:** expressões só com literais e `final` de literais são *constant expressions*, avaliadas na compilação e compartilhadas via pool — é por isso que `"a" == "a"` pode ser `true`. Concatenação envolvendo variáveis **não** entra no pool.

**Compact strings (Java 9+):** internamente `String` guarda bytes com codificação (latin-1 quando possível), mas isso é detalhe de implementação — para seu código, é texto.

**UTF-16 e surrogate pairs:** `length()`/`charAt` contam unidades de 16 bits; `👍` conta 2. Métodos `codePoints()`, `offsetByCodePoints` e `String.valueOf(int codePoint)` operam no código-point real.

**`isBlank` vs `isEmpty`:** `isEmpty()` é verdadeiro só para `""`; `isBlank()` também para `"   "` (Java 11). `strip()` remove espaços Unicode (ex.: `\u00A0`), `trim()` não.

**Chars vs Strings:** `char` é um único símbolo de 16 bits (`'a'`); `String` é sequência. Comparar `char` com `String` sempre dá `false` — tipagem correta evita o bug silencioso.
