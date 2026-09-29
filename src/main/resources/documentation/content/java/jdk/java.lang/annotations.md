---
id: java/jdk/java.lang/annotations
title: Anotações
type: concept
summary: Metadados declarativos em Java — como funcionam @Override, anotações customizadas, retenção e reflexão, e o que a anotação realmente faz.
level: beginner
duration: 8
officialDocs:
  label: JLS §9 — Annotations
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html
related:
  - java/jdk/fundamentos/classes
  - java/jdk/java.lang/enums
  - java/spring/boot-basics
---

> [!INFO] Anotação é **metadado declarativo** (`@Override`, `@Test`): sozinha, ela não muda nada do programa — quem executa é quem **lê** (compilador, JVM ou framework). Anotação marca *intenção*, não comportamento.

## Por que existe

Comentário envelhece e ninguém lê; código continua. `@Override` faz o compilador **reclamar** se o método que você achava que sobrescrevia não existe mais — um comentário nunca faria isso. Em framework, `@Transactional` diz ao Spring "abra transação aqui" sem uma linha de configuração externa.

Anotação padroniza declaração: fica **no lugar certo** (ao lado do que descreve), tem **formato verificável** e pode ser lida por máquina. É a forma moderna de dizer "isto aqui é um teste", "isto aqui é deprecated", "este campo vai para a coluna X".

## Anatomia da sintaxe

```java
class Formulario {

    @Override
    public String toString() { return "Formulario[...]"; }   // compilador confere

    @Deprecated(since = "2.0", forRemoval = true)
    void campoAntigo() { /* não use mais */ }
}
```

Declaração — criando a sua própria anotação:

```java
@Target(ElementType.METHOD)                    // onde pode ser usada
@Retention(RetentionPolicy.RUNTIME)            // existe em tempo de execução
@interface Validar {
    String mensagem() default "valor inválido";   // elemento com padrão (opcional)
    boolean obrigatorio();                        // sem padrão → obrigatório
}

// uso:
@Validar(mensagem = "email obrigatório", obrigatorio = true)
void salvar(String email) { }
```

| Parte | O que é | Exemplo |
|-------|---------|---------|
| `@interface` | declara o tipo da anotação | `@interface Validar` |
| elementos | métodos sem corpo, viram `nome = valor` | `String mensagem() default "..."` |
| `default` | torna o elemento opcional | `int nivel() default 1;` |
| meta-anotações | anotações **sobre** anotações | `@Target`, `@Retention` |

Meta-anotações que você vai usar ao criar a sua:

| Meta-anotação | O que define | Valores comuns |
|---------------|--------------|----------------|
| `@Target` | onde pode ser aplicada | `METHOD`, `FIELD`, `TYPE`, `PARAMETER`, `CONSTRUCTOR` |
| `@Retention` | quanto tempo ela existe | `SOURCE`, `CLASS` (padrão), `RUNTIME` |
| `@Documented` | aparece no Javadoc gerado | — |
| `@Inherited` | subtipo herda da classe pai | — |
| `@Repeatable` | pode ser repetida no mesmo lugar | (Java 8+) |

## Como funciona

Uma anotação é só um par `nome = valor` gravado em metadados — o efeito depende de **quem lê**:

1. **Compilador** — `@Override` confere a sobrescrita, `@Deprecated` avisa quem usar, `@SuppressWarnings` silencia avisos. Tudo em tempo de compilação.
2. **Ferramentas de bytecode** — geradores e agentes leem as anotações (retenção `CLASS` ou `RUNTIME`) e transformam os `.class`.
3. **Reflexão em execução** — com `@Retention(RUNTIME)`, o framework consulta `metodo.isAnnotationPresent(...)` na inicialização e age de acordo (Spring, JUnit).

## Exemplos

Lendo anotação por reflexão:

```java
class Servico {
    @Validar(mensagem = "email obrigatório")
    void salvar(String email) { }
}

Method m = Servico.class.getDeclaredMethod("salvar", String.class);
if (m.isAnnotationPresent(Validar.class)) {
    Validar v = m.getAnnotation(Validar.class);
    System.out.println(v.mensagem());      // Saída: email obrigatório
}
```

Onde as anotações famosas aparecem — JUnit e Spring só *leem* as suas:

```java
@Test                                  // JUnit: "este método é teste"
void calculaTotal() {
    assertEquals(10, 5 + 5);
}

@Service                                // Spring: registra como bean do container
public class PedidoService {

    @Transactional                      // Spring: transação ao redor do método
    public void fechar(Pedido p) { ... }
}
```

Sobrescrita e avisos — as built-in do `java.lang`:

```java
@Override public String toString() { return "..."; }   // erro se não sobrescrever
@Deprecated(since = "1.4", forRemoval = true) void velho() { ... }
```

## Armadilhas comuns

> [!WARNING] Anotação com `@Retention(SOURCE)` **não chega aos arquivos `.class`** — `isAnnotationPresent` devolve `false` sempre. Se você vai ler por reflexão, o alvo é obrigatoriamente `RetentionPolicy.RUNTIME` (o padrão `CLASS` também não é visível em execução).

**Anotação não executa nada sozinha:**

```java
@Logar                                  // só uma marca no código
void metodo() { }

metodo();                               // não imprime nada
// enquanto não houver reflexão/processador lendo @Logar, ela é ignorada
```

Esperar que a anotação "aja" sozinha é o erro conceitual mais comum — o comportamento mora em quem processa.

**Elemento sem `default` é obrigatório:**

```java
@interface Coluna { String nome(); }

@Coluna                                 // erro de compilação: falta "nome = ..."
void m() { }

@Coluna(nome = "usuario_id")            // correto
void ok() { }
```

O compilador exige valor para todo elemento sem padrão — é a anotação dizendo "não dá para deixar em aberto".

## Profundidade

**Sintaxe declarativa (JLS §9):** `@interface` declara um tipo cujos "métodos" são elementos de valor — corpo vazio, retorno restrito a primitivo, `String`, `Class`, enum, anotação ou array desses, e nomes seguem a convenção de atributo (`nome = valor`, sem parênteses no uso). Valores precisam ser constantes em tempo de compilação — não dá para passar variável.

**Metadado, não comportamento (JLS §9.1):** gravar anotação não altera o bytecode por conta própria. O efeito sempre vem de fora: checagem do compilador (`@Override`), transformação de bytecode (geradores, ASM) ou reflexão (`RUNTIME`). Uma anotação sem leitor é documentação estruturada — útil, mas inerte.

**Retenção e ciclo de vida:** `SOURCE` existe só durante a compilação e é descartada (`@Override`, `@SuppressWarnings`); `CLASS` fica nos `.class` mas não é exposta à JVM em execução (padrão, usada por ferramentas de bytecode); `RUNTIME` fica disponível via `java.lang.reflect` — é a exigência de qualquer framework (Spring, JUnit, Jackson).

**Meta-anotações de composição:** `@Inherited` só vale para anotações de **tipo** e faz o `getAnnotation` de subclasse cair na superclasse; `@Documented` controla presença no Javadoc; `@Repeatable` gera um "container" interno e permite `@A @A` no mesmo lugar (ex.: múltiplas anotações de autor).

**Ecossistema:** quase toda stack moderna é construída sobre anotações `RUNTIME` lidas na inicialização — Spring (`@Component`, `@Autowired`, `@Transactional`), JUnit (`@Test`, `@BeforeEach`), Jackson (`@JsonProperty`). Entender retenção e reflexão explica por que o mesmo código muda de comportamento com um framework ligado: quem muda é quem lê as anotações, não a linguagem.
