import { describe, expect, it } from 'vitest';
import { parseLessonRunDiagnostics } from './lessonRunDiagnostics';

const entry = {
  uri: 'lesson://variables/s1/main',
  severity: 'ERROR',
  code: 'compiler.err.prob.assign.incompat.types',
  message: 'incompatible types: java.lang.String cannot be converted to int',
  startLine: 3, startColumn: 5, endLine: 3, endColumn: 20,
  education: {
    title: 'Tipos incompatíveis',
    summary: 'O valor não cabe no tipo da variável.',
    explanation: 'Java não converte automaticamente entre tipos incompatíveis.',
    pattern: 'int preco = 12.5;', correctedPattern: 'double preco = 12.5;',
    tip: 'Use o tipo adequado ao valor.',
    relatedContent: [{ id: 'java/jdk/fundamentos/variables', title: 'Variáveis', description: 'Tipos' }]
  }
};

describe('parseLessonRunDiagnostics', () => {
  it('agrupa por uri preservando mensagem, posição e educação', () => {
    const groups = parseLessonRunDiagnostics({ diagnostics: [entry] });
    expect(groups).toHaveLength(1);
    expect(groups[0].uri).toBe('lesson://variables/s1/main');
    const diagnostic = groups[0].diagnostics[0];
    expect(diagnostic.severity).toBe('ERROR');
    expect(diagnostic.startLine).toBe(3);
    expect(diagnostic.message).toContain('incompatible types');
    expect(diagnostic.education?.title).toBe('Tipos incompatíveis');
    expect(diagnostic.education?.relatedContent[0].id).toBe('java/jdk/fundamentos/variables');
  });

  it('agrupa múltiplas entradas do mesmo arquivo na ordem recebida', () => {
    const groups = parseLessonRunDiagnostics({ diagnostics: [
      { ...entry, startLine: 3 },
      { ...entry, startLine: 7, message: "';' expected" }
    ] });
    expect(groups).toHaveLength(1);
    expect(groups[0].diagnostics).toHaveLength(2);
    expect(groups[0].diagnostics[1].startLine).toBe(7);
  });

  it('descarta entradas sem uri, sem mensagem ou sem linha (não inventa local)', () => {
    expect(parseLessonRunDiagnostics({ diagnostics: [{ ...entry, uri: '' }] })).toHaveLength(0);
    expect(parseLessonRunDiagnostics({ diagnostics: [{ ...entry, message: '' }] })).toHaveLength(0);
    expect(parseLessonRunDiagnostics({ diagnostics: [{ ...entry, startLine: 0 }] })).toHaveLength(0);
  });

  it('separa arquivos diferentes', () => {
    const groups = parseLessonRunDiagnostics({ diagnostics: [
      entry,
      { ...entry, uri: 'lesson://variables/s1/helper' }
    ] });
    expect(groups).toHaveLength(2);
  });

  it('tolera payloads inválidos sem fabricar diagnósticos', () => {
    expect(parseLessonRunDiagnostics(null)).toEqual([]);
    expect(parseLessonRunDiagnostics({})).toEqual([]);
    expect(parseLessonRunDiagnostics({ diagnostics: 'nope' })).toEqual([]);
    expect(parseLessonRunDiagnostics({ diagnostics: [null, 42, 'x'] })).toEqual([]);
  });

  it('sem educação a entrada continua válida', () => {
    const groups = parseLessonRunDiagnostics({ diagnostics: [{ ...entry, education: undefined }] });
    expect(groups[0].diagnostics[0].education).toBeUndefined();
  });
});
