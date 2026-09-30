---
id: java/spring/boot/profiles
title: Profiles
type: guide
summary: Como alternar configuração por ambiente com application-<profile>.properties, @Profile em beans e ativação em runtime via propriedade ou variável de ambiente.
level: beginner
duration: 7
officialDocs:
  label: Spring Boot — Profiles
  url: https://docs.spring.io/spring-boot/reference/features/profiles.html
related:
  - java/spring/boot/application-properties
  - java/spring/boot-basics
  - java/spring/core/dependency-injection
---

> [!INFO] Profiles separam configuração por ambiente: `application-dev.properties` para desenvolvimento, `application-prod.properties` para produção, e `@Profile("dev")` para ligar/desligar beans inteiros. Você ativa o profile em **runtime** (`spring.profiles.active`) — o mesmo JAR roda nos dois mundos.

## Cenário

Em desenvolvimento você quer banco H2 em memória e logs detalhados; em produção, PostgreSQL e logs só de warnings — **sem mudar código**. Profiles resolvem: cada ambiente vira um arquivo de propriedades e, quando necessário, um conjunto de beans condicionais.

## Passo a passo

### Passo 1 — Crie os arquivos por profile

`src/main/resources/application.properties` (comum a todos):

```properties
spring.application.name=pedidos-api
```

`src/main/resources/application-dev.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:devdb
logging.level.com.exemplo=DEBUG
```

`src/main/resources/application-prod.properties`:

```properties
spring.datasource.url=jdbc:postgresql://db.interno:5432/pedidos
logging.level.com.exemplo=WARN
server.port=80
```

### Passo 2 — Ative o profile em runtime

```bash
# via argumento
java -jar app.jar --spring.profiles.active=prod

# via variável de ambiente (padrão em containers)
SPRING_PROFILES_ACTIVE=prod java -jar app.jar
```

### Passo 3 — Condicione beans com @Profile

```java
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

public interface Notificador {
    void enviar(String mensagem);
}

@Service
@Profile("dev")
class NotificadorConsole implements Notificador {
    public void enviar(String mensagem) {
        System.out.println("[DEV] " + mensagem);
    }
}

@Service
@Profile("prod")
class NotificadorEmail implements Notificador {
    public void enviar(String mensagem) {
        // envia e-mail de verdade
    }
}
```

Quem injeta `Notificador` recebe a implementação do profile ativo — sem `if` de ambiente no código.

## Como funciona

No startup, o Boot monta o `Environment` em camadas:

1. Carrega `application.properties` (o **base**, sempre aplicado);
2. Sobrepõe as chaves de cada `application-<profile>.properties` ativo — a última camada vence;
3. Registra apenas os beans cujo `@Profile` casa com os profiles ativos (beans sem `@Profile` são sempre registrados).

Sem nenhum profile ativo, o Boot usa o profile `default` e lê apenas os arquivos base.

## Variações

**Múltiplos profiles ativos** — `--spring.profiles.active=dev,debug` combina arquivos e beans (propriedades do último profile da lista prevalecem).

**Expressões no `@Profile`** — `@Profile("!prod")` (tudo menos produção), `@Profile("dev | test")`, `@Profile("dev & cloud")`.

**YAML multi-documento** — um único YAML pode conter vários profiles separados por `---` com `spring.config.activate.on-profile`:

```yaml
spring.datasource.url: jdbc:h2:mem:base
---
spring.config.activate.on-profile: prod
spring.datasource.url: jdbc:postgresql://db.interno:5432/pedidos
```

## Armadilhas

> [!WARNING] Nunca versione segredos em `application-prod.properties`: senhas e chaves ficam no Git junto com o código. Em produção, injete segredos por **variável de ambiente** (`${DB_PASSWORD}`) ou gerenciador de segredos.

- **Typo no nome do profile**: `spring.profiles.active=producao` simplesmente não carrega `application-prod.properties` — o app sobe com defaults e você só percebe quando conecta no banco errado. Prefira nomes curtos e padronizados;
- **`spring.profiles.active` dentro de `application-<profile>.properties`** é ignorado por design — um profile não pode ativar outro;
- **`@Profile` em bean parcialmente sobreposto**: lembre que o bean sem `@Profile` continua existindo nos dois ambientes — dois candidatos causam `NoUniqueBeanDefinitionException`;
- **Lógica de negócio espalhada em profiles**: profiles são para **infraestrutura** (datasource, segurança), não para regras de negócio que variam por ambiente — isso indica problema de produto.

## Profundidade

Profiles são uma facilidade do Spring **Environment**: uma abstração sobre `PropertySource`s (arquivos, variáveis de ambiente, argumentos de linha de comando) com precedência definida. A ordem completa de precedência do Boot tem mais de dez fontes — argumentos de linha de comando vencem variáveis de ambiente, que vencem arquivos de propriedades, que vencem os defaults do código. Essa hierarquia é o que permite o mesmo artefato rodar em qualquer lugar, configurado de fora.
