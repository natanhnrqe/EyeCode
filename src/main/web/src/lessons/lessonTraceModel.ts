import type { LessonTracePayload, LessonTraceStatus, LessonTraceStep } from './protocol';

export type LessonTraceViewStep = LessonTraceStep & {
  previous: LessonTraceStep | null;
  changed: string[];
  enteredScope: string[];
  leftScope: string[];
};

export type LessonTraceView = {
  steps: LessonTraceViewStep[];
  status: LessonTraceStatus;
  runtimeError: string;
  limitations: string[];
  hasVariables: boolean;
};

export type LessonTracePhase = 'idle' | 'capturing' | 'available' | 'failed';

export function buildLessonTraceView(trace: LessonTracePayload): LessonTraceView {
  const steps: LessonTraceViewStep[] = [];
  let previous: LessonTraceStep | null = null;
  for (const step of trace.steps) {
    steps.push({
      ...step,
      previous,
      changed: variableChanges(previous, step),
      enteredScope: variablesEnteringScope(previous, step),
      leftScope: variablesLeavingScope(previous, step)
    });
    previous = step;
  }
  return {
    steps,
    status: trace.status,
    runtimeError: trace.runtimeError,
    limitations: trace.limitations,
    hasVariables: steps.some(step => step.variables.length > 0)
  };
}

export function variableChanges(previous: LessonTraceStep | null, current: LessonTraceStep): string[] {
  if (!previous) return [];
  const before = variableMap(previous);
  return current.variables
    .filter(variable => before.get(variable.name)?.value !== variable.value)
    .map(variable => variable.name);
}

export function variablesEnteringScope(previous: LessonTraceStep | null, current: LessonTraceStep): string[] {
  if (!previous) return current.variables.map(variable => variable.name);
  const before = variableMap(previous);
  return current.variables
    .filter(variable => !before.has(variable.name))
    .map(variable => variable.name);
}

export function variablesLeavingScope(previous: LessonTraceStep | null, current: LessonTraceStep): string[] {
  if (!previous) return [];
  const now = variableMap(current);
  return previous.variables
    .filter(variable => !now.has(variable.name))
    .map(variable => variable.name);
}

export function lessonTracePhase(capturing: boolean, trace: LessonTracePayload | null): LessonTracePhase {
  if (capturing) return 'capturing';
  if (!trace) return 'idle';
  if (trace.status === 'COMPILE_ERROR' || trace.status === 'FAILED') return 'failed';
  return 'available';
}

export function describeLessonTraceStatus(status: LessonTraceStatus): string {
  switch (status) {
    case 'COMPLETED': return 'Execução concluída';
    case 'TRUNCATED': return 'Traço truncado — o limite de passos foi atingido antes do fim do programa';
    case 'COMPILE_ERROR': return 'O programa não compilou — corrija os erros para capturar a execução';
    case 'RUNTIME_ERROR': return 'A execução foi interrompida por um erro';
    case 'STOPPED': return 'Captura interrompida';
    case 'TIMEOUT': return 'A execução demorou demais e foi encerrada';
    case 'FAILED': return 'Não foi possível capturar a execução';
  }
}

export function defaultStepIndex(view: LessonTraceView): number | null {
  return view.steps.length ? view.steps[view.steps.length - 1].index : null;
}

export function stepByIndex(view: LessonTraceView, index: number | null): LessonTraceViewStep | null {
  return view.steps.find(step => step.index === index) ?? view.steps[view.steps.length - 1] ?? null;
}

function variableMap(step: LessonTraceStep): Map<string, { value: string }> {
  return new Map(step.variables.map(variable => [variable.name, { value: variable.value }]));
}
