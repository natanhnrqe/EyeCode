---
id: java/spring/boot/actuator
title: Actuator
type: guide
summary: Como adicionar o Spring Boot Actuator, expor endpoints de saúde (/actuator/health) e métricas, e controlar o que fica público em produção.
level: intermediate
duration: 7
officialDocs:
  label: Spring Boot Actuator — Production-ready Features
  url: https://docs.spring.io/spring-boot/reference/actuator/index.html
related:
  - java/spring/boot/application-properties
  - java/spring/boot/profiles
  - java/spring/boot-basics
---

> [!INFO] O Actuator adiciona **endpoints de operação** prontos à sua aplicação: saúde (`/actuator/health`), métricas (`/actuator/metrics`), info, beans, loggers. Basta uma dependência — e uma configuração consciente, porque expor tudo em produção é risco de segurança.

## Cenário

Sua API vai para produção e o time de plataforma pergunta: "qual o endpoint de health check para o Kubernetes?" e "como vemos métricas de memória?". Em vez de escrever controllers à mão, você adiciona o starter do Actuator e expõe exatamente o necessário.

## Passo a passo

### Passo 1 — Adicione a dependência

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Com o starter no classpath, os endpoints são autoconfigurados — por padrão só `/actuator/health` e `/actuator/info` ficam expostos via HTTP.

### Passo 2 — Teste o health

```bash
curl http://localhost:8080/actuator/health
```

```json
{"status":"UP"}
```

Com `spring.datasource` configurado, o health passa a incluir o banco: `{"status":"UP","components":{"db":{"status":"UP"}, ...}}` quando você habilita detalhes.

### Passo 3 — Exponha o que precisa (e só o que precisa)

```properties
# expõe apenas health e metrics via web
management.endpoints.web.exposure.include=health,info,metrics

# mostra detalhes do health apenas para usuários autenticados
management.endpoint.health.show-details=when-authorized

# roda os endpoints de gerenciamento em PORTA SEPARADA (rede interna)
management.server.port=9090
```

```bash
curl http://localhost:9090/actuator/metrics/jvm.memory.used
```

## Como funciona

O Actuator é uma coleção de autoconfigurações: cada endpoint (`@Endpoint` com `@ReadOperation`) é registrado no contexto quando o starter está presente. A camada web do Actuator decide, por `management.endpoints.web.exposure.include/exclude`, quais deles ganham rota HTTP — os demais continuam acessíveis apenas via JMX/local.

O `health` agrega **indicadores** (`HealthIndicator`): o Boot registra um por tecnologia detectada (datasource, disco, Redis, RabbitMQ...) e o resultado final é o pior status do conjunto — um banco fora derruba o status geral.

## Variações

**Health indicator customizado** — para uma dependência crítica sua:

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CepApiHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean ok = pingViaCep();
        return ok ? Health.up().build()
                  : Health.down().withDetail("motivo", "timeout").build();
    }

    private boolean pingViaCep() { return true; }
}
```

**Endpoint de info versionado**:

```properties
info.app.nome=pedidos-api
info.app.versao=1.4.0
```

**Liveness/Readiness para Kubernetes**: com `management.endpoint.health.probes.enabled=true`, o Boot cria `/actuator/health/liveness` e `/actuator/health/readiness` — plugue-os nos probes do deployment.

## Armadilhas

> [!WARNING] Nunca exponha todos os endpoints em produção (`exposure.include=*` abre `/actuator/heapdump`, `/env`, `/beans` — vazamento de segredos e vetor de ataque). Exponha o mínimo, em porta separada se possível, e proteja com autenticação.

- **Health detalhado para o mundo**: `show-details=always` revela host de banco e versões — use `when-authorized` ou `never` no profile de produção;
- **Liveness apontando para dependência externa**: se o probe de liveness falha porque um serviço externo caiu, o Kubernetes reinicia seu pod em loop — liveness deve medir apenas o processo local;
- **Métricas sem backend**: `/actuator/metrics` lê o Micrometer em memória — para série temporal, adicione um registro (Prometheus: `micrometer-registry-prometheus`) e o endpoint `/actuator/prometheus`;
- **Esquecer de testar o health**: um indicador customizado que lança exceção derruba o endpoint inteiro — capture e converta em `Health.down()`.

## Profundidade

O Actuator é construído sobre o **Micrometer**, a fachada de métricas do ecossistema Spring: seus timers e contadores saem pelo mesmo pipeline para Prometheus, Datadog, Graphite etc., sem mudança de código. A separação `management.server.port` cria um segundo servidor HTTP interno — isolamento de rede real entre tráfego de usuário e tráfego de operação, padrão recomendado em ambientes sérios.
