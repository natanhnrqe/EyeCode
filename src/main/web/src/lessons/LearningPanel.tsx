import { useEffect, useState, type ReactNode } from 'react';
import type { LessonContentBlock, LessonSession, PracticeVerificationResult } from './protocol';
import { InlineContent, LessonBlock, LessonBlocks } from './LessonPanel';
import { DockPane } from '../workspace/DockPane';

type LearningPanelTab = 'exercise' | 'concepts' | 'faq' | 'examples' | 'assistant';
type Props = {
  session: LessonSession;
  verification: PracticeVerificationResult | null;
  verifying: boolean;
  onVerify(): void;
  onPrevious(): void;
  onNext(): void;
  onExit(): void;
  hasDiagnostics: boolean;
  onShowProblems(): void;
};

const tabs: Array<{ id: LearningPanelTab; label: string }> = [
  { id: 'exercise', label: 'Exercício' },
  { id: 'concepts', label: 'Conceitos' },
  { id: 'faq', label: 'Dúvidas' },
  { id: 'examples', label: 'Exemplos' },
  { id: 'assistant', label: 'Assistente IA' }
];

export function LearningPanel({ session, verification, verifying, onVerify, onPrevious, onNext, onExit, hasDiagnostics, onShowProblems }: Props) {
  const [activeTab, setActiveTab] = useState<LearningPanelTab>('exercise');
  const phaseClass = session.phase === 'PRESENTATION' ? ' is-presentation' : session.phase === 'PRACTICE' ? ' is-practice-active' : ' is-completed';
  const lessonClass = session.kind === 'THEORY' ? ' is-theory' : ` is-practice${phaseClass}${session.practiceCompleted ? ' is-practice-completed' : ''}`;
  return <DockPane paneId="lesson" className={`learning-panel${lessonClass}`} label="Painel de aprendizagem" headerClassName="learning-panel-tabs" header={<div role="tablist" aria-label="Recursos da aula">
    {tabs.map(tab => <button key={tab.id} type="button" role="tab" aria-selected={activeTab === tab.id}
      className={activeTab === tab.id ? 'is-active' : ''} onClick={() => setActiveTab(tab.id)}>{tab.label}</button>)}
  </div>} bodyClassName="learning-panel-content" footerClassName="learning-panel-actions" footer={<><button type="button" onClick={onExit}>Voltar ao roteiro</button><div><button type="button" onClick={onPrevious} disabled={!session.canPrevious}>Anterior</button><button type="button" className="primary-action" onClick={onNext} disabled={!session.canNext}>Próximo</button></div></>}>
    <div hidden={activeTab !== 'exercise'}><ExerciseTab session={session} verification={verification} verifying={verifying} onVerify={onVerify} hasDiagnostics={hasDiagnostics}
      onShowProblems={onShowProblems} onShowConcepts={() => setActiveTab('concepts')} /></div>
    {activeTab === 'concepts' && <ConceptsTab blocks={session.contentBlocks} />}
    {activeTab === 'faq' && <EmptyTab title="Dúvidas frequentes" message="Esta aula ainda não possui dúvidas frequentes." />}
    {activeTab === 'examples' && <ExamplesTab blocks={session.contentBlocks} />}
    {activeTab === 'assistant' && <EmptyTab title="Assistente da aula" message="Em breve, o assistente usará esta aula e o seu código para oferecer ajuda contextual." />}
  </DockPane>;
}

function ExerciseTab({ session, verification, verifying, onVerify, hasDiagnostics, onShowProblems, onShowConcepts }: Pick<Props, 'session' | 'verification' | 'verifying' | 'onVerify' | 'hasDiagnostics' | 'onShowProblems'> & { onShowConcepts(): void }) {
  const practiceActive = session.phase === 'PRACTICE' && session.practice;
  const hints = session.contentBlocks.filter(block => block.type === 'CALLOUT' && block.title?.trim().toLocaleLowerCase() === 'dica');
  const [revealedHints, setRevealedHints] = useState(0);
  const [stuckOpen, setStuckOpen] = useState(false);
  const [guidance, setGuidance] = useState<'task' | 'start' | 'code' | 'error' | null>(null);
  useEffect(() => {
    setRevealedHints(0);
    setStuckOpen(false);
    setGuidance(null);
  }, [session.sessionId, session.currentStep, session.practice?.id]);
  const completed = session.practiceCompleted;
  const revealHint = () => setRevealedHints(current => Math.min(hints.length, current + 1));
  const chooseGuidance = (choice: 'task' | 'start' | 'code' | 'error') => {
    setGuidance(choice);
    if (choice === 'start' && hints.length) setRevealedHints(current => Math.max(1, current));
  };
  return <article className="learning-tab exercise-tab"><header className="exercise-header"><p className="learning-panel-kicker">{session.phase === 'PRESENTATION' ? 'Apresentação' : completed ? 'Prática concluída' : 'Prática'} <span>{session.currentStep + 1} de {session.totalSteps}</span></p><h1>{session.title}</h1>{session.message && <p className="learning-tab-lead">{session.message}</p>}</header>
    {practiceActive ? <><section className="exercise-task"><h2>Sua tarefa</h2><p><InlineContent content={session.practice!.instruction} /></p>{verification && <Feedback verification={verification} />}{completed && !verification && <section className="exercise-feedback is-success"><strong>Prática concluída</strong><p>Você já concluiu esta etapa.</p></section>}<button type="button" className="primary-action" onClick={onVerify} disabled={verifying || completed}>{verifying ? 'Verificando...' : completed ? 'Resposta verificada' : 'Verificar resposta'}</button></section>
      {!completed && <><section className="exercise-help-actions" aria-label="Ajuda para a atividade">{hints.length > 0 && <button type="button" onClick={revealHint}>{revealedHints ? revealedHints < hints.length ? 'Próxima dica' : 'Dicas exibidas' : '💡 Pedir uma dica'}</button>}<button type="button" aria-expanded={stuckOpen} onClick={() => setStuckOpen(open => !open)}>？ Estou travado</button></section>
        {revealedHints > 0 && <section className="exercise-hints" aria-label="Dicas reveladas">{hints.slice(0, revealedHints).map((hint, index) => <div key={index}><strong>Dica {index + 1}</strong><LessonBlock block={hint} theory={false} /></div>)}</section>}
        {stuckOpen && <StuckFlow guidance={guidance} hasHints={hints.length > 0} hasDiagnostics={hasDiagnostics} message={session.message} onChoose={chooseGuidance} onShowProblems={onShowProblems} onShowConcepts={onShowConcepts} />}</>}
    </> : <LessonBlocks blocks={session.contentBlocks} theory={session.kind === 'THEORY'} />}
  </article>;
}

