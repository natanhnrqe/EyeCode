---
id: java/libs/logback/configuration
title: Logback: configuração
type: guide
summary: Configure o Logback — o backend de logging padrão do Spring Boot — com logback.xml, appenders de console e arquivo com rotação, níveis por logger e o pattern da mensagem.
level: beginner
duration: 7
officialDocs:
  label: Manual do Logback
  url: https://logback.qos.ch
related:
  - java/libs/slf4j/logging-basics
  - java/spring/boot/application-properties
  - java/spring/boot/profiles
---

> [!INFO] O Logback é o **backend** que imprime o log do SLF4J: onde o log vai (console, arquivo), em que formato (pattern) e com que nível mínimo. Tudo isso se configura no `logback.xml` — e no Spring Boot, o Logback já vem pronto por padrão.

## Cenário

Você já loga com SLF4J (`LoggerFactory` + placeholders `{}`), mas o log sai cru no console — sem timestamp legível, sem arquivo para auditoria e com o debug ligado para o sistema inteiro. O Logback resolve: appenders definem o destino, o pattern define o formato de cada linha e a hierarquia de loggers permite nível diferente por pacote.

## Dependência

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.12</version>
</dependency>
```

```groovy
implementation 'ch.qos.logback:logback-classic:1.5.12'
```

O `logback-classic` traz o SLF4J como dependência transitiva e é a ponte entre a fachada e o motor do Logback. No Spring Boot, o starter já inclui tudo — você só cria o arquivo de configuração.

## Passo a passo

### Passo 1 — Crie o logback.xml no lugar certo

Crie `src/main/resources/logback.xml` — o Logback procura o arquivo na raiz do classpath automaticamente, sem nenhuma configuração de código:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

Pronto: `log.info(...)` imprime no console com timestamp, nível e nome do logger.

### Passo 2 — Entenda o pattern da mensagem

| Conversor | O que imprime |
|-----------|---------------|
| `%d{HH:mm:ss.SSS}` | data/hora no formato dado |
| `%-5level` | nível alinhado à esquerda em 5 caracteres |
| `%logger{36}` | nome do logger abreviado em até 36 chars |
| `%msg` | a mensagem do `log.info(...)` |
| `%n` | quebra de linha |
| `%thread` | nome da thread |
| `%M` / `%line` | método e linha (caro — evite em produção) |

O pattern é uma string de conversores: uma linha típica sai como `12:30:01.250 INFO  com.eyecode.PedidoService - Processando pedido 42`.

### Passo 3 — Adicione log em arquivo com rotação

```xml
<appender name="ARQUIVO" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>10MB</maxFileSize>
        <maxHistory>7</maxHistory>
        <totalSizeCap>100MB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

Um `<appender-ref ref="ARQUIVO"/>` no `root` liga o appender. A política de rotação gira o arquivo por dia **e** por tamanho (10 MB), comprime os antigos com gzip e apaga o que passar de 7 dias ou de 100 MB no total.

### Passo 4 — Ajuste níveis por logger

```xml
<logger name="com.eyecode.repositorio" level="DEBUG"/>
<logger name="org.hibernate" level="WARN"/>

<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="ARQUIVO"/>
</root>
```

O `<root>` define o nível mínimo global; cada `<logger>` afinha por pacote — `DEBUG` ligado só para o seu repositório, `WARN` silenciando o ruído do Hibernate.

## Como funciona

Loggers formam uma **hierarquia pelo nome**: `com.eyecode` é pai de `com.eyecode.PedidoService`, e um logger sem nível explícito **herda** o do ancestral mais próximo. Um `Appender` é o destino (console, arquivo, rede); o `appender-ref` liga appenders a loggers — e pela regra da **aditividade**, o log de um logger também vai para os appenders dos ancestrais (por isso o `root` com dois appender-refs atende todo o sistema).

## Variações

**Cores no console** — `%highlight(%-5level)` colore o nível; combine com `%d{HH:mm:ss}` para um console de desenvolvimento mais legível.

**Appender assíncrono** — `AsyncAppender` emfile a escrita em outra thread; útil quando o log em arquivo pesa no caminho da requisição.

**Propriedade de contexto** — `<property name="APP_NAME" value="eyecode"/>` e `%property{APP_NAME}` no pattern para etiquetar cada linha com o nome do app.

**Spring Boot** — `logback-spring.xml` (em vez de `logback.xml`) habilita `<springProfile name="dev">` para configurar por perfil; e `logging.level.com.eyecode=DEBUG` no `application.properties` cobre níveis sem XML.

## Armadilhas

> [!WARNING] Com `logback.xml` (em vez de `logback-spring.xml`), as tags `<springProfile>` do Spring Boot **não funcionam** — o Logback comum não as conhece e a configuração pode falhar ao subir. Se o projeto é Spring, prefira `logback-spring.xml` sempre.

- **Log duplicado**: um `<logger>` com appender próprio **e** aditividade para o `root` imprime a linha duas vezes — use `additivity="false"` no logger;
- **Arquivo no lugar errado**: `logback.xml` precisa estar na **raiz do classpath** (`src/main/resources/`), não em subpacote;
- **`%M`/`%line` em produção**: capturar método e linha exige capturar stack trace — queda mensurável de throughput;
- **Referência inexistente**: `<appender-ref>` com nome que não corresponde a nenhum appender falha no parse do XML.

## Profundidade

A configuração do Logback é processada pelo **Joran** (configurador XML próprio) na primeira chamada de log — erros de XML aparecem como *status* no console. É possível dividir a configuração com `<include resource="..."/>`, auditar o pipeline com `<statusListener class="..."/>` e reconfigurar sem reiniciar com `scan="true"` (o Logback monitora o arquivo). No ecossistema Spring, as propriedades do `application.properties` resolvem a maioria dos casos; o XML fica para rotação fina, appenders múltiplos e patterns por destino.
