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

export function challengeFor(id: string | null | undefined): Challenge {
  if (!id || id === CNPJ_VALIDATOR_CHALLENGE.id) return CNPJ_VALIDATOR_CHALLENGE;
  const catalog = CHALLENGE_CATALOG.find(item => item.id === id);
  if (!catalog) return CNPJ_VALIDATOR_CHALLENGE;
  const title = catalog.title;
  const className = id.split('-').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join('');
  const fundamentals = FUNDAMENTALS_CHALLENGES[id];
  const starterCode = id === 'fizzbuzz'
    ? `package br.com.eyecode.challenge;\n\nimport java.util.List;\n\npublic class ${className} {\n    public List<String> fizzBuzz(int start, int end) {\n        return List.of();\n    }\n}`
    : id === 'conversor-temperatura'
      ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public double toFahrenheit(double celsius) {\n        return 0.0;\n    }\n}`
      : id === 'ano-bissexto'
        ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public boolean isLeapYear(int year) {\n        return false;\n    }\n}`
        : id === 'media-aprovacao'
          ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public boolean isApproved(double[] grades) {\n        return false;\n    }\n}`
          : id === 'palindromo'
            ? `package br.com.eyecode.challenge;\n\npublic class ${className} {\n    public boolean isPalindrome(String text) {\n        return false;\n    }\n}`
            : `package br.com.eyecode.challenge;\n\npublic class ${className} {\n\n    public boolean solve(String input) {\n        return false;\n    }\n}`;
  return {
    id,
    title,
    entryClassName: className,
    descriptionMarkdown: fundamentals?.descriptionMarkdown ?? `# ${title}\n\n${catalog.summary}`,
    starterCode,
    statement: fundamentals?.statement ?? { objective: catalog.summary },
    metadata: { language: 'Java' }
  };
}
