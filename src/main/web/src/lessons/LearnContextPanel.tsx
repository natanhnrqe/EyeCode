import { useEffect, useRef, useState } from 'react';
import { ProblemsPanel, toProblems, type Problem } from '../diagnostics/ProblemsPanel';
import type { DiagnosticsViewState, WebDiagnostic } from '../diagnostics/protocol';
import { DockPane } from '../workspace/DockPane';
import type { RunOutputChunk, RunState } from '../workspace/protocol';

export type LearnContextTab = 'problems' | 'output' | 'understand' | 'variables' | 'visualization';
type DocumentLabel = { uri: string; displayName: string };
type Props = { active: LearnContextTab; output: RunOutputChunk[]; runState: RunState; diagnostics: DiagnosticsViewState | null; documents: DocumentLabel[]; onSelect(id: LearnContextTab): void; onNavigateProblem(uri: string, diagnostic: WebDiagnostic): void };

const tabs: Array<{ id: LearnContextTab; label: string }> = [
  { id: 'problems', label: 'Problemas' }, { id: 'output', label: 'Saída' }, { id: 'understand', label: 'Entender' }, { id: 'variables', label: 'Variáveis' }, { id: 'visualization', label: 'Visualização' }
];

export function LearnContextPanel({ active, output, runState, diagnostics, documents, onSelect, onNavigateProblem }: Props) {
  const problemCount = diagnostics?.results.reduce((total, result) => total + result.diagnostics.length, 0) ?? 0;
  const problems = toProblems(diagnostics, documents);
  const [selectedProblemKey, setSelectedProblemKey] = useState<string | null>(null);
  useEffect(() => {
    setSelectedProblemKey(current => problems.some(problem => problem.key === current) ? current : problems[0]?.key ?? null);
  }, [diagnostics, documents]);
  const selectedProblem = problems.find(problem => problem.key === selectedProblemKey) ?? problems[0] ?? null;
  return <DockPane paneId="bottom" className="bottom-panel learn-context-panel" label="Contexto da aula" headerClassName="bottom-tabs" header={<div role="tablist" aria-label="Contexto do programa">
    {tabs.map(tab => <button type="button" key={tab.id} role="tab" aria-selected={active === tab.id} className={active === tab.id ? 'is-active' : ''} onClick={() => onSelect(tab.id)}>{tab.label}{tab.id === 'problems' && problemCount ? ` ${problemCount}` : ''}</button>)}
  </div>} bodyClassName="bottom-panel-content">
    {active === 'problems' ? <ProblemsPanel state={diagnostics} documents={documents} onNavigate={onNavigateProblem} learnMode selectedKey={selectedProblem?.key}
      onSelectProblem={(problem: Problem) => setSelectedProblemKey(problem.key)} onUnderstand={() => onSelect('understand')} />
      : active === 'output' ? <LearnOutput output={output} runState={runState} />
        : active === 'understand' ? <DiagnosticUnderstanding problem={selectedProblem} />
          : active === 'variables' ? <ContextEmpty title="Variáveis em tempo de execução" message="A visualização de variáveis estará disponível em exercícios compatíveis." />
            : <ContextEmpty title="Visualização de execução" message="Visualizações de estruturas de dados estarão disponíveis em exercícios compatíveis." />}
  </DockPane>;
}

function DiagnosticUnderstanding({ problem }: { problem: Problem | null }) {
  const education = problem?.education;
  if (!problem || !education) return <ContextEmpty title="Entenda seu código" message="Selecione um problema com explicação pedagógica para ver como raciocinar sobre ele." />;
  return <article className="learn-diagnostic-understanding"><p className="learning-panel-kicker">Entendendo o problema</p><h2>{education.title}</h2><section><h3>O que aconteceu?</h3><p>{education.explanation}</p></section><section><h3>Como pensar sobre isso?</h3><p>{education.summary}</p><pre><code>{education.pattern}</code></pre></section>{education.tip && <section className="learn-problem-tip"><strong>💡 Dica</strong><span>{education.tip}</span></section>}<details><summary>Detalhes técnicos</summary><p>{problem.message} · linha {problem.startLine}, coluna {problem.startColumn}</p></details></article>;
}

function LearnOutput({ output, runState }: Pick<Props, 'output' | 'runState'>) {
  const outputRef = useRef<HTMLPreElement>(null);
  useEffect(() => { if (outputRef.current) outputRef.current.scrollTop = outputRef.current.scrollHeight; }, [output]);
  const status = runState.running ? 'Executando…' : runState.finished ? runState.exitCode === 0 ? 'Programa finalizado' : `Processo finalizado com código ${runState.exitCode ?? -1}` : 'Execute a atividade para ver a saída.';
  return <section className="learn-output"><header className={runState.finished && runState.exitCode === 0 ? 'is-success' : ''}>{status}</header><pre ref={outputRef}>{output.length ? output.map((chunk, index) => <span key={index} className={chunk.error ? 'is-error' : ''}>{chunk.text}</span>) : 'A saída do programa aparecerá aqui.'}</pre></section>;
}

function ContextEmpty({ title, message }: { title: string; message: string }) {
  return <section className="context-empty-state"><h2>{title}</h2><p>{message}</p></section>;
}
