---
id: java/jdk/java.util/map
title: Map
type: api
summary: Mapas chave-valor do Java, com HashMap e LinkedHashMap na prática, métodos modernos de consulta e as armadilhas do null retornado por get.
level: beginner
duration: 9
officialDocs:
  label: API java.util.Map
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html
related:
  - java/jdk/java.util/collections
  - java/jdk/java.util/objects
  - java/jdk/java.util/set
---

> [!INFO] `Map` guarda **pares chave → valor**: a chave é única e serve para buscar o valor. `get` devolve `null` tanto para "chave inexistente" quanto para "valor guardado foi null" — por isso prefira `getOrDefault`, `containsKey` ou `merge`.

## Visão geral

Mapa é o dicionário do Java: você indexa informações por uma chave (CPF, nome de configuração, id de usuário) e recupera o valor em O(1) médio. `HashMap` é o padrão; `LinkedHashMap` preserva a ordem de inserção (ou de acesso); `TreeMap` mantém as chaves ordenadas.

```java
Map<String, Integer> idades = new HashMap<>();        // rápido, sem ordem
Map<String, Integer> ordem  = new LinkedHashMap<>();  // ordem de inserção
Map<String, Integer> chaves = new TreeMap<>();        // chaves ordenadas
Map<String, String>  fixo   = Map.of("a", "1");       // imutável
```

`Map` **não** é uma `Collection` — para percorrer, use `keySet()`, `values()` ou `entrySet()`.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `put(K chave, V valor)` | `V` | grava; devolve o valor anterior (ou `null`) |
| `get(Object chave)` | `V` | valor da chave ou `null` |
| `remove(Object chave)` | `V` | remove e devolve o valor antigo |
| `containsKey(Object)` / `containsValue(Object)` | `boolean` | existência da chave / valor |
| `getOrDefault(K, V)` | `V` | valor ou padrão se ausente |
| `putIfAbsent(K, V)` | `V` | grava só se a chave não existir |
| `computeIfAbsent(K, Function)` | `V` | grava o resultado da função se ausente |
| `merge(K, V, BiFunction)` | `V` | soma/combinar valores na chave |
| `size()` / `isEmpty()` | `int` / `boolean` | quantidade / vazio |
| `keySet()` | `Set<K>` | view das chaves |
| `values()` | `Collection<V>` | view dos valores |
| `entrySet()` | `Set<Map.Entry<K,V>>` | view dos pares |
| `forEach(BiConsumer)` | `void` | percorre chave e valor |
| `putAll(Map)` | `void` | copia todos os pares |
| `Map.of(K, V...)` (estático) | `Map<K,V>` | mapa imutável |

## Gravar e remover

```java
Map<String, Integer> estoque = new HashMap<>();
estoque.put("cafe", 10);           // devolve null (não havia valor anterior)
estoque.put("cafe", 7);            // devolve 10 (substituiu)
estoque.putIfAbsent("cha", 3);     // grava porque "cha" não existia
estoque.remove("cha");             // devolve 3
estoque.size();                    // 1
System.out.println(estoque);       // {cafe=7}
// put devolve o valor ANTERIOR — é o valor novo que fica no mapa
```

Para contar ocorrências, `merge` evita o padrão get-soma-put:

```java
Map<String, Integer> contagem = new HashMap<>();
for (String palavra : List.of("a", "b", "a")) {
    contagem.merge(palavra, 1, Integer::sum);   // 1 = valor base, soma = combinação
}
System.out.println(contagem);       // {a=2, b=1}
```

**Armadilha:** `merge(chave, null, funcao)` lança `NullPointerException` no `HashMap` — o valor base nunca pode ser `null`.

## Consultar

```java
Map<String, Integer> idades = new HashMap<>();
idades.put("ana", 30);

idades.get("ana");                     // 30
idades.get("bia");                     // null — chave ausente
idades.getOrDefault("bia", 0);         // 0 (forma segura, não grava nada)
idades.containsKey("ana");             // true
idades.getOrDefault("bia", -1);        // -1 — o mapa continua igual
idades.computeIfAbsent("bia", k -> k.length());   // 3 — grava "bia"->3 e devolve
System.out.println(idades.get("bia")); // 3 — agora a chave existe
```

