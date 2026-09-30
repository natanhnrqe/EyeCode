---
id: java/libs/mockito/mocking
title: Mockito
type: guide
summary: Teste unitário com Mockito — @Mock com MockitoExtension, given/when/verify para controlar e verificar chamadas, ArgumentCaptor para inspecionar argumentos e quando NÃO mockar.
level: intermediate
duration: 9
officialDocs:
  label: Mockito
  url: https://site.mockito.org
related:
  - java/junit/first-test
  - java/libs/assertj/assertions
  - java/spring/testing/spring-boot-test
---

> [!INFO] Mockito substitui as dependências do seu teste por **mocks**: você controla o que cada dependência devolve (stubbing), executa a classe sob teste e **verifica** quais chamadas aconteceram (`verify`). A receita: `@Mock` + `MockitoExtension`, `given(...).willReturn(...)` e `verify(...)` no fim.

## Cenário

Você vai testar `PedidoService`, que depende de `PedidoRepository` (banco) e `PagamentoGateway` (HTTP). Teste unitário não toca banco nem rede: o Mockito substitui as duas dependências — você ensina o mock a responder e verifica que o serviço chamou o gateway e marcou o pedido como pago.

## Dependência

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.14.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.14.2</version>
    <scope>test</scope>
</dependency>
```

```groovy
testImplementation 'org.mockito:mockito-core:5.14.2'
testImplementation 'org.mockito:mockito-junit-jupiter:5.14.2'
```

O `mockito-junit-jupiter` habilita `@ExtendWith(MockitoExtension.class)` no JUnit 5 — é ele que inicializa os mocks e valida stubbing desnecessário.

## Passo a passo

### Passo 1 — Declare os mocks

```java
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    PedidoRepository repositorio;

    @Mock
    PagamentoGateway gateway;

    @InjectMocks
    PedidoService service;

    @Test
    void marcaPedidoComoPago() {
        // arrange, act e assert nos passos seguintes
    }
}
```

`@Mock` cria o substituto; `@InjectMocks` injeta os mocks na classe sob teste (por construtor, setter ou campo). A extensão inicializa tudo antes de cada teste.

### Passo 2 — Configure o retorno (stubbing)

```java
@Test
void marcaPedidoComoPago() {
    given(repositorio.buscar(42L)).willReturn(new Pedido(42L, 100.0));
    given(gateway.cobrar(any(Pagamento.class))).willReturn(true);

    service.processar(42L);
}
```

O `given(...).willReturn(...)` (estilo BDD) ensina o mock: quando `buscar(42L)` for chamado, devolve o pedido. Matchers úteis: `any()` (qualquer argumento), `any(Class)` (do tipo), `eq(valor)` (valor exato).

Armadilha: **nunca misture** matcher e literal na mesma chamada — `cobrar(any(), 100.0)` lança `InvalidUseOfMatchersException`; o literal vira matcher com `eq(100.0)`.

### Passo 3 — Verifique as chamadas

```java
service.processar(42L);

verify(gateway).cobrar(any(Pagamento.class));   // foi chamado (uma vez)
verify(repositorio).marcarPago(42L);
verify(gateway, never()).estornar(anyLong());   // NÃO foi chamado
verify(gateway, times(1)).cobrar(any());        // forma explícita
```

O `verify(mock).metodo(...)` falha se a chamada não aconteceu; `never()`, `times(n)` e `atLeastOnce()` afinham a contagem. Sem `verify`, o teste só garante que **não lançou exceção**.

### Passo 4 — Capture os argumentos

```java
@Captor
ArgumentCaptor<Pagamento> captor;

@Test
void cobraOValorDoPedido() {
    given(repositorio.buscar(42L)).willReturn(new Pedido(42L, 100.0));
    given(gateway.cobrar(any())).willReturn(true);

    service.processar(42L);

    verify(gateway).cobrar(captor.capture());
    assertThat(captor.getValue().valor()).isEqualTo(100.0);
}
```

O `ArgumentCaptor` captura o objeto que o serviço passou ao mock — você testa o **que** foi enviado, não só que o método foi chamado.

## Como funciona

Um mock é um **proxy dinâmico** (gerado pelo Byte Buddy) que intercepta cada chamada: durante o stubbing, o Mockito registra "quando os argumentos casarem, devolva isto"; na execução, o proxy consulta o registro e devolve o valor ensinado (ou o padrão — `null`, `0`, `false`); no `verify`, o Mockito confere as interações gravadas. Por isso a regra de ouro: em uma chamada do mock, todos os argumentos são **matchers ou literais via `eq()`** — nunca os dois misturados.

## Variações

**`when` em vez de `given`** — `when(repositorio.buscar(42L)).thenReturn(...)` é a forma clássica, idêntica ao BDD `given`/`willReturn`.

**`doReturn` para spies** — com `@Spy` (mock parcial que executa o método real), o stubbing no arrange pode executar o método de verdade; `doReturn(valor).when(spy).metodo()` evita isso.

**Stubbing de exceção** — `given(gateway.cobrar(any())).willThrow(new IOException("fora do ar"))` testa o caminho de falha do serviço.

**Spring Boot** — `@MockBean` substitui o bean real no contexto de integração (`@SpringBootTest`); em teste unitário, `@Mock` com a extensão.

## Armadilhas

> [!WARNING] **Mock demais** isola o teste da realidade: se você mocka a lógica interna (além das dependências de fronteira — banco, HTTP, relógio, arquivo), o teste verifica que a implementação se comporta como a implementação — e não testa nada. Mock é para a fronteira; a lógica do domínio roda de verdade.

- **Matcher misturado com literal**: `cobrar(any(), 100.0)` lança `InvalidUseOfMatchersException` — envolva o literal em `eq(100.0)`;
- **Stubbing desnecessário**: com `MockitoExtension`, stub que o teste nunca consome falha com `UnnecessaryStubbingException` — remova o stub ou use `lenient()`;
- **Retorno padrão silencioso**: método não ensinado devolve `null`/`0` — o teste falha longe da causa (`NullPointerException` no assert, não no mock);
- **Verificar o que não deve acontecer**: `never()` costuma ser esquecido — sem ele, a chamada errada passa despercebida.

## Profundidade

O Mockito 5 usa o **inline mock maker** por padrão: mocks de classes `final` e métodos `final` funcionam sem configuração extra. O **strict stubbing** (padrão desde o Mockito 2) reporta stubbing não consumido e argumentos não casados — é por isso que a extensão falha cedo em vez de deixar o teste passar silencioso. Quando o teste precisa de banco/HTTP de verdade, ele deixa de ser unitário: use `@SpringBootTest` com `@MockBean` só para as dependências externas, e o resto do contexto real.
