import { useEffect, useMemo, useRef, useState } from 'react';
import { ProblemsPanel, toProblems, type Problem } from '../diagnostics/ProblemsPanel';
import type { DiagnosticsViewState, WebDiagnostic } from '../diagnostics/protocol';
import { highlightLearningJavaSource } from '../learning/highlightJava';
import { DockPane } from '../workspace/DockPane';
import type { RunOutputChunk, RunState } from '../workspace/protocol';
import type { LessonTracePayload, LessonTraceVariable } from './protocol';
import {
  buildLessonTraceView, defaultStepIndex, describeLessonTraceStatus, lessonTracePhase,
  stepByIndex, type LessonTraceView, type LessonTraceViewStep
} from './lessonTraceModel';
import { windowRunOutput } from './learnOutputModel';

export type LearnContextTab = 'problems' | 'output' | 'understand' | 'variables' | 'visualization';
type DocumentLabel = { uri: string; displayName: string };
type Props = {
  active: LearnContextTab;
  output: RunOutputChunk[];
  outputDropped: number;
  runState: RunState;
  diagnostics: DiagnosticsViewState | null;
  documents: DocumentLabel[];
  trace: LessonTracePayload | null;
  capturing: boolean;
  onSelect(id: LearnContextTab): void;
  onNavigateProblem(uri: string, diagnostic: WebDiagnostic): void;
  onOpenRelated?(id: string): void;
};

const tabs: Array<{ id: LearnContextTab; label: string }> = [
  { id: 'problems', label: 'Problemas' }, { id: 'output', label: 'Saída' }, { id: 'understand', label: 'Entender' }, { id: 'variables', label: 'Variáveis' }, { id: 'visualization', label: 'Visualização' }
];

export function LearnContextPanel({ active, output, outputDropped, runState, diagnostics, documents, trace, capturing, onSelect, onNavigateProblem, onOpenRelated }: Props) {
  const problemCount = diagnostics?.results.reduce((total, result) => total + result.diagnostics.length, 0) ?? 0;
  const problems = toProblems(diagnostics, documents);
  const [selectedProblemKey, setSelectedProblemKey] = useState<string | null>(null);
  useEffect(() => {
    setSelectedProblemKey(current => problems.some(problem => problem.key === current) ? current : problems[0]?.key ?? null);
  }, [diagnostics, documents]);
  const selectedProblem = problems.find(problem => problem.key === selectedProblemKey) ?? problems[0] ?? null;

  const traceView = trace ? buildLessonTraceView(trace) : null;
  const tracePhase = lessonTracePhase(capturing, trace);
  const [selectedStepIndex, setSelectedStepIndex] = useState<number | null>(null);
  useEffect(() => {
    setSelectedStepIndex(traceView ? defaultStepIndex(traceView) : null);
  }, [trace]);
  const selectedStep = traceView ? stepByIndex(traceView, selectedStepIndex) : null;

  return <DockPane paneId="bottom" className="bottom-panel learn-context-panel" label="Contexto da aula" headerClassName="bottom-tabs" header={<div role="tablist" aria-label="Contexto do programa">
    {tabs.map(tab => <button type="button" key={tab.id} role="tab" aria-selected={active === tab.id} className={active === tab.id ? 'is-active' : ''} onClick={() => onSelect(tab.id)}>{tab.label}{tab.id === 'problems' && problemCount ? ` ${problemCount}` : ''}</button>)}
  </div>} bodyClassName="bottom-panel-content">
    {active === 'problems' ? <ProblemsPanel state={diagnostics} documents={documents} onNavigate={onNavigateProblem} learnMode selectedKey={selectedProblem?.key}
      onSelectProblem={(problem: Problem) => setSelectedProblemKey(problem.key)} onUnderstand={() => onSelect('understand')} onOpenRelated={onOpenRelated} />
      : active === 'output' ? <LearnOutput output={output} outputDropped={outputDropped} runState={runState} />
        : active === 'understand' ? <DiagnosticUnderstanding problem={selectedProblem} />
          : active === 'variables' ? <LessonVariablesTab phase={tracePhase} view={traceView} trace={trace} selectedStep={selectedStep}
            onSelectStep={setSelectedStepIndex} />
            : <LessonVisualizationTab phase={tracePhase} view={traceView} trace={trace} selectedStep={selectedStep}
              onSelectStep={setSelectedStepIndex} />}
  </DockPane>;
}

type TraceTabProps = {
  phase: ReturnType<typeof lessonTracePhase>;
  view: LessonTraceView | null;
  trace: LessonTracePayload | null;
  selectedStep: LessonTraceViewStep | null;
  onSelectStep(index: number): void;
};

function TraceSourceLabel() {
  return <p className="learn-trace-source">Fonte: runtime — valores capturados durante a execução real da lição.</p>;
}

