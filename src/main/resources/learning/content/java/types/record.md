# Records em Java
Intermediário • 10 min

---

## 💡 O que é isso?

Um `record` é uma forma concisa de declarar uma classe imutável que serve apenas para transportar dados.

Introduzido no Java 14 (oficial no Java 16), o record elimina cerimônia: você declara apenas os campos no cabeçalho e o compilador gera automaticamente o construtor, getters, `equals`, `hashCode` e `toString`.

---

## 🏠 Analogia

## 📘 Informação
Pense em um formulário de cadastro pré-preenchido.

Você só preenche os campos obrigatórios e pronto — o sistema já sabe que o formulário tem nome, data e assinatura. Não precisa desenhar o formulário do zero.

O record funciona igual: você lista os componentes e o Java cuida de todo o resto. Não precisa escrever construtor manual, getters ou `toString`.


---

## 🌎 Onde isso aparece?

APIs REST que recebem e devolvem dados.

Um endpoint que retorna dados de um usuário pode usar um record `UsuarioResponse` com os campos `id`, `nome` e `email`. O record garante que os dados são imutáveis depois de criados e já vem com `toString` para logging.

Antes dos records, você precisava escrever uma classe inteira com construtor, getters, `equals`, `hashCode` e `toString`. Com record, tudo isso é gerado automaticamente.

---

## 💻 Exemplo

```java
record Pessoa(String nome, int idade) {}

public class Main {
    public static void main(String[] args) {
        Pessoa p = new Pessoa("João", 25);
        System.out.println(p.nome());   // João
        System.out.println(p.idade());  // 25
        System.out.println(p);          // Pessoa[nome=João, idade=25]
    }
}
```

Uma única linha `record Pessoa(String nome, int idade)` substitui dezenas de linhas de código boilerplate.

Os getters não seguem o padrão JavaBeans (`getNome()`) e sim o nome do campo diretamente (`nome()`).

O `toString` gerado mostra o nome do record e todos os campos com seus valores.

---

## 🧠 Como funciona?

O compilador transforma o record em uma classe final com campos `private final`.

↓

Gera um construtor que recebe todos os campos na ordem declarada (construtor canônico).

↓

Gera automaticamente: `equals`, `hashCode`, `toString`, e um accessor para cada campo.

↓

O record não pode estender outra classe (mas implementa interfaces normalmente). Não pode ter campos de instância adicionais (apenas os declarados no cabeçalho).

- Todos os campos são `private final` — imutabilidade garantida.
- É possível adicionar métodos, `static` fields e construtores compactos.
- O construtor compacto permite validação sem reatribuir os campos.

---

## ⚠️ Erros comuns

## ⚠️ Atenção
Tentar modificar um campo do record depois de criado.


Records são imutáveis. Se você precisa modificar dados, crie um novo record ou use uma classe normal.

## ⚠️ Atenção
Adicionar lógica complexa no record.


Record é para dados. Lógica de negócio complexa deve ficar em classes separadas (serviços, validadores).

## ⚠️ Atenção
Usar record para classes que precisam de herança.


Record é `final` e não pode estender outras classes. Se você precisa de herança, use uma classe abstrata ou interface.

---

## ➡️ Próximo passo

Com classes, interfaces, enums e records, você já conhece os principais tipos do Java.

O próximo passo é explorar o polimorfismo — como escrever código flexível que funciona com qualquer tipo que implemente um determinado contrato.