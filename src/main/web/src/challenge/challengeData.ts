import type { Challenge } from './types';
import { CHALLENGE_CATALOG } from './catalog';

export const CNPJ_VALIDATOR_CHALLENGE: Challenge = {
  id: 'cnpj-validator',
  title: 'Validador de CNPJ',
  entryClassName: 'CnpjValidator',
  metadata: {
    track: 'Fundamentos de Java',
    topic: 'Strings e validação',
    language: 'Java'
  },
  statement: { objective: 'Confira os dígitos verificadores e valide documentos com ou sem máscara.' },
  descriptionMarkdown: [
    '# Validador de CNPJ',
    '',
    'Implemente a validação de um CNPJ brasileiro.',
    '',
    '## Regras de negócio',
    '',
    '- Um CNPJ possui 14 dígitos.',
    '- Ignore pontos, barras e traços (`./-`) ao contar os dígitos.',
    '- Rejeite sequências de dígitos repetidos (ex.: `00000000000000`).',
    '- Calcule e valide os dois dígitos verificadores.',
    '',
    '## Entrada',
    '',
    '```java',
    'isValid("04.252.011/0001-10")',
    '```',
    '',
    '## Saída',
    '',
    '```java',
    'true',
    '```'
  ].join('\n'),
  starterCode: [
    'package br.com.eyecode.challenge;',
    '',
    'public class CnpjValidator {',
    '',
    '    public boolean isValid(String cnpj) {',
    '        // TODO: implementar a validação de CNPJ',
    '        return false;',
    '    }',
    '}'
  ].join('\n')
};

const FUNDAMENTALS_CHALLENGES: Record<string, Pick<Challenge, 'statement' | 'descriptionMarkdown'>> = {
  fizzbuzz: {
    statement: {
      objective: 'Converta um número para a palavra correspondente às regras clássicas do FizzBuzz.',
      instructions: 'Implemente `fizzBuzz(int start, int end)` para retornar a sequência inclusiva de `start` até `end`. Múltiplos de 3 retornam `Fizz`, múltiplos de 5 retornam `Buzz`, múltiplos de ambos retornam `FizzBuzz`; os demais retornam o próprio número como texto.',
      examples: [{ input: 'start = 1, end = 5', output: '["1", "2", "Fizz", "4", "Buzz"]' }],
      constraints: ['Considere que `start` é menor ou igual a `end`.', 'Aplique as regras também a zero e a valores negativos.']
    },
    descriptionMarkdown: '# FizzBuzz Clássico'
  },
  'conversor-temperatura': {
    statement: {
      objective: 'Converta uma temperatura de Celsius para Fahrenheit.',
      instructions: 'Implemente `toFahrenheit(double celsius)` usando a relação Fahrenheit = Celsius × 9 / 5 + 32.',
      examples: [{ input: '0.0 °C', output: '32.0 °F' }, { input: '100.0 °C', output: '212.0 °F' }],
      constraints: ['Aceite valores decimais e negativos.']
    },
    descriptionMarkdown: '# Conversor de Temperatura'
  },
  'ano-bissexto': {
    statement: {
      objective: 'Determine se um ano é bissexto pelas regras do calendário gregoriano.',
      instructions: 'Implemente `isLeapYear(int year)`: anos divisíveis por 400 são bissextos; os divisíveis por 100 e não por 400 não são; nos demais casos, são bissextos quando divisíveis por 4.',
      examples: [{ input: '2000', output: 'true' }, { input: '1900', output: 'false' }, { input: '2024', output: 'true' }],
      constraints: ['Considere anos inteiros positivos.']
    },
    descriptionMarkdown: '# Verificador de Ano Bissexto'
  },
  'media-aprovacao': {
    statement: {
      objective: 'Calcule a média de notas e determine se a pessoa estudante atingiu a nota mínima.',
      instructions: 'Implemente `isApproved(double[] grades)`: calcule a média aritmética e retorne `true` quando ela for igual ou superior a 7.0.',
      businessRules: ['Cada nota deve estar entre 0.0 e 10.0.', 'Uma lista vazia ou contendo nota fora do intervalo não aprova.'],
      examples: [{ input: '[7.0, 8.0, 6.0]', output: 'true' }, { input: '[5.0, 6.0]', output: 'false' }],
      constraints: ['Não arredonde a média antes de compará-la com 7.0.']
    },
    descriptionMarkdown: '# Média para Aprovação'
  },
  palindromo: {
    statement: {
      objective: 'Verifique se uma frase é palíndroma ignorando espaços, pontuação e diferença entre maiúsculas e minúsculas.',
      instructions: 'Implemente `isPalindrome(String text)`. Compare os caracteres alfanuméricos do texto sem distinguir caixa.',
      examples: [{ input: 'Socorram-me, subi no ônibus em Marrocos', output: 'true' }, { input: 'EyeCode', output: 'false' }],
      constraints: ['Texto nulo ou sem caracteres alfanuméricos deve retornar `false`.']
    },
    descriptionMarkdown: '# Verificador de Palíndromo'
  }
};

