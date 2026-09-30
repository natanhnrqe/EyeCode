---
id: java/jdk/fundamentos/lambdas
title: Lambdas
type: concept
summary: Funções como valores — anatomia da expressão lambda, a regra SAM das interfaces funcionais, composição com andThen e a captura de variáveis effectively final.
level: beginner
duration: 8
officialDocs:
  label: JLS §15.27 — Lambda Expressions
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27
related:
  - java/jdk/fundamentos/interfaces
  - java/jdk/java.util/function
  - java/jdk/java.util/comparator
  - java/jdk/java.util/stream
---

> [!INFO] Uma **lambda** é uma função sem nome tratada como valor: `(a, b) -> a + b`. O alvo precisa ser uma **interface funcional** (um único método abstrato — a regra SAM), e é ela que conecta o Java orientado a objetos ao estilo funcional de coleções e streams.

## Por que existe

Passar comportamento sempre exigiu uma classe: para ordenar por tamanho você escrevia uma classe anônima com quatro linhas de cerimônia para uma linha de lógica. O ruído engolia a intenção.

A lambda (Java 8) remove a cerimônia: você escreve só a lógica e o compilador cria o objeto por baixo. Ela mudou a cara das coleções — `sort`, `removeIf`, `forEach`, `stream` — e virou a forma idiomática de parameterizar comportamento no Java moderno.

## Anatomia da sintaxe

```java
Runnable tarefa = () -> System.out.println("rodando");      // zero parâmetros: ()
Comparator<String> tam = (a, b) -> a.length() - b.length(); // dois parâmetros
Function<Integer, Integer> dobro = x -> x * 2;              // um parâmetro: sem parênteses
```

| Parte | O que é | Exemplo |
|-------|---------|---------|
| parâmetros | como num método; tipos opcionais | `(a, b)`, `(String s)`, `x` |
| `->` | separa a entrada do corpo | — |
| corpo expressão | o resultado é devolvido sozinho | `x -> x * 2` |
| corpo bloco | `{ }` com `return` obrigatório | `n -> { return n * 2; }` |

Corpo em bloco — quando a lógica tem mais de um passo:

```java
Function<Integer, Integer> fatorial = n -> {
    int resultado = 1;
    for (int i = 2; i <= n; i++) resultado *= i;
    return resultado;             // bloco exige return explícito
};
```

## Como funciona

A lambda não tem tipo próprio: ela **assume o tipo do alvo** (*target typing*). `x -> x * 2` sozinha não significa nada; atribuída a `Function<Integer, Integer>`, vira "recebe Integer, devolve Integer". O alvo tem que ser uma **interface funcional** — exatamente um método abstrato; `default` e `static` não contam na regra SAM.

No uso, a lambda **é** uma instância da interface: dá para chamar o método, passar como parâmetro, guardar em variável. Duas formas curtas nascem daí:

- **Method reference** — lambda que só repassa argumentos vira referência: `String::toUpperCase`, `System.out::println`;
- **Composição** — interfaces funcionais trazem `default` de encadeamento: `andThen`, `compose`, `or`, `negate`.

E a regra de ouro: a lambda roda **quando for chamada**, não quando você a escreve — quem recebe o comportamento decide se (e quantas vezes) ele executa.

## Exemplos

A troca clássica — da classe anônima para a lambda:

```java
Runnable antigo = new Runnable() {                        // antes: cerimônia
    public void run() { System.out.println("rodando"); }
};

Runnable novo = () -> System.out.println("rodando");     // agora: só a lógica
```

Comportamento em coleções — ordenar, filtrar e percorrer sem laço manual:

```java
List<String> nomes = new ArrayList<>(List.of("ana", "bernardo", "caio"));

nomes.sort((a, b) -> a.length() - b.length());     // menor nome primeiro
nomes.removeIf(n -> n.length() > 4);              // sobram "ana" e "caio"
nomes.forEach(n -> System.out.print(n + " "));    // Saída: ana caio
```

Composição — montar um pipeline sem criar classe nova:

```java
Function<String, String> trim = String::strip;           // method reference
Function<String, String> maiuscula = String::toUpperCase;

Function<String, String> pipeline = trim.andThen(maiuscula);
pipeline.apply("  ana  ");     // "ANA" — strip primeiro, uppercase depois
```

## Armadilhas

> [!WARNING] Lambda só captura variável local **effectively final** — atribuída uma vez e nunca mais reatribuída. Se você mexer na variável depois, o compilador recusa a captura, mesmo que a linha da lambda venha antes.

**Captura de variável que muda:**

```java
int contador = 0;
Runnable r = () -> System.out.println(contador);   // parece ok agora...
contador++;                                         // agora a linha acima para de compilar
```

**`this` dentro da lambda é o de fora:** a lambda não cria escopo próprio — `this` vale a instância da classe onde ela foi escrita (na classe anônima, `this` era o objeto anônimo). Diferença sutil, mas decisiva quando o corpo chama `this.metodo()`.

**Execução adiada surpreendendo:** `Runnable r = () -> System.out.println("x");` não imprime nada por si — só quando alguém invocar `r.run()`. Confundir "criei" com "executei" é o bug clássico de quem está começando.

**Corpo grande escondido:** lambda boa cabe em uma ou duas linhas e se lê da esquerda para a direita. Se precisa de laço, variável temporária e três `return`, extraia um método com nome — fica testável e a stack trace passa a dizer algo.

## Profundidade

**JLS §15.27:** a lambda não é açúcar de classe anônima — o compilador não gera uma classe por expressão; a primeira execução passa por `invokedynamic` com a `LambdaMetafactory`, que materializa o objeto na hora. Daí saem as diferenças práticas: `this`/`super` com a semântica do escopo externo e nenhum estado escondido.

**Interface funcional (SAM):** `@FunctionalInterface` pede verificação em compilação — exatamente um método abstrato. O pacote `java.util.function` traz o kit pronto: `Function<T,R>`, `Predicate<T>`, `Supplier<T>`, `Consumer<T>`, `BiFunction<T,U,R>` — as assinaturas que a API de streams usa por toda parte.

**Composição vem de `default` methods:** `andThen`, `compose`, `or` e `negate` são `default` das interfaces funcionais — o mesmo mecanismo da página de Interfaces, usado para encadear comportamento em vez de evoluir API.

**Method references (JLS §15.13):** quatro formas — método estático (`Integer::parseInt`), método de instância específica (`System.out::println`), método de instância arbitrária (`String::toUpperCase`) e construtor (`ArrayList::new`).
