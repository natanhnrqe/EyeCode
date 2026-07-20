# Enums em Java
Iniciante • 10 min

---

## 💡 O que é isso?

Um `enum` (enumeração) é um tipo especial em Java que define um conjunto fixo de constantes nomeadas.

Diferente de constantes soltas (`static final`), um enum agrupa valores relacionados em um único tipo, trazendo mais segurança e legibilidade ao código.

---

## 🏠 Analogia

## 📘 Informação
Pense nos dias da semana.

Existem exatamente 7 dias: segunda, terça, quarta, quinta, sexta, sábado e domingo. Não há um oitavo dia. Esse conjunto fixo e bem definido é uma enumeração.

Em Java, um enum funciona da mesma forma: você lista todos os valores possíveis e o compilador garante que ninguém use um valor fora dessa lista.

Usar `String` para representar dias da semana abre espaço para erros de digitação ("Segunda", "segunda", "SEGUNDA"). Com enum, o valor é exato e conhecido.


---

## 🌎 Onde isso aparece?

Status de um pedido em um e-commerce.

Um pedido pode estar em um destes estados: `PENDENTE`, `PAGO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`. Não faz sentido ter um pedido com status "qualquer coisa".

Com um enum `StatusPedido`, o sistema garante que todo pedido tem um status válido. Além disso, cada constante pode ter comportamentos próprios.

---

## 💻 Exemplo

```java
enum StatusPedido {
    PENDENTE,
    PAGO,
    ENVIADO,
    ENTREGUE,
    CANCELADO
}

public class Pedido {
    private StatusPedido status;

    public Pedido() {
        this.status = StatusPedido.PENDENTE;
    }

    public void pagar() {
        if (status != StatusPedido.PENDENTE) {
            throw new IllegalStateException("Pedido não está pendente");
        }
        status = StatusPedido.PAGO;
    }
}
```

O enum `StatusPedido` define exatamente 5 valores possíveis.

O campo `status` do pedido só pode receber um desses valores — o compilador não aceita nada fora disso.

O método `pagar()` verifica o status atual antes de mudar, prevenindo estados inválidos.

---

## 🧠 Como funciona?

Enums em Java são classes especiais. Cada constante enum é uma instância pública e imutável do próprio enum.

↓

Você pode adicionar campos, métodos e construtores a um enum, igual a uma classe normal.

↓

O construtor do enum é executado uma vez para cada constante, na ordem em que são declaradas.

↓

Enum pode implementar interfaces, mas não pode estender outra classe (todo enum já estende `java.lang.Enum` implicitamente).

- `Enum.valueOf("PAGO")` retorna a constante `PAGO`.
- `Enum.values()` retorna um array com todas as constantes.
- `Enum.ordinal()` retorna a posição (0-based) da constante.
- Enums funcionam perfeitamente em `switch` e `if`.

---

## ⚠️ Erros comuns

## ⚠️ Atenção
Usar `String` ou `int` quando um enum seria mais seguro.


Constantes soltas permitem valores inválidos. Um enum restringe as opções e centraliza a definição.

## ⚠️ Atenção
Comparar enums com `equals` em vez de `==`.


Enums podem ser comparados com `==` porque cada constante é um singleton. Isso é mais seguro e mais performático que `equals`.

## ⚠️ Atenção
Colocar lógica complexa dentro do enum.


Enums devem representar valores e comportamentos simples. Lógica de negócio complexa deve ficar em classes separadas.

---

## ➡️ Próximo passo

Agora que você conhece enums, o próximo passo é entender os records.

Records são uma forma concisa de criar classes imutáveis que servem apenas para carregar dados — perfeitas para DTOs e value objects.