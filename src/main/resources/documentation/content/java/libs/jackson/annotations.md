---
id: java/libs/jackson/annotations
title: Jackson: anotações
type: api
summary: Controle o que vai para o JSON — @JsonProperty renomeia campos, @JsonIgnore esconde, @JsonFormat molda datas e @JsonInclude limpa nulos e vazios.
level: beginner
duration: 7
officialDocs:
  label: Jackson Annotations — GitHub
  url: https://github.com/FasterXML/jackson-annotations
related:
  - java/libs/jackson/basic
  - java/jdk/java.time/localdate
  - java/jdk/java.lang/annotations
---

> [!INFO] As anotações do Jackson (pacote `com.fasterxml.jackson.annotation`) personalizam a serialização **sem código extra**: você decide o nome de cada campo no JSON, o que fica de fora, como datas aparecem e quando `null` deve ser omitido — tudo declarado na própria classe.

## Visão geral

Por padrão, Jackson serializa **todas** as propriedades com o nome do campo Java. As quatro anotações do dia a dia mudam isso por propriedade:

| Anotação | O que faz |
|----------|-----------|
| `@JsonProperty("nome_json")` | renomeia a propriedade no JSON |
| `@JsonIgnore` | exclui a propriedade da serialização |
| `@JsonFormat(shape, pattern)` | molda o formato (datas, números) |
| `@JsonInclude(NON_NULL)` | omite a propriedade quando `null`/vazio |

```java
record Usuario(
        @JsonProperty("nome_completo") String nome,
        @JsonIgnore String senha,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate nascimento) {}
```

## Dependência

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>2.18.3</version>
</dependency>
```

```groovy
implementation 'com.fasterxml.jackson.core:jackson-annotations:2.18.3'
```

Na prática você quase nunca declara o artefato sozinho: `jackson-databind` (e o starter web do Spring Boot) já traz as anotações transitivamente.

## Uso básico

```java
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(
        new Usuario("Ana Silva", "abc123", LocalDate.of(1996, 4, 12)));

// {"nome_completo":"Ana Silva","nascimento":"12/04/1996"}
// "senha" não apareceu — @JsonIgnore funcionou
```

- `@JsonProperty` vale nos dois sentidos: na leitura, o JSON precisa usar `nome_completo` para preencher `nome`;
- `@JsonIgnore` esconde também na leitura — o campo nunca é preenchido a partir do JSON.

Armadilha: Jackson junta campo + getter/setter numa propriedade só, mas anotações conflitantes (`@JsonIgnore` num, `@JsonProperty` no outro) dividem a propriedade em leitura e escrita separadas — não misture.

## Formatos de data

```java
@JsonFormat(pattern = "dd/MM/yyyy")
private LocalDate nascimento;

@JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime criadoEm;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
private BigDecimal preco;
```

Sem `@JsonFormat`, datas serializam como **timestamp** (número) a menos que o módulo JSR-310 esteja registrado — no Spring Boot ele já vem; fora dele, adicione `jackson-datatype-jsr310` e registre com `mapper.registerModule(new JavaTimeModule())`.

Armadilha: `pattern` usa o alfabeto do `SimpleDateFormat` — `mm` é minuto, `MM` é mês; trocar as letras muda o valor silenciosamente.

## Nulos e vazios

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
private String apelido;       // omitido quando null

@JsonInclude(JsonInclude.Include.NON_EMPTY)
private List<String> tags;    // omitido quando null OU vazia

// nível de classe — vale para todos os campos
@JsonInclude(JsonInclude.Include.NON_NULL)
record Config(String host, Integer porta, String senha) {}
```

Valores possíveis: `ALWAYS` (padrão), `NON_NULL`, `NON_EMPTY` (null, `""` e coleções vazias) e `NON_DEFAULT`. Armadilha: `NON_NULL` no nível de classe esconde **qualquer** campo nulo — se o consumidor do JSON espera todas as chaves presentes, ele quebra com a ausência.

## Combinando anotações

```java
public record Produto(
        @JsonProperty("nome_produto") String nome,
        @JsonProperty("preco_final")
        @JsonFormat(shape = JsonFormat.Shape.NUMBER) BigDecimal preco,
        @JsonIgnore String custoInterno,
        @JsonInclude(JsonInclude.Include.NON_NULL) String descricao) {}
```

As anotações são independentes e se combinam por propriedade: renomear + formatar, esconder + incluir — cada uma cuida de um aspecto da mesma propriedade.

## Armadilhas comuns

> [!WARNING] `@JsonIgnore` não é segurança: o campo continua existindo na classe e pode vazar por **outro caminho** — getter público dedicado, `@JsonProperty` em outro acessor ou serialização de fallback. Para dados sensíveis, não confie apenas em anotações; remova o valor do objeto antes de serializar.

- **Conflito de anotações**: `@JsonIgnore` no getter + `@JsonProperty` no campo dividem a propriedade em leitura e escrita separadas (Jackson 2.6+) — comportamento difícil de depurar;
- **`pattern` com letras trocadas**: `dd/MM/yyyy` vs `dd/mm/yyyy` — mês e minuto se confundem silenciosamente;
- **`NON_NULL` de classe quebra consumidores**: API que espera chaves fixas recebe JSON menor;
- **Esquecer o módulo de datas fora do Boot**: `LocalDate` vira timestamp ou objeto verboso — registre `JavaTimeModule`.

## Profundidade

As anotações são a camada declarativa sobre o `ObjectMapper` — tudo que elas fazem também é possível via configuração programática, mas anotações por propriedade vencem em legibilidade. Para casos repetidos, `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` renomeia **toda** a classe para `snake_case` de uma vez. Mixins (`mapper.addMixIn(Classe.class, Mixin.class)`) aplicam anotações a classes de terceiros que você não pode editar — a anotação vive numa classe sua e é "empréstada" para a original.
