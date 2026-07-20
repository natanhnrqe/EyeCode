# Objetos em Java
Iniciante • 8 min

---

## 💡 O que é isso?

Um objeto é uma instância concreta de uma classe.

Enquanto a classe é o molde, o objeto é a peça real criada a partir desse molde. Cada objeto tem seu próprio conjunto de valores (estado) e pode executar comportamentos definidos pela classe.

No Java, objetos são criados com a palavra `new`. Eles ocupam memória, têm identidade própria e podem interagir entre si.

---

## 🏠 Analogia

## 📘 Informação
Imagine uma fábrica de carros.

O projeto do carro (a planta com todas as especificações) é a classe. Cada carro que sai da linha de montagem seguindo esse projeto é um objeto.

Um carro pode ser vermelho, outro azul. Um pode ter câmbio automático, outro manual. Mesmo sendo diferentes, todos seguem o mesmo projeto.

Da mesma forma, dois objetos da mesma classe podem ter valores diferentes nos campos, mas compartilham a mesma estrutura de métodos.


---

## 🌎 Onde isso aparece?

Um formulário de cadastro em um site.

Quando você preenche seus dados e clica em "Salvar", o sistema cria um objeto `Cliente` com suas informações. Outra pessoa que se cadastra cria um objeto `Cliente` diferente, com dados próprios.

A classe `Cliente` define que todo cliente tem nome, e-mail e telefone. Cada objeto `Cliente` armazena os valores de uma pessoa específica.

---

## 💻 Exemplo

```java
Pessoa pessoa1 = new Pessoa();
pessoa1.nome = "João";
pessoa1.idade = 25;
pessoa1.apresentar();

Pessoa pessoa2 = new Pessoa();
pessoa2.nome = "Maria";
pessoa2.idade = 30;
pessoa2.apresentar();
```

`new Pessoa()` cria um novo objeto do tipo `Pessoa`.

`pessoa1` e `pessoa2` são variáveis que guardam referências para objetos diferentes.

Cada objeto tem seu próprio `nome` e `idade`. Quando chamamos `apresentar()`, cada um imprime seu próprio nome.

---

## 🧠 Como funciona?

Quando você usa `new`, a JVM aloca memória para o objeto.

↓

Os campos do objeto são inicializados com valores padrão (0, null, false).

↓

O construtor da classe é executado para definir os valores iniciais.

↓

A referência ao objeto é devolvida e armazenada na variável.

- A variável não contém o objeto em si, apenas uma referência (um endereço de memória).
- Duas variáveis podem referenciar o mesmo objeto.
- Se o objeto não tiver mais referências, o garbage collector o remove da memória.

---

## ⚠️ Erros comuns

## ⚠️ Atenção
Esquecer de usar `new` e tentar acessar um objeto nulo.


Toda variável de objeto que não foi inicializada vale `null`. Acessar `pessoa.nome` quando `pessoa` é `null` causa `NullPointerException`.

## ⚠️ Atenção
Achar que a variável é o objeto.


A variável é apenas uma referência. Atribuir `pessoa1 = pessoa2` não copia o objeto. As duas variáveis passam a apontar para o mesmo objeto na memória.

## ⚠️ Atenção
Criar objetos desnecessários dentro de loops.


Se um objeto pode ser reutilizado, não crie um novo a cada iteração. Isso sobrecarrega o garbage collector e deixa o programa lento.

---

## ➡️ Próximo passo

Agora que você entende o que são classes e objetos, está pronto para aprender sobre encapsulamento.

Encapsulamento é a técnica de esconder os detalhes internos de um objeto e expor apenas o que é necessário. É o que torna o código mais seguro e organizado.