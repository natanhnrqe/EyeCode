---
id: java/spring/web/exception-handling
title: Tratamento de Exceções
type: guide
summary: Como centralizar o tratamento de erros de uma API REST com @RestControllerAdvice, @ExceptionHandler e ProblemDetail no Spring Boot 3.
level: intermediate
duration: 8
officialDocs:
  label: Spring Framework — Error Responses
  url: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
related:
  - java/spring/web/rest-controller
  - java/spring/web/validation
  - java/jdk/java.lang/exceptions
---

> [!INFO] Você não deve espalhar `try/catch` pelos controllers: com `@RestControllerAdvice` + `@ExceptionHandler` toda exceção da aplicação vira uma resposta HTTP consistente em um único lugar — disponível desde o Spring 4, e desde Spring Boot 3 no padrão **RFC 7807 (`ProblemDetail`)**.

## Cenário

Sua API de pedidos lança `PedidoNaoEncontradoException` quando o id não existe. Sem tratamento central, o Spring devolve um 500 genérico com stack trace — horrível para o cliente. Você quer `404` com um corpo JSON padronizado, e `400` com a lista de campos inválidos quando a validação falha.

## Passo a passo

### Passo 1 — Exceção de domínio

```java
public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(Long id) {
        super("Pedido " + id + " não encontrado");
    }
}
```

### Passo 2 — O advice global

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(PedidoNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Pedido não encontrado");
        return pd;
    }
}
```

O corpo da resposta segue **RFC 7807** (`application/problem+json`): `type`, `title`, `status`, `detail`, `instance`.

### Passo 3 — Validar entrada e capturar erros de validação

Com `jakarta.validation` no DTO (`@NotBlank`, `@Positive`) + `@Valid` no controller, o Spring lança `MethodArgumentNotValidException`:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail validacao(MethodArgumentNotValidException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Dados inválidos");
    pd.setProperty("erros", ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList());
    return pd;
}
```

### Passo 4 — Herdar o comportamento padrão com ResponseEntityExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    // trata automaticamente 404 de path, 405 método, 415 content-type etc.
}
```

## Como funciona

Quando um método de controller lança exceção, o `DispatcherServlet` procura o `@ExceptionHandler` mais específico — primeiro dentro do próprio controller, depois em todos os `@RestControllerAdvice` registrados. O retorno passa pelo mesmo `HttpMessageConverter` de uma resposta normal (Jackson para `ProblemDetail` → JSON). `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`: métodos retornam o corpo direto, não um nome de view.

## Variações

- **Advice por pacote ou anotação:** `@RestControllerAdvice("com.exemplo.pedidos")` limita o escopo.
- **`ErrorResponseException`:** no próprio código você pode `throw new ErrorResponseException(HttpStatus.BAD_REQUEST, ...)` sem criar exceção própria.
- **Resposta customizada:** retorne `ResponseEntity<MeuErro>` em vez de `ProblemDetail` se você tem um formato de erro legado.

## Armadilhas

> [!WARNING] Uma exceção que ocorre **fora** do controller (filtro, security, serialização) não passa pelo `@ControllerAdvice` — configure `spring.mvc.problemdetails.enabled=true` ou trate via `ErrorController`/filtros para cobrir essas rotas.

- Retornar `ProblemDetail` sem definir o content-type: OK por padrão, mas um `ResponseEntity<MyError>` misturado produz formatos inconsistentes entre endpoints.
- `@ControllerAdvice` (sem `Rest`) em aplicação REST: o retorno é interpretado como nome de view e dá 500.
- Engolir a exceção com `catch (Exception e)` e dar `200 OK` — o cliente nunca sabe que falhou.

## Profundidade

A RFC 7807 existe justamente para acabar com o caos de formatos de erro: cada API inventava o seu. O Spring Boot 3 adota `ProblemDetail` como modelo nativo (`org.springframework.http.ProblemDetail`), e a `ResponseEntityExceptionHandler` usa o mesmo formato para erros de MVC que antes eram um HTML ou estrutura ad-hoc. O resultado: o cliente pode parsear **um** formato para todos os erros da aplicação — do 404 ao 500.
