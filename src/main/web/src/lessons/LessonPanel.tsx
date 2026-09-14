import { Fragment, useEffect, useState } from 'react';
import { highlightLearningJavaSource } from '../learning/highlightJava';
import type { LessonContentBlock, LessonInlineContent, LessonSession, PracticeVerificationResult } from './protocol';
import { DockPane } from '../workspace/DockPane';

type Breadcrumb = { category: string; topic: string; onCategory?(): void; onTopic?(): void };
type Props = { session: LessonSession; breadcrumb?: Breadcrumb; verification: PracticeVerificationResult | null; verifying: boolean; onVerify(): void; onPrevious(): void; onNext(): void; onExit(): void };

export function LessonPanel({ session, breadcrumb, verification, verifying, onVerify, onPrevious, onNext, onExit }: Props) {
  const practice = session.phase === 'PRACTICE' ? session.practice : undefined;
  const theory = session.kind === 'THEORY';
  const practiceLesson = session.kind === 'PRACTICE';
  const practicePhaseClass = session.phase === 'PRESENTATION' ? ' is-presentation' : session.phase === 'PRACTICE' ? ' is-practice-active' : ' is-completed';
  const practiceLead = practice?.instruction;
  return <DockPane paneId="lesson" className={`lesson-panel${theory ? ' is-theory' : ''}${practiceLesson ? ` is-practice${practicePhaseClass}${session.practiceCompleted ? ' is-practice-completed' : ''}` : ''}`} label="Conteúdo da aula" headerClassName="bottom-tabs lesson-pane-header" header={<>{theory ? <LessonBreadcrumb breadcrumb={breadcrumb} /> : <span className="lesson-pane-kicker">{practiceLesson ? 'Prática' : 'Conteúdo da aula'}</span>}<span className="lesson-part-indicator">Parte {session.currentStep + 1} de {session.totalSteps}</span></>} bodyClassName="lesson-pane-content lesson-panel-content learning-body" footerClassName="lesson-panel-actions" footer={<><button type="button" onClick={onExit}>Voltar ao roteiro</button><div><button type="button" onClick={onPrevious} disabled={!session.canPrevious}>Anterior</button><button type="button" className="primary-action" onClick={onNext} disabled={!session.canNext}>Próximo</button></div></>}>
      <article className="lesson-reading-article">{theory && <header className="lesson-chapter-header"><h1 className="lesson-chapter-title">{session.title}</h1>{session.message && <p className="lesson-chapter-lead">{session.message}</p>}</header>}{practiceLesson && <header className="lesson-practice-header"><p className="lesson-practice-kicker">Prática</p><h1 className="lesson-practice-title">{session.title}</h1>{practiceLead ? <p className="lesson-practice-lead"><InlineContent content={practiceLead} /></p> : session.message && <p className="lesson-practice-lead">{session.message}</p>}</header>}<LessonBlocks blocks={session.contentBlocks} theory={theory} /></article>
      {practice && <section className="lesson-practice">{verification && <aside className="lesson-callout lesson-practice-feedback"><strong>{verification.status === 'SUCCESS' ? 'Correto' : 'Revise sua resposta'}</strong><p>{verification.message}</p></aside>}<button type="button" className="primary-action" onClick={onVerify} disabled={verifying}>{verifying ? 'Verificando...' : 'Verificar'}</button></section>}
  </DockPane>;
}

function LessonBreadcrumb({ breadcrumb }: { breadcrumb?: Breadcrumb }) {
  if (!breadcrumb) return <span className="lesson-pane-kicker">Teoria</span>;
  return <nav className="lesson-breadcrumb" aria-label="Localização da aula">{breadcrumb.onCategory ? <button type="button" onClick={breadcrumb.onCategory}>{breadcrumb.category}</button> : <span className="lesson-breadcrumb-item">{breadcrumb.category}</span>}<span className="lesson-breadcrumb-separator" aria-hidden="true">›</span>{breadcrumb.onTopic ? <button type="button" onClick={breadcrumb.onTopic}>{breadcrumb.topic}</button> : <span className="lesson-breadcrumb-item">{breadcrumb.topic}</span>}</nav>;
}

function LessonBlocks({ blocks, theory }: { blocks: LessonContentBlock[]; theory: boolean }) {
  let section = 0;
  return <>{blocks.map((block, index) => {
    const sectionNumber = theory && block.type === 'HEADING' ? ++section : undefined;
    return <Fragment key={index}>{sectionNumber && sectionNumber > 1 && <div className="lesson-section-divider" />}{<LessonBlock block={block} theory={theory} sectionNumber={sectionNumber} />}</Fragment>;
  })}</>;
}

function LessonBlock({ block, theory, sectionNumber }: { block: LessonContentBlock; theory: boolean; sectionNumber?: number }) {
  if (block.type === 'HEADING') return <h2 className="lesson-heading">{sectionNumber && <span className="lesson-section-marker">{sectionNumber}</span>}<span><InlineContent content={block.inlineContent} fallback={block.text} /></span></h2>;
  if (block.type === 'PARAGRAPH') return <p className="lesson-paragraph"><InlineContent content={block.inlineContent} fallback={block.text} /></p>;
  if (block.type === 'CODE') return theory ? <LessonCodeBlock block={block} /> : <pre className="lesson-code-block"><code className={block.language === 'java' ? 'language-java' : undefined} dangerouslySetInnerHTML={block.language === 'java' ? { __html: highlightLearningJavaSource(block.code ?? '') } : undefined}>{block.language === 'java' ? undefined : block.code}</code></pre>;
  if (block.type === 'LIST') {
    const List = block.ordered ? 'ol' : 'ul';
    return <List className="lesson-list">{block.items?.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</List>;
  }
  return <aside className="lesson-callout"><strong>{block.title}</strong><p><InlineContent content={block.inlineContent} fallback={block.text} /></p></aside>;
}

function LessonCodeBlock({ block }: { block: LessonContentBlock }) {
  const [copied, setCopied] = useState(false);
  const source = block.code ?? '';
  const language = block.language?.trim();
  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(false), 1200);
    return () => window.clearTimeout(timer);
  }, [copied]);
  async function copy() {
    try {
      await navigator.clipboard.writeText(source);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }
  return <section className="lesson-code-frame">{language && <header className="lesson-code-frame-header"><span>{language}</span><button type="button" onClick={() => void copy()}>{copied ? 'Copiado' : 'Copiar'}</button></header>}<pre className="lesson-code-block"><code className={language === 'java' ? 'language-java' : undefined} dangerouslySetInnerHTML={language === 'java' ? { __html: highlightLearningJavaSource(source) } : undefined}>{language === 'java' ? undefined : source}</code></pre></section>;
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
