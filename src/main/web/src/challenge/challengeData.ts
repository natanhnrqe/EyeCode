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

export function challengeFor(id: string | null | undefined): Challenge {
  if (!id || id === CNPJ_VALIDATOR_CHALLENGE.id) return CNPJ_VALIDATOR_CHALLENGE;
  const catalog = CHALLENGE_CATALOG.find(item => item.id === id);
  if (!catalog) return CNPJ_VALIDATOR_CHALLENGE;
  const title = catalog.title;
  const className = id.split('-').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join('');
  const details = FUNDAMENTALS_CHALLENGES[id] ?? OOP_CHALLENGES[id];
  const oopStarter = OOP_CHALLENGES[id]?.starterCode;
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