function Feedback({ verification }: { verification: PracticeVerificationResult }) {
  return <section className={`exercise-feedback${verification.status === 'SUCCESS' ? ' is-success' : ''}`}><strong>{verification.status === 'SUCCESS' ? 'Correto' : 'Revise sua resposta'}</strong><p>{verification.message}</p></section>;
}

function StuckFlow({ guidance, hasHints, hasDiagnostics, message, onChoose, onShowProblems, onShowConcepts }: { guidance: 'task' | 'start' | 'code' | 'error' | null; hasHints: boolean; hasDiagnostics: boolean; message: string; onChoose(choice: 'task' | 'start' | 'code' | 'error'): void; onShowProblems(): void; onShowConcepts(): void }) {
  return <section className="exercise-stuck" aria-label="Assistência para a atividade"><h2>Onde está a dificuldade?</h2><div className="exercise-stuck-choices"><button type="button" onClick={() => onChoose('task')}>Não entendi a tarefa</button><button type="button" onClick={() => onChoose('start')}>Não sei como começar</button><button type="button" onClick={() => onChoose('code')}>Meu código não funciona</button><button type="button" onClick={() => onChoose('error')}>Não entendi um erro</button></div>
    {guidance === 'task' && <Guidance><p>{message || 'Confira o enunciado da tarefa e os conceitos relacionados antes de editar o código.'}</p><button type="button" onClick={onShowConcepts}>Ver conceitos</button></Guidance>}
    {guidance === 'start' && <Guidance><p>{hasHints ? 'A primeira dica foi revelada acima. Comece por ela e faça uma alteração pequena.' : 'Comece revisando os conceitos relacionados e faça uma alteração pequena no editor.'}</p><button type="button" onClick={onShowConcepts}>Ver conceitos</button></Guidance>}
    {guidance === 'code' && <DiagnosticGuidance hasDiagnostics={hasDiagnostics} onShowProblems={onShowProblems} />}
    {guidance === 'error' && <DiagnosticGuidance hasDiagnostics={hasDiagnostics} onShowProblems={onShowProblems} error />}
  </section>;
}

function Guidance({ children }: { children: ReactNode }) { return <div className="exercise-guidance">{children}</div>; }

function DiagnosticGuidance({ hasDiagnostics, onShowProblems, error = false }: { hasDiagnostics: boolean; onShowProblems(): void; error?: boolean }) {
  return <Guidance><p>{hasDiagnostics ? error ? 'Há problemas detectados no código. Consulte a lista para localizar o erro.' : 'Há problemas detectados no código. Consulte a lista antes de tentar novamente.' : error ? 'Nenhum erro de sintaxe foi detectado agora. Execute ou revise a tarefa para conferir o comportamento.' : 'Nenhum problema de sintaxe foi detectado agora. Confira o enunciado e a saída do programa.'}</p>{hasDiagnostics && <button type="button" onClick={onShowProblems}>Ver problemas</button>}</Guidance>;
}

function ConceptsTab({ blocks }: { blocks: LessonContentBlock[] }) {
  const concepts = blocks.filter(block => block.type === 'HEADING' || block.type === 'PARAGRAPH' || block.type === 'CALLOUT');
  return concepts.length ? <article className="learning-tab concepts-tab"><p className="learning-panel-kicker">Conceitos da aula</p><LessonBlocks blocks={concepts} theory={false} /></article>
    : <EmptyTab title="Conceitos" message="Os conceitos desta aula aparecerão aqui quando houver conteúdo relacionado." />;
}

function ExamplesTab({ blocks }: { blocks: LessonContentBlock[] }) {
  const examples = blocks.filter(block => block.type === 'CODE');
  return examples.length ? <article className="learning-tab examples-tab"><p className="learning-panel-kicker">Exemplos da aula</p><LessonBlocks blocks={examples} theory={false} /></article>
    : <EmptyTab title="Exemplos" message="Esta aula ainda não possui exemplos de código separados." />;
}

function EmptyTab({ title, message }: { title: string; message: string }) {
  return <section className="learning-empty-state"><h1>{title}</h1><p>{message}</p></section>;
}
