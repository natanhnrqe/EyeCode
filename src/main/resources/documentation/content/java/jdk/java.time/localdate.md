---
id: java/jdk/java.time/localdate
title: LocalDate
type: api
summary: Datas sem hora no eixo local — criação com now/of/parse, aritmética com plus/minus, ajustes com with, diferenças entre datas e formatação com DateTimeFormatter.
level: beginner
duration: 8
officialDocs:
  label: API java.time.LocalDate
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html
related:
  - java/jdk/java.time/localdatetime
  - java/jdk/java.time/duration
  - java/jdk/java.lang/exceptions
---

> [!INFO] `LocalDate` é uma **data de calendário sem hora e sem fuso** (`2026-09-29`): aniversários, prazos, dias de vencimento. É **imutável** — cada cálculo devolve uma instância nova — e segue o padrão ISO-8601.

## Visão geral

Antes do Java 8, datas viviam em `java.util.Date` + `Calendar`: mutáveis, confusas e com meses indexados de 0. A API `java.time` substituiu isso por tipos simples e imutáveis, e `LocalDate` é o mais usado deles — qualquer coisa que caiba em "dia, mês e ano".

```java
LocalDate hoje = LocalDate.now();               // 2026-09-29 (fuso padrão da JVM)
LocalDate prazo = LocalDate.of(2026, 12, 31);   // data exata
LocalDate lida = LocalDate.parse("2026-09-29"); // a partir de texto ISO

prazo.isAfter(hoje);   // true
```

A regra da classe: **nada muda no lugar** — `plusDays`, `withYear`, tudo devolve uma `LocalDate` nova.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `now()` / `now(ZoneId)` | `LocalDate` | hoje no fuso padrão / num fuso específico |
| `of(int ano, int mes, int dia)` | `LocalDate` | data exata (valida; inválida lança exceção) |
| `parse(CharSequence)` | `LocalDate` | lê texto ISO (`2026-09-29`) |
| `plusDays/plusWeeks/plusMonths/plusYears(long)` | `LocalDate` | soma no calendário |
| `minusDays/minusWeeks/minusMonths/minusYears(long)` | `LocalDate` | subtrai |
| `withDayOfMonth/withMonth/withYear(int)` | `LocalDate` | troca um campo específico |
| `with(TemporalAdjuster)` | `LocalDate` | ajuste avançado (ex.: `firstDayOfNextMonth()`) |
| `until(LocalDate)` | `Period` | diferença em anos/meses/dias |
| `until(LocalDate, TemporalUnit)` | `long` | diferença numa unidade (`ChronoUnit.DAYS`) |
| `format(DateTimeFormatter)` | `String` | converte em texto |
| `isBefore/isAfter/isEqualTo(LocalDate)` | `boolean` | comparação cronológica |
| `getYear()/getMonth()/getDayOfWeek()` | — | leitura dos campos (`Month` e `DayOfWeek` são enums) |
| `isLeapYear()` / `lengthOfMonth()` | — | calendário (ano bissexto, dias do mês) |
| `atStartOfDay()` / `atTime(int, int)` | `LocalDateTime` | combina com uma hora |

## Criação e leitura

```java
LocalDate natal = LocalDate.of(2026, 12, 25);

natal.getYear();          // 2026
natal.getMonth();         // Month.DECEMBER — enum, melhor que número
natal.getDayOfWeek();     // DayOfWeek.FRIDAY
natal.lengthOfMonth();    // 31

LocalDate.of(2026, 2, 30);   // DateTimeException — 30 de fevereiro não existe
```

`parse` sem argumentos extras só aceita o formato ISO `yyyy-MM-dd`; datas em outro formato pedem um `DateTimeFormatter` (ver grupo de diferenças e formatação).

## Aritmética e ajustes

```java
LocalDate entrega = LocalDate.of(2026, 1, 31);

entrega.plusDays(1);     // 2026-02-01 — avança no calendário de verdade
entrega.plusMonths(1);   // 2026-02-28 — clamp: 31/jan não vira 31/fev
entrega.withYear(2024);  // 2024-01-31 — troca só o ano
entrega.with(TemporalAdjusters.firstDayOfNextMonth());  // 2026-02-01

LocalDate novaData = entrega.plusMonths(12);   // 2027-01-31 — SEMPRE reatribua
```

`plus*` e `with*` cobrem quase tudo; os `TemporalAdjusters` prontos (`lastDayOfMonth()`, `next(DayOfWeek.MONDAY)`, `dayOfWeekInMonth(...)`) resolvem os ajustes de calendário chatos.

## Diferenças e formatação

```java
LocalDate inicio = LocalDate.of(2026, 1, 10);
LocalDate fim = LocalDate.of(2026, 3, 15);

Period p = inicio.until(fim);                    // P2M5D — 2 meses e 5 dias
long total = ChronoUnit.DAYS.between(inicio, fim);  // 64 — total em dias
long semanas = inicio.until(fim, ChronoUnit.WEEKS); // 9 semanas completas

LocalDate lida = LocalDate.parse("29/09/2026",
        DateTimeFormatter.ofPattern("dd/MM/yyyy"));
String exibicao = lida.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
// "29/09/2026"
```

> [!WARNING] `parse` lança `DateTimeParseException` (unchecked) quando o texto não bate com o formato — inclusive `"2026-09-29"` num formatter `dd/MM/yyyy`. Entrada de usuário pede try/catch ou validação prévia.

## Armadilhas comuns

> [!WARNING] Como `String`, `LocalDate` é imutável: `prazo.plusDays(1);` sozinho **não muda nada** — o resultado foi descartado. Esquecer de reatribuir compila normal e o bug é silencioso.

**Esquecer a reatribuição:**

```java
LocalDate prazo = LocalDate.of(2026, 9, 29);
prazo.plusDays(1);          // descartado — prazo continua 2026-09-29
prazo = prazo.plusDays(1);   // 2026-09-30 — agora sim
```

**Validação lança exceção:** `of` e `parse` são unchecked — `DateTimeException` e `DateTimeParseException` estouram em runtime. Datas vindas de tela, arquivo ou API precisam de try/catch.

**`until` não é total de dias:** devolve um `Period` com anos/meses/dias de calendário (P2M5D), não um número. Para "quantos dias" use `ChronoUnit.DAYS.between(a, b)`.

**`equals` exige a data exata:** `LocalDate` não tem tolerância — 2026-09-29 e 2026-09-30 nunca são iguais. Para intervalos, `isBefore`/`isAfter`/`isEqual` são as ferramentas certas.

## Profundidade

`java.time` é a JSR-310 (Java 8), fortemente inspirada em Joda-Time. `LocalDate` usa o calendário ISO-8601 proléptico (ano 0 existe, fevereiro tem 29 dias só em anos bissextos) e é uma *value-based class*: imutável, thread-safe, sem construtor público (`of` é a via canônica de construção). Diferente do velho `SimpleDateFormat`, `DateTimeFormatter` é imutável e seguro para compartilhar entre threads — crie uma constante por padrão e reutilize. `now()` consulta o fuso padrão da JVM; `now(ZoneId)` deixa explícito qual "hoje" você quer.