function TraceStepSelector({ view, selectedStep, onSelectStep }: { view: LessonTraceView; selectedStep: LessonTraceViewStep | null; onSelectStep(index: number): void }) {
  return <nav className="learn-trace-steps" aria-label="Passos da execução">
    {view.steps.map(step => <button type="button" key={step.index}
      className={selectedStep?.index === step.index ? 'is-active' : ''}
      aria-current={selectedStep?.index === step.index ? 'true' : undefined}
      onClick={() => onSelectStep(step.index)}
      title={`Estado observado ao chegar na linha ${step.line}, antes de executá-la`}>
      <span className="learn-trace-step-index">Passo {step.index + 1}</span>
      <span className="learn-trace-step-line">linha {step.line}{step.sampled ? ' · amostra' : ''}</span>
      {step.changed.length > 0 && <span className="learn-trace-step-marker" aria-label="variáveis mudaram neste passo">•</span>}
    </button>)}
  </nav>;
}

function TraceStatus({ trace, view }: { trace: LessonTracePayload; view: LessonTraceView }) {
  return <header className="learn-trace-header">
    <p className={`learn-trace-status is-${trace.status.toLowerCase()}`}>{describeLessonTraceStatus(trace.status)}</p>
    {view.runtimeError && <p className="learn-trace-error">Erro na execução: {view.runtimeError}</p>}
    {view.limitations.length > 0 && <ul className="learn-trace-limitations">
      {view.limitations.map((limitation, index) => <li key={index}>{limitation}</li>)}
    </ul>}
    <TraceSourceLabel />
  </header>;
}

function LessonVariablesTab({ phase, view, trace, selectedStep, onSelectStep }: TraceTabProps) {
  if (phase === 'idle') {
    return <ContextEmpty title="Variáveis em tempo de execução"
      message="Ainda não há captura. Execute a atividade com o botão Executar para acompanhar os valores reais das variáveis." />;
  }
  if (phase === 'capturing') {
    return <ContextEmpty title="Capturando a execução…"
      message="O EyeCode está executando a lição de forma instrumentada para registrar os valores das variáveis linha por linha." />;
  }
  if (phase === 'failed' || !view || !trace) {
    return <ContextEmpty title="Sem captura disponível"
      message={`${describeLessonTraceStatus(trace?.status ?? 'FAILED')} Confira a aba Problemas e a Saída para os detalhes reais.`} />;
  }
  if (!view.hasVariables) {
    return <section className="learn-trace">
      <TraceStatus trace={trace} view={view} />
      <ContextEmpty title="Nenhuma variável capturada"
        message="A execução concluiu, mas nenhum valor de variável local pôde ser observado nos pontos de captura." />
    </section>;
  }
  return <section className="learn-trace">
    <TraceStatus trace={trace} view={view} />
    <TraceStepSelector view={view} selectedStep={selectedStep} onSelectStep={onSelectStep} />
    {selectedStep && <SnapshotTable step={selectedStep} />}
  </section>;
}

function SnapshotTable({ step }: { step: LessonTraceViewStep }) {
  const previousValues = new Map((step.previous?.variables ?? []).map(variable => [variable.name, variable.value]));
  return <div className="learn-trace-snapshot" aria-label={`Estado no passo ${step.index + 1}`}>
    <p className="learn-trace-snapshot-caption">
      {step.sampled
        ? `Passo ${step.index + 1} — amostragem na linha ${step.line}: último estado observado enquanto o programa continuava executando.`
        : `Passo ${step.index + 1} — estado observado ao chegar na linha ${step.line}, antes de executá-la.`}
    </p>
    <table className="learn-trace-table">
      <thead><tr><th scope="col">Variável</th><th scope="col">Tipo</th><th scope="col">Valor agora</th><th scope="col">Antes</th></tr></thead>
      <tbody>
        {step.variables.map(variable => {
          const before = previousValues.get(variable.name);
          const changed = before !== undefined && before !== variable.value;
          const entered = before === undefined && step.previous !== null;
          return <tr key={variable.name} className={changed ? 'is-changed' : undefined}>
            <th scope="row">{variable.name}</th>
            <td>{variable.type}</td>
            <td className="learn-trace-value">{variable.value}{variable.truncated ? ' …' : ''}</td>
            <td className="learn-trace-before">
              {changed ? `${before} → mudou` : entered ? 'nova no escopo' : before ?? '—'}
            </td>
          </tr>;
        })}
      </tbody>
    </table>
    {step.leftScope.length > 0 && <p className="learn-trace-scope-note">Saíram de escopo neste passo: {step.leftScope.join(', ')}.</p>}
  </div>;
}

function LessonVisualizationTab({ phase, view, trace, selectedStep, onSelectStep }: TraceTabProps) {
  if (phase === 'idle') {
    return <ContextEmpty title="Visualização de execução"
      message="Ainda não há captura. Execute a atividade para visualizar variáveis, arrays e objetos com valores reais." />;
  }
  if (phase === 'capturing') {
    return <ContextEmpty title="Capturando a execução…"
      message="Registrando as estruturas do programa durante a execução real da lição." />;
  }
  if (phase === 'failed' || !view || !trace) {
    return <ContextEmpty title="Sem visualização disponível" message={describeLessonTraceStatus(trace?.status ?? 'FAILED')} />;
  }
  if (!selectedStep || selectedStep.variables.length === 0) {
    return <section className="learn-viz">
      <TraceStatus trace={trace} view={view} />
      <ContextEmpty title="Nada para visualizar" message="Nenhuma variável está disponível neste passo da execução." />
    </section>;
  }
  return <section className="learn-viz">
    <TraceStatus trace={trace} view={view} />
    <TraceStepSelector view={view} selectedStep={selectedStep} onSelectStep={onSelectStep} />
    <p className="learn-trace-snapshot-caption">
      Passo {selectedStep.index + 1} — linha {selectedStep.line}, antes de executá-la.
    </p>
    <div className="learn-viz-grid">
      {selectedStep.variables.map(variable => <VisualizedVariable key={variable.name} variable={variable} />)}
    </div>
  </section>;
}

