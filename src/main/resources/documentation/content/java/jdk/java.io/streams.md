---
id: java/jdk/java.io/streams
title: Streams de arquivo
type: api
summary: Bytes e texto com InputStream/OutputStream, buffers com BufferedInputStream, conversão com charset via Reader/Writer e o ciclo seguro com try-with-resources.
level: intermediate
duration: 9
officialDocs:
  label: API java.io.InputStream
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/InputStream.html
related:
  - java/jdk/java.io/file
  - java/jdk/java.lang/string
  - java/jdk/java.lang/exceptions
---

> [!INFO] Um *stream* de `java.io` é uma **sequência de bytes**: `InputStream` lê, `OutputStream` escreve. Os wrappers `Buffered*` reduzem as chamadas ao sistema operacional, e `Reader`/`Writer` fazem a ponte bytes↔texto com um **charset** explícito. O ciclo correto é sempre try-with-resources.

## Visão geral

Quando `Files.readString` não basta — arquivo grande, formato binário, rede, pipe —, o nível de baixo é o par `InputStream`/`OutputStream`. Eles trabalham com bytes crus; tudo acima (buffer, texto, objetos) é construído envolvendo um stream:

```java
try (InputStream in = Files.newInputStream(Path.of("foto.jpg"));
     OutputStream out = Files.newOutputStream(Path.of("copia.jpg"))) {
    in.transferTo(out);     // copia tudo por dentro
}
```

A regra de ouro: **todo stream é um recurso** — abre, usa, fecha. E o jeito garantido de fechar é try-with-resources.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `read()` | `int` | 1 byte (0–255) ou **-1** no fim |
| `read(byte[] buffer)` | `int` | lê até encher; devolve quanto leu ou -1 |
| `readAllBytes()` (Java 9) | `byte[]` | tudo na memória |
| `transferTo(OutputStream)` (Java 9) | `long` | copia tudo direto |
| `write(int b)` | `void` | escreve 1 byte |
| `write(byte[] b[, int off, int len])` | `void` | escreve um trecho do buffer |
| `flush()` | `void` | empurra os bytes pendentes |
| `close()` | `void` | fecha e libera o recurso |
| `new BufferedInputStream(InputStream)` | `InputStream` | envolve com buffer de leitura |
| `new BufferedOutputStream(OutputStream)` | `OutputStream` | envolve com buffer de escrita |
| `new InputStreamReader(InputStream, Charset)` | `Reader` | bytes → texto com charset |
| `new OutputStreamWriter(OutputStream, Charset)` | `Writer` | texto → bytes com charset |
| `Files.newInputStream/newOutputStream(Path)` | `InputStream`/`OutputStream` | abre stream de arquivo (NIO) |

## Bytes: entrada e saída

```java
try (InputStream in = Files.newInputStream(Path.of("dados.bin"));
     OutputStream out = Files.newOutputStream(Path.of("dados-copia.bin"))) {

    byte[] buffer = new byte[8192];
    int lidos;
    while ((lidos = in.read(buffer)) != -1) {
        out.write(buffer, 0, lidos);   // escreve SÓ o que foi lido de verdade
    }
}
```

Esse laço é o protótipo de toda leitura de bytes: `read` devolve -1 no fim (não lança exceção) e pode encher o buffer **parcialmente** — por isso o `write` usa o retorno, nunca `buffer.length`. Para arquivos pequenos, `in.readAllBytes()` devolve um `byte[]` direto.

## Buffers

Sem buffer, cada `read()` vira uma chamada ao sistema operacional — lenta. `BufferedInputStream` lê blocos de 8 KB de uma vez e serve a partir da memória; `BufferedOutputStream` acumula a escrita e descarrega em blocos:

```java
Path origem = Path.of("video.mp4");
Path destino = Path.of("video-copia.mp4");

try (InputStream in = new BufferedInputStream(Files.newInputStream(origem));
     OutputStream out = new BufferedOutputStream(Files.newOutputStream(destino))) {
    in.transferTo(out);
}   // close descarrega o buffer do out na ordem certa
```

> [!WARNING] Buffer de escrita não descarregado = dados perdidos. Se o programa morrer sem `close()`/`flush()`, os últimos bytes nunca chegam ao disco — o try-with-resources é o que garante o `close` no fim.

## Texto: Reader e Writer

Bytes não são texto: para ler caracteres é preciso um charset. `InputStreamReader`/`OutputStreamWriter` fazem a conversão em fluxo, e `BufferedReader` adiciona `readLine`:

```java
try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
             Files.newInputStream(Path.of("notas.txt")), StandardCharsets.UTF_8));
     var writer = new java.io.OutputStreamWriter(
             Files.newOutputStream(Path.of("saida.txt")), StandardCharsets.UTF_8)) {

    String linha;
    while ((linha = reader.readLine()) != null) {   // null = fim do arquivo
        writer.write(linha.toUpperCase());
        writer.write("\n");
    }
}

// e o atalho moderno para o arquivo de texto inteiro:
String tudo = Files.readString(Path.of("notas.txt"));   // UTF-8
```

## Armadilhas comuns

> [!WARNING] Esquecer `close()` num `OutputStream` **perde dados**: o que ficou no buffer do `BufferedOutputStream` nunca chega ao disco — além de vazar o handle, bloqueando arquivos no Windows. Try-with-resources não é estilo, é correção; `flush()` manual só é necessário quando o conteúdo precisa ficar visível antes do fim.

**`read()` devolve -1 no fim:** streams não lançam exceção de fim de arquivo — o laço precisa testar o -1, senão lê byte "fantasma" para sempre.

**`read(byte[])` pode devolver menos:** mesmo com 8 KB pedidos, o disco/soquete pode entregar 512 bytes agora. O retorno manda — quem usa `buffer.length` grava lixo.

**Texto sem charset explícito:** `new String(bytes)` e `new InputStreamReader(in)` usam o charset padrão da JVM (UTF-8 desde o Java 18 — mas nem todo servidor roda Java 18+). Passe `StandardCharsets.UTF_8` sempre.

**Não confunda os streams:** `java.io.InputStream` (bytes em sequência) e `java.util.stream.Stream` (pipeline de dados) são coisas diferentes com nome parecido — o primeiro é IO, o segundo é processamento.

## Profundidade

As classes de `java.io` formam um sistema de **decoradores**: `FileInputStream` toca o disco, `BufferedInputStream` envolve e adiciona chunk de memória, `DataInputStream` adiciona leitura de tipos — a composição define o comportamento. `Reader`/`Writer` são a camada de texto: cada um carrega um `CharsetDecoder`/`Encoder` que transforma bytes em `char` segundo o charset. `Files.newInputStream` (NIO.2) é a fábrica moderna preferida a `new FileInputStream(...)`. Tudo aqui é bloqueante — cada `read` espera o dado chegar; para alta concorrência existe o mundo `java.nio.channels`, com I/O não bloqueante.
