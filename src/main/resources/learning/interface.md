# Interfaces em Java
Intermediário • 10 min

---

## 💡 O que é isso?

Uma interface define um contrato que qualquer classe pode assinar.

Ela declara métodos sem implementação — ou seja, diz o que deve ser feito, mas não como fazer. A classe que implementa a interface é responsável por fornecer o corpo de cada método.

Desde o Java 8, interfaces também podem conter métodos `default` e `static` com implementação.

---

## 🏠 Analogia

:::info
Pense em uma tomada elétrica.

A tomada é uma interface: ela define um padrão (dois ou três pinos, voltagem, formato) que qualquer aparelho deve seguir para se conectar à rede elétrica.

Não importa se o aparelho é um ventilador, um carregador ou uma lava-louças. Se ele segue o padrão da tomada, ele funciona.

Em Java é a mesma ideia: se uma classe implementa uma interface, ela garante que tem os métodos que a interface exige. O código que usa a interface não precisa saber qual classe concreta está por trás.
:::

---

## 🌎 Onde isso aparece?

Sistemas de pagamento online.

Uma interface `Pagamento` pode declarar o método `processar()`. Diversas classes implementam essa interface:

- `CartaoCredito` — processa pagamento com cartão.
- `Boleto` — gera um boleto bancário.
- `Pix` — processa pagamento instantâneo.

O sistema de checkout não precisa saber qual método de pagamento foi escolhido. Ele apenas chama `pagamento.processar()` e cada classe faz o seu trabalho.

---

## 💻 Exemplo

```java
interface Pagamento {
    void processar(double valor);
}

class CartaoCredito implements Pagamento {
    public void processar(double valor) {
        System.out.println("Cartão: R$ " + valor);
    }
}

class Pix implements Pagamento {
    public void processar(double valor) {
        System.out.println("Pix: R$ " + valor);
    }
}
```

A interface `Pagamento` define o contrato com o método `processar`.

`CartaoCredito` e `Pix` implementam a interface — cada um fornece sua própria versão do método.

No código que usa o pagamento, não importa qual implementação está em execução:

```java
Pagamento pag = new Pix();
pag.processar(150.00); // saída: Pix: R$ 150.00
```

---

## 🧠 Como funciona?

Quando uma classe usa `implements`, o compilador verifica se todos os métodos da interface foram implementados.

↓

Se faltar algum método, o código não compila. O contrato é obrigatório.

↓

Uma classe pode implementar várias interfaces ao mesmo tempo — diferente da herança de classes, que é única.

↓

Variáveis declaradas com o tipo da interface podem receber qualquer objeto que a implemente. Isso é polimorfismo.

- A interface não pode ser instanciada diretamente (`new Pagamento()` não compila).
- Métodos em interface são `public` por padrão.
- Campos em interface são `public static final` (constantes).

---

## ⚠️ Erros comuns

:::warning
Implementar a interface mas esquecer um método.
:::

O compilador aponta o erro na hora. Se a classe não implementa todos os métodos da interface, ela precisa ser declarada como `abstract`.

:::warning
Usar interface quando uma classe abstrata seria mais adequada.
:::

Se você precisa compartilhar estado (campos) ou construtores, prefira uma classe abstrata. Interface é apenas contrato.

:::warning
Criar interfaces muito grandes ("interface God").
:::

Uma interface deve ter uma responsabilidade bem definida. Vários métodos não relacionados em uma mesma interface dificultam a implementação e a manutenção.

---

## ➡️ Próximo passo

Interfaces são a base do polimorfismo em Java.

O próximo passo é explorar o polimorfismo: como uma mesma operação pode se comportar de maneiras diferentes dependendo do tipo real do objeto.
