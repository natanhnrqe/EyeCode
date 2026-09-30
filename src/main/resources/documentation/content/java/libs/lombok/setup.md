---
id: java/libs/lombok/setup
title: Lombok: instalação
type: guide
summary: Coloque o Lombok para funcionar — dependência com escopo provided, plugin no IDE e a primeira classe anotada compilando sem boilerplate.
level: beginner
duration: 6
officialDocs:
  label: Project Lombok
  url: https://projectlombok.org
related:
  - java/libs/lombok/annotations
  - java/spring/boot-basics
  - java/jdk/fundamentos/classes
---

> [!INFO] Lombok é um processador de anotações que **gera código na compilação** (getters, setters, construtores, `equals`...). Para funcionar você precisa de duas peças: a dependência com escopo `provided` no build e o plugin habilitado no IDE — sem o plugin, o IDE "não vê" os métodos gerados e marca tudo como erro.

## Cenário

Você abriu um projeto que usa Lombok e o IDE está cheio de erros: `getNome()` não existe, o construtor de 3 argumentos não existe — mas `mvn compile` passa. Ou o contrário: você quer começar a usar Lombok num projeto novo e não sabe onde a dependência entra. Este guia resolve os dois — a peça que o compilador precisa e a peça que o IDE precisa.

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

O escopo é `provided` (Maven) ou `compileOnly` (Gradle) porque o Lombok só é necessário **na compilação** — o código gerado entra direto no `.class`, e o JAR do Lombok não vai para o runtime nem para o pacote final.

## Passo a passo

### Passo 1 — Instale o plugin no IDE

- **IntelliJ IDEA**: o plugin Lombok já vem embutido nas versões recentes — confira em `Settings → Plugins` que está habilitado e ative `Settings → Build → Compiler → Annotation Processors → Enable annotation processing`;
- **Eclipse**: rode `java -jar lombok.jar` (o JAR baixado do site) e selecione a instalação do Eclipse — o instalador registra o processador;
- **VS Code**: instale a extensão "Language Support for Java" e garanta que o projeto Maven (com a dependência abaixo) está importado corretamente — o annotation processing vem do build.

### Passo 2 — Anote a primeira classe e compile

```java
import lombok.Getter;
import lombok.Setter;

public class Usuario {

    @Getter @Setter
    private String nome;

    @Getter @Setter
    private int idade;
}
```

Rode `mvn compile` — a compilação passa e o `Usuario.class` contém `getNome()`, `setNome(String)`, `getIdade()`, `setIdade(int)`. Use `javap target/classes/com/exemplo/Usuario.class` para conferir os métodos gerados.

### Passo 3 — Confirme que o IDE enxerga os métodos

No editor, digite `usuario.getNome()` — sem sublinhado vermelho e com autocompletar mostrando os métodos gerados. Se o IDE ainda marca erro, o plugin/annotation processing não está ativo: volte ao Passo 1 (o erro clássico no IntelliJ é "cannot find symbol" com annotation processing desligado).

## Como funciona

Lombok é um **annotation processor** que se gancho no compilador (`javac`) durante a fase de AST: ele lê as anotações (`@Getter`, `@Setter`...) e **modifica a árvore** antes da geração do bytecode — os métodos aparecem no `.class` como se você tivesse escrito à mão. Por isso:

- O código gerado **não existe no fonte** — o `.java` continua limpo;
- O IDE precisa do plugin porque ele analisa o fonte, não o `.class` — sem o plugin, não há como saber que `getNome()` existe;
- O JAR não vai para o runtime porque o trabalho terminou na compilação — por isso `provided`.

## Variações

**Spring Boot** — projetos gerados em `start.spring.io` já incluem Lombok com o escopo correto; você só precisa conferir o plugin no IDE:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Gradle (Kotlin DSL)**:

```kotlin
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}
```

**MapStruct + Lombok** — ao combinar os dois processadores, adicione também `lombok-mapstruct-binding` para eles conversarem na ordem certa.

## Armadilhas

> [!WARNING] Sem o plugin do IDE, o projeto **compila mas o editor marca erro em tudo** — o erro mais comum de quem começa com Lombok. Não "corrija" gerando os getters à mão: habilite o plugin e o annotation processing, e os erros desaparecem.

- **Annotation processing desligado**: `cannot find symbol: method getNome()` em IntelliJ — ative em `Build → Compiler → Annotation Processors`;
- **Escopo errado**: `compile` em vez de `provided` faz o JAR do Lombok ir para o pacote final — inofensivo, mas desnecessário;
- **Upgrade de JDK sem upgrade do Lombok**: versões antigas do Lombok quebram com JDKs novos (`NoSuchMethodError` na compilação) — atualize os dois juntos;
- **Código legado difícil de ler**: `java -jar lombok.jar delombok src -d src-delombok` expande o código gerado no fonte para leitura e migração.

## Profundidade

O Lombok funciona por **transformação de AST**, um truque que usa APIs internas do compilador — por isso cada JDK novo pode quebrar versões antigas, e por isso o projeto é mantido em sincronia com os releases do JDK. A alternativa "oficial" da linguagem são os **records** (imutáveis, com acessores e `equals`/`hashCode` de graça) — para classes imutáveis simples, prefira `record`; para classes mutáveis com builder e valores padrão, o Lombok ainda preenche a lacuna.
