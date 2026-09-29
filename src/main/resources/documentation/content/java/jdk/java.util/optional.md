---
id: java/jdk/java.util/optional
title: Optional
type: api
summary: Optional como retorno que força o chamador a lidar com a ausência de valor, com map, orElse e as armadilhas do get sem verificação.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Optional
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html
related:
  - java/jdk/java.util/objects
  - java/jdk/java.util/stream
  - java/jdk/java.lang/exceptions
---

> [!INFO] `Optional<T>` é um recipiente que tem **um valor ou está vazio** — e obriga quem recebe a pensar na ausência. O anti-padrão clássico é `get()` sem verificação; prefira `orElse`, `orElseGet`, `orElseThrow` ou encadear `map`/`filter`.

## Visão geral

Em APIs antigas, "não achei" era `null` e o chamador esquecia de checar → `NullPointerException` em produção. `Optional` transforma essa possibilidade em algo **visível na assinatura**: `Optional<Usuario> buscar(String id)` avisa "pode não existir".

```java
Optional<Usuario> u = repositorio.buscar("123");

String nome = u.map(Usuario::nome).orElse("desconhecido");   // sem Optional no meio do caminho
u.ifPresent(x -> System.out.println(x.nome()));             // só executa se houver valor
```

Regra de ouro: `Optional` é para **retorno de método**. Não use como campo, parâmetro nem dentro de coleção (`List<Optional<T>>` é sempre um sinal de projeto confuso).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Optional.of(T)` (estático) | `Optional<T>` | envolve valor não-nulo (`null` → exceção) |
| `Optional.ofNullable(T)` (estático) | `Optional<T>` | envolve valor ou vazio se `null` |
| `Optional.empty()` (estático) | `Optional<T>` | recipiente vazio |
| `isPresent()` | `boolean` | se tem valor |
| `isEmpty()` (Java 11) | `boolean` | se está vazio |
| `get()` | `T` | valor; `NoSuchElementException` se vazio |
| `orElse(T outro)` | `T` | valor ou o padrão |
| `orElseGet(Supplier)` | `T` | valor ou padrão **calculado na hora** |
| `orElseThrow()` | `T` | valor ou lança exceção |
| `orElseThrow(Supplier)` | `T` | valor ou exceção escolhida |
| `ifPresent(Consumer)` | `void` | executa algo se houver valor |
| `map(Function)` | `Optional<U>` | transforma; vazio continua vazio |
| `flatMap(Function)` | `Optional<U>` | transforma sem aninhar `Optional` |
| `filter(Predicate)` | `Optional<T>` | mantém só se satisfaz a condição |
| `stream()` (Java 9) | `Stream<T>` | stream de 0 ou 1 elemento |

## Criar

```java
Optional<String> comValor = Optional.of("ana");     // Optional[ana]
Optional<String> nulo = Optional.of(null);          // NullPointerException na hora!
Optional<String> seguro = Optional.ofNullable(null);// Optional.empty
Optional<String> vazio = Optional.empty();          // Optional.empty

boolean temAlgo = comValor.isPresent();             // true
boolean vazioDeVerdade = vazio.isEmpty();           // true (Java 11+)
```

**`of` vs `ofNullable`:** quando você *tem certeza* que não é `null`, use `of` (ele pega o bug cedo); quando o `null` é possível, use `ofNullable`.

## Ler o valor

```java
Optional<String> apelido = Optional.ofNullable(null);

String a = apelido.orElse("visitante");      // visitante (padrão literal)
String b = apelido.orElseGet(() -> "user" + 1);   // user1 (calculado só se vazio)
String c = apelido.orElseThrow();            // lançaria NoSuchElementException (está vazio)
String d = apelido.orElseThrow(() -> new IllegalArgumentException("sem apelido"));
// o Supplier escolhe a exceção — mais útil em pré-condições de contrato

Optional<String> presente = Optional.of("ana");
presente.ifPresent(x -> System.out.println("olá " + x));   // olá ana
System.out.println(presente.orElse("nada"));               // ana
```

**`orElse` avalia o padrão sempre que a expressão é criada** (argumento é por valor); `orElseGet` só calcula quando é preciso — se o padrão é caro (banco, arquivo), use `orElseGet`.

## Encadear transformações

```java
Optional<String> email = Optional.of("ana@exemplo.com");

Optional<String> dominio = email.map(e -> e.substring(e.indexOf('@') + 1));
System.out.println(dominio);                 // Optional[exemplo.com]

Optional<String> vazio = Optional.empty();
System.out.println(vazio.map(String::toUpperCase));   // Optional.empty — sem erro

Optional<String> filtrado = Optional.of("ana")
        .filter(s -> s.length() > 5)
        .map(String::toUpperCase);
System.out.println(filtrado);                // Optional.empty (falhou no filtro)

Integer r = Optional.ofNullable(2)
        .flatMap(n -> Optional.of(n * 10));  // map devolveria Optional<Optional<...>>
System.out.println(r);                       // 20
```

Armadilha: `map` de um `Optional` vazio **não executa** a função — é assim que a cadeia inteira fica à prova de `null`.

## Armadilhas comuns

> [!WARNING] `opt.get()` sem checar `isPresent()` é exatamente um `null` escondido: estoura `NoSuchElementException` no vazio. Prefira `orElse`, `orElseGet`, `orElseThrow` ou `map(...)` — deixe `get()` de fora do código de produção.

**`Optional` dentro de coleção ou como campo:**

```java
List<Optional<String>> lista;    // ruim: agora você tem Optional e null ao mesmo tempo
Map<String, Optional<Integer>> m;// ruim: dois níveis de "pode não ter"
Optional<List<String>> talvez;   // aceitável só se a LISTA é que pode faltar
// o caso idiomático é Optional<T> como retorno de método
```

**`orElse` com efeito colateral caro:**

```java
Optional<Config> cfg = Optional.ofNullable(carregarDoBanco());
Config c1 = cfg.orElse(buscarPadraoDoBanco());   // banco chamado MESMO quando cfg tem valor
Config c2 = cfg.orElseGet(() -> buscarPadraoDoBanco());   // chamado só se vazio
```

**Confundir `null` com vazio:** `Optional.ofNullable(null)` e `Optional.empty()` são o mesmo conteúdo; mas `Optional<Optional<String>>` aninhado quase nunca é a resposta — use `flatMap`.

## Profundidade

**Contrato dos métodos:** todos os operadores de `Optional` são *null-tolerant* no sentido de que vazio propaga vazio — `map`, `filter`, `flatMap`, `ifPresent` em `Optional.empty()` não lançam exceção, apenas devolvem vazio/`void`.

**`orElse` vs `orElseGet` vs `orElseThrow`:** `orElse` é para constante barata; `orElseGet` é para valor derivado/computado; `orElseThrow` é para quando a ausência é **erro de programação** (contrato violado) — aí a exceção documenta a pré-condição.

**Serialização:** `Optional` é `Serializable`, mas não é recomendado persistir/transportá-los — a API do projeto deve devolver `Optional` e converter na fronteira (JSON, DTO).

**Por que existe (história):** antes do Java 8, ausência era comunicada com `null` (`Map.get`), `-1` (`indexOf`) ou exceção — o chamador precisava lembrar de checar em todo ponto. `Optional` trouxe a ausência para o **tipo de retorno**. `Optional.get()` continua existindo por compatibilidade, não porque seja a forma correta.

**`Optional` e streams:** `stream()` transforma 0/1 elemento em stream, o que permite combinar com o resto do pipeline: `optional.stream().map(...).toList()` vira lista de no máximo um item (útil em filtros condicionais).
