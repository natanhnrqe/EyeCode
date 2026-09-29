---
id: java/jdk/java.util/regex
title: Regex (Pattern e Matcher)
type: api
summary: Expressões regulares no Java com Pattern e Matcher, incluindo escaping em string, grupos, substituição e os riscos de backtracking.
level: beginner
duration: 10
officialDocs:
  label: API java.util.regex.Pattern
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html
related:
  - java/jdk/java.util/scanner
  - java/jdk/java.lang/string
  - java/jdk/java.util/stream
---

> [!INFO] `Pattern` compila a **regex** (uma vez) e `Matcher` faz a busca no texto. Detalhe que derruba todo iniciante: a regex `\d` no texto precisa virar **`"\\d"`** no fonte Java — uma barra escapa a outra para o compilador e a que sobra chega ao motor de regex.

## Visão geral

Regex descreve um formato com curingas: validar e-mail, extrair trechos, trocar partes de texto. Em Java o padrão é `Pattern.compile(regex)` → `matcher(texto)` → `matches()` (texto inteiro) ou `find()` (busca trechos).

```java
Pattern p = Pattern.compile("\\d{3}-\\d{4}");   // compila UMA vez (regex é cara)
Matcher m = p.matcher("tel 555-1234 ok");
boolean achou = m.find();                       // true — achou "555-1234"
```

Para uso pontual existe `Pattern.matches(regex, texto)` e os próprios `String.matches`, `String.replaceAll`, `String.split` — todos aceitam regex (ver `String`).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Pattern.compile(String)` (estático) | `Pattern` | compila a regex |
| `Pattern.compile(String, int flags)` (estático) | `Pattern` | compila com flags (`CASE_INSENSITIVE`...) |
| `pattern.matcher(CharSequence)` | `Matcher` | cria o matcher para o texto |
| `Pattern.matches(String, CharSequence)` (estático) | `boolean` | teste de uma tacada (compila e joga fora) |
| `pattern.split(CharSequence)` | `String[]` | quebra o texto pela regex |
| `pattern.split(CharSequence, int limit)` | `String[]` | idem, limitando as divisões |
| `matcher.matches()` | `boolean` | se o texto **inteiro** casa com o padrão |
| `matcher.find()` | `boolean` | se encontra a próxima ocorrência |
| `matcher.lookingAt()` | `boolean` | se casa **no início** do texto |
| `matcher.group()` / `group(int)` | `String` | trecho capturado (ou o grupo `n`) |
| `matcher.groupCount()` | `int` | quantidade de grupos de captura |
| `matcher.start()` / `end()` | `int` | posição do trecho encontrado |
| `matcher.replaceAll(String)` | `String` | troca todas as ocorrências |
| `matcher.replaceFirst(String)` | `String` | troca só a primeira |
| `Pattern.quote(String)` (estático) | `String` | trata um texto literal como regex segura |

## Validar (matches)

```java
String cpfParcial = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}";

System.out.println(Pattern.matches(cpfParcial, "123.456.789-00"));   // true
System.out.println(Pattern.matches(cpfParcial, "12345678900"));      // false

Pattern email = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
System.out.println(email.matcher("ana@exemplo.com").matches());      // true
System.out.println(email.matcher("ana@exemplo").matches());          // false
```

**`matches()` exige casamento completo** — `"abc123"` não casa com `\d+`; para achar no meio, use `find()`.

## Buscar ocorrências (find)

```java
String texto = "pedidos: 10, 25 e 40";
Matcher m = Pattern.compile("\\d+").matcher(texto);

while (m.find()) {
    System.out.println(m.group() + " em [" + m.start() + "," + m.end() + ")");
}
// Saída:
// 10 em [9,11)
// 25 em [13,15)
// 40 em [18,20)

