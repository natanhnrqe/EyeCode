import type { Challenge, TestResult } from './types';

export const CNPJ_VALIDATOR_CHALLENGE: Challenge = {
  id: 'cnpj-validator',
  title: 'Validador de CNPJ',
  entryClassName: 'CnpjValidator',
  metadata: {
    track: 'Fundamentos de Java',
    topic: 'Strings e validação',
    language: 'Java',
    documentationReferences: [{
      title: 'Java SE 21 — String',
      url: 'https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html',
      relevance: 'Referência para operações com cadeias usadas no tratamento do CNPJ.'
    }]
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
    'public class CnpjValidator {',
    '',
    '    public boolean isValid(String cnpj) {',
    '        // TODO: implementar a validação de CNPJ',
    '        return false;',
    '    }',
    '}'
  ].join('\n')
};

export const HIDDEN_TESTS: TestResult[] = [
  { id: 'deveValidarCnpjValido', name: 'deveValidarCnpjValido', status: 'pending' },
  { id: 'deveAceitarCnpjComPontuacao', name: 'deveAceitarCnpjComPontuacao', status: 'pending' },
  { id: 'deveRejeitarDigitoVerificadorInvalido', name: 'deveRejeitarDigitoVerificadorInvalido', status: 'pending' }
];
