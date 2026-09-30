---
id: java/spring/web/rest-controller
title: REST Controller
type: guide
summary: Como criar endpoints REST com @RestController, mapear métodos HTTP com @GetMapping/@PostMapping e extrair dados com @RequestBody, @PathVariable e @RequestParam.
level: beginner
duration: 10
officialDocs:
  label: Spring MVC — Annotated Controllers
  url: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
related:
  - java/spring/boot-basics
  - java/spring/web/validation
  - java/spring/core/dependency-injection
  - java/spring/web/exception-handling
---

> [!INFO] `@RestController` transforma uma classe em pontos de entrada HTTP: cada método anotado (`@GetMapping`, `@PostMapping`...) vira um endpoint, o retorno vira **JSON automaticamente** via Jackson. Seu trabalho é só mapear a URL e extrair os dados — corpo com `@RequestBody`, trecho da URL com `@PathVariable`, query string com `@RequestParam`.

## Cenário

Você vai construir o CRUD de uma API de tarefas: listar (`GET /tarefas`), buscar por id (`GET /tarefas/42`), filtrar (`GET /tarefas?concluida=true`) e criar (`POST /tarefas` com JSON no corpo). Tudo isso cabe num controller enxuto, sem serialização manual.

## Passo a passo

### Passo 1 — Defina o modelo

```java
public record Tarefa(Long id, String titulo, boolean concluida) {}
```

### Passo 2 — Crie o controller com os mapeamentos

```java
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final Map<Long, Tarefa> tarefas = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    @GetMapping
    public List<Tarefa> listar(@RequestParam(required = false) Boolean concluida) {
        return tarefas.values().stream()
                .filter(t -> concluida == null || t.concluida() == concluida)
                .toList();
    }

    @GetMapping("/{id}")
    public Tarefa porId(@PathVariable long id) {
        Tarefa tarefa = tarefas.get(id);
        if (tarefa == null) {
            throw new TarefaNaoEncontradaException(id);
        }
        return tarefa;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tarefa criar(@RequestBody Tarefa nova) {
        Tarefa salva = new Tarefa(sequencia.incrementAndGet(), nova.titulo(), false);
        tarefas.put(salva.id(), salva);
        return salva;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable long id) {
        tarefas.remove(id);
    }
}
```

### Passo 3 — Teste com curl

```bash
curl -X POST http://localhost:8080/tarefas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Estudar Spring","concluida":false}'

curl "http://localhost:8080/tarefas?concluida=false"
curl http://localhost:8080/tarefas/1
```

## Como funciona

1. O `@RequestMapping("/tarefas")` na classe define o **prefixo**; cada anotação de método (`@GetMapping("/{id}")`) compõe a rota final;
2. O `DispatcherServlet` (o "porteiro" do Spring MVC) recebe o request do Tomcat e despacha para o método cuja assinatura casa método HTTP + caminho;
3. Os **argumentos são resolvidos por anotação**: `@PathVariable` vem do `{id}` da URL, `@RequestParam` vem da query string (`?concluida=true`), `@RequestBody` é o corpo JSON **desserializado pelo Jackson** (direto para o record `Tarefa`);
4. O valor de retorno passa pelo caminho inverso: Jackson serializa para JSON, com `Content-Type: application/json`.

`@RestController` = `@Controller` + `@ResponseBody` — ou seja, o retorno é o **corpo** da resposta, não o nome de uma view.

## Variações

**ResponseEntity** — quando você precisa controlar status e headers:

```java
import org.springframework.http.ResponseEntity;

@GetMapping("/{id}")
public ResponseEntity<Tarefa> porId(@PathVariable long id) {
    Tarefa tarefa = tarefas.get(id);
    return tarefa != null
            ? ResponseEntity.ok(tarefa)
            : ResponseEntity.notFound().build();
}
```

**Outros verbos**: `@PutMapping`, `@PatchMapping`, `@DeleteMapping` — mesma mecânica.

**Nome do parâmetro divergente**: `@RequestParam("concluida") boolean done` quando o nome Java não bate com o da query string.

## Armadilhas

> [!WARNING] Um método com `@GetMapping` numa classe sem `@RestController`/`@Controller` nunca é registrado: o endpoint dá **404** sem nenhum erro no startup. Verifique também se a classe está num pacote escaneado pelo `@SpringBootApplication`.

- **Esquecer `@RequestBody`**: o parâmetro chega `null` e o POST "funciona" silenciosamente com dados vazios — o erro aparece longe da causa;
- **`@PathVariable` com nome divergente** (`{id}` vs `long codigo`): sem compilação com `-parameters`, o Spring não casa — escreva `@PathVariable("id") long codigo`;
- **Retornar entidade de banco direto**: expõe campos internos e relacionamentos que viram JSON gigante (ou `LazyInitializationException`) — use DTOs/records na fronteira;
- **Controller gordo**: regra de negócio no controller duplica e dificulta teste — delegue ao `@Service`.

## Profundidade

O Spring MVC implementa o padrão **Front Controller**: um único `DispatcherServlet` recebe tudo, consulta o `HandlerMapping` (as rotas dos seus métodos) e usa `HandlerAdapter` + `HttpMessageConverter` (Jackson para JSON) para invocar seu método e converter o retorno. É uma camada fina sobre o HTTP — você programa em termos de objetos Java e o framework resolve o protocolo.
