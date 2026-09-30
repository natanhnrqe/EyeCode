---
id: java/jdk/java.lang/stringformat
title: String.format
type: api
summary: Formatação de texto e números com %s, %d, %f e %n — conversões do dia a dia, flags de largura e precisão, o papel do Locale e as armadilhas de tipo e de vírgula decimal.
level: beginner
duration: 8
officialDocs:
  label: API java.util.Formatter
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Formatter.html
related:
  - java/jdk/java.lang/string
  - java/jdk/java.lang/stringbuilder
  - java/jdk/java.lang/system
---

> [!INFO] `String.format` monta texto a partir de um **modelo com marcadores `%`** (`%s`, `%d`, `%f`) e uma lista de argumentos. O motor por trás é a classe `java.util.Formatter` — é ela que define a gramática de conversões, flags e largura; `printf` usa a mesma sintaxe.

## Visão geral

Formatação resolve o caso em que a concatenação com `+` fica ilegível: mensagens com muitos valores, colunas alinhadas, números com casas decimais fixas. Em vez de montar o texto pedaço a pedaço, você escreve a frase completa uma vez e marca onde cada valor entra:

```java
String nome = "Ana";
int pontos = 1250;

String linha = String.format("%s marcou %d pontos", nome, pontos);
// "Ana marcou 1250 pontos"

System.out.printf("%s marcou %d pontos%n", nome, pontos);  // imprime e pula linha
```

`System.out.printf` (console) e `"...".formatted(...)` (atalho de instância, Java 15+) usam exatamente a mesma gramática — aprender uma vez serve para todos.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `String.format(String fmt, Object... args)` | `String` | aplica o formato e devolve o texto pronto |
| `String.format(Locale l, String fmt, Object... args)` | `String` | idem, com `Locale` explícito |
| `"...".formatted(Object... args)` (Java 15+) | `String` | atalho de instância sobre a String modelo |
| `PrintStream.printf(String fmt, Object... args)` | `PrintStream` | formata e escreve (ex.: `System.out`) |
| `new Formatter(Appendable destino)` | `Formatter` | formatador que acumula no destino dado |
| `new Formatter(String arquivo)` | `Formatter` | formatador que escreve direto em arquivo |
| `Formatter.format(String fmt, Object... args)` | `Formatter` | acumula no destino (encadeável) |
| `Formatter.close()` | `void` | fecha o destino e libera o recurso |

## Conversões do dia a dia

O marcador `%X` escolhe como o argumento vira texto — e cada conversão exige um tipo compatível:

```java
String.format("hex=%x ativo=%b", 255, true);   // "hex=ff ativo=true"
String.format("%d", 2_000_000_000_000L);        // ok — %d aceita long e BigInteger
String.format("%,d", 12500);                    // "12.500" em pt-BR (agrupa milhar)
String.format("%.2f", 7.5);                     // "7,50" em pt-BR (duas casas)
String.format("%s", null);                      // "null" — %s aceita qualquer coisa
```

| Conversão | Aceita | Saída |
|-----------|--------|-------|
| `%s` | qualquer objeto (usa `toString()`) | texto |
| `%d` | `byte`, `short`, `int`, `long`, `BigInteger` | inteiro, sem casas |
| `%f` | `float`, `double`, `BigDecimal` | decimal fixo |
| `%e` | os mesmos tipos de `%f` | notação científica (`7.5e+00`) |
| `%b` | qualquer — `null` vira `false` | `true` / `false` |
| `%c` | `char`, `int` | caractere Unicode |
| `%x` / `%o` | inteiros | hexadecimal / octal |
| `%n` | nenhum argumento | separador de linha da plataforma |

> [!WARNING] Cada conversão valida o tipo do argumento: `String.format("%d", 7.5)` lança `IllegalFormatConversionException` — `%d` não arredonda nem tenta converter. Se o tipo pode variar em runtime, `%s` é a escolha que nunca falha.

## Largura, precisão e flags

Entre o `%` e a letra cabem largura, precisão e flags — o essencial para alinhar tabelas e padronizar decimais:

```java
String.format("[%10d]", 42);       // "[        42]" — largura 10, à direita
String.format("[%-10d]", 42);      // "[42        ]" — flag '-' alinha à esquerda
String.format("[%010d]", 42);      // "[0000000042]" — flag '0' preenche com zeros
String.format("[%5.2f]", 3.14159); // "[ 3.14]"      — largura 5, precisão 2
String.format("%1$s e %1$s", "Ana");  // "Ana e Ana" — índice de argumento
```

A regra de leitura é sempre a mesma: `%[índice$][flags][largura][.precisão]conversão`. Para `%s`, a precisão **trunca**: `String.format("%.3s", "abcdef")` devolve `"abc"`.

## Locale e %n

`%f` e `%,d` são sensíveis ao `Locale` — vírgula decimal, separador de milhar e até os dígitos mudam. Sem `Locale` explícito, vale o padrão da JVM:

```java
String.format("%.2f", 3.14);               // "3,14" em pt-BR — atenção!
String.format(Locale.ROOT, "%.2f", 3.14);  // "3.14" — estável para máquinas
String.format("%n").equals("\r\n");        // true no Windows, false no Linux
```

`%n` não é `\n`: é o separador de linha da plataforma. Em mensagens para humanos, `%n` é o correto; em formatos que especificam o texto byte a byte (protocolos, arquivos de configuração), use `\n` literal.

## Armadilhas comuns

> [!WARNING] O bug nº 1 é `Locale`: `%.2f` imprime `3,14` numa JVM pt-BR e `3.14` numa inglesa — CSV, SQL concatenado e JSON feito à mão quebram em produção, não na sua máquina. Saída para máquina pede sempre `String.format(Locale.ROOT, ...)`.

**Tipo errado no marcador:**

```java
String.format("%d", 7.5);   // IllegalFormatConversionException
String.format("%f", 10);    // IllegalFormatConversionException
String.format("%s", 7.5);   // "7.5" — %s nunca falha
```

**Contagem errada de argumentos:** faltar argumento lança `MissingFormatArgumentException`; sobrar é ignorado em silêncio — fácil de acontecer ao editar um modelo antigo.

**`Formatter` em arquivo sem `close`:** o conteúdo fica no buffer e o arquivo sai incompleto. Sempre try-with-resources:

```java
try (Formatter f = new Formatter("relatorio.txt")) {  // lança FileNotFoundException
    f.format("gerado em %tF%n", java.time.LocalDate.now());
}   // close() descarrega o buffer e fecha o arquivo
```

## Profundidade

A gramática completa está no javadoc de `java.util.Formatter` (fica em `java.util`, não em `java.lang`): cada marcador segue `%[argument_index$][flags][width][.precision]conversion`, e as conversões de data/hora (`%tF`, `%tT`) formam uma sublinguagem à parte. Para `%s`, um objeto pode implementar `java.util.Formattable` e controlar a própria formatação. Internamente `String.format` cria um `Formatter` descartável — em laços de milhões de iterações, `StringBuilder` direto evita o custo de reanalisar o modelo; abaixo disso, a legibilidade ganha.
