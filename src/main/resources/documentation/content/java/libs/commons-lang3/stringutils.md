---
id: java/libs/commons-lang3/stringutils
title: StringUtils
type: api
summary: Métodos null-safe para String da Apache Commons Lang — isEmpty, join, abbreviate, replace — que eliminam NullPointerException e ifs manuais de checagem.
level: beginner
duration: 7
officialDocs:
  label: Apache Commons Lang — StringUtils
  url: https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html
related:
  - java/jdk/java.lang/string
  - java/jdk/java.lang/stringbuilder
  - java/jdk/java.util/objects
---

> [!INFO] `StringUtils` (Apache Commons Lang) empacota utilitários **null-safe** para String — `isEmpty`, `join`, `abbreviate`, `replace` — todos aceitam `null` sem lançar exceção. A checagem manual `if (s != null)` some do código.

## Visão geral

`StringUtils` é uma caixa de utilitários **estáticos** para String: comparação, junção, divisão, preenchimento e transformação — tudo com a mesma convenção null-safe. Onde a API do `String` do JDK lança `NullPointerException` para `null`, o `StringUtils` devolve um valor previsível.

```java
import org.apache.commons.lang3.StringUtils;

StringUtils.isEmpty(null);         // true — null conta como vazio
StringUtils.isBlank("   ");        // true — só espaços
StringUtils.join(List.of("a", "b"), ", ");   // "a, b"
StringUtils.abbreviate("Documentação longa", 8);  // "Docum..."
```

## Dependência

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.17.0</version>
</dependency>
```

```groovy
implementation 'org.apache.commons:commons-lang3:3.17.0'
```

Biblioteca pequena, sem dependências transitivas — um único JAR no classpath.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `isEmpty(CharSequence)` | `boolean` | `null` **ou** `""` (espaços não contam) |
| `isBlank(CharSequence)` | `boolean` | `null`, `""` ou só espaços |
| `defaultString(String[, String])` | `String` | `null` vira `""` (ou o padrão dado) |
| `defaultIfBlank(String, String)` | `String` | fallback quando vazio/espaços |
| `join(Iterable, String)` | `String` | junta com separador (null-safe) |
| `split(String, String)` | `String[]` | quebra por separador **literal** |
| `replace(String, String, String)` | `String` | troca literal (sem regex) |
| `abbreviate(String, int)` | `String` | encurta com `"..."` |
| `leftPad(String, int[, char])` | `String` | preenche à esquerda até o tamanho |
| `capitalize(String)` | `String` | primeira letra maiúscula |
| `repeat(String, int)` | `String` | repete `n` vezes |
| `substringBefore/substringAfter` | `String` | trecho antes/depois do separador |
| `removeEnd(String, String)` | `String` | remove sufixo se existir |
| `equals(CharSequence, CharSequence)` | `boolean` | compara conteúdo (null-safe) |

## Checagens null-safe

```java
String nome = config.get("nome");

StringUtils.isEmpty(nome);                    // true se null ou ""
StringUtils.isBlank(nome);                    // true se null, "" ou "   "
StringUtils.defaultString(nome);              // null vira ""
StringUtils.defaultIfBlank(nome, "anônimo");  // fallback para vazio/espaços
```

Armadilha: `isEmpty` **não** considera espaços — `isEmpty("   ")` é `false`; use `isBlank` quando espaço em branco também é "vazio".

## Junção e divisão

```java
List<String> tags = List.of("java", "backend", "docs");

StringUtils.join(tags, ", ");             // "java, backend, docs"
StringUtils.split("java,backend", ",");   // ["java", "backend"]
StringUtils.split("a,,b", ",");           // ["a", "b"] — vazios descartados
```

Armadilha: `split` trata separadores adjacentes como um e `StringUtils.split(null)` retorna **`null`**, não array vazio — cheque o retorno antes de percorrer.

## Encurtamento e preenchimento

```java
StringUtils.abbreviate("Documentação muito longa", 12);  // "Documenta..."
StringUtils.abbreviate("curto", 12);                     // "curto" (não mexe)
StringUtils.leftPad("42", 5, '0');                       // "00042"
StringUtils.rightPad("42", 5);                           // "42   " (espaço padrão)
```

Armadilha: o `maxWidth` do `abbreviate` **inclui** o `"..."` — e `maxWidth < 4` lança `IllegalArgumentException`.

## Transformações

```java
StringUtils.replace("a.b.c", ".", "/");        // "a/b/c" — literal
StringUtils.capitalize("eyecode");             // "Eyecode"
StringUtils.removeEnd("arquivo.txt", ".txt");  // "arquivo"
StringUtils.repeat("ab", 3);                   // "ababab"
```

Armadilha: `replace` troca **literal** — nada de regex (o oposto de `String.replaceAll`); para padrão, use a API do JDK.

## Armadilhas comuns

> [!WARNING] `StringUtils.split(null)` retorna `null` — não um array vazio. Um `for (String s : StringUtils.split(null, ","))` estoura `NullPointerException` apesar da biblioteca ser "null-safe": o null-safety vale para a **entrada**; o retorno de `split` para entrada nula ainda é `null`.

**Métodos são `static`:** `StringUtils` não tem construtor — importe a classe e chame os métodos direto.

**Redundante no Java moderno:** `String.join`, `isBlank`, `repeat` e `Objects.requireNonNullElse` cobrem os casos mais comuns — adote a biblioteca pelos que faltam (`abbreviate`, `leftPad`, `substringBefore`), não por hábito.

## Profundidade

O null-safe do `StringUtils` é uma convenção da biblioteca: métodos que recebem `String` aceitam `null` e devolvem `null` (ou o padrão do método) — `IllegalArgumentException` aparece só em casos degenerados como `abbreviate` com `maxWidth < 4`. O pacote `commons-lang3` traz outros utilitários no mesmo estilo (`ObjectUtils`, `ArrayUtils`, `RandomUtils`). A biblioteca é estável e madura — o custo é um JAR extra no classpath para algo que o `String` do JDK moderno resolve parcialmente.