const OOP_CHALLENGES: Record<string, Pick<Challenge, 'statement' | 'descriptionMarkdown' | 'starterCode'>> = {
  'conta-bancaria': {
    statement: {
      objective: 'Encapsule o saldo de uma conta bancária e controle depósitos e saques com segurança.',
      instructions: 'Implemente a classe `ContaBancaria` com o construtor `ContaBancaria(double saldoInicial)`, os métodos `depositar(double valor)`, `sacar(double valor)` e `getSaldo()`. O saldo vive em um atributo privado e é exposto apenas para leitura.',
      businessRules: [
        'Depósitos com valor menor ou igual a zero não alteram o saldo.',
        'Só é possível sacar valores positivos até o limite do saldo.',
        'Saque acima do saldo ou valor inválido devolve `false` e mantém o saldo inalterado.'
      ],
      examples: [
        { input: 'new ContaBancaria(100.0); conta.depositar(50.0)', output: 'conta.getSaldo() == 150.0' },
        { input: 'new ContaBancaria(100.0); conta.sacar(200.0)', output: 'false (saldo permanece 100.0)' }
      ],
      constraints: ['Mantenha o saldo em um atributo privado da classe.', 'Não exponha o saldo em campo público.']
    },
    descriptionMarkdown: '# Conta Bancária',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class ContaBancaria {',
      '',
      '    private double saldo;',
      '',
      '    public ContaBancaria(double saldoInicial) {',
      '        // TODO: inicialize o saldo com o valor informado',
      '    }',
      '',
      '    public void depositar(double valor) {',
      '        // TODO: some valores positivos ao saldo',
      '    }',
      '',
      '    public boolean sacar(double valor) {',
      '        // TODO: devolva false quando o valor for inválido ou maior que o saldo',
      '        return false;',
      '    }',
      '',
      '    public double getSaldo() {',
      '        return saldo;',
      '    }',
      '}'
    ].join('\n')
  },
  'relogio-digital': {
    statement: {
      objective: 'Modele o estado de um relógio digital com validação, avanço de minutos e retorno à meia-noite.',
      instructions: 'Implemente `RelogioDigital()` (inicia às 00:00), `RelogioDigital(int hora, int minuto)`, `avancarMinutos(int minutos)` e `toString()` no formato `HH:MM`.',
      businessRules: [
        'O construtor rejeita hora fora de 0..23 ou minuto fora de 0..59 com `IllegalArgumentException`.',
        '`avancarMinutos` rejeita minutos negativos com `IllegalArgumentException`; zero é uma operação sem efeito.',
        'Ao ultrapassar 23:59 o relógio volta a 00:00, mantendo o ciclo de 24 horas.'
      ],
      examples: [
        { input: 'new RelogioDigital(9, 55).avancarMinutos(10)', output: '"10:05"' },
        { input: 'new RelogioDigital(23, 50).avancarMinutos(20)', output: '"00:10"' }
      ],
      constraints: ['Formate sempre dois dígitos para hora e minuto.', 'Considere 60 minutos por hora e 24 horas por dia.']
    },
    descriptionMarkdown: '# Relógio Digital',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class RelogioDigital {',
      '',
      '    private int hora;',
      '    private int minuto;',
      '',
      '    public RelogioDigital() {',
      '        // TODO: inicie o relógio às 00:00',
      '    }',
      '',
      '    public RelogioDigital(int hora, int minuto) {',
      '        // TODO: valide hora (0..23) e minuto (0..59) e armazene o estado',
      '    }',
      '',
      '    public void avancarMinutos(int minutos) {',
      '        // TODO: avance o relógio com retorno à meia-noite (ciclo de 24h)',
      '    }',
      '',
      '    @Override',
      '    public String toString() {',
      '        // TODO: formate o horário como HH:MM',
      '        return "00:00";',
      '    }',
      '}'
    ].join('\n')
  },
  'formas-geometricas': {
    statement: {
      objective: 'Implemente a interface `Forma` em círculo e retângulo e calcule medidas de forma polimórfica.',
      instructions: 'O arquivo do desafio contém a interface `FormasGeometricas.Forma` (com `area()` e `perimetro()`) e as classes `Circulo(double raio)` e `Retangulo(double largura, altura)` que a implementam. Complete os quatro métodos: a área do círculo é `Math.PI * raio * raio` e o perímetro `2 * Math.PI * raio`; o retângulo usa `largura * altura` e `2 * (largura + altura)`.',
      businessRules: [
        'Use `Math.PI` em todos os cálculos que envolvem o círculo.',
        'As classes continuam implementando `Forma` e são utilizáveis através do tipo da interface.',
        'Uma lista de `Forma` deve somar as áreas de formas diferentes sem if por tipo.'
      ],
      examples: [
        { input: 'new Circulo(1.0)', output: 'area() == Math.PI e perimetro() == 2 * Math.PI' },
        { input: 'new Retangulo(3.0, 4.0)', output: 'area() == 12.0 e perimetro() == 14.0' }
      ],
      constraints: ['Considere raio, largura e altura não negativos.', 'Valores decimais são comparados com tolerância de 1e-9.']
    },
    descriptionMarkdown: '# Formas Geométricas',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class FormasGeometricas {',
      '',
      '    public interface Forma {',
      '        double area();',
      '        double perimetro();',
      '    }',
      '',
      '    public static class Circulo implements Forma {',
      '',
      '        private final double raio;',
      '',
      '        public Circulo(double raio) {',
      '            this.raio = raio;',
      '        }',
      '',
      '        @Override',
      '        public double area() {',
      '            // TODO: use Math.PI * raio * raio',
      '            return 0.0;',
      '        }',
      '',
      '        @Override',
      '        public double perimetro() {',
      '            // TODO: use 2 * Math.PI * raio',
      '            return 0.0;',
      '        }',
      '    }',
      '',
      '    public static class Retangulo implements Forma {',
      '',
      '        private final double largura;',
      '        private final double altura;',
      '',
      '        public Retangulo(double largura, double altura) {',
      '            this.largura = largura;',
      '            this.altura = altura;',
      '        }',
      '',
      '        @Override',
      '        public double area() {',
      '            // TODO: use largura * altura',
      '            return 0.0;',
      '        }',
      '',
      '        @Override',
      '        public double perimetro() {',
      '            // TODO: use 2 * (largura + altura)',
      '            return 0.0;',
      '        }',
      '    }',
      '}'
    ].join('\n')
  },
  'folha-pagamento': {
    statement: {
      objective: 'Modele funcionários com herança abstrata e calcule o total da folha de forma polimórfica.',
      instructions: 'Complete `salario()` em `Horista` (horas trabalhadas × valor da hora) e em `Assalariado` (salário mensal fixo), e implemente `totalDaFolha()` somando `salario()` de cada funcionário contratado, tratando todos através do tipo base `Funcionario`.',
      businessRules: [
        '`Funcionario` é abstrata, fornece `getNome()` e declara `salario()` como contrato.',
        '`Horista` recebe nome, horas e valorHora no construtor; `Assalariado` recebe nome e salário mensal.',
        'A folha vazia tem total 0.0.',
        'A soma deve funcionar sem distinguir o tipo concreto de cada funcionário.'
      ],
      examples: [
        { input: 'new Horista("Ana", 40, 25.0)', output: 'salario() == 1000.0' },
        { input: 'folha com Ana (Horista) e Bia (Assalariado de 3500.0)', output: 'totalDaFolha() == 4500.0' }
      ],
      constraints: ['Não altere a estrutura de classes fornecida.', 'Use a lista interna de funcionários já criada.']
    },
    descriptionMarkdown: '# Folha de Pagamento',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.ArrayList;',
      'import java.util.List;',
      '',
      'public class FolhaPagamento {',
      '',
      '    public abstract static class Funcionario {',
      '',
      '        private final String nome;',
      '',
      '        protected Funcionario(String nome) {',
      '            this.nome = nome;',
      '        }',
      '',
      '        public String getNome() {',
      '            return nome;',
      '        }',
      '',
      '        public abstract double salario();',
      '    }',
      '',
      '    public static class Horista extends Funcionario {',
      '',
      '        private final int horas;',
      '        private final double valorHora;',
      '',
      '        public Horista(String nome, int horas, double valorHora) {',
      '            super(nome);',
      '            this.horas = horas;',
      '            this.valorHora = valorHora;',
      '        }',
      '',
      '        @Override',
      '        public double salario() {',
      '            // TODO: devolva horas * valorHora',
      '            return 0.0;',
      '        }',
      '    }',
      '',
      '    public static class Assalariado extends Funcionario {',
      '',
      '        private final double salarioMensal;',
      '',
      '        public Assalariado(String nome, double salarioMensal) {',
      '            super(nome);',
      '            this.salarioMensal = salarioMensal;',
      '        }',
      '',
      '        @Override',
      '        public double salario() {',
      '            // TODO: devolva o salário mensal fixo',
      '            return 0.0;',
      '        }',
      '    }',
      '',
      '    private final List<Funcionario> funcionarios = new ArrayList<>();',
      '',
      '    public void contratar(Funcionario funcionario) {',
      '        funcionarios.add(funcionario);',
      '    }',
      '',
      '    public double totalDaFolha() {',
      '        // TODO: some salario() de cada funcionário contratado',
      '        return 0.0;',
      '    }',
      '}'
    ].join('\n')
  },
  biblioteca: {
    statement: {
      objective: 'Componha livros dentro de uma biblioteca e implemente as consultas de páginas e títulos.',
      instructions: 'Implemente `adicionar(String titulo, int paginas)` (cria um `Livro` interno e o registra), `totalDePaginas()` (soma das páginas de todos os livros), `possui(String titulo)` (busca exata por título) mantendo `quantidadeDeLivros()` já fornecido.',
      businessRules: [
        'A biblioteca compõe uma lista interna de `Livro`; cada chamada a `adicionar` cria um exemplar novo.',
        '`possui` compara o título exatamente, incluindo a caixa das letras.',
        'Livros com o mesmo título contam como exemplares distintos.'
      ],
      examples: [
        { input: 'adicionar("Clean Code", 400); adicionar("Refatoração", 440)', output: 'totalDePaginas() == 840 e quantidadeDeLivros() == 2' },
        { input: 'possui("Clean Code")', output: 'true' }
      ],
      constraints: ['Não exponha a lista mutável interna da biblioteca.', 'Títulos diferentes por caixa não devem ser encontrados.']
    },
    descriptionMarkdown: '# Biblioteca',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.ArrayList;',
      'import java.util.List;',
      '',
      'public class Biblioteca {',
      '',
      '    public static class Livro {',
      '',
      '        private final String titulo;',
      '        private final int paginas;',
      '',
      '        public Livro(String titulo, int paginas) {',
      '            this.titulo = titulo;',
      '            this.paginas = paginas;',
      '        }',
      '',
      '        public String getTitulo() {',
      '            return titulo;',
      '        }',
      '',
      '        public int getPaginas() {',
      '            return paginas;',
      '        }',
      '    }',
      '',
      '    private final List<Livro> livros = new ArrayList<>();',
      '',
      '    public void adicionar(String titulo, int paginas) {',
      '        // TODO: crie um Livro e guarde na lista interna',
      '    }',
      '',
      '    public int totalDePaginas() {',
      '        // TODO: some as páginas de todos os livros',
      '        return 0;',
      '    }',
      '',
      '    public boolean possui(String titulo) {',
      '        // TODO: procure um livro com o título exato',
      '        return false;',
      '    }',
      '',
      '    public int quantidadeDeLivros() {',
      '        return livros.size();',
      '    }',
      '}'
    ].join('\n')
  },
  'estoque-produtos': {
    statement: {
      objective: 'Componha produtos em um estoque com registro, retirada e valor total.',
      instructions: 'Implemente `registrar(String nome, double preco, int quantidade)` (adiciona um `Produto` à lista interna), `getProdutos()` (devolve uma cópia da lista), `retirar(String nome, int quantidade)` (usa o método `reduzir` do produto quando há saldo) e `valorEmEstoque()` (soma `preco * quantidade` de cada produto).',
      businessRules: [
        '`retirar` devolve `true` e reduz a quantidade quando o produto existe e há saldo suficiente.',
        '`retirar` devolve `false` sem alterar nada quando o produto não existe ou o saldo é insuficiente.',
        'O produto retirado é localizado pelo nome exato.',
        'Produtos registrados aparecem na ordem de registro em `getProdutos()`.'
      ],
      examples: [
        { input: 'registrar("Arroz", 2.5, 4); registrar("Feijão", 4.0, 2)', output: 'valorEmEstoque() == 18.0' },
        { input: 'registrar("Arroz", 2.5, 4); retirar("Arroz", 1)', output: 'true e a quantidade passa a 3' }
      ],
      constraints: ['Considere preços e quantidades válidos no registro.', 'A retirada de produto inexistente devolve `false`.']
    },
    descriptionMarkdown: '# Estoque de Produtos',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.ArrayList;',
      'import java.util.List;',
      '',
      'public class EstoqueProdutos {',
      '',
      '    public static class Produto {',
      '',
      '        private final String nome;',
      '        private final double preco;',
      '        private int quantidade;',
      '',
      '        public Produto(String nome, double preco, int quantidade) {',
      '            this.nome = nome;',
      '            this.preco = preco;',
      '            this.quantidade = quantidade;',
      '        }',
      '',
      '        public String getNome() {',
      '            return nome;',
      '        }',
      '',
      '        public double getPreco() {',
      '            return preco;',
      '        }',
      '',
      '        public int getQuantidade() {',
      '            return quantidade;',
      '        }',
      '',
      '        public void reduzir(int quantidade) {',
      '            if (quantidade > this.quantidade) {',
      '                throw new IllegalArgumentException("quantidade insuficiente");',
      '            }',
      '            this.quantidade -= quantidade;',
      '        }',
      '    }',
      '',
      '    private final List<Produto> produtos = new ArrayList<>();',
      '',
      '    public void registrar(String nome, double preco, int quantidade) {',
      '        // TODO: adicione um novo Produto à lista interna',
      '    }',
      '',
      '    public List<Produto> getProdutos() {',
      '        // TODO: devolva uma cópia defensiva da lista de produtos',
      '        return List.of();',
      '    }',
      '',
      '    public boolean retirar(String nome, int quantidade) {',
      '        // TODO: encontre o produto, confira o saldo e chame reduzir',
      '        return false;',
      '    }',
      '',
      '    public double valorEmEstoque() {',
      '        // TODO: some preco * quantidade de cada produto',
      '        return 0.0;',
      '    }',
      '}'
    ].join('\n')
  }
};

