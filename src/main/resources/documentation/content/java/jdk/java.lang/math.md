---
id: java/jdk/java.lang/math
title: Math
type: api
summary: Constantes e funções matemáticas da JVM — arredondamento, potências, raízes, trigonometria e aleatoriedade, com as armadilhas de ponto flutuante e NaN.
level: beginner
duration: 7
officialDocs:
  label: API java.lang.Math
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html
related:
  - java/jdk/fundamentos/variables
  - java/jdk/java.lang/wrapper
---

> [!INFO] `Math` é uma caixa de ferramentas **estática e sem estado**: chame `Math.abs(-5)` sem criar objeto. Os métodos **não lançam exceção** por domínio inválido — `Math.sqrt(-1)` devolve `NaN`, e a conta segue.

## Visão geral

Todo cálculo — pontuação, conversão de unidade, sorteio, geometria — passa por `Math`. A classe só tem campos constantes (`Math.PI`, `Math.E`, `Math.TAU`) e métodos `static`, todos puros (mesma entrada, mesmo resultado, sem efeitos colaterais).

```java
double raio = 3.0;
double area = Math.PI * raio * raio;   // 28.274333882308138
int modulo = Math.abs(-42);            // 42
```

Aviso de precisão: `double` guarda fração binária, então `0.1` não é exatamente 0,1. Para dinheiro e contas que precisam ser exatas, use `BigDecimal` — `Math` opera sobre ponto flutuante.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `abs(int/long/float/double x)` | mesmo tipo | valor absoluto |
| `max(a, b)` / `min(a, b)` | mesmo tipo | maior / menor valor |
| `pow(base, exp)` | `double` | base elevada a exp |
| `sqrt(x)` / `cbrt(x)` | `double` | raiz quadrada / cúbica |
| `floor(x)` / `ceil(x)` | `double` | arredonda para baixo / cima |
| `round(x)` | `long` (`double`) / `int` (`float`) | arredonda (metade para cima) |
| `random()` | `double` | aleatório em `[0.0, 1.0)` |
| `sin(x)` / `cos(x)` / `tan(x)` | `double` | trigonometria em **radianos** |
| `toRadians(x)` / `toDegrees(x)` | `double` | graus ↔ radianos |
| `log(x)` / `log10(x)` / `exp(x)` | `double` | log natural / base 10 / e^x |
| `hypot(x, y)` | `double` | √(x² + y²) sem estouro |
| `signum(x)` | `double` | sinal: -1, 0 ou 1 |
| `addExact(a, b)` / `multiplyExact(a, b)` | `int`/`long` | soma/multiplicação que **acusa** estouro |
| `PI` / `E` / `TAU` | `double` | constantes matemáticas |

## Medidas e constantes

```java
Math.abs(-42);               // 42 (int)
Math.abs(-4.5);              // 4.5 (double)
Math.max(10, 3);             // 10
Math.min(10, 3);             // 3
Math.floor(3.7);             // 3.0 — sempre double
Math.ceil(3.2);              // 4.0
Math.round(3.5);             // 4  — devolve long
Math.round(3.4);             // 3
Math.addExact(Integer.MAX_VALUE, 1);   // ArithmeticException (não estoura em silêncio)
```

**`floor`/`ceil` devolvem `double` e `round` devolve `long`** — para guardar em `int`, faça o cast `(int) Math.floor(3.7)`; e `Math.round(-3.5)` dá `-3`, porque a metade é resolvida "para cima", não por magnitude.

## Potências, raízes e log

```java
Math.pow(2, 10);             // 1024.0
Math.sqrt(16);               // 4.0
Math.cbrt(27);               // 3.0
Math.log(Math.E);            // 1.0
Math.log10(1000);            // 3.0
Math.exp(1);                 // 2.718281828459045
Math.hypot(3, 4);            // 5.0
```

**`Math.pow(0.1, 2)` não é `0.01` exato** — ponto flutuante não representa toda fração binária. Compare com tolerância: `Math.abs(a - b) < 1e-9`, nunca com `==`.

## Trigonometria e conversão

