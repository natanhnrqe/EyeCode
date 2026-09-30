---
id: java/spring/boot/autoconfiguration
title: Auto-configuração
type: concept
summary: Como o Spring Boot decide quais beans criar por conta própria — @Conditional, condições, exclusões e como debugar com o report de auto-configuração.
level: advanced
duration: 9
officialDocs:
  label: Spring Boot — Auto-configuration
  url: https://docs.spring.io/spring-boot/reference/using/auto-configuration.html
related:
  - java/spring/boot-basics
  - java/spring/boot/application-properties
  - java/spring/core/dependency-injection
---

> [!INFO] Auto-configuração é a mágica do Spring Boot: ao encontrar certas classes no classpath, ele registra beans automaticamente (DataSource, Tomcat, Jackson). `@Conditional` determina quando criar; você pode excluir, sobrescrever ou debugar cada decisão.

## Por que existe

Sem auto-configuração, cada projeto Java web precisaria de dezenas de beans declarados manualmente: `DataSource`, `EntityManagerFactory`, `Jackson ObjectMapper`, servidor embutido. Boot elimina esse boilerplate — mas a mágica só ajuda se você entender quando ela atua e quando cede lugar à sua configuração.

## Anatomia da sintaxe

Auto-configuração básica:

```java
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;

@AutoConfiguration
@ConditionalOnClass(DataSource.class)     // só se tiver JDBC no classpath
@ConditionalOnMissingBean(DataSource.class) // cede se já existir seu bean
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
        name = "spring.datasource.url",
        matchIfMissing = false)           // só se definida uma URL
    DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

Excluir uma auto-configuração:

```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

Ou por propriedade (`application.properties`):

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

## Como funciona

As auto-configurações vivem em `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3 substituiu o antigo `spring.factories`). Elas são carregadas **depois** das suas configurações, então suas beans têm prioridade.

O fluxo de decisão usa **condições** (`Condition` interfaces):

| Condição | Significado |
|---|---|
| `@ConditionalOnClass` | classe presente no classpath |
| `@ConditionalOnMissingClass` | classe ausente |
| `@ConditionalOnBean` | bean já existe |
| `@ConditionalOnMissingBean` | bean não existe → Boot cria |
| `@ConditionalOnProperty` | propriedade definida (ou não definida) |
| `@ConditionalOnWebApplication` | aplicação web |

A ordem importa: `@AutoConfigureAfter`/`@AutoConfigureBefore` controlam a sequência. O resultado é que Boot registra ~100 beans potenciais, mas apenas os que passam nas condições efetivamente entram no contexto.

## Exemplos

Verificar o que está ativo:

```bash
./mvnw spring-boot:run -Ddebug=true
```

Ou em `application.properties`:

```properties
debug=true
# ou mais seletivo:
logging.level.org.springframework.boot.autoconfigure=DEBUG
```

No console aparece o **Condition Evaluation Report**:

```
Positive matches:
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.sql.DataSource'
      - @ConditionalOnMissingBean (types: javax.sql.DataSource) found no beans

Negative matches:
-----------------
   JpaRepositoriesAutoConfiguration:
      - @ConditionalOnProperty (spring.jpa.repositories.enabled) found different value
```

## Armadilhas

> [!WARNING] Quando sua auto-configuração "não pega", a primeira suspeita é sempre um outro bean seu que já existe — `@ConditionalOnMissingBean` silenciosamente cede lugar.

- Auto-configuração duplicada com seu próprio `@Bean`: o do Boot espera `@ConditionalOnMissingBean`, então seu bean vence — mas é fácil criar conflito de versões de configuração.
- `spring.autoconfigure.exclude` com classe errada (typo) ignora silenciosamente — use o report para confirmar.
- `@ConditionalOnProperty` com `matchIfMissing = true` ativa por padrão; o oposto (`false`) desativa por omissão — confundir os dois causa configurações que nunca ligam.

## Profundidade

Auto-configuração é o padrão **Convention over Configuration** levado ao extremo: o framework escolhe defaults inteligentes e só escala para você quando necessário. A chave é a **ordenada avaliação de condições**: primeiro o usuário define, depois o Boot preenche o que falta. Isso implementa o **Princípio da Substituição de Liskov** aplicado a configuração — a sua customização substitui o padrão sem quebrar o comportamento esperado.

O Condition Evaluation Report é a melhor ferramenta de debugging: ele mostra exatamente por que cada auto-configuração foi aplicada ou não. Aprenda a lê-lo — resolve 90 % dos mistérios de "Spring Boot fez algo estranho".
