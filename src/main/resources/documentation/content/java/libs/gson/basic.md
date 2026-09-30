---
id: java/libs/gson/basic
title: Gson: básico
type: api
summary: JSON sem anotações com Gson — toJson e fromJson em uma linha, campos transient excluídos automaticamente e leitura de listas com TypeToken.
level: beginner
duration: 7
officialDocs:
  label: Gson — GitHub (Google)
  url: https://github.com/google/gson
related:
  - java/libs/jackson/basic
  - java/jdk/java.util/list
  - java/jdk/fundamentos/generics
---

> [!INFO] Gson é a biblioteca de JSON do Google e funciona **sem anotações e sem getters**: serializa campos direto, ignora `transient` por padrão e converte objeto ↔ JSON com uma chamada — `toJson` / `fromJson`.

## Visão geral

Gson resolve o mesmo problema que Jackson (objeto ↔ JSON) com uma API menor. O trabalho do dia a dia fica em uma classe: **`com.google.gson.Gson`**.

```java
Gson gson = new Gson();

record Usuario(String nome, int idade) {}

String json = gson.toJson(new Usuario("Ana", 28));
// {"nome":"Ana","idade":28}

Usuario lido = gson.fromJson(json, Usuario.class);
// Usuario[nome=Ana, idade=28]
```

Diferenças-chave para o Jackson: funciona com classes **sem getters** (lê campos direto, inclusive privados), não lança exceção checked e falha **silenciosamente** em casos que o Jackson rejeita.

## Dependência

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

```groovy
implementation 'com.google.code.gson:gson:2.11.0'
```

Android já inclui Gson em vários templates; em projetos Java puros, declare a dependência acima.

## Uso básico

```java
Gson gson = new Gson();

// objeto → JSON
String json = gson.toJson(usuario);

// JSON → objeto
Usuario u = gson.fromJson(json, Usuario.class);

// JSON bonito (pretty print)
Gson bonito = new GsonBuilder().setPrettyPrinting().create();
String jsonBonito = bonito.toJson(usuario);
```

`fromJson` lança `JsonSyntaxException` (unchecked, filha de `RuntimeException`) quando o JSON é sintaticamente inválido — não obriga `try/catch`, mas também não avisa sobre campos que não casam.

## Campos transientes

Por padrão, Gson **não serializa** campos marcados com `transient`:

```java
public class Sessao {
    private String usuario;
    private String token;
    private transient long ultimoAcesso;   // excluído do JSON

    public Sessao(String usuario, String token) {
        this.usuario = usuario;
        this.token = token;
        this.ultimoAcesso = System.currentTimeMillis();
    }
}

String json = new Gson().toJson(new Sessao("ana", "t-9"));
// {"usuario":"ana","token":"t-9"} — "ultimoAcesso" não apareceu
```

Esse é o jeito idiomático de excluir campos sem anotação — `transient` já significa "não faça parte do estado serializável" no Java. Para o inverso (só serializar o que tem `@Expose`), use `new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()`.

Armadilha: `transient` afeta também a serialização padrão do Java (`Serializable`) — marcar um campo como transient para o Gson o exclui de qualquer serialização, não só do JSON.

## Listas e genéricos

Genéricos precisam de `TypeToken` por causa do type erasure:

```java
Gson gson = new Gson();

List<Usuario> lista = List.of(new Usuario("Ana", 28), new Usuario("Bia", 31));
String json = gson.toJson(lista);
// [{"nome":"Ana","idade":28},{"nome":"Bia","idade":31}]

// leitura: o tipo genérico vai no TypeToken
List<Usuario> lida = gson.fromJson(
        json, new TypeToken<List<Usuario>>() {}.getType());
```

Armadilha: `fromJson(json, List.class)` devolve `List<LinkedTreeMap>` — os elementos viram mapas, não `Usuario`, e o erro só explode quando você acessa um elemento.

## Mapas e árvore

```java
Gson gson = new Gson();

Map<String, Integer> estoque = Map.of("parafuso", 100, "porca", 50);
String json = gson.toJson(estoque);   // {"parafuso":100,"porca":50}

// árvore genérica (equivalente do JsonNode do Jackson)
JsonObject raiz = JsonParser.parseString(json).getAsJsonObject();
int parafuso = raiz.get("parafuso").getAsInt();   // 100
```

## Armadilhas comuns

> [!WARNING] Gson lê campos **privados direto, sem getters** — isso quebra o encapsulamento silenciosamente e, com Java 9+, pode exigir `--add-opens` para classes internas do JDK. Em classes que você controla, prefira getters; em `record`, o construtor canônico é usado naturalmente.

- **Falha silenciosa**: JSON inválido para um campo é ignorado sem exceção (o campo fica `null`) — ao contrário do Jackson, que falha por padrão;
- **Genéricos sem `TypeToken`**: `List<LinkedTreeMap>` no lugar da lista esperada;
- **Primitivo ausente no JSON**: campo `int` recebe `0`, `boolean` recebe `false` — sem aviso de que a chave não veio;
- **`null` omitido**: por padrão campos nulos não aparecem no JSON — `serializeNulls()` no builder inverte.

## Profundidade

O `Gson` é thread-safe e barato de criar, mas o padrão é um campo `static final` reutilizado. `GsonBuilder` é o configurador: `setPrettyPrinting()`, `serializeNulls()`, `setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)` para `snake_case` global. Para datas, `setDateFormat("dd/MM/yyyy")` cobre o caso comum. O modelo de árvore (`JsonElement`/`JsonObject`/`JsonArray`) é o equivalente do `JsonNode` do Jackson, navegado com `JsonParser.parseString` — Gson também tem API de streaming (`JsonReader`/`JsonWriter`), usada em leitura de arquivos grandes.
