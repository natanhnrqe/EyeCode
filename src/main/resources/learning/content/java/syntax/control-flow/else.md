---
id: java/syntax/control-flow/else
title: else
concept: else
level: beginner
duration: 2
category: FLUXO DE CONTROLE
depth: quick
related:
  - java/syntax/control-flow/if
  - java/syntax/control-flow/switch
---
else define o bloco alternativo quando a condição do if é falsa. else if permite testar outra condição antes do ramo final.

~~~java
if (saldo >= valor) {
    pagar();
} else if (saldo > 0) {
    avisarSaldoInsuficiente();
} else {
    recusar();
}
~~~

Use chaves mesmo para blocos de uma linha para evitar ambiguidades.
