---
id: java/spring/boot-basics
title: Spring Boot: Primeiros passos
summary: O que é Spring Boot, por que ele elimina o boilerplate do Java web, e como um endpoint HTTP nasce do zero.
level: beginner
duration: 9
officialDocs:
  label: Spring Boot Reference Documentation
  url: https://docs.spring.io/spring-boot/reference/getting-started/introducing-spring-boot.html
related:
  - java/jdk/fundamentos/classes
  - java/junit/first-test
---

> [!INFO] Spring Boot é a forma moderna de criar aplicações Java: ele configura tudo que a Spring Framework faria você montar à mão (servidor, dependências, configuração) e deixa você escrever só o que é negócio.

## Por que existe

A Spring Framework é poderosa, mas tradicionalmente exigia muito **boilerplate**: XML de configuração, web.xml, dependências versionadas à mão, servidor separado para implantar. Spring Boot nasceu (2014) justamente para isso — **convenção sobre configuração**: você adiciona uma dependência, escreve uma classe, e a aplicação já sobe com servidor embutido.

Para times Java, isso significa o mesmo ecossistema robusto (DI, transações, segurança, data access) com o custo inicial perto de zero.

## Anatomia da sintaxe

Uma aplicação Spring Boot mínima:

```java
package com.exemplo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

`@SpringBootApplication` é um "atalho" que combina:

- `@Configuration` — esta classe define beans;
- `@EnableAutoConfiguration` — configura automaticamente o que achar no classpath;
- `@ComponentScan` — escaneia o pacote atual (e subpacotes) por componentes.

Um endpoint HTTP em três linhas:

```java
import org.springframework.web.bind.annotation.*;

@RestController
public class OlaController {

    @GetMapping("/ola")
    public String ola(@RequestParam(defaultValue = "mundo") String nome) {
        return "Olá, " + nome + "!";
    }
}
```

`GET http://localhost:8080/ola?nome=EyeCode` → `Olá, EyeCode!`

## Como funciona

Quando você roda a classe `main`:

1. O Spring cria o **ApplicationContext** (o contêiner de beans);
2. A autoconfiguração lê o classpath — achar `spring-boot-starter-web` ⇒ sobe **Tomcat embutido** na porta 8080;
3. Os `@RestController`/`@Service`/`@Repository` são registrados e mapeados;
4. Requests chegam ao Tomcat → Spring MVC → seu método do controller.

O **starter** é a unidade de dependência do Boot: `spring-boot-starter-web` traz Spring MVC + Tomcat + Jackson versionados e testados juntos — você não escolhe versões individualmente, o Boot gerencia.

A configuração vive em `src/main/resources/application.properties`:

```properties
server.port=9090
spring.application.name=eye-api
```

Qualquer propriedade pode vir de variável de ambiente (`${PORTA}`) — é assim que se configura em produção sem mudar código.

## Exemplos

API REST com JSON de verdade:

```java
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final List<Usuario> usuarios = new ArrayList<>();

    @GetMapping
    public List<Usuario> listar() {
        return usuarios;
    }

    @PostMapping
    public Usuario criar(@RequestBody Usuario usuario) {
        usuarios.add(usuario);
        return usuario;   // Jackson serializa para JSON automaticamente
    }
}

record Usuario(Long id, String nome) {}
```

Rodando a partir do terminal (com o Maven Wrapper):

```bash
./mvnw spring-boot:run
```

E testando:

```bash
curl -X POST http://localhost:8080/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana"}'
```

## Armadilhas comuns

> [!WARNING] `@GetMapping` num método que não está numa classe `@RestController` (ou `@Controller`) simplesmente nunca é registrado — o endpoint dá 404 sem nenhum erro no startup. Confira se o pacote do controller está sob o do `@SpringBootApplication`.

**Esquecer `@RequestBody`/`@PathVariable`:**

```java
@GetMapping("/usuarios/{id}")
public Usuario porId(@PathVariable Long id) { ... }   // sem @PathVariable → id vira null

@PostMapping
public void criar(@RequestBody Usuario u) { ... }     // sem @RequestBody → corpo ignorado
```

**Achar que `new` de um `@Service` injeta dependências:** só o **container** do Spring injeta. Objetos criados com `new` têm campos `null` — use injeção (`constructor injection` é a forma recomendada):

```java
@Service
public class PedidoService {
    private final UsuarioRepository repo;
    public PedidoService(UsuarioRepository repo) {  // o Spring chama isto
        this.repo = repo;
    }
}
```

## Profundidade

**IoC / DI:** o Inversion of Control Container cria e gerencia os objetos (beans) e injeta dependências — sua código pede "eu preciso de um Repository", não cria. Benefícios: desacoplamento, substituição fácil (mock em testes), ciclo de vida gerenciado.

**Estereótipos principais:** `@Component` (genérico), `@Service` (regra de negócio), `@Repository` (acesso a dados, com tradução de exceções), `@RestController` + `@RequestMapping` (HTTP). Escopo padrão é *singleton* — um bean por aplicação.

**Autoconfiguração por condições:** classes `@ConditionalOnClass`, `@ConditionalOnMissingBean` etc. decidem o que registrar. Quando você define seu **próprio** bean do mesmo tipo, o do Boot cede lugar (`@ConditionalOnMissingBean`) — "customização por sobreposição", a pedra de toque da filosofia.

**Camadas típicas:** `controller` (HTTP) → `service` (regras) → `repository` (dados), com DTOs na fronteira — nunca exponha entidade de banco direto na API.

**Spring Data JPA (próximo passo):**

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByNomeContaining(String trecho);
}
```

A implementação é gerada em tempo de execução a partir do nome do método — sem escrever SQL para as consultas comuns.

**Profiles:** `application-dev.properties` vs `application-prod.properties` + `@Profile("dev")` para alternar comportamentos por ambiente.
