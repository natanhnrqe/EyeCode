import type { Challenge, TestResult } from './types';

export const CNPJ_VALIDATOR_CHALLENGE: Challenge = {
  id: 'cnpj-validator',
  title: 'Validador de CNPJ',
  entryClassName: 'CnpjValidator',
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
