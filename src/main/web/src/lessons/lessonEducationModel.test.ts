import { describe, expect, it } from 'vitest';
import type { DiagnosticsViewState, WebDiagnostic } from '../diagnostics/protocol';
import { selectLessonDiagnosticEducation } from './lessonEducationModel';

function diagnostic(severity: WebDiagnostic['severity'], line: number, education?: WebDiagnostic['education']): WebDiagnostic {
  return {
    severity, code: '', message: `problema na linha ${line}`,
    startLine: line, startColumn: 3, endLine: line, endColumn: 10,
    education
  };
}

const education = {
  title: 'Falta o ponto e vírgula',
  summary: 'Toda instrução termina com ;',
  explanation: 'O compilador esperava um ; nesta linha.',
  pattern: 'int a = 1', correctedPattern: 'int a = 1;', tip: 'Confira o fim da linha.',
  relatedContent: []
};

const state: DiagnosticsViewState = {
  activeUri: null, active: null,
  results: [
    {
      uri: 'lesson://java.fundamentals.variables/Main.java', requestId: '1', modelVersion: 1,
      diagnostics: [
        diagnostic('WARNING', 7),
        diagnostic('ERROR', 3, education),
        diagnostic('ERROR', 9, education)
      ]
    },
    {
      uri: 'lesson://outra/Other.java', requestId: '2', modelVersion: 1,
      diagnostics: [diagnostic('ERROR', 1, education)]
    }
  ]
};

describe('selectLessonDiagnosticEducation', () => {
  it('seleciona o erro mais severo e mais cedo do arquivo da lição', () => {
    const selected = selectLessonDiagnosticEducation(state, 'lesson://java.fundamentals.variables/Main.java');
    expect(selected?.range.startLineNumber).toBe(3);
    expect(selected?.title).toBe('Falta o ponto e vírgula');
    expect(selected?.tip).toBe('Confira o fim da linha.');
  });

  it('ignora diagnostics de outros arquivos', () => {
    expect(selectLessonDiagnosticEducation(state, 'lesson://nao-existe/Main.java')).toBeNull();
  });

  it('ignora diagnostics sem explicação pedagógica', () => {
    const onlyWarning: DiagnosticsViewState = {
      ...state, results: [{ ...state.results[0], diagnostics: [diagnostic('WARNING', 7)] }]
    };
    expect(selectLessonDiagnosticEducation(onlyWarning, 'lesson://java.fundamentals.variables/Main.java')).toBeNull();
  });

  it('retorna null sem estado ou uri', () => {
    expect(selectLessonDiagnosticEducation(null, 'lesson://a/Main.java')).toBeNull();
    expect(selectLessonDiagnosticEducation(state, null)).toBeNull();
  });

  it('converte a posição para o formato do editor', () => {
    const selected = selectLessonDiagnosticEducation(state, 'lesson://java.fundamentals.variables/Main.java');
    expect(selected?.range).toEqual({ startLineNumber: 3, startColumn: 3, endLineNumber: 3, endColumn: 10 });
  });
});