const DATA_STRUCTURE_CHALLENGES: Record<string, Pick<Challenge, 'statement' | 'descriptionMarkdown' | 'starterCode'>> = {
  'busca-binaria': {
    statement: {
      objective: 'Encontre a posição de um valor em um array ordenado com a técnica da busca binária.',
      instructions: 'Implemente `buscaBinaria(int[] numeros, int alvo)` para arrays em ordem crescente. Retorne o índice do alvo ou `-1` quando não existir.',
      businessRules: [
        'Compare o elemento do meio e descarte metade do intervalo a cada passo.',
        'O array chega ordenado em ordem crescente.',
        'Retorne `-1` quando o alvo não estiver presente.'
      ],
      examples: [
        { input: 'numeros = [1, 3, 5, 7, 9], alvo = 7', output: '3' },
        { input: 'numeros = [1, 3, 5], alvo = 4', output: '-1' }
      ],
      constraints: ['Um array vazio devolve `-1`.', 'A busca deve custar O(log n) — sem percorrer o array inteiro.']
    },
    descriptionMarkdown: '# Busca Binária',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class BuscaBinaria {',
      '',
      '    public int buscaBinaria(int[] numeros, int alvo) {',
      '        // TODO: compare o meio e siga apenas metade do intervalo',
      '        return -1;',
      '    }',
      '}'
    ].join('\n')
  },
  'ordenacao-bolha': {
    statement: {
      objective: 'Ordene uma cópia dos valores em ordem crescente usando a ordenação por bolha.',
      instructions: 'Implemente `ordenarCrescente(int[] numeros)` devolvendo um NOVO array com os valores em ordem crescente — a entrada não pode ser alterada.',
      businessRules: [
        'Compare vizinhos e troque quando o da esquerda for maior.',
        'Repita as passadas até uma rodada completa sem nenhuma troca.',
        'Devolva um array novo — não modifique a entrada.'
      ],
      examples: [
        { input: '[5, 1, 4, 2]', output: '[1, 2, 4, 5]' },
        { input: '[3, -1, 3, 0]', output: '[-1, 0, 3, 3]' }
      ],
      constraints: ['Arrays vazios e com um elemento já saem prontos.', 'Valores repetidos mantêm a contagem original.']
    },
    descriptionMarkdown: '# Ordenação por Bolha',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class OrdenacaoBolha {',
      '',
      '    public int[] ordenarCrescente(int[] numeros) {',
      '        // TODO: compare vizinhos, troque e repita até ordenar; devolva um array novo',
      '        return numeros;',
      '    }',
      '}'
    ].join('\n')
  },
  'remover-duplicados': {
    statement: {
      objective: 'Remova valores repetidos de uma lista ordenada preservando a ordem.',
      instructions: 'Implemente `removerDuplicados(List<Integer> numeros)` devolvendo uma NOVA lista com cada valor apenas uma vez. A entrada chega ordenada em ordem crescente.',
      businessRules: [
        'Compare cada valor com o anterior mantido no resultado.',
        'Repetições consecutivas aparecem só uma vez.',
        'Devolva uma lista nova — não altere a entrada.'
      ],
      examples: [
        { input: '[1, 1, 2, 3, 3]', output: '[1, 2, 3]' },
        { input: '[1, 2, 3]', output: '[1, 2, 3]' }
      ],
      constraints: ['Listas vazias devolvem lista vazia.', 'A ordem crescente original é mantida.']
    },
    descriptionMarkdown: '# Remover Duplicados',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.List;',
      '',
      'public class RemoverDuplicados {',
      '',
      '    public List<Integer> removerDuplicados(List<Integer> numeros) {',
      '        // TODO: mantenha apenas o primeiro valor de cada sequência repetida',
      '        return numeros;',
      '    }',
      '}'
    ].join('\n')
  },
  'interseccao-listas': {
    statement: {
      objective: 'Extraia os valores comuns de duas listas sem repetir resultados.',
      instructions: 'Implemente `interseccao(List<Integer> a, List<Integer> b)` devolvendo uma lista com os valores presentes nas duas entradas, na ordem da primeira ocorrência em `a`, sem duplicar.',
      businessRules: [
        'Um valor de `a` entra no resultado somente se também aparece em `b`.',
        'Repetições dentro de `a` não duplicam o resultado.',
        'A ordem segue a primeira ocorrência em `a`.'
      ],
      examples: [
        { input: 'a = [1, 2, 3], b = [2, 3, 4]', output: '[2, 3]' },
        { input: 'a = [2, 2, 3], b = [2]', output: '[2]' }
      ],
      constraints: ['Qualquer lista vazia resulta em lista vazia.', 'Valores iguais em posições diferentes contam uma única vez.']
    },
    descriptionMarkdown: '# Interseção de Listas',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.List;',
      '',
      'public class InterseccaoListas {',
      '',
      '    public List<Integer> interseccao(List<Integer> a, List<Integer> b) {',
      '        // TODO: percorra a mantendo apenas valores presentes em b, sem repetir',
      '        return List.of();',
      '    }',
      '}'
    ].join('\n')
  },
  'contagem-caracteres': {
    statement: {
      objective: 'Calcule a frequência de cada caractere de um texto com um mapa.',
      instructions: 'Implemente `contarCaracteres(String texto)` devolvendo um `Map<Character, Integer>` com quantas vezes cada caractere aparece — espaços contam e maiúsculas são diferentes de minúsculas.',
      businessRules: [
        'Cada caractere é contado individualmente, inclusive espaços.',
        'A comparação diferencia maiúsculas de minúsculas.',
        'Texto vazio devolve mapa vazio.'
      ],
      examples: [
        { input: '"aab"', output: '{a=2, b=1}' },
        { input: '"Aa"', output: '{A=1, a=1}' }
      ],
      constraints: ['Não descarte nenhum caractere.', 'A chave do mapa é o próprio caractere.']
    },
    descriptionMarkdown: '# Contagem de Caracteres',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.Map;',
      '',
      'public class ContagemCaracteres {',
      '',
      '    public Map<Character, Integer> contarCaracteres(String texto) {',
      '        // TODO: acumule a frequência de cada caractere no mapa',
      '        return Map.of();',
      '    }',
      '}'
    ].join('\n')
  },
  anagramas: {
    statement: {
      objective: 'Agrupe palavras que são anagramas entre si preservando a ordem original.',
      instructions: 'Implemente `agruparAnagramas(List<String> palavras)` devolvendo uma lista de grupos: cada grupo reúne palavras com exatamente as mesmas letras, na ordem em que aparecem na entrada.',
      businessRules: [
        'A comparação é exata quanto a maiúsculas e minúsculas.',
        'Os grupos seguem a ordem da primeira ocorrência de cada palavra.',
        'Dentro de um grupo, mantenha a ordem original das palavras.'
      ],
      examples: [
        { input: '["eat", "tea", "tan"]', output: '[["eat", "tea"], ["tan"]]' },
        { input: '["abc"]', output: '[["abc"]]' }
      ],
      constraints: ['Lista vazia devolve lista vazia.', 'Normalize a chave de agrupamento (ex.: ordenando os caracteres).']
    },
    descriptionMarkdown: '# Detector de Anagramas',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.List;',
      '',
      'public class Anagramas {',
      '',
      '    public List<List<String>> agruparAnagramas(List<String> palavras) {',
      '        // TODO: normalize cada palavra (ex.: ordene os caracteres) e agrupe por chave',
      '        return List.of();',
      '    }',
      '}'
    ].join('\n')
  },
  'par-com-soma': {
    statement: {
      objective: 'Encontre dois elementos de um array ordenado que somam o valor alvo.',
      instructions: 'Implemente `parComSoma(int[] numerosOrdenados, int alvo)` devolvendo um array com os DOIS ÍNDICES `i < j` cujos valores somam o alvo. Havendo mais de um par, devolva o de menor `i` (e menor `j`); sem par, devolva `new int[0]`.',
      businessRules: [
        'O array chega ordenado em ordem crescente.',
        'Use dois ponteiros: um no início e um no fim, movendo conforme a soma.',
        'Os índices devem ser crescentes (`i < j`).'
      ],
      examples: [
        { input: 'numeros = [1, 2, 3, 4], alvo = 5', output: '[0, 3] (1 + 4)' },
        { input: 'numeros = [1, 2, 3], alvo = 100', output: '[]' }
      ],
      constraints: ['Arrays de um elemento não têm par.', 'O par de menor índice inicial tem prioridade.']
    },
    descriptionMarkdown: '# Par com Soma',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class ParComSoma {',
      '',
      '    public int[] parComSoma(int[] numerosOrdenados, int alvo) {',
      '        // TODO: mova ponteiros do início e do fim conforme a soma cresça ou decresça',
      '        return new int[0];',
      '    }',
      '}'
    ].join('\n')
  },
  'janela-maior-soma': {
    statement: {
      objective: 'Encontre a maior soma entre janelas contíguas de tamanho fixo.',
      instructions: 'Implemente `maiorSomaJanela(int[] numeros, int tamanho)` retornando a maior soma de qualquer subarray contíguo com exatamente `tamanho` elementos. Lance `IllegalArgumentException` quando `tamanho < 1` ou `tamanho > numeros.length`.',
      businessRules: [
        'A janela tem tamanho fixo e percorre o array da esquerda para a direita.',
        'Valores negativos participam — a maior soma pode ser negativa.',
        'Tamanho inválido lança `IllegalArgumentException`.'
      ],
      examples: [
        { input: 'numeros = [1, 2, 3, 4], tamanho = 2', output: '7 (3 + 4)' },
        { input: 'numeros = [-1, -2, -3, -4], tamanho = 2', output: '-3 (-1 + -2)' }
      ],
      constraints: ['Reaproveite a soma da janela anterior (sliding window).', 'Só valide depois de conferir `tamanho` contra o array.']
    },
    descriptionMarkdown: '# Janela de Maior Soma',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class JanelaMaiorSoma {',
      '',
      '    public int maiorSomaJanela(int[] numeros, int tamanho) {',
      '        // TODO: valide a janela, calcule a soma inicial e deslize somando o novo e saindo o antigo',
      '        return 0;',
      '    }',
      '}'
    ].join('\n')
  },
  fibonacci: {
    statement: {
      objective: 'Calcule o n-ésimo número de Fibonacci por recursão validando a entrada.',
      instructions: 'Implemente `fibonacci(int n)` recursivamente: `fibonacci(0) = 0`, `fibonacci(1) = 1` e `fibonacci(n) = fibonacci(n - 1) + fibonacci(n - 2)`. Lance `IllegalArgumentException` para `n < 0`.',
      businessRules: [
        'Casos-base: 0 devolve 0 e 1 devolve 1.',
        'Cada chamada soma as duas anteriores.',
        '`n` negativo lança `IllegalArgumentException`.'
      ],
      examples: [
        { input: '10', output: '55' },
        { input: '0', output: '0' }
      ],
      constraints: ['Use recursão — sem laços.', 'Valores até 20 cabem em `long`.']
    },
    descriptionMarkdown: '# Fibonacci Recursivo',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class Fibonacci {',
      '',
      '    public long fibonacci(int n) {',
      '        // TODO: trate os casos-base e some as duas chamadas anteriores',
      '        return 0;',
      '    }',
      '}'
    ].join('\n')
  },
  'caminhos-escada': {
    statement: {
      objective: 'Conte quantos caminhos existem para subir uma escada avançando 1 ou 2 degraus.',
      instructions: 'Implemente `caminhosEscada(int degraus)` devolvendo o número de modos de chegar ao topo. `caminhosEscada(0)` é 1 (o caminho vazio) e degraus negativos lançam `IllegalArgumentException`.',
      businessRules: [
        'De cada posição você avança 1 ou 2 passos.',
        'A relação é a de Fibonacci: `f(n) = f(n - 1) + f(n - 2)`.',
        'Zero degraus tem exatamente 1 caminho.'
      ],
      examples: [
        { input: '2', output: '2 (1+1 ou 2)' },
        { input: '5', output: '8' }
      ],
      constraints: ['Resolva com recursão.', 'Valores negativos lançam `IllegalArgumentException`.']
    },
    descriptionMarkdown: '# Caminhos na Escada',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class CaminhosEscada {',
      '',
      '    public long caminhosEscada(int degraus) {',
      '        // TODO: some os caminhos de (degraus - 1) e (degraus - 2) com casos-base',
      '        return 0;',
      '    }',
      '}'
    ].join('\n')
  },
  'busca-largura': {
    statement: {
      objective: 'Meça a menor distância em arestas entre dois vértices de um grafo não ponderado.',
      instructions: 'Implemente `distancia(int vertices, int[][] arestas, int origem, int destino)` com BFS: cada aresta `[de, para]` é bidirecional. Retorne o número de arestas do caminho mais curto, `0` quando origem = destino e `-1` quando inalcançável.',
      businessRules: [
        'O grafo é não ponderado — cada aresta vale 1 passo.',
        'Arestas são bidirecionais.',
        'Vértices são identificados de 0 a `vertices - 1`.'
      ],
      examples: [
        { input: 'vértices = 4, arestas = [[0,1],[1,2],[2,3]], origem = 0, destino = 3', output: '3' },
        { input: 'vértices = 3, arestas = [[0,1]], origem = 0, destino = 2', output: '-1' }
      ],
      constraints: ['Use uma fila (BFS), não recursão.', 'Visite cada vértice no máximo uma vez.']
    },
    descriptionMarkdown: '# Busca em Largura',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'public class BuscaLargura {',
      '',
      '    public int distancia(int vertices, int[][] arestas, int origem, int destino) {',
      '        // TODO: faça BFS a partir da origem registrando a distância de cada vértice',
      '        return -1;',
      '    }',
      '}'
    ].join('\n')
  },
  'menor-caminho': {
    statement: {
      objective: 'Implemente Dijkstra para devolver a sequência de vértices do caminho de menor custo.',
      instructions: 'Implemente `menorCaminho(int vertices, int[][] arestas, int origem, int destino)` onde cada aresta é `[de, para, peso]` com peso não negativo, ligando os dois vértices nos dois sentidos. Devolva a lista de vértices percorridos, incluindo origem e destino.',
      businessRules: [
        'Pesos são não negativos — Dijkstra é válido.',
        'Sem caminho até o destino devolve lista vazia.',
        'Origem igual ao destino devolve a lista com um único vértice.'
      ],
      examples: [
        { input: 'vértices = 3, arestas = [[0,1,1],[1,2,1],[0,2,10]], origem = 0, destino = 2', output: '[0, 1, 2] (custo 2)' },
        { input: 'vértices = 3, arestas = [[0,1,1]], origem = 0, destino = 2', output: '[]' }
      ],
      constraints: ['Use uma fila de prioridade ordenada pela distância acumulada.', 'Pesos negativos não são suportados — não faça validação extra.', 'Caminhos ótimos com mesmo custo podem devolver qualquer ordem válida.']
    },
    descriptionMarkdown: '# Menor Caminho em Grafo',
    starterCode: [
      'package br.com.eyecode.challenge;',
      '',
      'import java.util.List;',
      '',
      'public class MenorCaminho {',
      '',
      '    public List<Integer> menorCaminho(int vertices, int[][] arestas, int origem, int destino) {',
      '        // TODO: execute Dijkstra com fila de prioridade pela distância acumulada',
      '        return List.of();',
      '    }',
      '}'
    ].join('\n')
  }
};

