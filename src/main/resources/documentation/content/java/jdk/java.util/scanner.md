---
id: java/jdk/java.util/scanner
title: Scanner
type: api
summary: Leitura e análise de texto e entrada do teclado no Java, com hasNext, next, nextInt e as armadilhas do nextLine após nextInt.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Scanner
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Scanner.html
related:
  - java/jdk/java.util/regex
  - java/jdk/java.lang/string
  - java/jdk/java.util/optional
---

> [!INFO] `Scanner` lê texto **token a token** usando um delimitador (por padrão, espaço em branco) e oferece métodos tipados como `nextInt()` e `nextLine()`. Sempre crie com **try-with-resources**, porque `Scanner` precisa de `close()`.

## Visão geral

É a classe usual para entrada do teclado (`System.in`) e para quebrar arquivos/strings em pedaços. Você pergunta "ainda tem?" com `hasNext...()` e só então consome com `next...()` — esse par evita exceções de fim de entrada.

```java
try (Scanner sc = new Scanner(System.in)) {
    System.out.print("Digite seu nome: ");
    String nome = sc.nextLine();           // lê a linha inteira
    System.out.println("Olá, " + nome);
}
// outras fontes: new Scanner(new File("dados.txt")) / new Scanner(Path.of("dados.txt"))
```

O scanner tem um buffer interno e trava (`next()` espera) quando a fonte é `System.in` — isso é esperado, não é travamento de programa.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `hasNext()` | `boolean` | se existe outro token |
| `hasNextInt()` / `hasNextDouble()` | `boolean` | se o próximo token é numérico |
| `hasNextLine()` | `boolean` | se existe outra linha |
| `next()` | `String` | próximo token (sem o delimitador) |
| `nextLine()` | `String` | linha inteira, sem o `\n` |
| `nextInt()` / `nextDouble()` | `int` / `double` | próximo token convertido |
| `nextLong()` / `nextBoolean()` | `long` / `boolean` | próximo token convertido |
| `findInLine(String regex)` | `String` | busca na linha atual |
| `useDelimiter(String regex)` / `useDelimiter(Pattern)` | `Scanner` | troca o delimitador |
| `skip(Pattern)` | `Scanner` | descarta o que casar com o padrão |
| `reset()` | `Scanner` | volta ao delimitador/locale originais |
| `close()` | `void` | fecha a fonte (obrigatório) |

## Ler tokens

```java
Scanner sc = new Scanner("ana 25\nbia 30\n");

while (sc.hasNext()) {
    String nome = sc.next();       // ana | bia (espaços são o delimitador)
    int idade = sc.nextInt();      // 25  | 30
    System.out.println(nome + " tem " + idade);
}
// Saída:
// ana tem 25
// bia tem 30
sc.close();
```

`hasNext()` devolve `false` no fim em vez de lançar exceção — é o teste que deve abrir todo laço de leitura.

**Armadilha:** `hasNext()` sozinho **não avança** a leitura — é `hasNext()` + `next()` sempre juntos, senão o laço repete o mesmo token.

## Ler números e linhas

```java
Scanner sc = new Scanner("10 20\n30");

if (sc.hasNextInt()) {
    System.out.println(sc.nextInt());   // 10
}
System.out.println(sc.nextInt());       // 20
System.out.println(sc.nextLine());      // ""  — pega só o resto da linha (vazio!)
System.out.println(sc.nextLine());      // 30
sc.close();
// Armadilha: o '\n' deixado por nextInt() é consumido pelo primeiro nextLine()
```

**Confira com `hasNextInt()` antes de `nextInt()`:** se o token for `"abc"`, o `nextInt()` lança `InputMismatchException`.

## Mudar o delimitador

```java
Scanner csv = new Scanner("ana,bia,caio");
csv.useDelimiter(",");
while (csv.hasNext()) System.out.print(csv.next() + " ");   // ana bia caio
csv.close();

Scanner dados = new Scanner("nome=ana;idade=25");
dados.useDelimiter("[;=]");          // regex: quebra por ; e por =
System.out.println(dados.next());    // nome
System.out.println(dados.next());    // ana
System.out.println(dados.next());    // idade
System.out.println(dados.next());    // 25
dados.close();

Scanner umLinha = new Scanner("a, b , c");
umLinha.useDelimiter("\\s*,\\s*");        // vírgula com espaços opcionais nos dois lados
System.out.println(umLinha.next());        // a
System.out.println(umLinha.next());        // b (sem espaço sobrando)
System.out.println(umLinha.hasNext("c"));  // true
umLinha.close();
// useDelimiter(",") puro deixaria " b" e " c" com o espaço junto do token
```

Armadilha: `useDelimiter` recebe **regex** — para separador literal, escape (`\\.`) ou use `Pattern.quote(".")`.

## Armadilhas comuns

> [!WARNING] Misturar `nextInt()`/`next()` com `nextLine()` deixa o `\n` no buffer: a primeira `nextLine()` devolve a sobra da linha (às vezes vazia). Leia a linha toda antes de ler tokens, ou chame `sc.nextLine()` uma vez para "limpar" o resto.

**Esquecer o `close()`:**

```java
Scanner sc = new Scanner(System.in);
// ... uso ...
sc.close();    // sem try-with-resources, esquece e a fonte fica aberta

try (Scanner s = new Scanner(System.in)) {   // fecha sozinho, sempre
    s.hasNext();
}
```

**Exceções quando o dado não é o esperado:**

```java
Scanner sc = new Scanner("abc");
sc.nextInt();        // InputMismatchException: "abc" não é int
Scanner vazio = new Scanner("");
vazio.nextLine();    // NoSuchElementException: não existe linha para ler
// sempre combine hasNextInt()/hasNextLine() com o next correspondente
```

**Scanner em arquivo grande:** ele é conveniente, mas ler linha a linha com `Files.lines(...)` ou `BufferedReader` costuma ser mais rápido e não exige fechar `Scanner` por token.

## Profundidade

**Delimitador é regex:** por padrão é `\\P{Whitespace}+` (tudo que não é espaço) — `next()` devolve o token sem o delimitador, `nextLine()` ignora o delimitador e vai até `\n`. Entender isso explica o comportamento "estranho" do `nextLine` depois de `nextInt`.

**Bloqueio e buffering:** `next()`/`nextInt()` **esperam** até houver token disponível — em `System.in`, é o programa esperando você digitar. Não é deadlock; é a semântica de leitura bloqueante da fonte.

**Estado do scanner:** o objeto guarda delimitador, locale e patrimônio de erro; `reset()` restaura o estado inicial definido no construtor, útil quando você troca o delimitador temporariamente.

**`hasNext` + `next` como protocolo:** o par equivale ao padrão "teste antes de consumir" e é o mesmo formato usado por `Iterator.hasNext()`/`next()` — seguir o protocolo elimina `NoSuchElementException` na leitura.

**`findInLine` e `skip`:** métodos com regex permitem extrair campos pontuais (ex.: `findInLine("\\d+")`) sem quebrar o fluxo inteiro — os padrões usados aqui são os mesmos da página de `regex` (`Pattern`/`Matcher`).
