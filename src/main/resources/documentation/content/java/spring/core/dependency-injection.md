---
id: java/spring/core/dependency-injection
title: Injeção de Dependência
type: guide
summary: Como o contêiner Spring monta seus objetos por você — injeção por construtor, estereótipos @Component/@Service/@Repository e escopos de bean.
level: beginner
duration: 9
officialDocs:
  label: Spring Framework — IoC Container
  url: https://docs.spring.io/spring-framework/reference/core/beans.html
related:
  - java/spring/boot-basics
  - java/spring/core/beans-lifecycle
  - java/spring/boot/autoconfiguration
---

> [!INFO] Injeção de Dependência (DI) é o coração do Spring: você **declara o que precisa** (no construtor) e o contêiner **instancia e conecta** os objetos. A receita moderna é: anote a classe com um estereótipo (`@Component`, `@Service`, `@Repository`) e receba as dependências pelo construtor — sem `@Autowired`.

## Cenário

Você está construindo uma API de pedidos. O `PedidoService` precisa de um `PedidoRepository`, e o controller precisa do service. Sem Spring, você escreveria `new PedidoService(new PedidoRepository())` em algum lugar — e amarraria tudo junto. Com DI, nenhuma classe conhece a construção das outras: você declara a necessidade, o Spring resolve.

## Passo a passo

### Passo 1 — Marque as classes com estereótipos

```java
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepository {
    public String buscar(long id) {
        return "pedido-" + id;
    }
}
```

```java
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    // Injeção por construtor — sem @Autowired necessário
    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    public String detalhe(long id) {
        return repository.buscar(id);
    }
}
```

### Passo 2 — Injete no consumidor

```java
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable long id) {
        return service.detalhe(id);
    }
}
```

### Passo 3 — Deixe o scan trabalhar

A classe com `@SpringBootApplication` escaneia o próprio pacote e subpacotes (`@ComponentScan`). Mantenha seus componentes **abaixo** desse pacote — classes fora da árvore são invisíveis.

## Como funciona

No startup, o Spring:

1. Escaneia o classpath procurando classes anotadas (`@Component` e meta-anotações como `@Service`, `@Repository`, `@RestController`);
2. Registra um `BeanDefinition` para cada uma no `ApplicationContext`;
3. Instancia os beans resolvendo o grafo de dependências — se `PedidoController` pede `PedidoService` que pede `PedidoRepository`, o contêiner cria o repository primeiro;
4. Entrega sempre a **mesma instância** por padrão: o escopo padrão é `singleton`.

Escopos disponíveis:

- `singleton` (padrão) — uma instância por contêiner;
- `prototype` — nova instância a cada injeção (`@Scope("prototype")`);
- `request` / `session` / `application` — ligados ao ciclo web.

## Variações

**Injeção por campo com `@Autowired`** — funciona, mas é desaconselhada: campos não podem ser `final`, escondem dependências e dificultam testes:

```java
@Autowired  // evite: prefira o construtor
private PedidoService service;
```

**Bean de método (`@Bean`)** — para classes de terceiros que você não pode anotar:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClienteConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
```

**Desempate com múltiplos candidatos** — use `@Primary` no preferido ou `@Qualifier("nome")` no ponto de injeção.

## Armadilhas

> [!WARNING] Nunca use `new` em um objeto que precisa de injeção: `new PedidoService()` cria uma instância **fora do contêiner** — sem dependências injetadas, sem proxy de transação, nada. Obtenha beans sempre do Spring.

- **Dependência circular** (`A` precisa de `B`, `B` precisa de `A`): o startup falha com `BeanCurrentlyInCreationException`. Quebre o ciclo extraindo uma terceira classe ou repense o design;
- **Dois beans do mesmo tipo sem desempate**: `NoUniqueBeanDefinitionException` — use `@Primary` ou `@Qualifier`;
- **Classe fora do pacote escaneado**: `NoSuchBeanDefinitionException` no startup — confira a estrutura de pacotes;
- **`prototype` injetado em `singleton`**: o singleton guarda UMA instância do prototype para sempre — o prototype não é recriado.

## Profundidade

O `ApplicationContext` é a implementação do padrão **Inversion of Control**: em vez de o seu código controlar a criação dos objetos, o contêiner controla — e a injeção por construtor expressa as dependências de forma explícita, imutável (`final`) e testável (basta `new PedidoService(new RepositórioFake())` num teste unitário, sem Spring algum).

Os estereótipos são semanticamente idênticos para o contêiner, mas carregam significado para humanos e recursos extras: `@Repository` ativa a tradução de exceções de banco (ex.: `SQLException` → `DataAccessException`), e `@Service` marca a camada de regras de negócio. Escolher o estereótipo certo documenta a arquitetura de graça.
