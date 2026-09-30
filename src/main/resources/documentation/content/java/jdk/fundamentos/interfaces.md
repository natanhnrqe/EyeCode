---
id: java/jdk/fundamentos/interfaces
title: Interfaces
type: concept
summary: Contrato puro de comportamento — métodos abstratos, default methods, interfaces marcadoras e a herança múltipla de tipo que só elas permitem.
level: beginner
duration: 8
officialDocs:
  label: JLS §9 — Interfaces
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html
related:
  - java/jdk/fundamentos/classes
  - java/jdk/fundamentos/lambdas
  - java/jdk/java.lang/comparable
  - java/jdk/java.util/function
---

> [!INFO] `interface` declara um **contrato**: a lista de comportamentos que uma classe se compromete a ter. Quem assina escreve `implements` — e pode assinar **vários contratos ao mesmo tempo** (herança múltipla de tipo). Desde o Java 8, a interface também pode trazer implementação pronta com `default`.

## Por que existe

Herança de classe só permite uma: se `Relatorio extends Documento`, não dá para estender também `Auditavel`. Mas o código real pede capacidades combinadas — um objeto pode ser imprimível **e** serializável **e** comparável sem que isso faça parte da identidade dele.

A interface separa as duas ideias: **classe diz o que o objeto é; interface diz o que ele sabe fazer**. E o contrato destrava o principal benefício do desacoplamento: você programa contra `List`, não contra `ArrayList` — trocar a implementação não quebra quem usa.

## Anatomia da sintaxe

O contrato e quem assina:

```java
interface Pagavel {
    double valor();               // implicitamente public abstract
    boolean pago();
}

class Boleto implements Pagavel {          // a classe assina o contrato
    private final double valor;
    Boleto(double valor) { this.valor = valor; }

    public double valor() { return valor; }     // implementação obrigatória
    public boolean pago() { return true; }
}
```

| Parte | O que é | Exemplo |
|-------|---------|---------|
| `interface X` | a declaração do contrato | `interface Pagavel` |
| métodos sem corpo | abstratos, `public` implícitos | `double valor();` |
| `default` | método com corpo, herdável (Java 8+) | `default void log()` |
| `static` | utilitário da interface (Java 8+) | `static Pagavel zero()` |
| `implements` | a classe que assina | `class Boleto implements Pagavel` |
| campos | constantes `public static final` implícitas | `int LIMITE = 100;` |

## Como funciona

Uma interface define um **tipo** — então uma variável declarada com ela aponta para qualquer implementação:

```java
Pagavel p = new Boleto(80.0);   // a referência só conhece o contrato
double total = p.valor();       // polimorfismo: roda o método do Boleto
```

O compilador exige que a classe implemente **todos** os métodos abstratos (ou que ela própria seja `abstract`). Uma classe pode implementar várias interfaces de uma vez, separadas por vírgula — a única herança múltipla que Java aceita: vários **tipos**, nunca vários estados.

`default` resolve evolução: adicionar método abstrato em interface antiga quebraria todas as classes que já a implementam; com corpo pronto, todo mundo herda sem mexer em nada.

## Exemplos

Capacidades combinadas — herança múltipla de tipo na prática:

```java
interface Imprimivel { void imprimir(); }
interface Serializavel { byte[] serializar(); }

class Relatorio implements Imprimivel, Serializavel {
    public void imprimir() { System.out.println("imprimindo relatorio"); }
    public byte[] serializar() { return new byte[0]; }
}

Imprimivel a = new Relatorio();   // o mesmo objeto, visto por dois contratos
Serializavel b = new Relatorio();
```

`default` estendendo o contrato sem quebrar as classes existentes:

```java
interface Notificavel {
    void enviar(String mensagem);

    default void enviarComLog(String mensagem) {   // corpo pronto, herdável
        System.out.println("[log] enviando: " + mensagem);
        enviar(mensagem);                          // chama a implementação da classe
    }
}

class Email implements Notificavel {
    public void enviar(String mensagem) { System.out.println("e-mail: " + mensagem); }
}

new Email().enviarComLog("ola");   // herda o default e implementa só o essencial
```

Interface marcadora — sem métodos, só um rótulo consultado com `instanceof`:

```java
interface Editavel { }                     // marcador

class Documento implements Editavel { }

if (new Documento() instanceof Editavel) {
    System.out.println("pode editar");     // o programa consulta o rótulo
}
```

## Armadilhas

> [!WARNING] Campo de interface é **sempre** `public static final`: `int contador;` numa interface é uma constante que precisa ser inicializada — nunca um estado por instância. Interface não guarda estado de objeto.

**Esquecer `public` na implementação:**

```java
class Boleto implements Pagavel {
    double valor() { return 0; }   // erro: attempting to assign weaker access privileges
}
```

Métodos de interface são implicitamente `public`; a implementação precisa manter essa visibilidade — nunca reduzir.

**Conflito de `default` (diamante):** se duas interfaces trouxerem o **mesmo** método `default`, a classe é obrigada a sobrescrever — e pode escolher (ou combinar) com `Primeira.super.metodo()` / `Segunda.super.metodo()`. Sem o override, não compila.

**Método `static` da interface não é herdado:** quem implementa não ganha `zero()` — ele se chama direto na interface (`Pagavel.zero()`), diferente de `static` em classe, que é herdado.

## Profundidade

**Interface é tipo de referência (JLS §9):** métodos sem corpo são `public abstract` implícitos; campos são `public static final` implícitos. A classe que implementa ganha a interface como supertipo — `instanceof`, cast e polimorfismo funcionam como na herança.

**Diamante resolvido em compilação:** Java proíbe herança múltipla de *estado*, mas permite de *tipo*. Quando dois `default` idênticos colidem, a JLS §9.4.1 exige override explícito na classe — a ambiguidade nunca chega em runtime.

**Interface funcional (SAM):** uma interface com exatamente **um** método abstrato pode receber uma expressão lambda direto — é assim que `Runnable`, `Comparator` e `Function` funcionam. `@FunctionalInterface` pede ao compilador que verifique a regra.

**`default` nasceu para evoluir APIs:** o exemplo canônico é `Collection.stream()` (Java 8) — um `default` que deu poder de stream a todas as coleções existentes sem recompilar nenhuma implementação.

**Marcador vs anotação:** `Serializable` e `Cloneable` são anteriores às anotações e continuam úteis quando o rótulo precisa virar **tipo** — ser salvo numa variável, aparecer em `instanceof`. Anotação descreve metadados; marcador participa da hierarquia.
