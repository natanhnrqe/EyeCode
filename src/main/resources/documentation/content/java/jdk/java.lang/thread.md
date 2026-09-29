---
id: java/jdk/java.lang/thread
title: Thread
type: api
summary: Criar e coordenar linhas de execução em Java 21 — ciclo de vida, sleep e join, interrupção e threads virtuais, com as armadilhas de dados compartilhados.
level: beginner
duration: 9
officialDocs:
  label: API java.lang.Thread
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html
related:
  - java/jdk/java.lang/exceptions
  - java/jdk/fundamentos/classes
---

> [!INFO] `Thread` é o objeto que representa uma **linha de execução**: `start()` cria a thread de verdade e chama `run()`; chamar `run()` direto roda **na própria thread atual**. Em Java 21, para trabalho de E/S prefira **virtual threads** (`Thread.ofVirtual()`).

## Visão geral

Uma thread é um fluxo independente de instruções com a sua própria pilha de chamadas. Várias threads rodam compartilhando a mesma memória do processo — por isso dados em comum precisam ser sincronizados.

```java
Thread t = new Thread(() -> System.out.println("trabalho paralelo"));
t.start();      // cria a thread e executa run() nela
t.join();       // espera terminar antes de seguir
```

Java 21 convive com duas espécies: **platform threads** (uma para uma com threads do sistema operacional — poucas e caras) e **virtual threads** (gerenciadas pela JVM, milhares ou milhões — ideais para I/O).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Thread(Runnable)` / `Thread(Runnable, String)` | — | constrói (não inicia) |
| `start()` | `void` | inicia a thread — só pode ser chamado **1 vez** |
| `run()` | `void` | corpo do trabalho |
| `sleep(long ms)` | `static void` | pausa (não libera locks) |
| `join()` / `join(long ms)` | `void` | espera a thread terminar |
| `currentThread()` | `static Thread` | a thread em execução |
| `interrupt()` | `void` | envia pedido de interrupção |
| `interrupted()` / `isInterrupted()` | `boolean` | consulta o sinal (estático / da instância) |
| `getName()` / `setName(String)` | `String` / `void` | nome (depuração) |
| `isAlive()` | `boolean` | já começou e ainda não terminou |
| `setDaemon(boolean)` / `isDaemon()` | `void` / `boolean` | thread daemon (morre com a JVM) |
| `ofVirtual()` / `startVirtualThread(Runnable)` | `Builder` / `Thread` | criação de thread virtual (Java 21) |
| `getState()` | `Thread.State` | estado atual (RUNNABLE, WAITING...) |

## Criação e ciclo de vida

```java
Thread t = new Thread(() -> {
    for (int i = 1; i <= 3; i++) System.out.println("tick " + i);
}, "worker-1");                 // nome ajuda muito na depuração

t.start();                      // inicia — run() roda na thread nova
t.start();                      // ERRO: IllegalThreadStateException
t.join();                       // espera terminar
System.out.println(t.isAlive());   // false — já finalizou
```

**`start()` só pode ser chamado uma vez** — a segunda chamada estoura `IllegalThreadStateException`. Para reexecutar o mesmo trabalho, crie um objeto `Thread` novo.

## Esperas — sleep e join

```java
Thread t = new Thread(trabalho);
t.start();

t.join();                       // espera indefinida até terminar
t.join(2000);                   // espera no máximo 2 s
if (t.isAlive()) {
    System.out.println("ainda rodando — tempo esgotado");
}
```

**`sleep` não libera o monitor**: dentro de um bloco `synchronized` ela continua segurando o lock e trava as outras threads. Quem espera **liberando** o lock é `wait` (ver a página de `Object`).

## Interrupção

```java
Thread t = new Thread(() -> {
    try {
        Thread.sleep(10_000);                 // espera longa
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();    // reacende o sinal (boa prática)
        System.out.println("interrompida, saindo");
    }
});
t.start();
t.interrupt();                   // acorda o sleep imediatamente
```

**O sinal de interrupção é uma *flag***: `sleep`/`join`/`wait` limpam a flag ao lançar `InterruptedException`. Se você capturar sem reacender (`Thread.currentThread().interrupt()`), quem checar depois com `isInterrupted()` verá `false` e o cancelamento se perde.

## Threads virtuais (Java 21)

```java
// platform thread — 1:1 com o SO, use só quando precisar de paralelismo de CPU
Thread plataforma = new Thread(trabalho);

