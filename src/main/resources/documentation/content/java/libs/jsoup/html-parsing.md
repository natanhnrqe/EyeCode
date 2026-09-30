---
id: java/libs/jsoup/html-parsing
title: Jsoup
type: api
summary: Parse e manipulação de HTML com Jsoup — parse, seleção via seletores CSS, extração de texto e atributos e sanitização segura com Jsoup.clean.
level: beginner
duration: 7
officialDocs:
  label: jsoup
  url: https://jsoup.org
related:
  - java/libs/okhttp/http-client
  - java/jdk/java.lang/string
  - java/jdk/java.util/list
---

> [!INFO] Jsoup faz o trabalho de HTML no Java: **parseia** HTML (mesmo malformado), **seleciona** elementos com seletores CSS, extrai **texto e atributos** e **sanitiza** HTML de usuário com `Jsoup.clean` — o antídoto contra XSS.

## Visão geral

```java
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

Document doc = Jsoup.parse("""
        <html><head><title>Página</title></head>
        <body><h1>Título</h1><a href="/sobre">Sobre</a></body></html>
        """);

doc.title();                          // "Página"
doc.select("h1").first().text();      // "Título"
doc.selectFirst("a").attr("href");    // "/sobre"
```

O parse produz um `Document` navegável (DOM próprio da biblioteca) e o parser **corrige** marcação incompleta — tags não fechadas são fechadas automaticamente.

## Dependência

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.3</version>
</dependency>
```

```groovy
implementation 'org.jsoup:jsoup:1.18.3'
```

Biblioteca única, sem dependências transitivas.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `parse(String html)` | `Document` | parseia HTML corrigindo a marcação |
| `connect(String url)` | `Connection` | builder para baixar com opções (`timeoutMs`, headers) |
| `connect(url).get()` | `Document` | baixa a URL e parseia (o antigo `Jsoup.parse(URL, int)` foi descontinuado desde o 1.18 — use sempre `connect`) |
| `select(String cssQuery)` | `Elements` | seleciona elementos via CSS |
| `selectFirst(String cssQuery)` | `Element` | primeiro match ou `null` |
| `getElementById(String id)` | `Element` | elemento com o id dado |
| `getElementsByTag(String tag)` | `Elements` | elementos com a tag dada |
| `Element.text()` | `String` | texto plano do elemento |
| `Element.attr(String)` | `String` | valor do atributo (`""` se inexistente) |
| `Element.html()` | `String` | HTML interno do elemento |
| `Document.title()` | `String` | texto da tag `<title>` |
| `Jsoup.clean(String, Safelist)` | `String` | remove HTML inseguro (anti-XSS) |

## Parse e estrutura

```java
Document doc = Jsoup.parse("<div><p>um<p>dois</div>");

doc.select("p").size();        // 2 — o parser fechou o primeiro <p>
doc.select("div > p").text();  // "um dois"
```

Armadilha: o parse **corrige** marcação malformada em vez de falhar — Jsoup é para HTML real (e inconsistente), não para validar estrutura estritamente.

## Seleção com CSS

```java
Document doc = Jsoup.parse(html);

doc.select("a[href]");               // links com href
doc.select("#menu li.ativo");        // id + classe via CSS
doc.select("div.produto > span.preco");
Element primeiro = doc.selectFirst("a[href]");   // null se nada casa
```

Armadilha: `selectFirst` retorna **`null`** quando nenhum elemento casa — cheque antes de chamar `.text()`/`.attr()` ou o `NullPointerException` aparece.

## Texto e atributos

```java
Element link = doc.selectFirst("a.link");

link.text();         // "Clique aqui" — texto plano
link.attr("href");   // "/sobre" — "" se o atributo não existe
link.html();         // "<strong>Clique</strong> aqui" — HTML interno
doc.title();         // título da página
```

Armadilha: `text()` **normaliza** espaços (colapsa e corta as pontas) — para o HTML cru use `html()` ou `outerHtml()`.

## Sanitização com Safelist

```java
import org.jsoup.safety.Safelist;

String sujo = "<p>Ok</p><script>alert(1)</script><a href='javascript:x()'>link</a>";

Jsoup.clean(sujo, Safelist.basic());  // "<p>Ok</p><a rel=\"nofollow\">link</a>"
```

A `Safelist.basic()` permite texto e links comuns, remove o `<script>` e derruba o `href` com protocolo perigoso. Há presets prontos (`simpleText`, `basic`, `basicWithImages`, `relaxed`, `none`) e customização com `addTags`/`addAttributes`.

Armadilha: `clean` remove tudo que não está na **Safelist** — nunca sanitize HTML de usuário com regex manual; para renderizar o que o usuário digitou, `clean` antes.

## Armadilhas comuns

> [!WARNING] Nunca renderize HTML vindo do usuário sem passar por `Jsoup.clean(texto, Safelist...)` — `<script>` e `onerror=` atravessam direto e viram XSS. Guarde o HTML limpo (a safelist remove scripts, atributos de evento e protocolos perigosos) e renderize só o que passou.

**`attr` não lança para atributo inexistente:** devolve `""` — para saber se o atributo existe use `hasAttr(String)`.

**Parser tolerante demais:** HTML malformado não falha no parse — o documento sai "corrigido"; confie no `select` sobre o documento, não na estrutura original.

**Download sem timeout:** `Jsoup.connect(url).get()` sem timeout pode travar a thread — `.timeout(5000)` antes de `.get()`.

## Profundidade

`Jsoup.connect(url)` é um cliente HTTP embutido: `.userAgent("...")`, `.header(...)`, `.cookie(...)`, `.timeout(millis)` e `.get()` (síncrono) ou `.execute()` (com `Connection.Response` completo, status e headers). A `Elements` é uma lista navegável — `stream()`, `eachText()`, `forEach` — e a seleção aceita a maioria dos seletores CSS (combinadores, atributos, pseudo-classes básicas). Internamente o parser é um DOM próprio (`Document`/`Element`/`Node`) — sem depender do `org.w3c.dom`.
