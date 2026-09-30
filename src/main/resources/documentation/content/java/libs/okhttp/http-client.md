---
id: java/libs/okhttp/http-client
title: OkHttp
type: api
summary: Cliente HTTP do Java com OkHttp — OkHttpClient, GET e POST com JSON, timeouts por cliente e interceptors para autenticação e logging.
level: intermediate
duration: 8
officialDocs:
  label: OkHttp
  url: https://square.github.io/okhttp/
related:
  - java/libs/jackson/basic
  - java/spring/web/rest-controller
  - java/spring/boot-basics
---

> [!INFO] OkHttp é um **cliente HTTP** eficiente do Java: requisições síncronas (`execute()`) e assíncronas (`enqueue()`), pooling de conexões, HTTP/2 e **interceptors** para cruzar o código de autenticação e log. A receita: um cliente compartilhado, `Request.Builder` e resposta fechada com try-with-resources.

## Visão geral

```java
OkHttpClient client = new OkHttpClient();

Request request = new Request.Builder()
        .url("https://api.exemplo.com/users/42")
        .build();

try (Response response = client.newCall(request).execute()) {
    int status = response.code();           // 200
    String json = response.body().string(); // corpo — uma única vez
}
```

Três peças: `OkHttpClient` (o cliente com pool e configuração), `Request` (método, URL, headers, corpo) e `Response` (status, headers, corpo). O cliente é thread-safe e deve ser **reutilizado** — criar um por requisição desperdiça o pool de conexões.

## Dependência

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

```groovy
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

O OkHttp 4.x é escrito em Kotlin, mas a API Java (`okhttp3.*`) é completa — seu projeto não precisa de código Kotlin.

## Uso básico

GET com a API síncrona:

```java
Request request = new Request.Builder()
        .url("https://api.exemplo.com/users")
        .header("Accept", "application/json")
        .get()
        .build();

try (Response response = client.newCall(request).execute()) {
    if (!response.isSuccessful()) {
        throw new IOException("Falha: HTTP " + response.code());
    }
    String corpo = response.body().string();
}
```

POST com corpo JSON:

```java
MediaType json = MediaType.parse("application/json; charset=utf-8");
String payload = """
        {"nome": "Ana", "idade": 28}
        """;

RequestBody body = RequestBody.create(payload, json);
Request request = new Request.Builder()
        .url("https://api.exemplo.com/users")
        .post(body)
        .build();
```

Para objetos reais, junte o OkHttp com Jackson: `ObjectMapper.writeValueAsString(usuario)` produz o payload — OkHttp transporta, Jackson serializa.

Armadilha: no OkHttp 4.x a ordem dos parâmetros de `RequestBody.create` **inverteu** em relação ao 3.x — agora o conteúdo vem primeiro (`create(payload, json)`).

## Timeouts e cliente compartilhado

```java
OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .callTimeout(Duration.ofSeconds(60))
        .build();
```

`connectTimeout` é o handshake TCP/TLS, `readTimeout` é o silêncio máximo entre bytes, `callTimeout` é o teto da chamada inteira (DNS + conexão + leitura). Sem configuração, os padrões são 10s para conexão e leitura — e `callTimeout` sem limite. Estouro vira `SocketTimeoutException` (uma `IOException`).

Armadilha: timeouts são por **cliente** — um cliente novo por requisição também perde pool e interceptors; construa um e compartilhe.

## Interceptors

```java
OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(chain -> {
            Request original = chain.request();
            Request autenticado = original.newBuilder()
                    .header("Authorization", "Bearer " + token())
                    .build();
            return chain.proceed(autenticado);
        })
        .build();
```

Um `Interceptor` vê e pode alterar cada requisição antes de sair (`chain.proceed`) — o lugar certo para token de autenticação, header padrão e log de requisições. `addInterceptor` roda uma vez por chamada; `addNetworkInterceptor` roda também em redirecionamentos e retries.

Armadilha: exceção lançada dentro do interceptor derruba a chamada inteira — trate o erro ou deixe `proceed` seguir.

## Armadilhas comuns

> [!WARNING] `response.body().string()` só pode ser chamada **uma vez** — o corpo é um stream de passada única e a segunda chamada devolve `""`. Guarde o resultado em uma variável; e sempre feche o `Response` com try-with-resources para não vazar conexão do pool.

**Cliente por requisição:** `new OkHttpClient()` em cada chamada perde o pool de conexões e os interceptors — construa **um** cliente e compartilhe.

**`IOException` sem tratamento:** `execute()` lança `IOException` em falha de conexão, DNS e timeout — trate na chamada, não deixe estourar na thread.

**Callback em thread do dispatcher:** `enqueue(Callback)` roda assíncrono — o `onResponse` executa em uma thread do dispatcher, não na sua; sincronize antes de tocar estado compartilhado.

## Profundidade

A eficiência vem do **pooling interno**: conexões TCP/TLS são reutilizadas para o mesmo host, e o HTTP/2 multiplexa várias chamadas em uma conexão. Há cache de resposta com `new Cache(diretorio, tamanho)` (respeita os headers HTTP de cache), `retryOnConnectionFailure(true)` por padrão para falhas de rota e um `Dispatcher` com `maxRequests` para controlar o paralelismo do `enqueue`. Para testes, o `MockWebServer` (mesmo grupo Square) simula o servidor sem rede real.
