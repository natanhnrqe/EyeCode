---
id: java/jdk/java.lang/object
title: Object
type: api
summary: A raiz de toda classe em Java — os métodos que todo objeto herda, o contrato de equals/hashCode e as regras de toString, clone e wait/notify.
level: beginner
duration: 8
officialDocs:
  label: API java.lang.Object
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html
related:
  - java/jdk/fundamentos/classes
  - java/jdk/java.util/map
---

> [!INFO] **Toda classe herda de `Object`**, mesmo sem `extends`. Por isso qualquer objeto responde a `equals`, `hashCode` e `toString` — e o padrão desses métodos compara **referência**, não conteúdo. Sobrescrevê-los é a primeira tarefa de uma classe de domínio.

## Visão geral

`java.lang.Object` é a superclasse de tudo (exceto os primitivos, que nem são objetos). Ela define o contrato mínimo que o resto da linguagem espera: identidade (`equals`), hash para coleções (`hashCode`), representação em texto (`toString`) e coordenação entre threads (`wait`/`notify`).

```java
class Conta { }                 // herda Object automaticamente
Conta c = new Conta();
System.out.println(c);          // Conta@1b6d3586 — padrão: classe@hash
c.equals(c);                    // true — mesma referência
```

A mensagem `Conta@1b6d3586` é o `toString()` padrão: nome da classe + hash em hexadecimal. Quando o usuário vê isso na tela, falta sobrescrever o método.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `equals(Object)` | `boolean` | igualdade lógica (padrão: mesma referência) |
| `hashCode()` | `int` | hash para `HashMap`/`HashSet` |
| `toString()` | `String` | representação em texto |
| `getClass()` | `Class<?>` | classe em tempo de execução |
| `wait()` / `wait(long ms)` | `void` | libera o monitor e espera |
| `notify()` / `notifyAll()` | `void` | acorda threads esperando |
| `clone()` | `Object` | cópia rasa (protegido, exige `Cloneable`) |
| `finalize()` | `void` | limpeza pós-morto (**não use**, obsoleto) |

## Identidade e igualdade

```java
String a = new String("oi");
String b = new String("oi");
a == b;                 // false — endereços diferentes na memória
a.equals(b);            // true  — String sobrescreve equals

Object x = new Object();
x.equals(x);            // true  — reflexividade (contrato)
a.equals(x);            // false — tipo diferente
a.equals(null);         // false — null nunca é igual
```

**O `equals` herdado de `Object` é exatamente `==`** — ele só sabe identidade. Classes de domínio (Conta, Produto, Usuário) precisam sobrescrever para comparar conteúdo.

## Representação com toString

```java
class Conta {
    private double saldo = 100.0;

    @Override
    public String toString() {
        return "Conta[saldo=" + saldo + "]";   // formato escolhido por você
    }
}

Conta c = new Conta();
System.out.println(c);          // Conta[saldo=100.0] — println chama toString()
String msg = "valor: " + c;     // concatenação também chama toString()
```

**`println` e `+` chamam `toString()` automaticamente** — se o objeto for `null`, imprimem a palavra `null` sem nenhuma exceção. Um bom `toString` é a ferramenta de depuração mais barata que existe.

## Contrato de equals e hashCode

```java
class Usuario {
    String email;

    @Override public boolean equals(Object o) {
        return o instanceof Usuario u && email.equals(u.email);
    }

    @Override public int hashCode() {
        return email.hashCode();   // mesmo campo usado no equals
    }
}

Set<Usuario> set = new HashSet<>();
set.add(new Usuario("ana@x.com"));
set.contains(new Usuario("ana@x.com"));   // true — equals + hashCode batem
```

**Iguais implicam mesmo hash; hash igual não implica igualdade** — mas sem sobrescrever os dois juntos, o `HashSet`/`HashMap` procura no balde errado e "perde" o objeto.

## Espera e notificação

```java
final Object lock = new Object();

synchronized (lock) {
    lock.wait();          // libera o lock e dorme até ser acordada
    // ... foi acordada: já re-adquireu o monitor automaticamente
}

// em outra thread, também sobre o mesmo lock:
synchronized (lock) {
    lock.notifyAll();     // acorda todas as que estavam esperando
}
```

**`wait`/`notify` só funcionam dentro de `synchronized` sobre o mesmo objeto** — fora disso estouram `IllegalMonitorStateException`. Na prática, prefira `java.util.concurrent` (`BlockingQueue`, `CountDownLatch`) em vez de coordenar threads à mão.

## Armadilhas comuns

> [!WARNING] Sobrescrever só `equals` sem `hashCode` (ou só um dos dois) quebra `HashMap`/`HashSet`: a busca usa o hash para achar o balde e o `equals` para confirmar dentro dele. Sempre os dois juntos, usando os mesmos campos.

**`equals` que estoura com `null`:**

```java
// correto — devolve false, nunca lança
@Override public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Usuario u = (Usuario) o;
    return email.equals(u.email);
}
```

O contrato exige que `equals(null)` devolva `false`, não que lance exceção.

**Depender de `finalize()` para limpar recursos:**

```java
// não faça — não há garantia de quando (ou se) roda
@Override protected void finalize() throws Throwable { fechar(); }

// faça — fechamento imediato e garantido
try (var in = Files.newBufferedReader(caminho)) { ... }
```

`finalize` está obsoleto para remoção desde o Java 9 — feche recursos com try-with-resources.

## Profundidade

**Contrato do `equals` (API de Object):** reflexivo (`x.equals(x)`), simétrico (`x.equals(y)` ⇔ `y.equals(x)`), transitivo, consistente entre chamadas e falso para `null`. Quebrar simetria (comparar subclasse com superclasse de um jeito e o inverso de outro) é o erro clássico de herança mal feita. O `hashCode` acompanhando o `equals` também é contrato — e precisa ficar **estável** enquanto o objeto estiver em coleção hash.

**`equals` e herança:** comparar com `getClass()` exige classes exatas (simetria garantida); comparar com `instanceof` aceita subtipos e pode quebrar simetria. Records e `final` evitam o dilema — `equals` de record é final e baseado nos componentes.

**Monitores (`wait`/`notify`):** cada objeto tem um monitor; `synchronized` o adquire, `wait` libera o monitor e entra na fila, `notify` escolhe uma thread de forma indefinida e `notifyAll` acorda todas. Como podem existir acordas falsas, o padrão correto é sempre `while (condicao) { lock.wait(); }`.

**`clone` raso:** `clone()` copia campo a campo — objetos internos ficam **compartilhados** entre original e cópia. A interface `Cloneable` é uma marca vazia só para o método não lançar exceção; a alternativa moderna é construtor de cópia ou factory. `getClass`, por sua vez, é `final`: é a base do `instanceof` em tempo de execução e da reflexão.
