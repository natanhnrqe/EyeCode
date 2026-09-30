---
id: java/libs/lombok/annotations
title: Lombok: anotações
type: api
summary: As anotações que eliminam boilerplate — @Getter/@Setter para acesso, @Builder para construção, @Value para imutável e por que @Data exige cuidado.
level: beginner
duration: 8
officialDocs:
  label: Project Lombok — Features
  url: https://projectlombok.org/features/
related:
  - java/libs/lombok/setup
  - java/jdk/fundamentos/records
  - java/jdk/fundamentos/classes
---

> [!INFO] Cada anotação do Lombok gera um grupo de métodos na compilação: **@Getter/@Setter** para acessores, **@Builder** para construção fluida, **@Value** para classe imutável e **@Data** para o pacote completo — o mais conveniente e o mais perigoso, porque inclui `equals`/`hashCode` automáticos.

## Visão geral

O Lombok é instalado via dependência + plugin (ver Lombok: instalação) — aqui o foco é **o que cada anotação gera**. As cinco do dia a dia:

| Anotação | Gera |
|----------|------|
| `@Getter` / `@Setter` | acessores por campo (ou por classe) |
| `@Builder` | padrão builder fluido |
| `@Value` | classe imutável: campos `private final`, acessores, `equals`/`hashCode`/`toString` |
| `@Data` | `@Getter` + `@Setter` + `@RequiredArgsConstructor` + `@ToString` + `@EqualsAndHashCode` |

```java
@Getter @Setter
public class Usuario {
    private String nome;
    private int idade;
}
// compila com getNome(), setNome(String), getIdade(), setIdade(int)...
```

## Dependência

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.36</version>
    <scope>provided</scope>
</dependency>
```

```groovy
compileOnly 'org.projectlombok:lombok:1.18.36'
annotationProcessor 'org.projectlombok:lombok:1.18.36'
```

Mesma dependência da instalação — as anotações vivem no JAR único do Lombok (`org.projectlombok:lombok`).

## Uso básico

`@Getter`/`@Setter` por campo ou por classe:

```java
public class Conta {

    @Getter                      // só o getter — saldo é somente leitura
    private BigDecimal saldo;

    @Getter @Setter              // os dois — apelido muda
    private String apelido;
}

// métodos gerados em uso:
conta.getSaldo();
conta.setApelido("novo");
```

Armadilha: `@Getter`/`@Setter` no nível da **classe** gera para todos os campos — incluir um campo que devia ser somente leitura vira setter de graça; prefira anotação por campo quando o controle importa.

## Construção com @Builder

```java
@Builder
public record Notificacao(String titulo, String corpo, boolean urgente) {}

Notificacao n = Notificacao.builder()
        .titulo("Deploy concluído")
        .corpo("v2.1 em produção")
        .urgente(false)
        .build();
```

Em classe comum, `@Builder` exige construtor com todos os argumentos (gerado por `@AllArgsConstructor` ou escrito à mão). Valores padrão entram com `@Builder.Default`:

```java
@Builder
public class Config {
    @Builder.Default
    private int timeout = 30;      // sem isso, o campo inicia 0 no builder
    private String host;
}
```

Armadilha: campo com inicializador (`= 30`) **sem** `@Builder.Default` é ignorado pelo builder — o valor vira `0`/`null` silenciosamente.

## Imutável com @Value e @Data

`@Value` cria a classe imutável completa:

```java
@Value
public class Ponto {
    int x;
    int y;
}
// campos private final, getX()/getY(), equals/hashCode/toString — sem setters
```

`@Data` é o pacote completo para classes mutáveis:

```java
@Data
public class Pedido {
    private long id;
    private String status;
}
// getters+setters, toString, equals/hashCode, construtor para campos final obrigatórios
```

O equivalente moderno: `record` (Java 16+) faz o mesmo que `@Value` sem Lombok — para classes imutáveis simples, prefira `record` (ver Profundidade).

## Armadilhas comuns

> [!WARNING] `@Data` em **entidades JPA** é a armadilha clássica: o `equals`/`hashCode`/`toString` gerados incluem todos os campos — incluindo relacionamentos `@OneToMany` — e tocar neles fora de transação dispara `LazyInitializationException` ou carrega a coleção inteira. Em entidades, gere `equals`/`hashCode` só com o id (`@EqualsAndHashCode(of = "id")`) e evite `@Data`.

- **`@Data` + herança**: o `@EqualsAndHashCode` gerado não considera a superclasse sem `callSuper = true` — dois objetos filhos "iguais" com pais diferentes dão `true`;
- **Builder sem `@Builder.Default`**: inicializador de campo é ignorado — valor padrão silencioso;
- **`@Data` em entidade JPA**: `LazyInitializationException` no `equals`/`toString` fora de transação;
- **`@Value` com coleção mutável**: os campos ficam `final`, mas a **coleção** continua mutável — `getItens().add(...)` funciona; embrulhe com `List.copyOf` no construtor.

## Profundidade

O Lombok gera código por transformação de AST na compilação — os métodos existem no `.class`, nunca no fonte. `@Value` é quase um `record` semântico: a diferença é que `record` declara o contrato no fonte (visível, padrão da linguagem, sem dependência), enquanto `@Value` o gera na compilação. `@Data` vale quando a classe é mutável, tem muitos campos e você controla o uso de `equals` — em entidades JPA e classes com estado pesado, avalie sempre o custo. Para `@Data` com controle fino, o combo `@Getter @ToString @EqualsAndHashCode @RequiredArgsConstructor` cobre o mesmo terreno sem os setters.