Pattern p = Pattern.compile("(\\d+)/(\\d+)/(\\d+)");
Matcher m2 = p.matcher("nascido em 05/09/1990");
if (m2.find()) {
    System.out.println(m2.group(0));   // 05/09/1990 — grupo 0 = trecho inteiro
    System.out.println(m2.group(1));   // 05 (dia)
    System.out.println(m2.group(2));   // 09 (mês)
    System.out.println(m2.groupCount());// 3 grupos de captura
}
```

Grupos são numerados pela ordem dos parênteses `(` de abertura — **só os parênteses com captura contam**, os `(?:...)` não numeram.

**Armadilha:** `group()` só existe **depois** de um `find()` que devolveu `true` — chamar antes dispara `IllegalStateException`.

## Substituir e dividir

```java
String texto = "contato: ana@x.com, bia@y.org";

String limpo = texto.replaceAll("[\\w.+-]+@", "***@");
System.out.println(limpo);          // contato: ***@x.com, ***@y.org

String semNumero = "abc123def456".replaceAll("\\d", "");
System.out.println(semNumero);      // abcdef

String[] partes = Pattern.compile("\\s*,\\s*").split("a, b ,c");
System.out.println(Arrays.toString(partes));   // [a, b, c]

System.out.println(Pattern.quote("2.5"));      // \Q2.5\E — ponto literal, sem escapar à mão
```

`split` é regex (por isso `String.split(".")` não funciona — ver `String`); com separador literal, `Pattern.quote` deixa o texto à prova de metacaracteres.

**Armadilha:** em `replaceAll`, `$1` e `$2` referenciam **grupos** — texto literal com `$` precisa escapar (`"\\$"`).

## Armadilhas comuns

> [!WARNING] Em string Java, `\d` e `\.` são **erro de compilação** (*illegal escape*) — só escapes conhecidos (`\n`, `\t`, `\\`, `\"`...) passam pelo compilador. A regex correta é sempre **duas baras** no fonte: `"\\d"`, `"\\."` (uma barra escapa a outra e a que sobra chega ao motor de regex).

**`matches` x `find` x `lookingAt`:**

```java
Matcher m = Pattern.compile("\\d+").matcher("abc 123");
m.lookingAt();    // false — precisa casar NO INÍCIO ("abc" não é dígito)
m.find();         // true — encontra "123" em qualquer lugar
m.matches();      // false — precisa casar o texto INTEIRO
```

**Compilar de novo a cada chamada:**

```java
// ruim: compila a regex a cada execução (regex é cara)
for (String s : lista) if (s.matches("\\d{3}")) { }

// bom: compila uma vez e reutiliza
Pattern p = Pattern.compile("\\d{3}");
for (String s : lista) if (p.matcher(s).matches()) { }
```

**Backtracking explosivo:**

```java
// (a+)+$ com texto longo de 'a' seguido de '!' -> o motor tenta toda combinação
Pattern ruim = Pattern.compile("(a+)+$");
ruim.matcher("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!").matches();   // pode levar segundos (ReDoS)

// limite quantificadores e evite grupos aninhados com quantificador: a+b, [a-z]+
```

## Profundidade

**Escapes em duas camadas:** a regex usa `\` como metacaractere e a string Java também — por isso `\\d`, `\\s`, `\\.`, `\\(`. Em text block ou `Pattern.quote`, a segunda camada desaparece: `Pattern.quote("(")` vira `\Q(\E`, literal sem significado.

**Segurança do padrão (ReDoS):** a API do Java (NFA com backtracking) pode ficar superlinear em padrões ambíguos — padrões como `(a+)+$`, `(a|a)*` com entrada longa são vetores de **ReDoS**. Prefira classes simples, limite `{n,m}` e teste com entrada maliciosa.

**`Matcher` não é thread-safe; `Pattern` sim:** `Pattern` é imutável após compilar e pode ser um campo `static final` compartilhado; cada `Matcher` guarda posição e estado de captura — sempre crie um novo por texto/thread.

**Flags:** `Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE` (sem depender de ASCII), `Pattern.DOTALL` (`.` passa a casar `\n`), `Pattern.MULTILINE` (`^`/`$` valem por linha). Dá para embutir inline com `(?idmsuxU)` logo no início da regex.

**Grupos nomeados:** `(?<dia>\\d+)` + `m.group("dia")` deixa a leitura do código bem melhor que `group(1)` quando há muitos grupos.
