import { highlightLearningJavaSource } from '../learning/highlightJava';
import type { LessonContentBlock, LessonInlineContent, LessonSession, PracticeVerificationResult } from './protocol';
import { DockPane } from '../workspace/DockPane';

type Props = { session: LessonSession; verification: PracticeVerificationResult | null; verifying: boolean; onVerify(): void; onPrevious(): void; onNext(): void; onExit(): void };

export function LessonPanel({ session, verification, verifying, onVerify, onPrevious, onNext, onExit }: Props) {
  const practice = session.phase === 'PRACTICE' ? session.practice : undefined;
  return <DockPane paneId="lesson" className={`lesson-panel${session.kind === 'THEORY' ? ' is-theory' : ''}`} label="Conteúdo da aula" headerClassName="bottom-tabs lesson-pane-header" header={<><strong>Aula: {session.title}</strong><span>Parte {session.currentStep + 1} de {session.totalSteps}</span></>} bodyClassName="lesson-pane-content lesson-panel-content learning-body" footerClassName="lesson-panel-actions" footer={<><button type="button" onClick={onExit}>Voltar ao roteiro</button><div><button type="button" onClick={onPrevious} disabled={!session.canPrevious}>Anterior</button><button type="button" className="primary-action" onClick={onNext} disabled={!session.canNext}>Próximo</button></div></>}>
      <article className="lesson-reading-article">{session.contentBlocks.map((block, index) => <LessonBlock key={index} block={block} />)}</article>
      {practice && <section className="lesson-practice"><h2>Sua vez</h2><p>{practice.instruction}</p>{verification && <aside className="lesson-callout"><p>{verification.message}</p></aside>}<button type="button" className="primary-action" onClick={onVerify} disabled={verifying}>{verifying ? 'Verificando...' : 'Verificar'}</button></section>}
  </DockPane>;
}

function LessonBlock({ block }: { block: LessonContentBlock }) {
  if (block.type === 'HEADING') return <h2><InlineContent content={block.inlineContent} fallback={block.text} /></h2>;
  if (block.type === 'PARAGRAPH') return <p><InlineContent content={block.inlineContent} fallback={block.text} /></p>;
  if (block.type === 'CODE') return <pre><code className={block.language === 'java' ? 'language-java' : undefined} dangerouslySetInnerHTML={block.language === 'java' ? { __html: highlightLearningJavaSource(block.code ?? '') } : undefined}>{block.language === 'java' ? undefined : block.code}</code></pre>;
  if (block.type === 'LIST') {
    const List = block.ordered ? 'ol' : 'ul';
    return <List>{block.items?.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</List>;
  }
  return <aside className="lesson-callout"><strong>{block.title}</strong><p><InlineContent content={block.inlineContent} fallback={block.text} /></p></aside>;
}

function InlineContent({ content, fallback }: { content?: LessonInlineContent[]; fallback?: string }) {
  if (!content?.length) return <>{fallback}</>;
  return <>{content.map((part, index) => {
    if (part.type === 'CODE') return <code key={index}>{part.text}</code>;
    if (part.type === 'EMPHASIS') return <em key={index}>{part.text}</em>;
    if (part.type === 'STRONG') return <strong key={index}>{part.text}</strong>;
    if (part.type === 'LINK') return <span key={index} className="lesson-inline-link">{part.text}</span>;
    return <span key={index}>{part.text}</span>;
  })}</>;
}