```java
double rad = Math.toRadians(90);   // 1.5707963267948966
Math.sin(rad);                      // 1.0 (aproximação da JVM)
Math.cos(0);                        // 1.0
Math.atan2(1, 1);                   // 0.7853981633974483 rad = 45°
Math.toDegrees(Math.PI / 4);        // 45.0
```

**As funções trigonométricas trabalham em radianos** — `Math.sin(90)` lê 90 como radianos (≈ 0,894), não como 90 graus. Converta sempre com `toRadians`.

## Aleatoriedade

```java
Math.random();                               // ex.: 0.7314... em [0.0, 1.0)
int dado = (int) (Math.random() * 6) + 1;    // 1 a 6
int tiro = ThreadLocalRandom.current().nextInt(1, 7);  // forma moderna (1 a 6)
```

**`Math.random()` não serve para teste determinístico** — a sequência muda a cada execução. Para reproducibilidade, use `new Random(42)` (mesma seed, mesma sequência).

## Armadilhas comuns

> [!WARNING] `NaN` contamina tudo: `Math.sqrt(-1)` é `NaN`, e **toda comparação com `NaN` dá `false`** — inclusive `x == x`. Valide as entradas antes de calcular, senão o erro vira um `if` que nunca entra.

**Ponto flutuante não é exato:**

```java
0.1 + 0.2 == 0.3;                    // false
0.1 + 0.2;                           // 0.30000000000000004
Math.abs(0.1 + 0.2 - 0.3) < 1e-12;   // true — compare com tolerância
```

Soma e multiplicação de `double` chegam no menor erro representável; para valores exatos (dinheiro), `BigDecimal`.

**`Infinity` em vez de exceção:**

```java
Math.pow(10, 400);        // Infinity — sem erro, sem exceção
1.0 / 0.0;                // Infinity (divisão de double)
(int) Math.pow(2, 40);    // 2147483647 — cast estoura e "satura" no limite do int
```

Ponto flutuante prefere continuar com `Infinity`/`NaN` a parar o programa; `Double.isNaN(x)` e `Double.isInfinite(x)` servem para checar.

**Cast truncado não arredonda:**

```java
(int) 3.9;         // 3 — corta a parte decimal
(int) -3.9;        // -3 (não -4)
Math.floor(-3.9);  // -4.0 — este sim arredonda para baixo
```

## Profundidade

**Métodos `static` puros:** `Math` não pode ser instanciada (construtor privado) e não guarda estado — a chamada `Math.sqrt(x)` é resolvida em tempo de compilação e não tem efeito colaterais, o que a torna segura em qualquer contexto.

**`Math` vs `StrictMath`:** `Math` pode usar atalhos de hardware (intrínsecos da JVM) e o resultado pode diferir em bits entre plataformas. `StrictMath` segue a referência matemática exata (fdlibm) — quando o resultado precisa ser idêntico bit a bit (relatórios assinados, testes de regressão), é `StrictMath`.

**IEEE-754 (JLS §4.2.3):** `double` é 64 bits com ~15 a 16 dígitos significativos. `0.1` é periódico em binário, por isso some erro a cada operação; `float` tem só 7 dígitos e ainda mais erro. O erro é conhecido e previsível — compare com ε (épslon), nunca com igualdade exata.

**`NaN` e infinitos:** `NaN` se propaga por qualquer conta (`NaN * 0` é `NaN`); `Infinity` também (`Infinity + 1` é `Infinity`). Esses não são erros de execução, são valores de domínio — por isso `Math` não lança exceção e a checagem é responsabilidade de quem chama.

**Arredondamento:** `floor`/`ceil` andam na reta numérica; `round` usa "metade para cima" (`Math.round(-3.5)` → `-3`); `rint` usa arredondamento para o par mais próximo (banker's rounding); `IEEEremainder` calcula o resto real da divisão. Escolha o que a regra do negócio pede.

**Aleatoriedade:** `Math.random()` mantém um gerador estatístico interno único da JVM — rápido, mas não criptográfico e não reprodutível. Para sequência reproduzível use `new Random(seed)`; em paralelo use `ThreadLocalRandom.current()`; para criptografia, `SecureRandom`.
