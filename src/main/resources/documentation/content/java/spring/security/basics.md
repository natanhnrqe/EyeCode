---
id: java/spring/security/basics
title: "Spring Security: básico"
type: guide
summary: Como proteger uma API com SecurityFilterChain, usuários em memória e autenticação HTTP Basic ou form login no Spring Boot 3.
level: intermediate
duration: 10
officialDocs:
  label: Spring Security Reference
  url: https://docs.spring.io/spring-security/reference/
related:
  - java/spring/boot-basics
  - java/spring/web/rest-controller
  - java/spring/web/validation
---

> [!INFO] Spring Security protege sua aplicação com um `SecurityFilterChain`: declara quais endpoints exigem autenticação, quem são os usuários e como eles provam identidade — HTTP Basic para APIs, form login para aplicações web.

## Cenário

Você tem `/api/admin/**` que só deve ser acessível por administradores autenticados, e `/api/public/**` aberto a todos. Sem segurança, qualquer um chama os endpoints sensíveis.

## Passo a passo

### Passo 1 — Dependência

`spring-boot-starter-security` no `pom.xml`. Ao iniciar, o Spring Security protege **tudo** por padrão com um formulário de login gerado e um usuário `user` com senha aleatória exibida no console — isso mostra que está ativo, mas é inútil em produção.

### Passo 2 — SecurityFilterChain

```java
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});              // ou .formLogin(...)
        return http.build();
    }
}
```

### Passo 3 — Usuários em memória

```java
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Bean
UserDetailsService usuarios() {
    UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("senha123"))
            .roles("ADMIN")
            .build();
    UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("123456"))
            .roles("USER")
            .build();
    return new InMemoryUserDetailsManager(admin, user);
}

@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Passo 4 — Autorização por método

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/usuarios/{id}")
    public void excluir(@PathVariable Long id) { ... }
}
```

Ative com `@EnableMethodSecurity` na configuração.

## Como funciona

O Spring Security funciona como uma cadeia de **filtros Servlet** que interceptam cada request antes do controller. Se o endpoint exige autenticação e o usuário não está autenticado, o filtro redireciona para login ou retorna 401. Se autenticado mas sem autoridade (role), retorna 403. O `Authentication` atual fica disponível em `SecurityContextHolder.getContext().getAuthentication()` — nunca passe `Principal` como parâmetro se puder injetar `Authentication` direto.

## Variações

- **Form login:** `.formLogin(form -> form.loginPage("/login"))` com página própria.
- **JWT/stateless:** para APIs REST modernas, troque Basic por filtro JWT (`oauth2ResourceServer`).
- **Banco de usuários:** implemente `UserDetailsService` com JPA em vez de memória.
- **CSRF:** para APIs stateless, desabilite com `.csrf(csrf -> csrf.disable())` — mas entenda as consequências (sem cookie de sessão = menos necessário).

## Armadilhas

> [!WARNING] Nunca grave senha em texto puro no `UserDetailsService`. O `BCryptPasswordEncoder` faz hashing lento e salgado — o Spring compara sem precisar descriptografar.

- Ordem de autorização importa: `permitAll()` antes de `anyRequest().authenticated()`, não depois.
- Padrão de path importa: `/api/admin` casa apenas a rota exata, já `/api/admin/**` cobre todos os subcaminhos.
- Sem `passwordEncoder()` o primeiro login falha com "There is no PasswordEncoder mapped for the id null" — a comparação assume hash, nunca texto puro.

## Profundidade

Spring Security implementa o padrão **Chain of Responsibility** com filtros. O conceito central é `Authentication` (quem é você, credenciais, autoridades) e `Authorization` (o que você pode fazer). RFC 7617 padroniza Basic Auth (`Authorization: Base64(user:pass)`); o Spring lê esse header, verifica contra o `UserDetailsService` e cria um `SecurityContext`. Form login usa sessão server-side; Basic é stateless por requisição — cada um com trade-offs que determinam a arquitetura da aplicação.
