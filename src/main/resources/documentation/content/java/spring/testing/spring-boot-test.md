---
id: java/spring/testing/spring-boot-test
title: Testes com Spring Boot
type: guide
summary: Como testar com @SpringBootTest, @WebMvcTest + MockMvc e @DataJpaTest — escolhendo o slice certo para cada tipo de teste.
level: intermediate
duration: 9
officialDocs:
  label: Spring Boot — Testing
  url: https://docs.spring.io/spring-boot/reference/testing/
related:
  - java/junit/first-test
  - java/spring/web/rest-controller
  - java/spring/data/jpa-repository
  - java/libs/mockito/mocking
---

> [!INFO] Spring Boot oferece "fatias" de teste: `@SpringBootTest` sobe o contexto inteiro (integração), `@WebMvcTest` sobe só a camada web (rápido para controllers), `@DataJpaTest` sobe só o JPA com banco em memória. Escolher a fatia certa = testes rápidos e confiáveis.

## Cenário

`UsuarioController` delega para `UsuarioService` + `UsuarioRepository`. Você quer: (1) testar a API HTTP sem subir o servidor completo; (2) testar o repositório com banco real; (3) testar tudo integrado num cenário crítico de negócio.

## Passo a passo

### Passo 1 — Teste do controller (@WebMvcTest + MockMvc)

```java
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean UsuarioService service;

    @Test
    void deveRetornar404QuandoNaoExiste() throws Exception {
        when(service.porId(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/usuarios/1"))
           .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarUsuario() throws Exception {
        Usuario salvo = new Usuario(1L, "Ana");
        when(service.criar(any())).thenReturn(salvo);
        mvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.nome").value("Ana"));
    }
}
```

### Passo 2 — Teste do repositório (@DataJpaTest)

```java
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired UsuarioRepository repo;

    @Test
    void deveEncontrarPorEmail() {
        repo.save(new Usuario("ana@exemplo.com"));
        Optional<Usuario> achado = repo.findByEmail("ana@exemplo.com");
        assertTrue(achado.isPresent());
    }
}
```

Usa H2 em memória por padrão; cada teste roda em transação com rollback automático.

### Passo 3 — Teste de integração completo (@SpringBootTest)

```java
@SpringBootTest
class IntegracaoTest {

    @Autowired UsuarioController controller;

    @Test
    void fluxoCompleto() {
        var criado = controller.criar(new CriarUsuarioRequest("Ana"));
        var buscado = controller.porId(criado.id());
        assertEquals("Ana", buscado.getBody().nome());
    }
}
```

Sobe o contexto inteiro; use para cenários críticos e de fumaça (smoke test).

### Passo 4 — Mock de dependências externas

```java
@SpringBootTest
class IntegracaoComMockTest {

    @MockitoBean PagamentoGateway gateway; // substitui o bean real
}
```

## Como funciona

`@WebMvcTest` registra apenas o controller especificado + infra de MVC (sem serviços/repositórios). `@DataJpaTest` registra só componentes JPA + configura um banco em memória. `@SpringBootTest` roda a aplicação inteira como faria `main()` (com `webEnvironment` opcional). O `MockitoBean` substitui beans no contexto de teste por mocks do Mockito.

## Variações

- **`TestRestTemplate`:** para testes de integração com servidor real (`webEnvironment = RANDOM_PORT`).
- **`@Import`:** trazer beans extras no slice (`@WebMvcTest @Import(SecurityConfig.class)`).
- **`Testcontainers`:** banco real (PostgreSQL) em vez de H2 quando as diferenças de SQL importam.
- **`MockMvc` fluente:** `andExpect(jsonPath("$.erros[0]").value("campo: obrigatório"))` para validar corpos de erro.

## Armadilhas

> [!WARNING] `@WebMvcTest` não carrega `@Service` e `@Repository` — injetar um `UsuarioService` sem `@MockitoBean` causa o erro de "no qualifying bean". A fatia existe justamente para isolar a camada web.

- `@SpringBootTest` com banco em memória e Flyway ativo pode falhar – desabilite migrations no perfil de teste (`spring.flyway.enabled=false`).
- `MockMvc` injetado pelo `@WebMvcTest` já valida status, headers e corpo (`andExpect(header().string(...))`); `MockMvcBuilders` só é necessário para montar o MockMvc manualmente (`standaloneSetup`).
- Testes `@DataJpaTest` com relacionamento LAZY causam `LazyInitializationException` fora da transação — acrescente `@Transactional` ou ajuste o fetch.

## Profundidade

A **pirâmide de testes** (Martin Fowler) recomenda: muitos unitários rápidos, alguns de integração, poucos end-to-end. As anotações do Spring Boot implementam essa pirâmide: use `@WebMvcTest` para a regra HTTP/JSON (visão de cliente), `@DataJpaTest` para a regra de query/mapping (visão de banco), e reserve `@SpringBootTest` para fluxos completos que métricas de aceite exigem. Cada fatia otimiza o startup ao carregar só os componentes necessários — o que deixa o ciclo de feedback rápido.
