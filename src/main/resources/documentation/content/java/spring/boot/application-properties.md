---
id: java/spring/boot/application-properties
title: Application Properties
type: guide
summary: Como configurar uma aplicação Spring Boot com application.properties/yml, ler valores com @Value e agrupar propriedades próprias com @ConfigurationProperties.
level: beginner
duration: 8
officialDocs:
  label: Spring Boot — Externalized Configuration
  url: https://docs.spring.io/spring-boot/reference/features/external-config.html
related:
  - java/spring/boot/profiles
  - java/spring/boot-basics
  - java/spring/core/dependency-injection
---

> [!INFO] Toda configuração de um Spring Boot vive em `application.properties` (ou `.yml`) e pode ser sobrescrita de fora — variáveis de ambiente, argumentos de linha. Para suas próprias chaves, use `@ConfigurationProperties`: um POJO tipado que o Boot preenche e valida sozinho.

## Cenário

Sua API precisa de parâmetros configuráveis: URL de um serviço externo, timeout, limite de tentativas. Espalhar `@Value("${...}")` por aí funciona, mas vira caça ao tesouro. A receita: declare as chaves em `application.properties` e mapeie tudo para uma classe tipada com `@ConfigurationProperties`.

## Passo a passo

### Passo 1 — Declare as propriedades

`src/main/resources/application.properties`:

```properties
server.port=8080
spring.application.name=pedidos-api

# suas chaves customizadas
integracao.cep.url=https://viacep.com.br/ws
integracao.cep.timeout-ms=2000
integracao.cep.tentativas=3
```

### Passo 2 — Mapeie para uma classe tipada

```java
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integracao.cep")
public class CepProperties {

    private String url;
    private int timeoutMs = 1000;   // default
    private int tentativas = 1;

    // getters e setters (obrigatórios para binding por setter)
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getTentativas() { return tentativas; }
    public void setTentativas(int tentativas) { this.tentativas = tentativas; }
}
```

### Passo 3 — Registre e injete

```java
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CepProperties.class)
public class IntegracaoConfig {
}
```

```java
import org.springframework.stereotype.Service;

@Service
public class CepService {

    private final CepProperties props;

    public CepService(CepProperties props) {
        this.props = props;
    }

    public String montarUrl(String cep) {
        return props.getUrl() + "/" + cep;
    }
}
```

O Boot faz o *binding*: `integracao.cep.timeout-ms` → `setTimeoutMs(int)` (o *relaxed binding* aceita `timeout-ms`, `timeoutMs` ou `TIMEOUT_MS`).

## Como funciona

O Boot monta um `Environment` ordenado (simplificando, da maior para a menor precedência): argumentos de linha de comando → variáveis de ambiente → `application-<profile>.properties` → `application.properties` → defaults no código. Por isso `SERVER_PORT=9090 java -jar app.jar` vence o `server.port=8080` do arquivo sem recompilar nada.

`@ConfigurationProperties` é um bean comum: passa pelo ciclo de vida normal, pode ser validado com `jakarta.validation` (`@NotBlank`, `@Min`) e falha no startup se `url` obrigatória vier vazia — erro cedo, não em runtime.

## Variações

**YAML** — hierarquia mais legível para muitas chaves:

```yaml
server:
  port: 8080
integracao:
  cep:
    url: https://viacep.com.br/ws
    timeout-ms: 2000
```

**@Value para casos pontuais** — aceitável para UMA chave solta:

```java
@Value("${spring.application.name}")
private String appName;
```

**Record imutável com `@ConfigurationProperties`** (binding por construtor):

```java
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integracao.cep")
public record CepProps(String url, int timeoutMs, int tentativas) {}
```

## Armadilhas

> [!WARNING] `@ConfigurationProperties` sem registro (`@EnableConfigurationProperties` ou `@ConfigurationPropertiesScan`) simplesmente não vira bean — a injeção falha com `NoSuchBeanDefinitionException` e você perde tempo procurando a causa errada.

- **Esquecer os setters** na classe por setter: o binding silenciosamente deixa tudo `null`/default — com record + binding por construtor isso não acontece;
- **Segredo no properties versionado**: senhas em `application.properties` vão para o Git — use `${DB_PASSWORD}` + variável de ambiente;
- **`@Value` com valor errado não falha cedo**: `@Value("${chave.inexistente}")` explode só quando a classe carrega, ou pior, injeta a string literal `"${chave.inexistente}"` — `@ConfigurationProperties` com validação é bem mais seguro;
- **Misturar `.properties` e `.yml`** na mesma aplicação: funciona, mas confunde — padronize um formato.

## Profundidade

A configuração externizada é um dos pilares do *twelve-factor app*: código igual, configuração por ambiente. Internamente, cada fonte é um `PropertySource` empilhado no `Environment`; o *relaxed binding* normaliza as variantes de nome (`kebab-case` no arquivo → `camelCase` no Java → `UPPER_SNAKE` em variável de ambiente). Com o starter `spring-boot-configuration-processor` no `pom.xml`, sua IDE ganha até autocompletar das suas próprias chaves.
