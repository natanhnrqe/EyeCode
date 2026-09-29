---
id: java/jdk/java.util/objects
title: Objects
type: api
summary: Utilitários nulos-seguros do Java, com equals, hash, toString e requireNonNull explicando quando cada um evita NullPointerException.
level: beginner
duration: 6
officialDocs:
  label: API java.util.Objects
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html
related:
  - java/jdk/java.util/arrays
  - java/jdk/java.lang/string
  - java/jdk/java.lang/exceptions
---

> [!INFO] `Objects` reúne métodos `static` que **não estouram com `null`**: `Objects.equals(a, b)` compara mesmo que um dos lados seja `null`, `Objects.hash(...)` monta `hashCode` e `requireNonNull` garante pré-condições. É a caixa de ferramentas para lidar com ausência de valor.

## Visão geral

Toda vez que você escreve `a.equals(b)` e `a` pode ser `null`, o programa morre. `Objects` resolve isso com versões seguras e prontas — e evita reimplementar `hashCode`/`toString` manualmente a cada projeto.

```java
Objects.equals("a", null);            // false, sem exceção
Objects.hash("ana", 30);              // hash determinístico dos campos (2999435)
Objects.requireNonNull(config, "config não pode ser null");  // falha cedo e com mensagem
```

A classe é final com construtor privado — só estáticos, nunca `new Objects()`.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `equals(Object a, Object b)` | `boolean` | `equals` seguro com `null` |
| `deepEquals(Object a, Object b)` | `boolean` | igual, mas aprofunda arrays |
| `hashCode(Object o)` | `int` | hash de um objeto; `0` se `null` |
| `hash(Object... valores)` | `int` | hash combinando vários valores |
| `toString(Object o)` | `String` | `String.valueOf` seguro (`null` → `"null"`) |
| `toString(Object o, String padrao)` | `String` | idem, com padrão para `null` |
| `requireNonNull(T o)` | `T` | lança `NullPointerException` se `null` |
| `requireNonNull(T o, String msg)` | `T` | idem, com mensagem |
| `requireNonNull(T o, Supplier)` | `T` | idem, mensagem calculada só se preciso |
| `isNull(Object o)` | `boolean` | se é `null` |
| `nonNull(Object o)` | `boolean` | se não é `null` |
| `compare(T a, T b, Comparator)` | `int` | delega ao comparador (NPE se o comparador for `null`) |
| `checkIndex(int, int)` (Java 9) | `int` | valida índice; lança `IndexOutOfBoundsException` |

## Comparar sem medo de null

```java
String a = null;
String b = "ana";

System.out.println(Objects.equals(a, b));      // false (a.equals(b) estouraria NPE)
System.out.println(Objects.equals(a, null));   // true
System.out.println(Objects.equals("x", "x"));  // true
System.out.println(Objects.isNull(a));         // true
System.out.println(Objects.nonNull(b));        // true

// em equals() da sua classe:
// return Objects.equals(nome, outro.nome) && Objects.equals(idade, outro.idade);
```

**Armadilha:** `Objects.equals` compara com `equals` dos elementos — para arrays aninhados, use `deepEquals`.

O padrão `Objects.equals(campo, outro.campo)` dentro de `equals()` elimina todos os `if (campo == null)`.

## Validar entradas (requireNonNull)

```java
public void guardar(Cliente cliente) {
    Objects.requireNonNull(cliente, "cliente não pode ser null");
    Objects.requireNonNull(cliente.nome(), () -> "nome ausente para " + cliente.id());
    // se chegar null aqui, a falha acontece NA ENTRADA do método,
    // e não três telas depois — stack trace aponta o culpado real
}

Objects.requireNonNull(null);   // NullPointerException
Objects.requireNonNull("ok");   // devolve "ok" — pode encadear
```

`requireNonNull` devolve o próprio argumento, então dá para usar inline: `this.campo = requireNonNull(campo);`.

**Armadilha:** `requireNonNull` só protege o que é **obrigatório** — se o valor pode faltar mesmo, use `Optional` ou um padrão, senão você transforma ausência em exceção.

## Gerar hash e texto

```java
int h1 = Objects.hash("ana", 30);              // hash combinado de dois campos
int h2 = Objects.hash("ana", 30);
System.out.println(h1 == h2);                  // true (mesmos campos, mesmo hash)

System.out.println(Objects.hashCode(null));    // 0 (não estoura)
System.out.println(Objects.toString(null));    // null (texto, não exceção)
System.out.println(Objects.toString(null, "-"));// -
System.out.println(Objects.toString(List.of(1))); // [1]
```

**Armadilha:** `Objects.toString(obj)` para `null` devolve a string `"null"` — é seguro, mas pode esconder o bug se você esperava uma exceção.

## Armadilhas comuns

> [!WARNING] `requireNonNull` lança `NullPointerException` de propósito — serve para **erro de programação** (pré-violada, parâmetro interno), nunca para validar dados do usuário. Para entrada inválida, lance `IllegalArgumentException` ou uma exceção de domínio.

**`equals` profundo depende do tipo:**

```java
Integer[] a = {1, 2};
Integer[] b = {1, 2};
Objects.equals(a, b);        // false — array compara por referência
Objects.deepEquals(a, b);    // true — compara elemento a elemento
Objects.equals(List.of(1), List.of(1));   // true — List implementa equals
```

**Hash de `Object` não serve em `HashSet`:**

```java
class Pessoa { String nome; }              // sem equals/hashCode herdados de Object
Set<Pessoa> s = new HashSet<>();
s.add(new Pessoa());
s.contains(new Pessoa());                  // false — usa identidade, não conteúdo
// corrija com: Objects.hash(nome) em hashCode() e Objects.equals(...) em equals()
```

**`Objects.toString` não é `String.valueOf` de gravação:** os dois devolvem `"null"` para `null`, mas `Objects.toString(o, padrao)` dá um texto personalizado — use-o em mensagens de erro, não na lógica de negócio.

## Profundidade

**Contrato `equals`/`hashCode`:** objetos iguais devem ter o mesmo `hashCode`. `Objects.hash(...)` aplica o algoritmo padrão da API (`31 * h + valor`), igual ao que o compilador gera — usar o mesmo método nos dois lados evita a divergência clássica de "hash escrito à mão".

**Por que `31`:** o multiplicador 31 é ímpar e cabe em 32 bits com bons bits de propagação (JLS/API de `String.hashCode`); não é místico, é convenção histórica compatível com a implementação da JVM.

**`requireNonNull` com `Supplier`:** a segunda forma só calcula a mensagem se o valor for `null` — importante quando montar a string é caro (ex.: concatenar um estado grande).

**`compare` com comparator:** `Objects.compare(a, b, cmp)` encurta `a == b ? 0 : cmp.compare(a, b)` e falha cedo se o comparator for `null`. Para ordenar com ausências, combine com `Comparator.nullsFirst`/`nullsLast` — aí o próprio comparador trata o `null`.

**`checkIndex` (Java 9):** valida `0 <= index < length` e devolve o índice — usado internamente por `String.charAt` e `List.get` modernos; útil para checagens rápidas sem escrever `if` repetido.