export function challengeFor(id: string | null | undefined): Challenge {
  if (!id || id === CNPJ_VALIDATOR_CHALLENGE.id) return CNPJ_VALIDATOR_CHALLENGE;
  const catalog = CHALLENGE_CATALOG.find(item => item.id === id);
  if (!catalog) return CNPJ_VALIDATOR_CHALLENGE;
  const title = catalog.title;
  const className = id.split('-').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join('');
  const details = FUNDAMENTALS_CHALLENGES[id] ?? OOP_CHALLENGES[id] ?? DATA_STRUCTURE_CHALLENGES[id];
  const oopStarter = OOP_CHALLENGES[id]?.starterCode ?? DATA_STRUCTURE_CHALLENGES[id]?.starterCode;
  const starterCode = oopStarter ?? (id === 'fizzbuzz'
    ? `package br.com.eyecode.challenge;\n\nimport java.util.List;\n\npublic class ${className} {\n    public List<String> fizzBuzz(int start, int end) {\n        return List.of();\n    }\n}`
    : id === 'conversor-temperatura'
      ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public double toFahrenheit(double celsius) {\n        return 0.0;\n    }\n}`
      : id === 'ano-bissexto'
        ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public boolean isLeapYear(int year) {\n        return false;\n    }\n}`
        : id === 'media-aprovacao'
          ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public boolean isApproved(double[] grades) {\n        return false;\n    }\n}`
          : id === 'palindromo'
            ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public boolean isPalindrome(String text) {\n        return false;\n    }\n}`
            : `package br.com.eyecode.challenge;\n\npublic class ${className} {\n\n    public boolean solve(String input) {\n        return false;\n    }\n}`);
  return {
    id,
    title,
    entryClassName: className,
    descriptionMarkdown: details?.descriptionMarkdown ?? `# ${title}\n\n${catalog.summary}`,
    starterCode,
    statement: details?.statement ?? { objective: catalog.summary },
    metadata: { language: 'Java' }
  };
}