function VisualizedVariable({ variable }: { variable: LessonTraceVariable }) {
  if (variable.kind === 'array') {
    return <article className="learn-viz-card is-array">
      <header><strong>{variable.name}</strong><span>{variable.type}</span></header>
      <ol className="learn-viz-array">
        {variable.children.map(child => <li key={child.name}>
          <span className="learn-viz-index">{child.name}</span>
          <span className="learn-viz-value">{child.value}</span>
        </li>)}
      </ol>
      {variable.truncated && <p className="learn-viz-note">Representação truncada: apenas os primeiros elementos são exibidos.</p>}
    </article>;
  }
  if (variable.kind === 'object') {
    return <article className="learn-viz-card is-object">
      <header><strong>{variable.name}</strong><span>{variable.value}</span></header>
      <dl className="learn-viz-fields">
        {variable.children.map(field => <div key={field.name}>
          <dt>{field.name}</dt>
          <dd>{field.value}{field.truncated ? ' …' : ''}</dd>
        </div>)}
      </dl>
      {variable.truncated && <p className="learn-viz-note">Apenas os primeiros campos são exibidos.</p>}
    </article>;
  }
  if (variable.kind === 'reference' || variable.kind === 'unsupported') {
    return <article className="learn-viz-card">
      <header><strong>{variable.name}</strong><span>{variable.type}</span></header>
      <p className="learn-viz-note">{variable.kind === 'reference'
        ? 'Valor não expandido neste passo para evitar repetição de referências.'
        : 'Tipo de valor não suportado pela visualização nesta versão.'}</p>
    </article>;
  }
  return <article className="learn-viz-card">
    <header><strong>{variable.name}</strong><span>{variable.type}</span></header>
    <p className="learn-viz-value">{variable.value}{variable.truncated ? ' …' : ''}</p>
  </article>;
}

function DiagnosticUnderstanding({ problem }: { problem: Problem | null }) {
  const education = problem?.education;
  const patternHtml = useMemo(() => education?.pattern ? highlightLearningJavaSource(education.pattern) : '', [education?.pattern]);
  if (!problem || !education) return <ContextEmpty title="Entenda seu código" message="Selecione um problema com explicação pedagógica para ver como raciocinar sobre ele." />;
  return <article className="learn-diagnostic-understanding"><p className="learning-panel-kicker">Entendendo o problema</p><h2>{education.title}</h2><section><h3>O que aconteceu?</h3><p>{education.explanation}</p></section><section><h3>Como pensar sobre isso?</h3><p>{education.summary}</p></section><section><h3>Padrão de referência</h3><pre><code className="language-java" dangerouslySetInnerHTML={{ __html: patternHtml }} /></pre></section>{education.tip && <section className="learn-problem-tip"><strong>💡 Dica</strong><span>{education.tip}</span></section>}<details><summary>Detalhes técnicos</summary><p>{problem.message} · linha {problem.startLine}, coluna {problem.startColumn}</p></details></article>;
}

function LearnOutput({ output, outputDropped, runState }: Pick<Props, 'output' | 'outputDropped' | 'runState'>) {
  const outputRef = useRef<HTMLPreElement>(null);
  const visible = useMemo(() => windowRunOutput(output), [output]);
  useEffect(() => { if (outputRef.current) outputRef.current.scrollTop = outputRef.current.scrollHeight; }, [output]);
  const status = runState.running ? 'Executando…' : runState.finished ? runState.exitCode === 0 ? 'Programa finalizado' : `Processo finalizado com código ${runState.exitCode ?? -1}` : 'Execute a atividade para ver a saída.';
  const hiddenChars = visible.droppedChars + outputDropped;
  return <section className="learn-output"><header className={runState.finished && runState.exitCode === 0 ? 'is-success' : ''}>{status}</header><pre ref={outputRef}>
    {hiddenChars > 0 && <span className="learn-output-dropped">…{hiddenChars.toLocaleString('pt-BR')} caracteres anteriores não exibidos — a exibição mostra apenas o final da saída…{'\n'}</span>}
    {visible.chunks.length ? visible.chunks.map((chunk, index) => <span key={index} className={chunk.error ? 'is-error' : ''}>{chunk.text}</span>) : 'A saída do programa aparecerá aqui.'}
  </pre></section>;
}

function ContextEmpty({ title, message }: { title: string; message: string }) {
  return <section className="context-empty-state"><h2>{title}</h2><p>{message}</p></section>;
}
