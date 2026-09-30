---
id: java/jdk/java.io/file
title: Arquivos com Path/Files
type: api
summary: Arquivos com java.nio.file — Path, Files.readString/writeString, cópia, exclusão e a varredura da árvore com Files.walk, tudo dentro de try-with-resources.
level: beginner
duration: 9
officialDocs:
  label: API java.nio.file.Files
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html
related:
  - java/jdk/java.io/streams
  - java/jdk/java.lang/exceptions
  - java/jdk/java.util/stream
---

> [!INFO] A dupla moderna de arquivos é **`Path`** (o caminho, só isso) + **`Files`** (as operações, todas estáticas): `Files.readString` lê o arquivo inteiro em uma linha, `Files.walk` varre a árvore como um `Stream` — e todo `Stream` de diretório exige try-with-resources.

## Visão geral

Ler e gravar arquivo no Java moderno raramente pede mais que duas classes: `Path` representa o caminho (exista ou não o arquivo) e `Files` executa tudo o que toca o disco — ler, gravar, copiar, apagar, listar, medir. Chega de `File` legado com métodos confusos e de abrir streams na mão para texto simples.

```java
Path config = Path.of("config", "app.properties");   // só o caminho

if (Files.exists(config)) {
    String conteudo = Files.readString(config);      // arquivo inteiro, UTF-8
    Files.writeString(Path.of("config", "backup.properties"), conteudo);
}
```

A divisão de papéis é sempre a mesma: `Path` é o endereço; `Files` é quem visita.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Path.of(String... segmentos)` | `Path` | cria um caminho (não toca o disco) |
| `p.resolve(String)` / `p.getParent()` / `p.getFileName()` | `Path` | navega no caminho |
| `Files.exists(Path)` | `boolean` | o arquivo existe? |
| `Files.size(Path)` | `long` | tamanho em bytes |
| `Files.isDirectory/isRegularFile(Path)` | `boolean` | metadados básicos |
| `Files.readString(Path[, Charset])` (Java 11) | `String` | arquivo inteiro em texto (UTF-8) |
| `Files.writeString(Path, CharSequence[, OpenOption...])` (Java 11) | `Path` | grava texto (cria ou trunca) |
| `Files.readAllLines(Path[, Charset])` | `List<String>` | todas as linhas |
| `Files.lines(Path[, Charset])` | `Stream<String>` | linhas como stream (lazy; fechar) |
| `Files.list(Path)` | `Stream<Path>` | conteúdo do diretório (não desce; fechar) |
| `Files.walk(Path[, int profundidade])` | `Stream<Path>` | árvore inteira (fechar) |
| `Files.copy(Path, Path[, CopyOption...])` | `Path` | copia |
| `Files.move(Path, Path[, CopyOption...])` | `Path` | move/renomeia |
| `Files.delete(Path)` / `deleteIfExists(Path)` | `void`/`boolean` | apaga |
| `Files.createDirectories(Path)` | `Path` | cria a pasta e as faltantes acima |

## Caminho e leitura

```java
Path dados = Path.of("dados").resolve("usuarios.csv");   // dados/usuarios.csv

Files.exists(dados);              // o arquivo existe?
Files.size(dados);                // tamanho em bytes
Files.readString(dados);           // String — o arquivo inteiro (UTF-8)
Files.readAllLines(dados);        // List<String> — uma String por linha

try (java.util.stream.Stream<String> linhas = Files.lines(dados)) {
    long comEmail = linhas.filter(l -> l.contains("@")).count();
}   // o Stream de linhas é lazy e precisa ser fechado
```

Para arquivos pequenos e médios, `readString`/`readAllLines` resolvem; o `Files.lines` só compensa quando o arquivo é grande o bastante para não caber na memória de uma vez.

## Escrita e cópia

```java
Path nota = Path.of("log", "hoje.txt");
Files.createDirectories(nota.getParent());      // cria 'log/' se faltar

Files.writeString(nota, "abertura do log\n");    // cria ou trunca
Files.writeString(nota, "mais uma linha\n",
        java.nio.file.StandardOpenOption.APPEND);   // soma ao final

Files.copy(nota, Path.of("log", "copia.txt"),
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
Files.move(Path.of("log", "copia.txt"), Path.of("log", "arquivada.txt"));
Files.delete(Path.of("log", "arquivada.txt"));   // some — ou lança exceção
```

O padrão do `writeString` é `CREATE` + `TRUNCATE_EXISTING`: gravar de novo começa do zero. Com `APPEND` você acumula — as duas opções trocam o comportamento por completo.

## Varredura com list e walk

```java
Path raiz = Path.of("projeto");

try (java.util.stream.Stream<Path> arquivos = Files.walk(raiz)) {
    long javas = arquivos
            .filter(p -> p.toString().endsWith(".java"))
            .count();
    System.out.println(javas + " arquivos .java na árvore");
}

try (java.util.stream.Stream<Path> nivel1 = Files.list(raiz)) {
    nivel1.forEach(System.out::println);   // só o primeiro nível
}
```

`Files.walk` desce recursivamente (aceita limite de profundidade); `Files.list` mostra apenas o primeiro nível. Ambos devolvem `Stream` lazy — o que nos leva direto às armadilhas.

## Armadilhas comuns

> [!WARNING] `Files.lines`, `Files.list` e `Files.walk` mantêm um **descritor de diretório aberto** enquanto o `Stream` vive. Fora do try-with-resources o handle vaza — e no Windows o diretório fica bloqueado (nem dá para apagá-lo). Sempre: `try (var s = Files.walk(...)) { ... }`.

**Arquivo inteiro na memória:** `readString`/`readAllLines` carregam tudo — um log de 2 GB vira uma `String` de 2 GB. Para arquivos grandes, use `Files.lines` ou `BufferedReader` e processe aos poucos.

**`delete` não perdoa:** `Files.delete` lança `NoSuchFileException` (uma `IOException`) se o arquivo não existir; quando a ausência é esperada, use `Files.deleteIfExists`, que devolve `boolean`.

**Escrever por cima sem querer:** o padrão do `writeString` **trunca** o arquivo existente. Só `APPEND` preserva o conteúdo.

**Charset:** as APIs de `Files` usam UTF-8 por padrão — ótimo —, mas `new String(bytes)` e `InputStreamReader` antigos usam o charset padrão da JVM. Ao misturar APIs, seja explícito com `StandardCharsets.UTF_8`.

## Profundidade

`java.nio.file` é a NIO.2 (JSR 203, JDK 7): `Path` é uma interface de caminho imutável, e `Files` é a fachada que delega ao `FileSystemProvider` — o mesmo mecanismo que monta arquivos `.zip` como filesystem navegável. `walk` é lazy (depth-first, vendo um diretório por vez), o que explica o handle aberto: o percurso só termina quando o `Stream` é consumido ou fechado. `readString`/`writeString` chegaram no Java 11 e padronizaram UTF-8, encerrando a era do charset implícito nas operações de arquivo.
