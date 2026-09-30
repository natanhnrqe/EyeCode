---
id: java/jdk/java.time/localdatetime
title: LocalDateTime
type: api
summary: Data e hora no eixo local, sem fuso — criação, aritmética, conversão para ZonedDateTime/Instant, formatação e a armadilha clássica de tratar relógio de parede como instante.
level: intermediate
duration: 8
officialDocs:
  label: API java.time.LocalDateTime
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDateTime.html
related:
  - java/jdk/java.time/localdate
  - java/jdk/java.time/duration
  - java/jdk/java.lang/exceptions
---

> [!INFO] `LocalDateTime` é a junção de `LocalDate` + `LocalTime` **sem fuso horário**: o que um relógio de parede mostra (`2026-09-29T14:30`). É imutável e ótimo para eventos locais — mas **não é um instante absoluto**: para virar `Instant` é preciso informar o fuso.

## Visão geral

Reuniões, horários de funcionamento, prazos internos: o mundo humano opera em "dia e hora" sem se preocupar com fusos. `LocalDateTime` modela exatamente isso — e só isso. Ele não sabe **onde** aquela parede está; converter para um momento absoluto (`Instant`) exige dizer qual fuso aplica.

```java
LocalDateTime agora = LocalDateTime.now();
LocalDateTime reuniao = LocalDateTime.of(2026, 9, 29, 14, 30);
LocalDateTime lida = LocalDateTime.parse("2026-09-29T14:30:00");

reuniao.toLocalDate();   // 2026-09-29
reuniao.toLocalTime();   // 14:30
```

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `now()` / `now(ZoneId)` | `LocalDateTime` | parede do fuso padrão / de um fuso específico |
| `of(int ano, int mes, int dia, int h, int min[, int s, int ns])` | `LocalDateTime` | data e hora exatas (valida tudo) |
| `of(LocalDate, LocalTime)` | `LocalDateTime` | combina os dois valores |
| `parse(CharSequence)` | `LocalDateTime` | lê ISO (`2026-09-29T14:30:00`) |
| `plusDays/plusHours/plusMinutes/plusSeconds/plusNanos/plusWeeks/plusMonths/plusYears(long)` | `LocalDateTime` | aritmética |
| `withHour/withMinute/withDayOfMonth/withYear(int)` | `LocalDateTime` | ajusta um campo |
| `with(TemporalAdjuster)` | `LocalDateTime` | ajuste avançado (ex.: `next(MONDAY)`) |
| `toLocalDate()` / `toLocalTime()` | `LocalDate` / `LocalTime` | separa as partes |
| `atZone(ZoneId)` | `ZonedDateTime` | adiciona as regras de um fuso |
| `atOffset(ZoneOffset)` | `OffsetDateTime` | adiciona um offset fixo |
| `toInstant(ZoneOffset)` / `toEpochSecond(ZoneOffset)` | `Instant` / `long` | momento absoluto — exige o offset |
| `format(DateTimeFormatter)` | `String` | converte em texto |
| `isBefore/isAfter/isEqualTo(LocalDateTime)` | `boolean` | comparação de parede |

## Criação e combinação

```java
LocalDate dia = LocalDate.of(2026, 9, 29);
LocalTime hora = LocalTime.of(14, 30);

LocalDateTime a = LocalDateTime.of(dia, hora);   // 2026-09-29T14:30
LocalDateTime b = dia.atTime(20, 0);             // 2026-09-29T20:00
LocalDateTime c = hora.atDate(dia);              // 2026-09-29T14:30
LocalDateTime lida = LocalDateTime.parse("2026-09-29T14:30:00");  // com "T"
```

Os três caminhos (`of`, `date.atTime`, `time.atDate`) produzem o mesmo valor — escolha o que lê melhor no seu contexto.

## Conversão com fuso: ZonedDateTime e Instant

```java
LocalDateTime reuniao = LocalDateTime.of(2026, 9, 29, 14, 30);

ZonedDateTime comFuso = reuniao.atZone(ZoneId.of("America/Sao_Paulo"));
Instant instante = comFuso.toInstant();    // o momento absoluto (UTC)

// atalho quando você já sabe o offset (São Paulo = -03:00)
Instant direto = reuniao.toInstant(ZoneOffset.of("-03:00"));

// caminho de volta: momento absoluto → parede local
LocalDateTime agoraAqui = LocalDateTime.ofInstant(
        Instant.now(), ZoneId.systemDefault());
```

> [!WARNING] Não existe `reuniao.toInstant()` sem argumento — o compilador exige o offset, e isso é proteção, não burocracia: o mesmo `14:30` em São Paulo e em Lisboa são momentos diferentes no mundo real. Sempre pergunte "que fuso é este?" antes de converter.

## Aritmética e formatação

```java
LocalDateTime prazo = LocalDateTime.of(2026, 9, 29, 14, 30);

prazo.plusHours(3);   // 2026-09-29T17:30
prazo.plusWeeks(1);   // 2026-10-06T14:30
prazo.withMinute(0).withSecond(0).withNano(0);  // 2026-09-29T14:00
prazo.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
// 2026-10-05T14:30 (29/09 é terça; a próxima segunda é 05/10)

DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
prazo.format(br);                                 // "29/09/2026 14:30"
LocalDateTime lido = LocalDateTime.parse("29/09/2026 14:30", br);
```

## Armadilhas comuns

> [!WARNING] `LocalDateTime` **não é um instante**: duas ocorrências idênticas de `14:30` em fusos diferentes são momentos diferentes. Gravar `LocalDateTime` como timestamp global (logs, auditoria, eventos entre servidores) é o erro clássico — para isso existe `Instant` (sempre UTC).

**Horário de verão não existe aqui:** `plusDays` conta 24 horas de relógio de parede, sem DST. Quem resolve gap/sobreposição de horário de verão é `ZonedDateTime` — converta antes de calcular.

**parse ISO exige o "T":** o formato padrão é `2026-09-29T14:30:00`; espaço no lugar do `T` ou `dd/MM/yyyy HH:mm` precisa de um `DateTimeFormatter` explícito — senão, `DateTimeParseException`.

**Comparação entre fusos não é ordem real:** `isBefore` compara parede com parede. Duas `LocalDateTime` "iguais" em fusos distintos são dois momentos distintos no mundo.

**`now()` depende do fuso da JVM:** a mesma aplicação devolve `2026-09-29T23:00` no Brasil e `2026-09-30T03:00` em Berlim (UTC+1) — em testes, prefira `LocalDateTime.of(...)` fixo ou `now(ZoneId)` explícito.

## Profundidade

A JSR-310 organiza o tempo em quatro eixos: **Local** (`LocalDateTime`: só a parede), **Offset** (`OffsetDateTime`: parede + offset fixo, bom para troca de dados), **Zoned** (`ZonedDateTime`: parede + regras completas de IANA, com DST e histórico) e **Instant** (momento absoluto em UTC). `LocalDateTime` é um par (LocalDate, LocalTime) sem zona nenhuma — value-based, imutável, thread-safe. `atZone(ZoneId)` consulta as regras do banco tzdata (o mesmo que o sistema operacional usa, atualizado a cada JDK); por isso a conversão também é onde aparecem as bordas de horário de verão.
