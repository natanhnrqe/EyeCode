# Classes em Java
Iniciante • 16 min

---

## 💡 O que é isso?

Uma classe é como um molde ou uma receita.

Ela define como algo será criado, quais informações esse algo terá e o que ele será capaz de fazer.

Por exemplo: antes de construir vários carros iguais, alguém criou um projeto com as medidas e as peças de cada carro. Esse projeto é a classe. Os carros construídos a partir dele são os objetos.

No Java, tudo começa com uma classe. Até o seu primeiro programa usa uma classe.

---

## 🏠 Analogia

:::info
Imagine que você quer fazer bolos de chocolate.

Antes de colocar a mão na massa, você precisa de uma receita. A receita diz quais ingredientes usar e em qual ordem misturá-los.

A receita é a classe. Cada bolo que você faz seguindo essa receita é um objeto.

Você pode fazer vários bolos diferentes usando a mesma receita. Cada bolo pode ter uma cobertura diferente, mas todos seguem a mesma estrutura base.

É exatamente assim que classe e objeto funcionam em Java.
:::

---

## 🌎 Onde isso aparece?

Pense em uma caderneta de clientes de um pequeno comércio.

Todos os clientes têm informações parecidas: nome, telefone e e-mail. A estrutura "cliente" é a mesma para todos.

Essa estrutura é uma classe. Cada cliente preenchendo essa estrutura é um objeto.

Outros exemplos:

- Um cadastro de alunos: todo aluno tem nome, matrícula e série.
- Uma conta bancária: toda conta tem número, saldo e titular.
- Um catálogo de produtos: todo produto tem nome, preço e código.

Em todos esses casos, a classe define o modelo. Os objetos são os dados reais.

---

## 💻 Exemplo

```java
class Pessoa {
    String nome;
    int idade;

    void apresentar() {
        System.out.println("Olá, eu sou " + nome);
    }
}
```

A palavra `class` diz ao Java que estamos criando uma classe.

`Pessoa` é o nome da classe.

Dentro dela temos campos (`nome`, `idade`) e um método (`apresentar`).

Repare que apenas declaramos a classe. Nenhum dado foi preenchido ainda. Isso acontece quando criamos objetos a partir dela.

---

## 🧠 Como funciona?

Quando você compila um arquivo .java, o Java transforma sua classe em um arquivo .class.

Esse arquivo .class contém bytecode — uma linguagem intermediária que a JVM (Máquina Virtual Java) entende.

↓

Quando seu programa executa, a JVM carrega a classe na memória. A partir daí ela pode ser usada para criar objetos.

↓

Cada objeto criado a partir da mesma classe ocupa seu próprio espaço na memória, mas todos compartilham a mesma estrutura definida pela classe.

↓

Você não precisa entender bytecode agora. O importante é saber que:

- A classe é o plano.
- O objeto é a construção real feita a partir desse plano.
- A JVM é a fábrica que faz essa construção acontecer.

---

## ⚠️ Erros comuns

:::warning
Achar que classe e objeto são a mesma coisa.
:::

A classe é o molde. O objeto é o resultado. Confundir os dois é como confundir a receita de bolo com o bolo pronto.

:::warning
Colocar tudo dentro do método `main`.
:::

Muitos iniciantes escrevem o programa inteiro dentro do `main`. O `main` é só o ponto de entrada. A lógica deve ficar organizada dentro de métodos e classes.

:::warning
Usar nomes confusos para as classes.
:::

O nome da classe deve ser claro: `Cliente`, `Produto`, `Pedido`. Evite nomes como `Coisa`, `Executar`, `Teste123`. Um bom nome faz o código virar uma leitura natural.

---

## ➡️ Próximo passo

Agora que você sabe o que é uma classe, o próximo passo é descobrir como criar objetos a partir dela.

Enquanto a classe é o molde, o objeto é a peça real que você usa no programa. É com objetos que seu código ganha vida.

Na próxima aula você vai aprender a instanciar objetos, acessar seus campos e chamar seus métodos. É aí que a mágica realmente acontece.

---

## 📚 Ver documentação oficial

A documentação oficial da Oracle será integrada futuramente.
