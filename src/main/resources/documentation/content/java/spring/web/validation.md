---
id: java/spring/web/validation
title: Validação
type: guide
summary: Como validar requests com jakarta.validation (@NotNull, @Size), ativar com @Valid no controller e devolver erros amigáveis com @ExceptionHandler.
level: beginner
duration: 8
officialDocs:
  label: Spring Framework — Validation
  url: https://docs.spring.io/spring-framework/reference/core/validation.html
related:
  - java/spring/web/rest-controller
  - java/spring/web/exception-handling
  - java/spring/boot-basics
  - java/jdk/java.lang/annotations
---

> [!INFO] O Spring Boot valida requests com **jakarta.validation**: você anota os campos do DTO (`@NotBlank`, `@Size`, `@Email`), marca o parâmetro do controller com `@Valid` e recebe um `MethodArgumentNotValidException` automático quando algo falha — que você converte em resposta 400 amigável com `@ExceptionHandler`.

## Cenário

Seu `POST /usuarios` aceita qualquer JSON — nome vazio, e-mail inválido, idade negativa só explodem mais tarde, no banco ou na regra de negócio. Validar na **fronteira** (controller) falha cedo, com mensagem clara para o cliente da API.

## Passo a passo

### Passo 1 — Adicione o starter de validação

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Passo 2 — Anote o DTO

```java
import jakarta.validation.constraints.*;

public record UsuarioRequest(

        @NotBlank(message = "nome é obrigatório")
        @Size(min = 2, max = 80, message = "nome deve ter entre 2 e 80 caracteres")
        String nome,

        @NotBlank(message = "e-mail é obrigatório")
        @Email(message = "e-mail inválido")
        String email,

        @NotNull(message = "idade é obrigatória")
        @Min(value = 18, message = "idade mínima é 18")
        Integer idade
) {}
```

### Passo 3 — Ative a validação no controller

```java
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRequest criar(@Valid @RequestBody UsuarioRequest request) {
        return request;  // aqui só chega dado válido
    }
}
```

### Passo 4 — Formate o erro com @ExceptionHandler

```java
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ValidacaoExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratar(MethodArgumentNotValidException ex) {
        var erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage(),
                        (a, b) -> a));
        return Map.of("status", 400, "erros", erros);
    }
}
```

`POST /usuarios` com `{"nome": "", "email": "x"}` responde:

```json
{"status":400,"erros":{"nome":"nome deve ter entre 2 e 80 caracteres","email":"e-mail inválido","idade":"idade é obrigatória"}}
```

## Como funciona

O starter traz o **Hibernate Validator**, a implementação de referência da especificação Jakarta Bean Validation. Quando o parâmetro tem `@Valid`, o Spring MVC executa as constraints **antes** de chamar seu método; em falha, lança `MethodArgumentNotValidException` carregando um `BindingResult` com cada `FieldError` (campo + mensagem). O `@RestControllerAdvice` intercepta a exceção globalmente e você formata a resposta como quiser.

## Variações

**Validação de parâmetros individuais** — `@Validated` na classe + constraint no parâmetro:

```java
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping("/{id}")
    public String porId(@PathVariable @Min(1) long id) {
        return "ok";
    }
}
```

**Grupos de validação** (`@NotNull(groups = OnCreate.class)`) — regras diferentes para criação e atualização.

**Constraint customizada** — crie uma anotação + `ConstraintValidator` quando uma regra (ex.: CPF válido) não existe pronta.

## Armadilhas

> [!WARNING] Sem o `spring-boot-starter-validation`, as anotações `@NotBlank`/`@Size` são **ignoradas em silêncio**: o request inválido atravessa até o service. O starter web do Boot 3 **não** inclui validação por padrão — adicione explicitamente.

- **Confundir `jakarta.validation` com `javax.validation`**: no Spring Boot 3 só `jakarta.*` funciona;
- **`@Valid` esquecido no parâmetro**: as constraints do DTO não disparam — o método executa com dados inválidos;
- **`@NotNull` em `Integer` ausente**: sem ele, idade `null` passa pelo `@Min` (constraints numéricas ignoram `null`) — sempre combine as duas;
- **Confiar só na validação da API**: valide também na entidade/banco (`@Column(nullable = false)`) — a validação do controller protege a fronteira, não outras entradas (jobs, mensageria).

## Profundidade

A Jakarta Bean Validation declara constraints como **metadados** (anotações) separados da lógica — seu código de negócio fica limpo e as regras ficam declarativas e reusáveis (o mesmo DTO validado no controller pode ser revalidado antes de persistir). O Spring integra a especificação no pipeline do MVC; para validação fora do HTTP (parâmetros de métodos de `@Service`), existe também o `MethodValidationPostProcessor`, que aplica as mesmas constraints em qualquer bean.
