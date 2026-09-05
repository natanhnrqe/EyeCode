---
id: java/syntax/control-flow/return
title: return
concept: return
level: beginner
duration: 2
category: FLUXO DE CONTROLE
depth: quick
related:
  - java/syntax/types/void
  - java/methods/declaration
  - java/syntax/control-flow/yield
  - java/syntax/control-flow/if
---
return encerra o método atual. Métodos com tipo de retorno fornecem uma expressão; métodos void podem usar return; para sair mais cedo.

~~~java
int dobro(int valor) {
    if (valor < 0) {
        return 0;
    }
    return valor * 2;
}
~~~

return sai de um método, enquanto break sai de um laço ou switch. Em uma expressão switch, use yield para produzir o valor de um bloco.
