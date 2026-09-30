---
id: java/jdk/java.util/iterator
title: Iterator
type: api
summary: Percorrer coleções elemento a elemento — Iterator com hasNext/next/remove, ListIterator bidirecional e o motivo do fail-fast ConcurrentModificationException.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Iterator
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Iterator.html
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/set
  - java/jdk/java.util/map
---

> [!INFO] `Iterator` é o **cursor das coleções**: `hasNext`/`next` percorrem elemento a elemento, e `remove` é a única forma segura de apagar **durante** o percurso. O for-each que você escreve todos os dias é açúcar sintático sobre exatamente esses três métodos — e mexer na coleção por fora do iterator dispara `ConcurrentModificationException`.

## Visão geral

Toda coleção (`List`, `Set`, e indiretamente `Map` via `entrySet()`) implementa `Iterable`, cujo único trabalho é devolver um `Iterator`. O protocolo é mínimo e sempre igual: pergunte se existe próximo, avance, repita:

```java
List<String> times = new ArrayList<>(List.of("Fla", "Vasco"));

for (String t : times) {          // açúcar sintático sobre Iterator
    System.out.println(t);
}

// o que o for-each faz por dentro (equivalente):
for (Iterator<String> it = times.iterator(); it.hasNext(); ) {
    String t = it.next();
    System.out.println(t);
}
```

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Iterable.iterator()` | `Iterator<T>` | devolve um cursor novo |
| `Iterator.hasNext()` | `boolean` | existe próximo elemento? |
| `Iterator.next()` | `T` | avança e devolve o próximo |
| `Iterator.remove()` | `void` | apaga o último devolvido por `next()` |
| `Iterator.forEachRemaining(Consumer)` | `void` | processa o resto (Java 8) |
| `List.listIterator([int índice])` | `ListIterator<T>` | cursor bidirecional de `List` |
| `ListIterator.hasPrevious()` / `previous()` | `boolean` / `T` | caminha para trás |
| `ListIterator.nextIndex()` / `previousIndex()` | `int` | índices do próximo / do anterior |
| `ListIterator.set(E)` | `void` | substitui o último devolvido |
| `ListIterator.add(E)` | `void` | insere na posição atual |
| `Collection.removeIf(Predicate)` | `boolean` | remoção em massa segura (Java 8) |

## O protocolo hasNext/next

```java
Iterator<String> it = List.of("a", "b").iterator();

it.hasNext();   // true
it.next();      // "a" — avança e devolve
it.next();      // "b"
it.hasNext();   // false
// it.next();   // NoSuchElementException — o cursor acabou

Iterator<String> cursor = List.of("x", "y", "z").iterator();
while (cursor.hasNext()) {
    String atual = cursor.next();   // next() UMA vez por volta
    System.out.println(atual);
}
```

Chamar `next()` duas vezes por iteração pula elementos; chamar sem `hasNext()` no fim lança `NoSuchElementException` — o par `hasNext`/`next` é um acordo, não decorativo.

## Remoção segura durante o laço

```java
List<Integer> numeros = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));

// ERRADO: mexer na lista durante o for-each
for (Integer n : numeros) {
    // numeros.remove(n);  // ConcurrentModificationException na próxima volta
}

// CERTO com Iterator: remove() apaga o último elemento devolvido por next()
for (Iterator<Integer> it = numeros.iterator(); it.hasNext(); ) {
    if (it.next() % 2 == 0) {
        it.remove();
    }
}
// numeros = [1, 3, 5]

// forma moderna (Java 8+): removeIf faz o protocolo seguro por você
numeros.removeIf(n -> n % 3 == 0);
// numeros = [1, 5]
```

O iterator é o único observador que a coleção reconhece: `it.remove()` atualiza a lista **e** o cursor juntos. Qualquer outro caminho deixa os dois dessincronizados — e é aí que nasce a exceção.

## ListIterator: o cursor bidirecional

Exclusivo de `List`, o `ListIterator` anda para frente e para trás, e ainda edita no lugar:

```java
List<String> nomes = new ArrayList<>(List.of("ana", "bo", "caio"));
ListIterator<String> li = nomes.listIterator();

li.next();        // "ana" — cursor após o índice 0
li.set("ANA");    // substitui o último devolvido — a lista muda
li.add("novo");   // insere na posição atual
li.previous();    // "novo" — voltou um passo
li.nextIndex();   // 1 — índice do próximo elemento
```

## Armadilhas comuns

> [!WARNING] Alterar a coleção **durante** a iteração — `list.remove(...)` dentro do for-each, `map.put(...)` enquanto percorre o `entrySet()` — dispara `ConcurrentModificationException` na próxima chamada de `next()`. O mecanismo é *fail-fast* e **best effort**: serve para revelar o bug cedo, não é um detector de concorrência em que se possa confiar.

**`next()` sem `hasNext()`:** lança `NoSuchElementException` — em código defensivo, o `while (it.hasNext())` é obrigatório, não opcional.

**`remove()` exige um `next()` prévio:** e só pode ser chamado **uma vez** por `next()` — segunda chamada em sequência lança `IllegalStateException`.

**Iterator é descartável:** ele é um cursor vivo sobre a coleção no momento da criação; não guarde para reusar depois de modificações — peça `iterator()` novo a cada percurso.

**`Map` não é `Iterable`:** percorra `map.entrySet()` / `keySet()` / `values()` — cada `iterator()` dessas views devolve um cursor novo e independente.

## Profundidade

O for-each funciona com qualquer `Iterable` (JLS §14.14.2) — implementar `iterator()` na sua própria classe a torna percorrível com o mesmo `for (X x : minhaClasse)`. O fail-fast de `ArrayList`/`HashMap` usa um contador de modificações estruturais (`modCount`) que o iterator compara a cada `next()`; por isso a exceção aparece na chamada seguinte, não no momento da modificação. Coleções concorrentes como `ConcurrentHashMap` usam iteradores *weakly consistent* que nunca lançam — refletem o estado de quando nasceram e vão o mais longe que dá. O padrão Iterator (GoF) unifica o percurso desde o Java 1.2, substituindo a antiga `Enumeration`; `ListIterator` é a extensão bidirecional que só faz sentido em listas, onde existe ordem para voltar.
