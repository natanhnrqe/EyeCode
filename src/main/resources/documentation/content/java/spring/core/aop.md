---
id: java/spring/core/aop
title: AOP
type: concept
summary: Programação Orientada a Aspectos no Spring: como @Aspect, pointcut e advice criam proxies para funcionalidades transversais como logging, cache e transação.
level: intermediate
duration: 9
officialDocs:
  label: Spring Framework — AOP
  url: https://docs.spring.io/spring-framework/reference/core/aop.html
related:
  - java/spring/core/dependency-injection
  - java/spring/data/transactions
  - java/spring/boot/autoconfiguration
---

> [!INFO] AOP (Aspect-Oriented Programming) extrai código repetido que atravessa várias camadas — logging, segurança, cache, transações — para um local centralizado. No Spring, é implementado por proxies: o bean que você injeja é um wrapper que executa lógica extra antes/depois do método real.

## Por que existe

Funcionalidades "transversais" (cross-cutting) são aquelas que apareceriam em quase todo método se escritas manualmente: logar entrada/saída, medir tempo, verificar permissão, abrir/fechar transação. Mover esse código para **aspectos** deixa os métodos de negócio limpos e garante consistência (não depende do desenvolvedor lembrar de logar).

## Anatomia da sintaxe

Um aspecto de logging:

```java
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* com.exemplo.service.*.*(..))")
    public void logAntes(JoinPoint jp) {
        System.out.println("Chamando: " + jp.getSignature());
    }

    @AfterReturning(
        pointcut = "execution(* com.exemplo.service.*.*(..))",
        returning = "result")
    public void logDepois(Object result) {
        System.out.println("Retornou: " + result);
    }
}
```

Pointcut com anotação customizada:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditar {
    String valor();
}

@Around("@annotation(auditar)")
public Object auditar(ProceedingJoinPoint pjp, Auditar auditar) throws Throwable {
    System.out.println("Auditando: " + auditar.valor());
    return pjp.proceed();
}
```

Uso: `@Auditar("transferencia")` no método que deve ser auditado.

## Como funciona

O Spring cria **proxies dinâmicos** ao redor dos beans. Quando um service é injetado em outro, o recebido é o proxy — não o objeto original. Chamadas ao método passam primeiro pelos advices (lógica do aspecto) e depois chegam à implementação real.

Por isso AOP funciona apenas em chamadas **externas** ao bean:

- `this.metodo()` dentro da própria classe → **não é interceptado**;
- Métodos `private`/`final` → a palavra-chave impede o proxy dinâmico (JDK) ou a subclasse (CGLib) de sobrescrever o comportamento.

Spring AOP usa dois tipos de proxy:
- **JDK dynamic proxy** — quando o bean implementa interface
- **CGLib** — quando não há interface (o proxy é uma subclasse)

## Exemplos

Medição de tempo com `@Around` (o advice mais poderoso):

```java
@Around("execution(* com.exemplo..*Service.*(..))")
public Object medirTempo(ProceedingJoinPoint pjp) throws Throwable {
    long inicio = System.currentTimeMillis();
    try {
        return pjp.proceed();
    } finally {
        long duracao = System.currentTimeMillis() - inicio;
        System.out.println(pjp.getSignature() + ": " + duracao + "ms");
    }
}
```

## Armadilhas

> [!WARNING] AOP por si só não cria transação nem segurança. O `@Transactional` do Spring é implementado **com** AOP — mas AOP sozinho é só o mecanismo de interceptação.

- Chamada interna (`this.metodo()`) não passa pelo proxy → advice não executa. Solução: injetar o próprio bean (`@Lazy` ou `ObjectProvider`), ou reestruturar.
- Métodos `final` não podem ser interceptados (CGLib não consegue sobrescrever). Spring Boot 2+ usa CGLib por padrão mesmo com interfaces — mas `final` continua sendo limite.
- Pointcut muito amplo (`execution(* com.exemplo..*(..))`) captura getters, toString, hashCode — gera ruído e overhead.
- A ordem de execução quando há múltiplos aspectos não é garantida — use `@Order` para controlar.

## Profundidade

AOP formaliza o conceito de **separação de preocupações** (separation of concerns): o código de negócio ("o que") fica isolado do código de infraestrutura ("como/onde registrar"). O modelo AspectJ é mais completo (tem weaving em compile/load-time), mas Spring AOP (proxy-based) cobre ~95 % dos casos e é mais simples de configurar. A escolha de proxy JDK vs CGLib depende de haver interface; Spring Boot 2 mudou o padrão para CGLib porque subclasses evitam problemas de casting quando o código chamador espera o tipo concreto.
