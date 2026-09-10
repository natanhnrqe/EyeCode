# Do arquivo Java ao programa em execução

Quando você escreve uma classe no EyeCode, cria **código-fonte Java**: um arquivo legível por pessoas, normalmente com a extensão `.java`. Esse código descreve classes, métodos e regras do programa, mas ainda não é entendido diretamente pelo sistema operacional.

## O compilador e o bytecode

O compilador `javac` analisa o código-fonte, aponta erros de linguagem e gera arquivos `.class`. Esses arquivos contêm *bytecode*, uma representação intermediária pensada para ser executada pela máquina virtual Java.

```shell
javac Main.java
java Main
```

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Olá, JVM");
    }
}
```

> O bytecode não é código de máquina específico do Windows, macOS ou Linux. Ele é a ponte entre o seu programa e a JVM instalada em cada plataforma.

## JVM, JRE e JDK

- **JVM (Java Virtual Machine):** executa o bytecode e oferece serviços como gerenciamento de memória e coleta de lixo.
- **JRE (Java Runtime Environment):** reúne a JVM e as bibliotecas necessárias para executar aplicações Java.
- **JDK (Java Development Kit):** inclui o ambiente de execução e as ferramentas de desenvolvimento, como `javac`, `java` e `javadoc`.

Em resumo: quem apenas executa um programa precisa de um ambiente de execução; quem cria programas precisa do JDK. Na prática atual, as distribuições do JDK já trazem o runtime necessário para executar o que você compila.

## Fluxo simplificado

```text
Main.java
  | javac
  v
Main.class (bytecode)
  | JVM
  v
Programa em execução
```

A JVM interpreta e otimiza o bytecode para a plataforma em que está rodando. Por isso o mesmo arquivo `.class` pode executar em sistemas diferentes, desde que exista uma JVM compatível.

## Portabilidade na prática

A expressão **write once, run anywhere** resume essa separação: você compila para bytecode uma vez e deixa cada JVM adaptar a execução ao seu ambiente. Isso não elimina diferenças de arquivos, rede ou interface do sistema, mas mantém a linguagem e grande parte do programa independentes da plataforma.

## Relação com o EyeCode

O EyeCode ajuda você a escrever, navegar e aprender Java. Quando você compilar ou executar um projeto, as ferramentas do JDK fazem a transformação do código; a JVM executa o resultado. Conhecer essas camadas torna mensagens de compilação, versões do Java e configurações de projeto mais fáceis de entender.
