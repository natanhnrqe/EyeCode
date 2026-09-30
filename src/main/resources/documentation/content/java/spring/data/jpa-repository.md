---
id: java/spring/data/jpa-repository
title: JPA Repository
type: guide
summary: Como usar JpaRepository com consultas derivadas, @Query, paginação e ordenação — sem escrever SQL para as operações comuns.
level: intermediate
duration: 10
officialDocs:
  label: Spring Data JPA Reference
  url: https://docs.spring.io/spring-data/jpa/reference/
related:
  - java/spring/boot-basics
  - java/spring/data/transactions
  - java/jdk/java.util/optional
---

> [!INFO] `JpaRepository` é a interface do Spring Data JPA que gera automaticamente implementações para CRUD, consultas por nome de método (`findByEmail`), paginação e ordenação — você só declara a assinatura e o Spring escreve o JPQL/SQL por você.

## Cenário

Você tem uma entidade `Usuario` mapeada com JPA/Hibernate e precisa de operações comuns de banco: salvar, buscar por id, listar por status, paginar uma listagem. Escrever DAOs com `EntityManager` para cada caso é boilerplate; `JpaRepository` elimina isso.

## Passo a passo

### Passo 1 — Entidade JPA

```java
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String nome;
    private boolean ativo;
}
```

### Passo 2 — O repositório

```java
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByAtivoTrueAndNomeContainingIgnoreCase(String trecho);
}
```

O Spring gera a implementação em tempo de execução; **nenhuma classe é necessária**. Basta injetar:

```java
@Service
public class UsuarioService {
    private final UsuarioRepository repo;
    public UsuarioService(UsuarioRepository repo) { this.repo = repo; }
}
```

### Passo 3 — Consulta custom com @Query

Quando o nome derivado fica longo demais ou a consulta é complexa:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("select u from Usuario u where u.ativo = true and u.nome like %:trecho%")
    List<Usuario> buscarAtivos(@Param("trecho") String trecho);

    @Query(value = "select * from usuarios where email like %?1%", nativeQuery = true)
    List<Usuario> porDominio(String dominio);
}
```

### Passo 4 — Paginação e ordenação

```java
Page<Usuario> pagina = repo.findAll(
        PageRequest.of(0, 20, Sort.by("nome").ascending()));

pagina.getContent();    // lista da página atual
pagina.getTotalPages(); // total de páginas
pagina.hasNext();       // próxima existe?
```

## Como funciona

Na inicialização, o Spring Data examina as interfaces que estendem `Repository` (ou seus subtipos) e cria proxies dinâmicos. Para métodos derivados, um parser lê o nome (`findBy + Campo + Operador`) e gera JPQL; para métodos com `@Query`, o JPQL é validado na partida contra o metamodelo. O CRUD padrão (`save`, `findById`, `findAll`, `deleteById`) vem da `SimpleJpaRepository`, implementação concreta interna. Tudo roda dentro da transação gerenciada pelo `@Transactional` do serviço que chama.

## Variações

- **`CrudRepository`:** se não precisa de paginação/JPA-specific, use a interface mínima.
- **Projections:** retorne uma interface/DTO em vez da entidade (`Optional<UsuarioResumo> findByEmail(...)`).
- **Modifying queries:** `@Modifying @Query("update Usuario u set u.ativo = false where ...")` para updates deletes em massa.
- **`List` vs `Optional` vs nulo:** prefira `Optional` quando a consulta pode não achar nada; evita `NullPointerException`.

## Armadilhas

> [!WARNING] O método derivado só "enxerga" campos mapeados com acesso a **propriedade** (getter) ou **campo** (`@Id` no campo). Misturar os dois no mesmo tipo pode gerar consulta com o nome errado.

- Retornar `List` numa consulta com paginação: a assinatura precisa ser `Page<T> findAll(Pageable)` — `List` ignora o `Pageable`.
- `findByNome` retorna **todos** os usuários com aquele nome; se quiser primeiro apenas, use `findFirstByNome` / `findTop1ByNome`.
- Consulta nativa (`nativeQuery = true`) perde portabilidade de banco e não valida na partida.
- `count()` em repositório herdado pode gerar `select count(*)` caro em tabelas enormes — considere `existsById` para verificar existência.

## Profundidade

O Spring Data JPA usa o padrão **Repository** da DDD como fachada de coleção: o repositório é a fronteira entre o domínio e a persistência. Por baixo, usa o **Proxy** e a **query method strategy**: `PartTree` parseia o nome do método em cláusulas (`findBy` = where, `OrderBy` = order, `Top/First` = limit). O resultado é uma solução que cobre ~80 % das consultas de uma aplicação típica sem SQL manual, enquanto `@Query` cuida do restante (queries com JPQL inválida falham na inicialização, não em produção).
