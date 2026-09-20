import { useState } from 'react';
import type { LessonContentBlock, LessonSession, PracticeVerificationResult } from './protocol';
import { InlineContent, LessonBlocks, PracticeSupport } from './LessonPanel';
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
};

const tabs: Array<{ id: LearningPanelTab; label: string }> = [
  { id: 'exercise', label: 'Exercício' },
  { id: 'concepts', label: 'Conceitos' },
  { id: 'faq', label: 'Dúvidas' },
  { id: 'examples', label: 'Exemplos' },
  { id: 'assistant', label: 'Assistente IA' }
];

export function LearningPanel({ session, verification, verifying, onVerify, onPrevious, onNext, onExit }: Props) {
  const [activeTab, setActiveTab] = useState<LearningPanelTab>('exercise');
  const phaseClass = session.phase === 'PRESENTATION' ? ' is-presentation' : session.phase === 'PRACTICE' ? ' is-practice-active' : ' is-completed';
  const lessonClass = session.kind === 'THEORY' ? ' is-theory' : ` is-practice${phaseClass}${session.practiceCompleted ? ' is-practice-completed' : ''}`;
  return <DockPane paneId="lesson" className={`learning-panel${lessonClass}`} label="Painel de aprendizagem" headerClassName="learning-panel-tabs" header={<div role="tablist" aria-label="Recursos da aula">
    {tabs.map(tab => <button key={tab.id} type="button" role="tab" aria-selected={activeTab === tab.id}
      className={activeTab === tab.id ? 'is-active' : ''} onClick={() => setActiveTab(tab.id)}>{tab.label}</button>)}
  </div>} bodyClassName="learning-panel-content" footerClassName="learning-panel-actions" footer={<><button type="button" onClick={onExit}>Voltar ao roteiro</button><div><button type="button" onClick={onPrevious} disabled={!session.canPrevious}>Anterior</button><button type="button" className="primary-action" onClick={onNext} disabled={!session.canNext}>Próximo</button></div></>}>
    {activeTab === 'exercise' && <ExerciseTab session={session} verification={verification} verifying={verifying} onVerify={onVerify} />}
    {activeTab === 'concepts' && <ConceptsTab blocks={session.contentBlocks} />}
    {activeTab === 'faq' && <EmptyTab title="Dúvidas frequentes" message="Esta aula ainda não possui dúvidas frequentes." />}
    {activeTab === 'examples' && <ExamplesTab blocks={session.contentBlocks} />}
    {activeTab === 'assistant' && <EmptyTab title="Assistente da aula" message="Em breve, o assistente usará esta aula e o seu código para oferecer ajuda contextual." />}
  </DockPane>;
}

function ExerciseTab({ session, verification, verifying, onVerify }: Pick<Props, 'session' | 'verification' | 'verifying' | 'onVerify'>) {
  const practiceActive = session.phase === 'PRACTICE' && session.practice;
  return <article className="learning-tab exercise-tab"><p className="learning-panel-kicker">{session.phase === 'PRESENTATION' ? 'Apresentação' : session.phase === 'PRACTICE' ? 'Prática' : 'Concluída'} · {session.currentStep + 1} de {session.totalSteps}</p><h1>{session.title}</h1>{session.message && <p className="learning-tab-lead">{session.message}</p>}
    {practiceActive ? <><section className="exercise-task"><h2>Sua tarefa</h2><p><InlineContent content={session.practice!.instruction} /></p>{verification && <section className={`exercise-feedback${verification.status === 'SUCCESS' ? ' is-success' : ''}`}><strong>{verification.status === 'SUCCESS' ? 'Correto' : 'Revise sua resposta'}</strong><p>{verification.message}</p></section>}<button type="button" className="primary-action" onClick={onVerify} disabled={verifying}>{verifying ? 'Verificando...' : 'Verificar resposta'}</button></section><PracticeSupport blocks={session.contentBlocks} /></> : <LessonBlocks blocks={session.contentBlocks} theory={session.kind === 'THEORY'} />}
  </article>;
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
