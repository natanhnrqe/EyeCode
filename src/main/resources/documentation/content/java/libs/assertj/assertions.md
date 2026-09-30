---
id: java/libs/assertj/assertions
title: AssertJ
type: api
summary: Assertions fluentes com AssertJ — assertThat com matchers de coleção, string e objeto, mensagens de falha melhores e SoftAssertions para coletar todas as falhas.
level: beginner
duration: 8
officialDocs:
  label: AssertJ — Fluent assertions
  url: https://assertj.github.io/doc/
related:
  - java/junit/first-test
  - java/libs/mockito/mocking
  - java/jdk/java.util/list
---

> [!INFO] AssertJ traz **assertions fluentes** ao JUnit: `assertThat(valor).isEqualTo(...)` encadeia como frase, os matchers de coleção e string são ricos (`containsExactly`, `startsWith`) e a mensagem de falha mostra **exatamente** o que deu errado — com diff.

## Visão geral

```java
import static org.assertj.core.api.Assertions.assertThat;

// JUnit: assertEquals(3, nomes.size()); assertTrue(nomes.contains("Ana"));
assertThat(nomes).hasSize(3).contains("Ana");
```

O `assertThat(valor)` devolve um objeto de assertion **do tipo do valor**: para `String` você ganha `startsWith`/`containsIgnoringCase`, para `List` você ganha `containsExactly`/`hasSize` — o autocomplete mostra o que existe para cada tipo.

## Dependência

```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.27.0</version>
    <scope>test</scope>
</dependency>
```

```groovy
testImplementation 'org.assertj:assertj-core:3.27.0'
```

Em `test`/`testImplementation` — assertions nunca vão para produção.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `assertThat(actual)` | assertion do tipo | ponto de entrada — uma por tipo |
| `isEqualTo(Object)` | assertion | igual por `equals` |
| `isNull()` / `isNotNull()` | assertion | checagem de null |
| `isTrue()` / `isFalse()` | assertion | checagem de `Boolean` |
| `hasSize(int)` | assertion | tamanho de coleção, mapa ou string |
| `contains(Object...)` | assertion | contém todos os itens (ordem livre) |
| `containsExactly(Object...)` | assertion | contém **na ordem exata** |
| `containsExactlyInAnyOrder(Object...)` | assertion | mesmos itens, ordem livre |
| `doesNotContain(Object...)` | assertion | não contém nenhum |
| `startsWith(String)` / `endsWith(String)` | assertion | prefixo / sufixo (String) |
| `containsIgnoringCase(String)` | assertion | contém ignorando caixa (String) |
| `matches(String regex)` | assertion | casa com a regex (String) |
| `extracting(String)` | assertion | extrai campo/propriedade de cada item |
| `hasFieldOrPropertyWithValue(String, Object)` | assertion | campo do objeto com o valor dado |

## Assertions básicas

```java
String nome = "Ana";
int idade = 28;

assertThat(nome).isEqualTo("Ana").isNotEqualTo("Bia");
assertThat(nome).isNotNull().isNotBlank();
assertThat(idade).isPositive().isGreaterThan(18);
```

Armadilha: `isEqualTo` usa `equals` — para objetos sem `equals` implementado, use `usingRecursiveComparison()`.

## Matchers de String

```java
assertThat("EyeCode 2026")
        .startsWith("Eye")                // true
        .contains("Code")                 // true
        .containsIgnoringCase("eyecode")  // true
        .matches("EyeCode \\d{4}")        // true
        .hasSize(12);
```

Armadilha: `matches` casa a string **inteira** com a regex — para trecho, use `containsPattern(String)`.

## Matchers de coleção

```java
List<String> nomes = List.of("Ana", "Bia", "Clara");

assertThat(nomes)
        .hasSize(3)
        .contains("Bia")
        .containsExactly("Ana", "Bia", "Clara")
        .doesNotContain("Zeca");

assertThat(nomes)
        .extracting(String::toUpperCase)
        .containsExactly("ANA", "BIA", "CLARA");
```

Armadilha: `containsExactly` exige **ordem exata** — para mesmos itens em qualquer ordem, use `containsExactlyInAnyOrder`.

## Soft assertions

```java
import org.assertj.core.api.SoftAssertions;

SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(nome).isEqualTo("Ana");
    softly.assertThat(idade).isGreaterThan(18);
    softly.assertThat(nomes).hasSize(3);
});
```

As assertions padrão **param na primeira falha** — com `assertSoftly`, todas as falhas são coletadas e reportadas juntas, útil para validar vários campos de uma resposta.

Armadilha: soft assertions mascaram qual assertion falhou primeiro e pesam mais — use no teste de mapeamento completo, não em todos os testes.

## Armadilhas comuns

> [!WARNING] `assertThat(valor)` sozinho **não verifica nada** — sem um matcher no fim (`.isEqualTo(...)`, `.hasSize(...)`), a linha compila, não falha e o teste passa sempre. O autocomplete devolve a assertion, mas o matcher é o que de fato testa.

**`isEqualTo` em objetos sem `equals`:** falha sempre que as instâncias diferem — use `usingRecursiveComparison().isEqualTo(...)` para comparar campo a campo.

**Encadeamento incompleto:** `.extracting(...)` sem um matcher depois não testa nada — encadeie `containsExactly` (ou equivalente) no fim.

## Profundidade

A vantagem central é a **mensagem de falha**: `expected: 42 but was: 43`, com diff visual para coleções e strings — você diagnostica sem reexecutar com debug. Existe o estilo BDD via `BDDAssertions.then(valor)` com os mesmos matchers, módulos específicos (`assertj-guava`, `assertj-db`, Reactive Streams) e o `usingRecursiveComparison` para objetos sem `equals`. Para a maioria dos testes, `assertThat` + os matchers do tipo cobrem tudo — a leitura fluente vale mais do que a contagem de matchers.
