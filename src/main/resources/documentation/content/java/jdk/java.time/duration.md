---
id: java/jdk/java.time/duration
title: Duration e Period
type: api
summary: Medição de tempo em duas escalas — Duration (horas, minutos e segundos de máquina) e Period (anos, meses e dias de calendário), mais ChronoUnit.between e conversões.
level: beginner
duration: 7
officialDocs:
  label: API java.time.Duration
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html
related:
  - java/jdk/java.time/localdate
  - java/jdk/java.time/localdatetime
  - java/jdk/java.lang/thread
---

> [!INFO] Existem duas formas de medir tempo: **Duration** é a escala de máquina (horas/minutos/segundos exatos — timeouts, cronômetros) e **Period** é a escala humana (anos/meses/dias de calendário — "daqui a 3 meses"). `ChronoUnit.between` calcula a diferença na unidade que você escolher.

## Visão geral

Somar "30 dias" a uma data parece simples — até fevereiro, anos bissextos e horário de verão entrarem na conversa. A JSR-310 separou o problema em dois tipos honestos: `Duration`, com tamanho **exato** em relógio (segundos + nanos), e `Period`, com unidades de **calendário** cujo tamanho real depende de onde cai.

```java
Duration timeout = Duration.ofMinutes(30);
Period garantia = Period.ofMonths(12);

Instant expira = Instant.now().plus(timeout);  // 30 minutos exatos
LocalDate fimGarantia = LocalDate.now().plus(garantia);  // 12 meses de calendário
```

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Duration.ofSeconds(long[, int nanos])` | `Duration` | duração em segundos (com nanos opcionais) |
| `Duration.ofMinutes/ofHours/ofDays(long)` | `Duration` | duração na unidade dada |
| `Duration.ofMillis/ofNanos(long)` | `Duration` | frações finas |
| `Duration.parse(CharSequence)` | `Duration` | lê ISO-8601 (`PT1H30M`) |
| `Duration.between(Temporal, Temporal)` | `Duration` | mede entre dois tempos com hora |
| `toHours()/toMinutes()/toSeconds()/toMillis()` | `long` | converte — **trunca** o resto |
| `toHoursPart()/toMinutesPart()/toSecondsPart()` | `int` | o resto dentro da unidade maior |
| `plus(Duration)/minus(Duration)/multipliedBy(long)` | `Duration` | aritmética exata |
| `isZero()/isNegative()` | `boolean` | sinal da duração |
| `Period.of(int anos, int meses, int dias)` | `Period` | período de calendário |
| `Period.ofDays/ofWeeks/ofMonths/ofYears(long)` | `Period` | período numa unidade |
| `Period.between(LocalDate, LocalDate)` | `Period` | diferença em anos/meses/dias |
| `Period.normalize()` | `Period` | junta anos + meses excedentes |
| `ChronoUnit.X.between(Temporal, Temporal)` | `long` | diferença na unidade `X` (dias, horas...) |

## Duration: tempo de máquina

```java
Duration aula = Duration.between(
        java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 30));  // PT1H30M

Duration lida = Duration.parse("PT2H15M");  // P = período, T = parte de hora
Instant expira = Instant.now().plus(Duration.ofMinutes(30));

aula.toMinutes();      // 90 — o total
aula.toHours();        // 1 — trunca os 30 minutos restantes!
aula.toHoursPart();    // 1
aula.toMinutesPart();  // 30 — o resto, dentro da hora

Thread.sleep(Duration.ofMillis(200));   // Java 19+: sleep aceita Duration
```

Use `Duration` para tudo que o relógio medi: expiração de sessão, timeout de rede, tempo de execução. A conversão de volta para `long` usa `to*()` — e as `to*Part()` respondem "quantas sobraram".

## Period: tempo de calendário

```java
LocalDate compra = LocalDate.of(2026, 1, 31);
Period garantia = Period.ofMonths(12);

compra.plus(garantia);              // 2027-01-31 — um ano de calendário
compra.plus(Period.ofMonths(1));    // 2026-02-28 — clamp no fim do mês
Period.of(1, 13, 60).normalize();   // P2Y1M60D — 13 meses viram 1 ano e 1 mês

Period idade = Period.between(
        LocalDate.of(1990, 5, 14), LocalDate.now());   // ex.: P36Y4M15D
```

`Period` é a escolha quando o ser humano manda: "parcelas mensais", "garantia de 1 ano", "a cada 3 meses". Nenhum `Period` vira milissegundos sem você decidir quantos dias tem "um mês" — essa decisão é sua, não da API.

## ChronoUnit e conversões

`ChronoUnit` é o enum que unifica as unidades: `between(a, b)` devolve a diferença completa na unidade pedida, sem decompor:

```java
LocalDate a = LocalDate.of(2026, 1, 1);
LocalDate b = LocalDate.of(2026, 3, 15);

ChronoUnit.DAYS.between(a, b);    // 73
ChronoUnit.WEEKS.between(a, b);   // 10 semanas completas
ChronoUnit.MONTHS.between(a, b);  // 2

var ini = java.time.LocalDateTime.of(2026, 1, 1, 8, 0);
var fim = java.time.LocalDateTime.of(2026, 1, 1, 9, 45);
ChronoUnit.MINUTES.between(ini, fim);   // 105
```

## Armadilhas comuns

> [!WARNING] `Duration.between` exige tipos com **hora**: `Duration.between(localDate1, localDate2)` lança `UnsupportedTemporalTypeException`, porque `LocalDate` não tem segundos. Para diferença entre datas use `Period.between` ou `ChronoUnit.DAYS.between`.

**Conversões truncam:**

```java
Duration d = Duration.ofSeconds(90);
d.toMinutes();      // 1 — os 30 segundos somem
d.toMinutesPart();  // 30 — para o resto, use as versões Part
```

**Period não tem tamanho fixo:** 1 mês pode ser 28, 29, 30 ou 31 dias — nunca converta `Period` em millis assumindo 30 dias. Quem tem tamanho exato em relógio é `Duration`; se precisa de millis, comece por Duration.

**Clamp no fim do mês:** `LocalDate.of(2026, 1, 31).plus(Period.ofMonths(1))` → `2026-02-28`. Em contratos ("vence dia 31") decida explicitamente com `with(TemporalAdjusters.lastDayOfMonth())` ou regra de negócio.

**parse é ISO-8601:** `"PT1H30M"` (P período, T tempo) — textos como `"1h30"` ou `"90 minutos"` lançam `DateTimeParseException`; converta antes de chamar `parse`.

## Profundidade

A JSR-310 separa deliberadamente as duas escalas: `Duration` modela tempo na escala de máquina (uma quantidade de segundos + nanos, como `Instant`) e suporta aritmética exata (`multipliedBy`, `dividedBy`); `Period` guarda anos/meses/dias como campos independentes, na escala humana, e nem expõe `toMillis`. A sintaxe `PnYnMnDTnHnMnS` vem do ISO-8601 — é por isso que `toString()` de um `Period` de 1 ano é `P1Y` e de uma `Duration` de 90 minutos é `PT1H30M`. `ChronoUnit` fecha o círculo com `between()` para qualquer par de tipos que suportem a unidade pedida.