// virtual thread — barata, criada por tarefa
Thread virtual = Thread.ofVirtual().name("vt-1").start(trabalho);
Thread.startVirtualThread(trabalho);

// estrutura recomendada: um executor que cria uma virtual por tarefa
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(trabalho);
    executor.submit(outraTarefa);
}
```

**Threads virtuais não devem ser reutilizadas em pool** — criá-las é barato (é só um agendamento, não uma thread do SO). Pooling é a técnica das platform threads; com virtuais, uma por tarefa.

## Armadilhas comuns

> [!WARNING] Estado compartilhado sem sincronização: duas threads escrevendo na mesma variável produzem resultado imprevisível (corrida de dados). O clássico `contador++` **perde incrementos**, porque é ler-e-escrever em dois passos — use `AtomicInteger`, `synchronized` ou estruturas concorrentes.

```java
// ruim — 2 threads × 10.000 incrementos nunca dá 20.000
int soma = 0; void incrementar() { soma++; }

// correto — operação atômica
AtomicInteger atomico = new AtomicInteger();
atomico.incrementAndGet();       // sem trava manual
```

**`run()` no lugar de `start()`:**

```java
Thread t = new Thread(() -> System.out.println("oi"));
t.run();          // roda NA thread atual — nenhum paralelismo
t.start();        // correto — cria e executa na thread nova
```

**Daemon encerra junto com a JVM:**

```java
Thread daemon = new Thread(tarefa);
daemon.setDaemon(true);   // precisa vir ANTES do start()
daemon.start();
// quando só restarem threads daemon, a JVM pode encerrar —
// escrita em arquivo no meio é cortada
```

## Profundidade

**`start` vs `run` (semântica da JVM):** `run()` é um método comum — chamado direto, executa no chamador. `start()` pede ao runtime criar uma thread (do SO ou virtual) que chama `run()`; a segunda chamada de `start()` falha porque aquela thread já foi criada.

**Platform vs virtual (Project Loom):** platform threads são 1:1 com o SO e ocupam ~1 MB de pilha; virtuais são milhares de objetos gerenciados pela JVM, multiplexados num pequeno pool de threads de plataforma. Bloqueio de E/S em virtual não consome thread do SO — por isso Java 21 permite I/O bloqueante em escala massiva.

**Visibilidade e memória (JLS §17.4):** cada thread tem sua própria pilha; campos estáticos e de instância são **compartilhados**. Duas threads só têm garantia de ver as alterações uma da outra após um evento de sincronização — `synchronized`, `volatile`, início e fim de `start()`/`join()`. Sem isso, o compilador e a CPU podem reaproveitar valores registrados e cada thread enxerga um mundo levemente diferente.

**`synchronized` e monitores (JLS §17.1):** o bloco adquire o monitor do objeto, é reentrante (a mesma thread pode reentrar) e **libera o monitor mesmo com exceção** — daí não precisar de `finally` para desbloquear. `volatile` garante visibilidade imediata, mas **não** torna `i++` atômico (aí precisa de `Atomic` ou trava).

**Estados da thread:** `NEW` (criada, não iniciada) → `RUNNABLE` (pronta ou rodando) → `BLOCKED`/`WAITING`/`TIMED_WAITING` (esperando lock, `join`, `sleep`) → `TERMINATED`. `getState()` expõe isso — depuração de deadlock costuma revelar duas threads em `BLOCKED` esperando o monitor uma da outra.

**`join` cria garantia de ordem:** quando `join()` retorna, todas as ações da thread finalizada são visíveis para quem chamou o `join` — é o encerramento simétrico do `start` (JLS §17.4.4).
