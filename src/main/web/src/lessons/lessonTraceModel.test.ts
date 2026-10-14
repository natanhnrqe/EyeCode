import { describe, expect, it } from 'vitest';
import type { LessonTracePayload, LessonTraceStep } from './protocol';
import {
  buildLessonTraceView, defaultStepIndex, describeLessonTraceStatus, lessonTracePhase, stepByIndex,
  variableChanges, variablesEnteringScope, variablesLeavingScope
} from './lessonTraceModel';

function step(index: number, line: number, variables: Array<[name: string, value: string]>): LessonTraceStep {
  return {
    index, className: 'Main', line, sampled: false,
    variables: variables.map(([name, value]) => ({
      name, type: 'int', value, kind: 'primitive', children: [], truncated: false
    }))
  };
}

const trace: LessonTracePayload = {
  traceId: 1, mainClass: 'Main', status: 'COMPLETED', exitCode: 0, runtimeError: '', runtimeErrorLine: -1,
  steps: [
    step(0, 3, []),
    step(1, 4, [['total', '10']]),
    step(2, 5, [['total', '10'], ['inc', '5']]),
    step(3, 6, [['total', '15'], ['inc', '5']])
  ],
  output: [{ text: '15\n', error: false }],
  limitations: []
};

describe('buildLessonTraceView', () => {
  it('liga cada passo ao anterior com as mudanças reais', () => {
    const view = buildLessonTraceView(trace);
    expect(view.steps).toHaveLength(4);
    expect(view.steps[0].previous).toBeNull();
    expect(view.steps[3].previous?.index).toBe(2);
    expect(view.steps[3].changed).toEqual(['total']);
    expect(view.steps[3].enteredScope).toEqual([]);
    expect(view.steps[1].enteredScope).toEqual(['total']);
    expect(view.steps[2].enteredScope).toEqual(['inc']);
    expect(view.hasVariables).toBe(true);
  });

  it('detecta variáveis que saíram de escopo', () => {
    const scoped: LessonTracePayload = {
      ...trace,
      steps: [
        step(0, 3, [['total', '10']]),
        step(1, 4, [])
      ]
    };
    const view = buildLessonTraceView(scoped);
    expect(view.steps[1].leftScope).toEqual(['total']);
  });
});

describe('variableChanges', () => {
  it('ignora valores iguais e lista apenas o que mudou', () => {
    const current = step(1, 4, [['total', '15'], ['inc', '5']]);
    const previous = step(0, 3, [['total', '10'], ['inc', '5']]);
    expect(variableChanges(previous, current)).toEqual(['total']);
  });

  it('retorna vazio quando não há passo anterior', () => {
    expect(variableChanges(null, step(0, 3, [['total', '10']]))).toEqual([]);
  });
});

describe('lessonTracePhase', () => {
  it('deriva os estados explícitos da aba', () => {
    expect(lessonTracePhase(false, null)).toBe('idle');
    expect(lessonTracePhase(true, null)).toBe('capturing');
    expect(lessonTracePhase(true, trace)).toBe('capturing');
    expect(lessonTracePhase(false, trace)).toBe('available');
    expect(lessonTracePhase(false, { ...trace, status: 'COMPILE_ERROR' })).toBe('failed');
    expect(lessonTracePhase(false, { ...trace, status: 'FAILED' })).toBe('failed');
    expect(lessonTracePhase(false, { ...trace, status: 'RUNTIME_ERROR' })).toBe('available');
  });
});

describe('describeLessonTraceStatus', () => {
  it('explica cada status em linguagem simples', () => {
    expect(describeLessonTraceStatus('COMPLETED')).toBe('Execução concluída');
    expect(describeLessonTraceStatus('TRUNCATED')).toContain('limite de passos');
    expect(describeLessonTraceStatus('COMPILE_ERROR')).toContain('não compilou');
    expect(describeLessonTraceStatus('RUNTIME_ERROR')).toContain('erro');
    expect(describeLessonTraceStatus('STOPPED')).toContain('interrompida');
    expect(describeLessonTraceStatus('TIMEOUT')).toContain('demorou demais');
  });
});

describe('seleção de passo', () => {
  const view = buildLessonTraceView(trace);

  it('padrão é o último passo (estado final observado)', () => {
    expect(defaultStepIndex(view)).toBe(3);
  });

  it('localiza o passo pelo índice e cai no último quando ausente', () => {
    expect(stepByIndex(view, 2)?.line).toBe(5);
    expect(stepByIndex(view, null)?.index).toBe(3);
    expect(stepByIndex(view, 99)?.index).toBe(3);
  });
});

describe('amostragem dentro de loops', () => {
  it('mantém a marcação de amostra e deriva as mudanças reais entre amostras', () => {
    const sampled: LessonTracePayload = {
      ...trace,
      steps: [
        { ...step(0, 5, [['contador', '0']]), sampled: true },
        { ...step(1, 5, [['contador', '1200']]), sampled: true },
        { ...step(2, 5, [['contador', '1200']]), sampled: true }
      ]
    };
    const view = buildLessonTraceView(sampled);
    expect(view.steps.every(entry => entry.sampled)).toBe(true);
    expect(view.steps[1].changed).toEqual(['contador']);
    expect(view.steps[2].changed).toEqual([]);
  });
});
