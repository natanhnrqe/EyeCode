---
id: java/jdk/fundamentos/classes
title: Classes e Objetos
summary: O modelo do Java em uma página: classe como planta, objeto como construção, com atributos, métodos e o caminho completo do `new`.
level: beginner
duration: 10
officialDocs:
  label: JLS §8 — Classes
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html
related:
  - java/jdk/fundamentos/variables
  - java/jdk/java.lang/string
---

> [!INFO] **Classe** é a planta (projeto); **objeto** é a construção (a instância real na memória). Todo código Java existe dentro de uma classe — não existe função solta no topo do arquivo.

## Por que existe

Variáveis isoladas não escalam: um cadastro com 1000 usuários exigiria 1000 variáveis soltas. Orientação a objetos resolve isso juntando **dados** (atributos) e **comportamentos** (métodos) numa unidade só — a classe. Você cria quantas instâncias quiser, cada uma com seu próprio estado, mas com o mesmo comportamento definido uma única vez.

## Anatomia da sintaxe

```java
public class Pessoa {
    // atributos — o estado de cada objeto
    String nome;
    int idade;

    // método — o comportamento
    void seApresentar() {
        System.out.println("Oi, sou " + nome);
    }
}
```

Criando e usando um objeto:

```java
Pessoa ana = new Pessoa();   // 1) criar (instanciar)
ana.nome = "Ana";            // 2) guardar estado
ana.seApresentar();          // 3) usar comportamento
// Saída: Oi, sou Ana
```

## Como funciona

`new Pessoa()` faz três coisas:

1. **Aloca memória** para os atributos da classe (todos `nome`, `idade`... daquela instância);
2. **Inicializa** os atributos com seus valores padrão (`null` para objetos, `0`/`false` para primitivos);
3. **Devolve a referência** — o endereço do objeto — que guardamos em `ana`.

`ana` não *é* o objeto: é um **ponteiro** para ele. Por isso `Pessoa b = a;` copia o ponteiro, não o objeto — `a` e `b` apontam para a mesma construção.

O método `construtor` (constructor) roda exatamente no momento do `new`, permitindo exigir estado inicial:

```java
public class Pessoa {
    String nome;
    int idade;

    public Pessoa(String nome, int idade) {
        this.nome = nome;   // "this" = o objeto que está sendo criado
        this.idade = idade;
    }
}

Pessoa ana = new Pessoa("Ana", 28);   // obrigatório passar os dois valores
```

## Exemplos

Classe completa com estado, comportamento e encapsulamento:

```java
public class Conta {
    private double saldo;                    // só a própria classe mexe

    public void depositar(double valor) {    // controla a regra de negócio
        if (valor > 0) saldo += valor;
    }

    public double getSaldo() {               // leitura controlada
        return saldo;
    }
}

Conta conta = new Conta();
conta.depositar(100.0);
System.out.println(conta.getSaldo());   // Saída: 100.0
```

Várias instâncias, estados independentes:

```java
Pessoa a = new Pessoa("Ana", 28);
Pessoa b = new Pessoa("Bia", 31);
// a.nome é "Ana", b.nome é "Bia" — mesmo projeto, estados diferentes
```

## Armadilhas comuns

> [!WARNING] Acessar atributo sem `new`: `Pessoa p; p.nome = "Ana";` — `p` é `null`, isso estoura `NullPointerException`. Crie o objeto antes de usá-lo.

**Esquecer `static` e tentar usar sem instanciar:**

```java
class Util {
    static int dobro(int x) { return x * 2; }   // método de classe
}
Util.dobro(4);            // ok, sem objeto
new Util().dobro(4);      // também ok, mas desnecessário
```

Métodos sem `static` pertencem a **objetos**: precisam de `new Util().metodo()` ou de um objeto já criado.

**Comparar objetos com `==`:**

```java
String a = new String("oi");
String b = new String("oi");
a == b;              // false — são dois objetos diferentes na memória
a.equals(b);         // true — conteúdo igual (use este!)
```

## Profundidade

**`static` vs instância:** membros `static` pertencem à **classe** (existe um só, carregado junto com a classe); membros de instância pertencem a **cada objeto** (um por objeto). Campos `static final` em caixa alta são a convenção de constantes.

**Encapsulamento:** `private` esconde o atributo do mundo externo e expõe regras via métodos públicos. Garante que estado inválido nunca exista (ex.: saldo negativo) e mantém a liberdade de mudar a implementação sem quebrar quem usa a classe.

**Herança (`extends`):** uma classe pode herdar atributos e métodos de outra. `class Gato extends Animal` é um `Animal` — polimorfismo permite tratar Gato como Animal e chamar métodos sobrescritos (`@Override`). Regra Liskov: subtipo deve substituir supertipo sem quebrar o comportamento esperado.

**Referência vs valor (JLS §4.3):** primitivos (`int`, `double`...) são armazenados **por valor** — a variável contém o dado. Objetos (`String`, `Pessoa`...) são armazenados **por referência** — a variável contém o endereço. É por isso que passar um `int` para um método copia o valor, mas passar um objeto passa o ponteiro.

**Métodos equals e hashCode:** classes de domínio devem sobrescrever `equals` (quando é "igual") e `hashCode` (para funcionar em `HashMap`/`Set`) — o contrato exige: iguais ⇒ mesmo hash; e o hash estável enquanto o objeto estiver em coleção.

**Records (Java 16+):** `record Ponto(int x, int y)` declara uma classe imutável com getters automáticos (`x()`, não `getX()`), `equals`/`hashCode`/`toString` prontos — ideal para portadores de dados sem comportamento mutável.
