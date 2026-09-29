---
id: java/jdk/java.lang/system
title: System
type: api
summary: Canal da JVM para o mundo externo — saída, tempo, propriedades, ambiente e cópias de array, com as armadilhas de exit, null e escalas de tempo.
level: beginner
duration: 7
officialDocs:
  label: API java.lang.System
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html
related:
  - java/jdk/fundamentos/classes
  - java/jdk/java.lang/exceptions
---

> [!INFO] `System` reúne o **estado global da JVM**: saída (`out`/`err`), relógio (`currentTimeMillis`, `nanoTime`), propriedades, variáveis de ambiente e encerramento. Todos os membros são `static` — a classe nem pode ser instanciada.

## Visão geral

`System` é o ponto de ligação entre o seu programa e a máquina que o executa: console, relógio do sistema, mapa de propriedades e sinal de saída. Não há estado por instância — tudo é compartilhado por toda a JVM.

```java
System.out.println("Hello");                    // saída padrão (stdout)
System.err.println("falha no carregamento");    // saída de erro (stderr)
String usuario = System.getProperty("user.name");
```

`System.out` e `System.err` são `PrintStream` prontos para uso; `System.in` é o fluxo de entrada bruto (quase sempre envolvido por `Scanner`, que verá depois).

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `out` / `err` (campos) | `PrintStream` | saída padrão / saída de erro |
| `in` (campo) | `InputStream` | entrada padrão (pouco usada direto) |
| `currentTimeMillis()` | `long` | milissegundos desde 1970 (hora da parede) |
| `nanoTime()` | `long` | nanos de um relógio **monotônico** |
| `exit(int status)` | `void` | encerra a JVM (`0` = sucesso) |
| `arraycopy(src, i, dest, j, n)` | `void` | copia `n` elementos de array |
| `lineSeparator()` | `String` | `\n` ou `\r\n` da plataforma |
| `getProperty(String)` | `String` | propriedade da JVM (`null` se não existe) |
| `getProperty(String, String)` | `String` | propriedade com valor padrão |
| `setProperty(String, String)` | `String` | define e devolve o valor anterior |
| `getenv(String)` | `String` | variável de ambiente (`null` se não existe) |
| `identityHashCode(Object)` | `int` | hash da **referência** (padrão do objeto) |
| `gc()` | `void` | pede uma coleta de lixo (só um pedido) |

## Saída no console

```java
System.out.println("linha com quebra");      // imprime e quebra a linha
System.out.print("sem quebra ");             // sem \n
System.out.printf("%s tem %d anos%n", "Ana", 28);   // Ana tem 28 anos
System.out.format("id=%05d%n", 42);         // id=00042
System.out.println();                        // só a quebra de linha
```

**`println` já quebra a linha** (um `\n` extra vira linha em branco), enquanto `printf` **não quebra sozinho** — é o especificador `%n` que cuida disso.

## Tempo de execução

```java
long inicio = System.currentTimeMillis();
long t0 = System.nanoTime();

// ... trabalho ...

long duracaoMs = System.currentTimeMillis() - inicio;   // ex.: 42 ms
long duracaoNs = System.nanoTime() - t0;                // ex.: 42123456 ns
```

**`nanoTime()` não é hora do dia** — o valor de origem é arbitrário e só a **diferença** entre duas chamadas significa alguma coisa. Para medir duração, subtraia sempre dois `nanoTime()`.

## Propriedades e ambiente

```java
System.getProperty("os.name");           // "Windows 11"
System.getProperty("java.version");      // "21.0.x"
System.getProperty("user.home");         // diretório do usuário
System.getenv("PATH");                   // variável de ambiente (pode ser null)
System.setProperty("app.modo", "debug"); // define propriedade própria
```

**`getProperty` e `getenv` devolvem `null`** quando a chave não existe — trate isso antes de chamar `.equals()` ou interpolar, ou use a sobrecarga com padrão: `System.getProperty("chave", "padrao")`.

## Cópias rápidas de array

```java
int[] origem = {1, 2, 3, 4, 5};
int[] destino = new int[3];
System.arraycopy(origem, 1, destino, 0, 3);   // destino = {2, 3, 4}
System.arraycopy(origem, 0, origem, 1, 4);    // origem  = {1, 1, 2, 3, 4}
```

**`arraycopy` lança exceção quando o intervalo não cabe** (`IndexOutOfBoundsException` para posição, `ArrayStoreException` para tipo errado) — ele não corta em silêncio. A cópia é um atalho nativo da JVM e lida corretamente com sobreposição de origem e destino.

## Armadilhas comuns

> [!WARNING] `System.exit(status)` encerra a JVM **na hora**: blocos `finally` do caminho que chamou não rodam (só os shutdown hooks). Em biblioteca ou código de teste, devolva um status em vez de encerrar a JVM inteira.

```java
void salvar() {
    try {
        gravarArquivo();
        System.exit(1);        // nunca retorna
    } finally {
        arquivo.close();       // NÃO roda — a JVM já está encerrando
    }
}
```

**Misturar as duas escalas de tempo:**

```java
long a = System.nanoTime();
long b = System.currentTimeMillis();
b - a;                 // número sem sentido — escalas e origens diferentes
```

`currentTimeMillis` conta desde 1970 e pode andar para trás (ajuste de NTP); `nanoTime` tem origem arbitrária e nunca deve ser impresso como data.

**Propriedade inexistente vaza `null`:**

```java
String valor = System.getProperty("nao.existe");
valor.equals("on");                            // NullPointerException
String seguro = System.getProperty("nao.existe", "off");   // "off"
```

Mesmo cuidado com `System.getenv("VAR_INEXISTENTE")` — sempre ofereça um padrão ou verifique `!= null`.

## Profundidade

**Classe utilitária por desenho:** `System` tem construtor privado, campos `final` e só métodos `static` — não existe "uma System". O estado que expõe (streams, propriedades) é único da JVM em execução e pode ser substituído (`System.setOut`) por questões de captura de saída em testes.

**Dois relógios com propósitos diferentes:** `currentTimeMillis` é o relógio civil (susceptível a ajustes para frente e para trás, ruim para medir duração); `nanoTime` é monotônico (só avança) e serve para intervalos. Para data/hora com fuso e imutabilidade, use `java.time.Instant.now()` e `Duration.between(...)` — a API moderna substitui os dois métodos no código novo.

**Codificação de texto (JEP 400):** desde o JDK 18 o padrão da plataforma é UTF-8. A saída de console usa a propriedade `stdout.encoding`, e `file.encoding` controla arquivos — configure via `-Dstdout.encoding=UTF-8` quando o terminal divergir.

**Propriedades vs ambiente:** `getProperty` lê o mapa de propriedades da JVM, alterável em tempo de execução (`setProperty`) e na inicialização (`-Dminha.flag=1`); `getenv` lê o ambiente do processo operacional, **somente leitura**. Cada uma tem seu papel: configuração da aplicação nas propriedades, segredos/ajustes do deploy no ambiente.

**`arraycopy` é intrínseco:** a JVM substitui a chamada por uma cópia nativa otimizada — muito mais rápida que laço em Java. Para estruturas de alto nível prefira `List.copyOf`, `System.arraycopy` em arrays e os métodos de `Arrays` (`Arrays.copyOf`, `Arrays.fill`).

**Alternativa moderna de saída:** `System.getLogger("nome")` (Java 9+) oferece níveis e formatos sem depender de `println`, e frameworks de log (SLF4J, Logback) são o padrão em aplicação — `System.out` é ótimo para rascunho e ruim em produção (sem timestamp, nível nem destino configurável).