**`getOrDefault` é preferível a `get` + `null`**: devolve o padrão sem risco de `NullPointerException`.

## Percorrer

```java
Map<String, Integer> idades = Map.of("ana", 30, "bia", 25, "caio", 41);

idades.forEach((nome, idade) -> System.out.println(nome + "=" + idade));
// ana=30 | bia=25 | caio=41 (a ordem de iteração não é garantida)

for (Map.Entry<String, Integer> e : idades.entrySet()) {
    System.out.println(e.getKey() + " -> " + e.getValue());
}
// percorrer entrySet é o mais rápido: já traz chave e valor juntos

idades.keySet().forEach(System.out::println);   // só chaves
idades.values().stream().mapToInt(Integer::intValue).sum();  // 96
```

`keySet()`, `values()` e `entrySet()` são **views**: mudanças nelas mudam o mapa (e `remove` durante o laço também estoura `ConcurrentModificationException`).

**Armadilha:** gravar (`put`) dentro do laço também dispara `ConcurrentModificationException` — acumule as mudanças e aplique depois.

## Armadilhas comuns

> [!WARNING] `map.get(chave) == null` é ambíguo: a chave pode não existir ou existir com valor `null`. Nunca use `if (map.get(k) == null)` para testar existência — use `containsKey` ou `getOrDefault`.

**Chave que muda depois de virar chave:**

```java
Map<StringBuilder, Integer> m = new HashMap<>();
StringBuilder chave = new StringBuilder("x");
m.put(chave, 1);
chave.append("y");                 // mudou o conteúdo da chave
m.get(chave);                      // null — o hash não é mais o mesmo
// use Strings (imutáveis) como chave; nunca objetos mutáveis
```

**`HashMap` não tem ordem:**

```java
Map<String, Integer> m = new HashMap<>(Map.of("b", 1, "a", 2));
System.out.println(m);            // qualquer ordem ({a=2, b=1} ou {b=1, a=2})

Map<String, Integer> ordem = new LinkedHashMap<>();
ordem.put("b", 1); ordem.put("a", 2);
System.out.println(ordem.keySet());  // [b, a] — ordem de inserção preservada
```

**Misturar tipos nas chaves:** `map.put(1, "a"); map.get("1");` → `null` (hash e `equals` batem só para objetos do mesmo tipo). `Integer` `1` e `String` `"1"` nunca são a mesma chave.

## Profundidade

**Buckets e load factor:** `HashMap` guarda entradas em buckets por `hashCode`; com `0.75` de ocupação média, ele redimensiona (copia tudo para um array maior). Colisões viram listas encadeadas e, em Java 8+, árvores quando o balde cresce demais — O(1) médio, O(n) no pior caso.

**`LinkedHashMap` e acesso:** no modo `accessOrder = true`, o mapa reordena pelo **último acesso** — é a base de um LRU cache (`removeEldestEntry`). No modo padrão, mantém a ordem de inserção.

**`TreeMap` e `Comparator`:** chaves ficam em árvore rubro-negra, O(log n), sempre ordenadas; `firstKey`, `lastKey`, `headMap`, `subMap` vêm de `NavigableMap`. O comparador define "mesma chave" — `compare == 0` colapsa duas chaves diferentes.

**Contrato da chave (JLS/API):** chave precisa de `hashCode` constante enquanto viver no mapa e `equals` coerente. `String`, `Integer`, `enum` e `record` são excelentes chaves; a maioria das classes `Object` com estado mutável, não.

**Views são live:** `keySet()`/`values()`/`entrySet()` não copiam — `entrySet().remove(...)` remove do mapa. Para uma cópia, use `new ArrayList<>(map.keySet())`.

**Thread safety:** `HashMap` não é thread-safe; em cenário concorrente use `ConcurrentHashMap` (escritas bloqueadas por segmento) em vez de `Collections.synchronizedMap`.
