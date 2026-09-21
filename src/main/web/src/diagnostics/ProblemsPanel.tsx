import { useEffect, useState } from 'react';
import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import type { DiagnosticsViewState, WebDiagnostic } from './protocol';

type DocumentLabel = { uri: string; displayName: string };
export type Problem = WebDiagnostic & { uri: string; fileName: string; key: string };
type Props = {
  state: DiagnosticsViewState | null;
  documents: DocumentLabel[];
  onNavigate(uri: string, diagnostic: WebDiagnostic): void;
  learnMode?: boolean;
  selectedKey?: string | null;
  onSelectProblem?(problem: Problem): void;
  onUnderstand?(): void;
};

const rank = (diagnostic: WebDiagnostic): number => diagnostic.severity === 'ERROR' ? 0
  : diagnostic.severity === 'WARNING' ? 1 : diagnostic.severity === 'INFO' ? 2 : 3;

export function toProblems(state: DiagnosticsViewState | null, documents: DocumentLabel[]): Problem[] {
  const names = new Map(documents.map(document => [document.uri, document.displayName]));
  return (state?.results ?? []).flatMap(result => result.diagnostics.map(diagnostic => {
    const key = `${result.uri}:${diagnostic.startLine}:${diagnostic.startColumn}:${diagnostic.endLine}:${diagnostic.endColumn}:${diagnostic.code}:${diagnostic.message}`;
    return { ...diagnostic, uri: result.uri, fileName: names.get(result.uri) ?? fileName(result.uri), key };
  })).sort((first, second) => rank(first) - rank(second) || first.fileName.localeCompare(second.fileName)
    || first.startLine - second.startLine || first.startColumn - second.startColumn);
}

export function ProblemsPanel({ state, documents, onNavigate, learnMode = false, selectedKey, onSelectProblem, onUnderstand }: Props) {
  const problems = toProblems(state, documents);
  if (!problems.length) return <div className="problems-empty">{learnMode ? <><strong>Nenhum problema encontrado</strong><span>Seu código está pronto para continuar.</span></> : 'No problems detected'}</div>;
  if (!learnMode) return <div className="problems-list" role="list">{problems.map(problem => <ProblemRow key={problem.key} problem={problem} onNavigate={onNavigate} />)}</div>;
  const selected = problems.find(problem => problem.key === selectedKey) ?? problems[0];
  const select = (problem: Problem) => { onSelectProblem?.(problem); onNavigate(problem.uri, problem); };
  return <LearnProblemsExperience problems={problems} selected={selected} onSelect={select} onUnderstand={onUnderstand} />;
}

function ProblemRow({ problem, onNavigate }: { problem: Problem; onNavigate(uri: string, diagnostic: WebDiagnostic): void }) {
  return <button type="button" className={`problem-entry severity-${problem.severity.toLowerCase()}`} onClick={() => onNavigate(problem.uri, problem)} role="listitem">
    <EyeCodeIcon name="problem" /><span className="problem-message">{problem.message}</span><span className="problem-file">{problem.fileName}</span><span className="problem-location">{problem.startLine}:{problem.startColumn}</span>
  </button>;
}

function LearnProblemsExperience({ problems, selected, onSelect, onUnderstand }: { problems: Problem[]; selected: Problem; onSelect(problem: Problem): void; onUnderstand?: () => void }) {
  const [exampleOpen, setExampleOpen] = useState(false);
  useEffect(() => setExampleOpen(false), [selected.key]);
  const education = selected.education;
  return <div className="learn-problems-experience">
    {problems.length > 1 && <nav className="learn-problem-selector" aria-label="Problemas encontrados">{problems.map((problem, index) => <button type="button" key={problem.key} className={problem.key === selected.key ? 'is-selected' : ''} aria-current={problem.key === selected.key ? 'true' : undefined} onClick={() => onSelect(problem)}><span className={`severity-dot severity-${problem.severity.toLowerCase()}`} />{problem.startLine}:{problem.startColumn} · {problem.education?.title ?? problem.message}<small>{index + 1} de {problems.length}</small></button>)}</nav>}
    <div className="learn-problems-grid">
      <section className={`learn-problem-card learn-problem-error severity-${selected.severity.toLowerCase()}`} aria-labelledby="learn-problem-title">
        <header><EyeCodeIcon name="problem" /><span>{selected.severity === 'ERROR' ? 'Erro' : selected.severity === 'WARNING' ? 'Aviso' : selected.severity === 'INFO' ? 'Informação' : 'Dica'}</span></header>
        <h2 id="learn-problem-title">Linha {selected.startLine} — {education?.title ?? 'Diagnóstico técnico'}</h2>
        <p>{education?.summary ?? 'O analisador encontrou um problema nesta parte do código.'}</p>
        {selected.sourceExcerpt && <SourceExcerpt excerpt={selected.sourceExcerpt} />}
        {education?.explanation && <p className="learn-problem-short-explanation">{education.explanation}</p>}
        <div className="learn-problem-actions">{education && onUnderstand && <button type="button" className="primary-action" onClick={onUnderstand}>Entender este erro</button>}{education?.correctedPattern && <button type="button" onClick={() => setExampleOpen(open => !open)}>{exampleOpen ? 'Ocultar exemplo' : 'Ver exemplo parecido'}</button>}</div>
        {exampleOpen && education?.correctedPattern && <pre className="learn-problem-example"><code>{education.correctedPattern}</code></pre>}
        <details className="learn-problem-technical"><summary>Detalhes técnicos</summary><dl><div><dt>Mensagem</dt><dd>{selected.message}</dd></div><div><dt>Localização</dt><dd>linha {selected.startLine}, coluna {selected.startColumn}</dd></div>{selected.code && <div><dt>Código</dt><dd>{selected.code}</dd></div>}</dl></details>
      </section>
      <section className="learn-problem-card learn-problem-explanation"><header><EyeCodeIcon name="documentation" /><span>Explicação simples</span></header>{education ? <><p>{education.explanation}</p>{education.pattern && <pre className="learn-problem-pattern"><code>{education.pattern}</code></pre>}{education.tip && <aside className="learn-problem-tip"><strong>💡 Dica</strong><span>{education.tip}</span></aside>}</> : <p>Uma explicação pedagógica ainda não está disponível para este diagnóstico. Use os detalhes técnicos e a localização para investigar o código.</p>}</section>
      <aside className="learn-problem-card learn-problem-related"><header><span>Conteúdo relacionado</span></header>{education?.relatedContent.length ? <ul>{education.relatedContent.map(item => <li key={item.id}><button type="button" onClick={onUnderstand}><strong>{item.title}</strong><span>{item.description}</span></button></li>)}</ul> : <p>Não há conteúdo relacionado disponível para este diagnóstico.</p>}</aside>
    </div>
  </div>;
}

function SourceExcerpt({ excerpt }: { excerpt: NonNullable<WebDiagnostic['sourceExcerpt']> }) {
  return <pre className="learn-problem-source" aria-label="Trecho do código com o problema">{excerpt.lines.map(line => <span className={line.lineNumber === excerpt.pointerLine ? 'is-pointer-line' : ''} key={line.lineNumber}><b>{String(line.lineNumber).padStart(3, ' ')}</b> {line.text}{line.lineNumber === excerpt.pointerLine && <i>{' '.repeat(Math.max(0, excerpt.pointerColumn + 3))}^</i>}{'\n'}</span>)}</pre>;
}

function fileName(uri: string): string {
  const path = decodeURIComponent(uri.split('/').pop() ?? uri);
  return path || uri;
}
