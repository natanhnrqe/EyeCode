---
id: java/libs/jackson/basic
title: Jackson: básico
type: api
summary: Serialize e leia JSON com ObjectMapper e JsonNode — gravação e leitura de objetos em uma linha, navegação por árvore e configurações que evitam surpresas.
level: beginner
duration: 8
officialDocs:
  label: Jackson Databind — GitHub
  url: https://github.com/FasterXML/jackson-databind
related:
  - java/libs/jackson/annotations
  - java/spring/web/rest-controller
  - java/jdk/fundamentos/records
---

> [!INFO] Jackson é o conversor padrão entre objetos Java e JSON. Com **ObjectMapper** você grava (`writeValueAsString`) e lê (`readValue`) um objeto em uma linha; com **JsonNode** você navega um JSON desconhecido sem escrever classes para ele.

## Visão geral

Jackson é a biblioteca de referência para JSON no ecossistema Java — é ela que o Spring Boot usa por baixo de `@RestController`. O trabalho do dia a dia fica em duas classes do pacote `com.fasterxml.jackson.databind`: **`ObjectMapper`** (o motor — converte objeto → JSON e JSON → objeto) e **`JsonNode`** (a árvore — representa JSON genérico para leitura dinâmica).

```java
ObjectMapper mapper = new ObjectMapper();

record Usuario(String nome, int idade) {}

String json = mapper.writeValueAsString(new Usuario("Ana", 28));
// {"nome":"Ana","idade":28}

Usuario lido = mapper.readValue(json, Usuario.class);
// Usuario[nome=Ana, idade=28]
```

## Dependência

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.3</version>
</dependency>
```

```groovy
implementation 'com.fasterxml.jackson.core:jackson-databind:2.18.3'
```

No Spring Boot (`spring-boot-starter-web`), Jackson já vem incluído — declare a dependência só em projetos fora do Boot.

## Uso básico

O ciclo completo com um objeto comum (POJO ou `record`):

```java
ObjectMapper mapper = new ObjectMapper();

// objeto → JSON
String json = mapper.writeValueAsString(usuario);

// JSON → objeto (classe alvo explícita)
Usuario u = mapper.readValue(json, Usuario.class);

// JSON → lista de objetos (TypeReference para genéricos)
List<Usuario> usuarios = mapper.readValue(jsonArray, new TypeReference<List<Usuario>>() {});
```

`readValue` lança `JsonProcessingException` (checked) quando o JSON é inválido ou não casa com a classe — trate ou repasse.

## Gravando objetos

```java
ObjectMapper mapper = new ObjectMapper();

String json = mapper.writeValueAsString(new Usuario("Ana", 28));
// {"nome":"Ana","idade":28}

mapper.writeValue(new File("usuario.json"), usuario);   // direto no arquivo
```

Campos `null` aparecem por padrão no JSON; para omitir use `@JsonInclude` (ver Anotações). Pretty print entra com `INDENT_OUTPUT` (ver Configurando). Armadilha: campos sem getter visível não são serializados — a detecção padrão é por propriedades, não por campos.

## Lendo JSON em objetos

```java
String json = """
        {"nome":"Ana","idade":28,"email":"ana@x.com"}
        """;

// se Usuario tem só nome e idade...
Usuario u = mapper.readValue(json, Usuario.class);
// UnrecognizedPropertyException: "email" não existe na classe!
```

Por padrão Jackson **falha** ao encontrar propriedade desconhecida. Para tolerar:

```java
ObjectMapper tolerante = mapper.copy()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
Usuario u = tolerante.readValue(json, Usuario.class);   // ok, "email" ignorado
```

## Navegando com JsonNode

Para JSON de estrutura desconhecida (resposta de API de terceiro, configuração), leia como árvore:

```java
String resposta = """
        {"nome":"Ana","idade":28,"tags":["admin","dev"]}
        """;

JsonNode raiz = mapper.readTree(resposta);

raiz.get("nome").asText();         // "Ana"
raiz.path("idade").asInt();        // 28
raiz.path("endereco").path("cidade").asText();  // "" — path nunca estoura
raiz.has("tags");                  // true
raiz.get("tags").isArray();        // true
raiz.get("tags").get(0).asText();  // "admin"
```

Armadilha: `get()` devolve `null` para chave inexistente — chamar `.asText()` em cima estoura `NullPointerException`; use `path()`, que devolve um nó "missing" seguro.

## Configurando o ObjectMapper

```java
ObjectMapper mapper = JsonMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)                 // pretty print
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // tolerante
        .build();
```

> [!TIP] Crie **um** `ObjectMapper` por aplicação e reutilize: ele é thread-safe após configurado e a construção é cara. Um campo `static final` é o padrão usual.

## Armadilhas comuns

> [!WARNING] `readValue` exige que a classe alvo seja construtível: precisa de construtor sem argumentos (ou anotação/registro para outro construtor). Classes sem construtor vazio e sem configuração falham com `InvalidDefinitionException` — e a exceção só aparece em runtime, na primeira leitura.

- **Propriedade desconhecida**: `UnrecognizedPropertyException` por padrão — desligue `FAIL_ON_UNKNOWN_PROPERTIES` para APIs de terceiro que mudam o contrato;
- **`get()` + `asText()` em chave ausente**: `NullPointerException` — use `path()` para navegação defensiva;
- **Datas**: por padrão serializam como timestamp (número); use `@JsonFormat` ou o módulo `jackson-datatype-jsr310` para ISO-8601;
- **`JsonProcessingException` é checked**: esquecer de tratar não compila — decida entre tratar, relançar como runtime ou usar `readTree` para payloads tolerantes.

## Profundidade

O `ObjectMapper` tem três camadas de API: **streaming** (`JsonGenerator`/`JsonParser`, baixo nível e rápido), **tree** (`JsonNode`) e **databind** (objetos) — a maioria dos projetos usa só databind e tree. Após configurado, o mapper é imutável na prática; para variantes, `mapper.copy()` cria uma cópia ajustável sem afetar a original. No Spring Boot, o mapper autoconfigurado já registra o módulo JSR-310 (datas ISO-8601) e pode ser ajustado via `application.properties` (`spring.jackson.*`).
