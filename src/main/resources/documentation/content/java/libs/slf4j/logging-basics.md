---
id: java/libs/slf4j/logging-basics
title: SLF4J: logging básico
type: guide
summary: Logue com SLF4J — LoggerFactory, placeholders {} que evitam concatenação, os níveis na ordem certa e como o backend (Logback) entra na jogada.
level: beginner
duration: 7
officialDocs:
  label: SLF4J — Simple Logging Facade for Java
  url: https://www.slf4j.org
related:
  - java/libs/logback/configuration
  - java/spring/boot-basics
  - java/jdk/java.lang/system
---

> [!INFO] SLF4J é a **fachada** de logging do Java: seu código fala com a API (`LoggerFactory` + `Logger`), e um backend (Logback, Log4j2...) faz o trabalho real. A receita do dia a dia: um `Logger` por classe, mensagens com placeholders `{}` e o nível certo para cada situação.

## Cenário

Você tem um serviço que precisa registrar o que acontece — requisições, erros, valores de debug — e está usando `System.out.println`. O problema: sem nível, sem formato, sem destino configurável e sem como desligar o debug em produção. SLF4J resolve tudo isso com uma API que você escreve uma vez e troca de backend sem tocar no código.

## Dependência

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.16</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.12</version>
    <scope>runtime</scope>
</dependency>
```

```groovy
implementation 'org.slf4j:slf4j-api:2.0.16'
runtimeOnly 'ch.qos.logback:logback-classic:1.5.12'
```

A API (`slf4j-api`) fica em `implementation`; o backend em `runtime` — seu código nunca importa classes do Logback, só da fachada. No Spring Boot, o starter já inclui SLF4J + Logback prontos.

## Passo a passo

### Passo 1 — Obtenha o logger

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    public void processar(String pedido) {
        log.info("Processando pedido {}", pedido);
    }
}
```

Um logger por classe, `private static final` — o nome do logger sai da classe passada e aparece no log para filtrar.

### Passo 2 — Logue com placeholders

```java
log.info("Pedido {} processado em {} ms", id, duracao);
log.debug("Cache hit: chave={}, ttl={}", chave, ttl);
log.error("Falha ao salvar pedido {}", id, excecao);   // exceção por último
```

O `{}` é substituído por cada argumento, na ordem. A exceção vai como **último argumento**, sem placeholder — o backend imprime o stack trace completo.

Armadilha: concatenação com `+` avalia **antes** da chamada — `log.debug("x=" + caro())` executa `caro()` mesmo com debug desligado; com `{}`, os argumentos só são avaliados se o nível está ativo.

### Passo 3 — Escolha o nível certo

Da menos grave para a mais grave:

| Nível | Quando usar |
|-------|-------------|
| `TRACE` | detalhe fino, só para depurar um problema específico |
| `DEBUG` | diagnóstico no desenvolvimento |
| `INFO` | eventos normais do sistema (startup, requisição processada) |
| `WARN` | algo estranho, mas o sistema continua |
| `ERROR` | falha que precisa de atenção |

O nível mínimo é configurado no backend (no Logback, `logback.xml` com `<root level="INFO">`): `INFO` esconde `DEBUG` e `TRACE`, mas mostra `WARN` e `ERROR`.

### Passo 4 — Ligue um backend

Sem backend no classpath, o SLF4J imprime um aviso (`No SLF4J providers were found`) e descarta tudo. Com `logback-classic`, crie `src/main/resources/logback.xml`:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

Agora `log.info(...)` imprime no console com o formato do pattern.

## Como funciona

O SLF4J é uma fachada: `LoggerFactory.getLogger(...)` procura um **provedor** no classpath (Logback, Log4j2, slf4j-simple...) e devolve um `Logger` que delega para ele. Seu código conhece só `org.slf4j.*` — trocar de backend é trocar a dependência de runtime, sem tocar nas classes. Os placeholders `{}` têm avaliação preguiçosa: os argumentos só viram string se o nível está ativo — é por isso que vencem a concatenação com `+` em código de log.

## Variações

**Logger por nome customizado** — `LoggerFactory.getLogger("audit")` cria um logger com nome fixo, útil para separar trilhas de auditoria do log geral.

**Guarda de nível explícita** — para montar mensagem cara fora de placeholder:

```java
if (log.isDebugEnabled()) {
    log.debug("Estado completo: {}", estadoCaroDeMontar());
}
```

**Marcação (`Marker`)** — `MarkerFactory.getMarker("CONFIDENCIAL")` etiqueta a mensagem para filtros do backend.

**Spring Boot** — níveis por pacote direto no `application.properties`: `logging.level.com.eyecode=DEBUG` — sem `logback.xml`.

## Armadilhas

> [!WARNING] Nunca interpole dados sensíveis (senha, token, CPF) em mensagens de log — o log vai para arquivo, console e ferramentas de observabilidade, e o dado vaza para qualquer um com acesso. Mascare antes: `log.info("Login usuario={}, token=***", usuario)`.

- **Concatenação com `+`**: avalia a expressão mesmo com o nível desligado — use `{}` sempre;
- **Exceção no meio dos argumentos**: `log.error("Falha {}", e, id)` — a exceção não imprime stack trace; ela deve ser o **último** argumento;
- **Backend esquecido**: sem provedor no classpath, o log é descartado com um aviso na primeira chamada;
- **`log.isInfoEnabled()` desnecessário**: a checagem já é interna com `{}` — a guarda explícita só vale para montagem cara da mensagem.

## Profundidade

A fachada resolve o problema do acoplamento: bibliotecas de terceiros logam com SLF4J, e o **seu** backend decide formato e destino — é por isso que toda biblioteca moderna (Spring, Hibernate, Netty) usa SLF4J. No Spring Boot, o Logback é o backend padrão e o `spring-boot-starter-logging` já vem no starter web; a configuração por `application.properties` (`logging.level.*`, `logging.file.name`) cobre a maioria dos casos — o `logback.xml` fica para patterns customizados, appenders múltiplos e rotação fina.
