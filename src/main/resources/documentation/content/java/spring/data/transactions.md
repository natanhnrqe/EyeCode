---
id: java/spring/data/transactions
title: Transações
type: guide
summary: Como usar @Transactional, controlar propagação, isolamento e readOnly — e entender por que self-invocation quebra tudo.
level: intermediate
duration: 9
officialDocs:
  label: Spring Framework — Transaction Management
  url: https://docs.spring.io/spring-framework/reference/data-access/transaction.html
related:
  - java/spring/core/dependency-injection
  - java/spring/data/jpa-repository
  - java/spring/core/aop
---

> [!INFO] `@Transactional` declara que um método (ou classe) roda dentro de uma transação de banco de dados: se qualquer exception sair, o Spring faz **rollback**; se terminar normal, **commit**. Mas é proxy-based — chamar o método de dentro da própria classe **não ativa** a transação.

## Cenário

`PedidoService.criar(...)` precisa salvar o pedido e cada item **atomicamente**: se o estoque abaixo de um item falhar, nenhum pedido parcial pode persistir. Sem transação, você teria `pedido` salvo e itens órfãos — dados corrompidos.

## Passo a passo

### Passo 1 — Ativar no serviço

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ItemRepository itemRepo;
    private final EstoqueClient estoque;

    // injeção por construtor...

    @Transactional
    public Pedido criar(CriarPedidoRequest req) {
        Pedido p = pedidoRepo.save(req.toEntity());
        req.itens().forEach(i -> itemRepo.save(i.comPedido(p)));
        estoque.baixar(req.itens()); // se falhar → rollback de tudo
        return p;
    }
}
```

### Passo 2 — Consultas read-only

```java
@Transactional(readOnly = true)
public Optional<Pedido> buscarPorId(Long id) {
    return pedidoRepo.findById(id);
}
```

`readOnly = true` deixa Hibernate/JPA saberem que podem otimizar (não flush, não dirty checking) — melhora performance e previne escrita acidental.

### Passo 3 — Rollback customizado

Por padrão, só **unchecked** exceptions (`RuntimeException` e subclasses) causam rollback. Para checked:

```java
@Transactional(rollbackFor = Exception.class)
public void importarArquivo() throws IOException {
    // ...
    throw new IOException("arquivo corrompido"); // rollback explícito
}
```

### Passo 4 — Propagação

Quando um método `@Transactional` chama outro `@Transactional`, o **padrão é `REQUIRED`**: junta-se à transação existente ou cria nova se não houver.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void registrarLogOperacao(LogOperacao log) {
    // roda numa TRANSAÇÃO INDEPENDENTE — commit mesmo se o externo falhar
}
```

| Propagação | Comportamento |
|---|---|
| `REQUIRED` (padrão) | junta se existe, cria se não existe |
| `REQUIRES_NEW` | suspende a atual e cria nova separada |
| `SUPPORTS` | roda dentro se existe, sem transação se não |
| `NEVER` | falha se houver transação ativa |
| `NOT_SUPPORTED` | suspende a atual e roda fora de transação |

## Como funciona

O Spring cria um **proxy** (via Spring AOP) ao redor do bean. Quando a chamada entra por fora (outro bean), o proxy abre a transação, delega, e decide commit/rollback. Quando a chamada vem de **dentro** da mesma classe (`this.metodo()`), o proxy **não intercepta** — é uma chamada Java normal, sem transação.

## Variações

- **`@Transactional` na classe:** aplica a todos os métodos públicos (não a `private`/`protected` nem `final`).
- **Programática:** `TransactionTemplate` para controle fino quando a declarativa não basta.
- **`@TransactionalEventListener`:** roda após commit/rollback — ideal para enviar e-mails/notificações só se o banco confirmou.

## Armadilhas

> [!WARNING] Self-invocation não ativa transação. Se `PedidoService` tem `@Transactional void a()` e `void b()` chama `a()` internamente, a transação de `a()` é ignorada. Solução: injete a si mesmo (`@Lazy`), extraia para outro bean, ou use `TransactionTemplate`.

- `@Transactional` em métodos `private`/`final` não funciona — o proxy dinâmico não enxerga.
- Exceção checked sem `rollbackFor` = commit com dados parciais.
- Transação longa em método que faz chamada HTTP externa: segura lock no banco enquanto espera resposta.

## Profundidade

**ACID** = Atomicidade, Consistência, Isolamento, Durabilidade. O Spring gerencia a transação via `PlatformTransactionManager` (DataSourceTransactionManager para JDBC, JpaTransactionManager para JPA). O proxy cria um **bound connection** no ThreadLocal — toda operação JPA/JDBC do mesmo thread usa a mesma conexão e o mesmo `commit`/`rollback`. Isso explica por que self-invocation quebra: o proxy não é o objeto `this`, é outra referência.

Isolamento padrão (`DEFAULT`) usa o do banco (geralmente `READ_COMMITTED`). Para relatórios concorrentes pesados, considere `REPEATABLE_READ` ou `SERIALIZABLE` com cuidado — locks mais fortes = menos concorrência.
