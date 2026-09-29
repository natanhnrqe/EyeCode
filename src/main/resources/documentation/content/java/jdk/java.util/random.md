---
id: java/jdk/java.util/random
title: Random
type: api
summary: Números pseudoaleatórios no Java, com nextInt, nextDouble, ThreadLocalRandom e a diferença entre aleatório de verdade e SecureRandom.
level: beginner
duration: 6
officialDocs:
  label: API java.util.Random
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Random.html
related:
  - java/jdk/java.util/collections
  - java/jdk/java.util/arrays
  - java/jdk/java.lang/wrapper
---

> [!INFO] `Random` gera números **pseudoaleatórios** reproduzíveis a partir de uma semente: `nextInt(6)` cai em `[0, 6)`, `nextDouble()` em `[0.0, 1.0)`. Para concorrência use `ThreadLocalRandom`; para segurança use **`SecureRandom`**.

## Visão geral

Sorteio, amostragem, jitter, teste com dados sintéticos — toda vez que o programa precisa de um número imprevisível, a resposta é `Random`. A sequência é determinística: mesma semente, mesma sequência (o que é ótimo para **reproduzir bugs em teste**).

```java
Random r = new Random();                       // semente aleatória
Random fixo = new Random(42);                  // sequência reproduzível
int sorteio = r.nextInt(100);                  // 0..99
ThreadLocalRandom.current().nextInt(10);       // forma recomendada em threads
```

Desde o Java 17, `Random` implementa `java.util.random.RandomGenerator` — a interface comum a todos os geradores do JDK.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `nextInt()` | `int` | inteiro completo (com sinal) |
| `nextInt(int bound)` | `int` | inteiro em `[0, bound)` |
| `nextInt(int origin, int bound)` (17) | `int` | inteiro em `[origin, bound)` |
| `nextLong()` / `nextLong(long bound)` | `long` | inteiro longo completo / limitado |
| `nextDouble()` | `double` | `[0.0, 1.0)` — nunca chega a 1.0 |
| `nextFloat()` | `float` | `[0.0f, 1.0f)` |
| `nextBoolean()` | `boolean` | `true`/`false` com 50% |
| `nextGaussian()` | `double` | distribuição normal (média 0, desvio 1) |
| `nextBytes(byte[])` | `void` | preenche o array com bytes aleatórios |
| `setSeed(long)` | `void` | reseta a sequência |
| `ints(long, int, int)` (8) | `IntStream` | stream de inteiros aleatórios |
| `ThreadLocalRandom.current()` (8) | `ThreadLocalRandom` | gerador por thread |

## Inteiros

```java
Random r = new Random(42);

System.out.println(r.nextInt());        // qualquer int (ex.: -1234567)
System.out.println(r.nextInt(100));      // 0 a 99 (0 inclusivo, 100 exclusivo)
System.out.println(r.nextInt(6) + 1);    // dado de 6 faces: 1 a 6
System.out.println(r.nextInt(10, 20));   // 10 a 19 (Java 17+, origem inclusiva)

Random s = new Random(42);
System.out.println(s.nextInt(100));      // mesmo valor da primeira chamada acima
// mesma semente => mesma sequência: perfeito para teste determinístico
```

Armadilha: o limite é **exclusivo** — `nextInt(10)` nunca devolve 10; para incluir, some 1 ou use `nextInt(origin, bound)`.

## Ponto flutuante e booleanos

```java
Random r = new Random();

double p = r.nextDouble();               // [0.0, 1.0) — para 1.0, use probabilidade
System.out.println(p >= 0 && p < 1.0);   // true (sempre)
double preco = 10 + r.nextDouble() * 90; // [10, 100)
boolean cara = r.nextBoolean();          // 50/50
double z = r.nextGaussian();             // normal: ~68% em [-1, 1]
// 50% de chance: if (r.nextBoolean()) { ... }
```

**`nextDouble()` nunca devolve `1.0`** — por isso `x < limite` é a comparação correta em probabilidades.

## Escolher e embaralhar

```java
List<String> frutas = new ArrayList<>(List.of("uva", "maçã", "banana"));
Random r = new Random();

String sorteada = frutas.get(r.nextInt(frutas.size()));     // escolha uniforme
Collections.shuffle(frutas, r);                             // embaralha com a semente (in place)
System.out.println(frutas);                                 // ordem nova (variável)

String[] opcoes = {"sim", "nao"};
String escolha = opcoes[r.nextInt(opcoes.length)];          // idem para array

IntStream amostra = r.ints(5, 1, 7);                  // 5 números em 1..6
System.out.println(amostra.toList());                       // ex.: [3, 1, 6, 2, 4]
```

Para sortear **sem repetição**, embaralhe e pegue os N primeiros; para com repetição, chame `nextInt` N vezes.

**Armadilha:** `shuffle` embaralha **no lugar** — se você ainda precisa da lista original, copie antes (`new ArrayList<>(origem)`).

## Armadilhas comuns

> [!WARNING] `Random` **não é criptográfico**: a sequência é previsível se alguém souber a semente. Nunca use para senhas, tokens de sessão, chaves ou sorteios com prêmio — para isso, use `java.security.SecureRandom`.

**Vários `Random` criados em sequência:**

```java
// ruim: um gerador novo dentro de um laço => sementes custam correlacionar
for (int i = 0; i < 5; i++) {
    int x = new Random().nextInt(1000);   // funciona, mas é desperdício
}
Random unico = new Random();               // bom: uma instância, várias chamadas
// em código multithread, prefira ThreadLocalRandom.current()
```

**`nextInt(0)` ou limite negativo:**

```java
new Random().nextInt(0);    // IllegalArgumentException: bound must be positive
new Random().nextInt(-5);   // idem
// valide o limite antes de sorteá-lo
```

**Confundir `nextInt(bound)` com faixa personalizada:**

```java
Random r = new Random();
int idade = r.nextInt(18, 81);            // 18 a 80 (Java 17+) — bounds correto
int antigo = r.nextInt(63) + 18;          // mesma faixa, jeito manual
```

## Profundidade

**Algoritmo e semente:** `Random` usa um gerador linear congruencial de 48 bits — rápido, barato e **reprodutível**: `new Random(42)` sempre gera a mesma sequência. Isso é recurso para testes (`setSeed` devolve o cenário ao início), não defeito.

**Reprodutibilidade vs aleatoriedade:** para depurar um sorteio que falhou, anote a semente e rode de novo com `new Random(semente)` — a sequência se repete e o bug aparece. Para jogos e simulação, isso é desejável; para criptografia, é inaceitável.

**`ThreadLocalRandom`:** cada thread tem seu próprio gerador, sem bloqueio nem contenção — é a escolha em código paralelo (`ThreadLocalRandom.current().nextInt(...)`). `Random` compartilhado é thread-safe (o seed é um `AtomicLong` atualizado com CAS), mas o atômico vira ponto de contenção sob muita concorrência.

**`RandomGenerator` (Java 17):** a nova interface comum dos geradores — `Random`, `ThreadLocalRandom` e `SplittableRandom` a implementam (`SecureRandom` entra por herança) e ela traz métodos prontos (`nextLong(bound)`, `nextDouble(origin, bound)`, `ints()`, `doubles()`, `nextExponential()`). Dá para programar contra a interface e trocar o gerador.

**Distribuição:** `nextInt(bound)` é uniforme; `nextGaussian()` segue a normal — quem precisa de pesos diferentes (ex.: sorteio com 70/30) compara `nextDouble()` com os limites cumulativos, não chama `nextInt` de forma desigual.
