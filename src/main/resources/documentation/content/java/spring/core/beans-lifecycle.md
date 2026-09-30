---
id: java/spring/core/beans-lifecycle
title: Ciclo de Vida dos Beans
type: guide
summary: O que acontece entre o scan e o shutdown — instanciação, callbacks @PostConstruct/@PreDestroy e o papel do ApplicationContext.
level: intermediate
duration: 8
officialDocs:
  label: Spring Framework — Customizing the Nature of a Bean
  url: https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html
related:
  - java/spring/core/dependency-injection
  - java/spring/boot-basics
  - java/spring/boot/actuator
---

> [!INFO] Todo bean Spring nasce e morre dentro do **ApplicationContext**: o contêiner instancia, injeta dependências, chama o callback de inicialização (`@PostConstruct`), entrega o bean para uso e, no shutdown, chama o callback de destruição (`@PreDestroy`). Você só pluga os ganchos.

## Cenário

Seu serviço de cache precisa **aquecer** dados assim que a aplicação sobe e **fechar uma conexão** quando ela desce. Fazer isso no construtor é cedo demais (as dependências ainda não foram injetadas por completo em alguns casos) e "quando alguém lembrar" é tarde demais. O ciclo de vida do bean resolve com dois ganchos formais.

## Passo a passo

### Passo 1 — Crie o bean com os callbacks

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class CacheDePrecos {

    private final PrecoClient client;

    public CacheDePrecos(PrecoClient client) {
        this.client = client;
    }

    @PostConstruct
    void aquecer() {
        // roda DEPOIS do construtor e das injeções
        System.out.println("Cache aquecido com " + client.contar() + " preços");
    }

    @PreDestroy
    void desligar() {
        // roda no shutdown gracioso do contexto
        System.out.println("Cache liberado");
    }
}
```

Repare no `jakarta.annotation` — desde o Spring Boot 3 / Spring Framework 6, as anotações `javax.annotation.*` migraram para `jakarta.annotation.*`.

### Passo 2 — Inspecione o contexto (opcional)

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class InspetorDeBeans implements CommandLineRunner {

    private final ApplicationContext context;

    public InspetorDeBeans(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(String... args) {
        System.out.println("Beans registrados: " + context.getBeanDefinitionCount());
    }
}
```

`CommandLineRunner` roda quando o contexto está pronto — ideal para checagens de boot.

## Como funciona

A sequência completa para um bean singleton:

1. **Registro**: o scan gera um `BeanDefinition` (metadados, nenhum objeto ainda);
2. **Instanciação**: o contêiner chama o construtor (resolvendo as dependências escolhidas);
3. **Injeção**: campos/setters restantes são preenchidos;
4. **`@PostConstruct`**: callback de inicialização — o bean está completo;
5. **Uso**: o bean fica disponível no contexto para injeção e chamadas;
6. **`@PreDestroy`**: no fechamento do contexto (shutdown gracioso), o callback de destruição roda.

Beans `prototype` têm ciclo encurtado: o contêiner instancia, inicializa e **esquece** — `@PreDestroy` nunca é chamado neles (a limpeza é sua).

## Variações

**Interfaces da própria Spring** (`InitializingBean`/`DisposableBean`) — funcionam, mas acoplam seu código ao framework; prefira as anotações Jakarta:

```java
import org.springframework.beans.factory.InitializingBean;

@Component
public class Legado implements InitializingBean {
    @Override
    public void afterPropertiesSet() {
        // equivalente ao @PostConstruct
    }
}
```

**`initMethod`/`destroyMethod` no `@Bean`** — para classes de terceiros:

```java
@Bean(destroyMethod = "close")
public DataSource dataSource() {
    return new HikariDataSource();
}
```

**`@EventListener(ApplicationReadyEvent.class)`** — quando a inicialização depende da aplicação **inteira** de pé (servidor HTTP incluso), não só do contexto.

## Armadilhas

> [!WARNING] Não faça trabalho pesado ou demorado no construtor nem no `@PostConstruct`: eles rodam no startup e **atrasam a subida da aplicação inteira**. Para aquecimento caro, prefira `ApplicationReadyEvent` ou carregamento preguiçoso.

- **Import errado**: `javax.annotation.PostConstruct` não funciona no Spring Boot 3 — é `jakarta.annotation.PostConstruct`;
- **`@PreDestroy` nunca executa em `prototype`**: o contêiner não rastreia prototypes após entregá-los;
- **Shutdown abrupto mata o `@PreDestroy`**: se o processo morre com `SIGKILL`, nenhum callback roda — o shutdown gracioso precisa estar habilitado (`server.shutdown=graceful`);
- **Chamar `context.getBean()` em loop**: é service locator, um antipadrão — injete pelo construtor.

## Profundidade

O `ApplicationContext` é o contêiner de alto nível do Spring (estende o `BeanFactory` básico com eventos, internacionalização e integração AOP). Além do ciclo de vida, ele publica **eventos de aplicação** (`ContextRefreshedEvent`, `ApplicationReadyEvent`) aos quais você pode reagir com `@EventListener` — a base de muito do comportamento do Boot, incluindo a autoconfiguração.

A ordem de inicialização entre beans é definida pelo grafo de dependências; quando não há aresta entre dois beans, você pode forçá-la com `@DependsOn`. Raramente é necessário — mas existe.
