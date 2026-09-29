---
id: java/junit/first-test
title: Seu primeiro teste com JUnit 5
summary: Por que testar, anatômica de um teste, os padrões Arrange-Act-Assert e as anotações que você usará todos os dias.
level: beginner
duration: 8
officialDocs:
  label: JUnit 5 User Guide
  url: https://junit.org/junit5/docs/current/user-guide/
related:
  - java/jdk/fundamentos/classes
  - java/spring/boot-basics
---

> [!INFO] Teste automatizado é código que **verifica** seu código: roda sozinho, falha alto quando algo quebra e te deixa refatorar com confiança. JUnit 5 é o padrão da JVM — anotações (`@Test`) e asserts prontos.

## Por que existe

"Funciona na minha máquina" é o momento em que o bug nasce. Teste automatizado transforma comportamento esperado em **especificação executável**: você muda o código hoje e descobre em milissegundos se algo quebrou — sem clicar em toda a interface. Em equipe, a suíte de testes é a documentação viva que nunca desatualiza.

## Anatomia da sintaxe

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraTest {

    @Test
    void somaDoisNumeros() {
        Calculadora calc = new Calculadora();      // Arrange (preparar)
        int resultado = calc.somar(2, 3);          // Agir
        assertEquals(5, resultado);                // Verificar
    }
}
```

Convenções:

- Classe de teste: nome da classe testada + `Test` (`Calculadora` → `CalculadoraTest`);
- Método: nome explicativo em português ou inglês (`somaDoisNumeros`, nunca `teste1`);
- `@Test` marca o método que o JUnit deve executar.

## Como funciona

O JUnit roda cada `@Test` em um **objeto novo da classe de teste** — os campos são recriados a cada teste, então um teste nunca suja o anterior. O ciclo é:

1. **Arrange** — prepare objetos e entradas;
2. **Act** — execute exatamente **uma** ação;
3. **Assert** — verifique o resultado com `assertEquals`, `assertTrue`, `assertThrows`...

Se qualquer assert falhar, o teste falha e o JUnit registra a diff (esperado × obtido). Um teste que não tem assert prova nada — evite-o ou use `assertDoesNotThrow`.

## Exemplos

Os asserts mais usados:

```java
import static org.junit.jupiter.api.Assertions.*;

assertEquals(5, calc.somar(2, 3));        // igual (com sobrecarga para delta em double)
assertTrue(lista.isEmpty());              // booleano verdadeiro
assertFalse(existe);                      // booleano falso
assertNull(valor);                        // é null
assertNotNull(objeto);                    // não é null
assertThrows(IllegalArgumentException.class, () -> calc.dividir(1, 0));
assertArrayEquals(new int[]{1, 2}, arrays);
assertAll(                                // vários asserts no mesmo teste
    () -> assertEquals(1, a),
    () -> assertEquals(2, b)
);
```

Teste de exceção — parte do contrato:

```java
@Test
void dividirPorZeroLancaExcecao() {
    Calculadora calc = new Calculadora();
    assertThrows(ArithmeticException.class, () -> calc.dividir(10, 0));
}
```

Agrupando por estado (`@BeforeEach`):

```java
class CarrinhoTest {
    Carrinho carrinho;

    @BeforeEach
    void preparar() {                      // roda antes de CADA @Test
        carrinho = new Carrinho();
        carrinho.adicionar("Café", 25.0);
    }

    @Test void totalInicial() { assertEquals(25.0, carrinho.total()); }

    @Test void removerZeraTotal() { carrinho.remover("Café"); assertEquals(0.0, carrinho.total()); }
}
```

Rodando com Maven: `mvn test` (ou `./mvnw test`).

## Armadilhas comuns

> [!WARNING] Teste que depende da ordem de execução é frágil — o JUnit **não** garante ordem entre métodos. Cada teste deve montar o próprio estado (`@BeforeEach`) e ser independente.

**Assert errado para double:**

```java
assertEquals(0.1 + 0.2, resultado);            // frágil: 0.30000000000000004
assertEquals(0.3, resultado, 1e-9);            // correto: tolerância explícita
```

**Comparar objetos com `==` no assert:** `assertEquals` já chama `equals` — mas para objetos novos sem `equals` sobrescrito, o padrão `equals` é por identidade. Sobrescreva `equals`/`hashCode` (ou use records).

**Testar método privado:** métodos privados são testados **indiretamente** pela interface pública. Se você precisa testá-lo direto, talvez o desenho da classe precise mudar, não o acesso.

## Profundidade

**JUnit 5 (Jupiter) é modular:** `junit-jupiter-api` (anotações/`Assertions`), `junit-jupiter-engine` (executor), `junit-platform-launcher`. Ferramentas (Maven Surefire/Gradle) montam isso automaticamente quando você declara a dependência de teste.

**Ciclo de vida completo (Jupiter):** `@BeforeAll`/`@AfterAll` (uma vez por classe, exige `static` ou `@TestInstance(PER_CLASS)`), `@BeforeEach`/`@AfterEach` (por teste), `@Disabled("motivo")` (pula), `@DisplayName("texto legível")` (nome bonito nos relatórios), `@Nested` (subclasses aninhadas para agrupar cenários), `@ParameterizedTest` + `@ValueSource`/`@CsvSource`/`@MethodSource` (mesmo teste com muitas entradas), `@Timeout(2)` (teste não pode demorar).

**Pirâmide de testes:** muitos unitários (rápidos, sem I/O) → alguns de integração (banco/API real em memória) → poucos E2E (fluxo completo). Unitário testa a regra; integração testa a costura; E2E testa o usuário.

**Test-Driven Development:** escrever o teste **antes** do código — o teste falha (vermelho), você implementa o mínimo para passar (verde), refatora. O teste vira a especificação executável do comportamento pedido.

**Cobertura:** `%` de linhas executadas (Maven: `mvn test jacoco:report`) é **diagnóstico, não meta** — 100% de cobertura com asserts fracos não protege nada. Leia os relatórios, mas julgue a qualidade pelos cenários cobertos: caminho feliz, erros, limites (`0`, vazio, máximo), exceções.

**Testes no Spring Boot:** `@SpringBootTest` sobe o contexto completo (pesado), `@WebMvcTest` testa só a camada web, `@DataJpaTest` com banco em memória — e `TestRestTemplate`/`MockMvc` exercitam endpoints sem deploy.
