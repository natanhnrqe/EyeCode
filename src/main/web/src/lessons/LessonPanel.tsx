import { Fragment, useEffect, useState } from 'react';
import { highlightLearningJavaSource } from '../learning/highlightJava';
import type { LessonContentBlock, LessonInlineContent, LessonSession } from './protocol';

export function LessonContent({ session }: { session: LessonSession }) {
  const theory = session.kind === 'THEORY';
  const practiceLesson = session.kind === 'PRACTICE';
  const practiceActive = practiceLesson && session.phase === 'PRACTICE' && session.practice !== undefined;
  const practicePhaseClass = session.phase === 'PRESENTATION' ? ' is-presentation' : session.phase === 'PRACTICE' ? ' is-practice-active' : ' is-completed';
  return <article className={`lesson-reading-article${practicePhaseClass}${session.practiceCompleted ? ' is-practice-completed' : ''}`}>{theory && <header className="lesson-chapter-header"><h1 className="lesson-chapter-title">{session.title}</h1>{session.message && <p className="lesson-chapter-lead">{session.message}</p>}</header>}{practiceLesson && <header className="lesson-practice-header"><p className="lesson-practice-kicker">Prática</p><h1 className="lesson-practice-title">{session.title}</h1>{session.message && <p className="lesson-practice-lead">{session.message}</p>}</header>}{practiceActive ? <PracticeSupport blocks={session.contentBlocks} /> : <LessonBlocks blocks={session.contentBlocks} theory={theory} />}</article>;
}

export function PracticeSupport({ blocks }: { blocks: LessonContentBlock[] }) {
  const syntax = blocks.find(block => block.type === 'CODE');
  const hints = blocks.filter(block => block.type === 'CALLOUT');
  const concepts = blocks.filter(block => block.type === 'HEADING').slice(1).map(block => block.text).filter((text): text is string => Boolean(text));
  return <section className="lesson-practice-support"><section><h2>O que fazer</h2><ol><li>Use o editor para realizar a tarefa indicada no card.</li><li>Faça a alteração necessária dentro do método <code>main</code>.</li><li>Revise o código antes de verificar.</li></ol></section>{syntax && <section><h2>Exemplo de sintaxe</h2><LessonBlock block={syntax} theory={false} /></section>}{hints.length > 0 && <details className="lesson-practice-disclosure"><summary>Dicas</summary>{hints.map((hint, index) => <LessonBlock key={index} block={hint} theory={false} />)}</details>}{concepts.length > 0 && <details className="lesson-practice-disclosure"><summary>Conceitos relacionados</summary><ul>{concepts.map(concept => <li key={concept}>{concept}</li>)}</ul></details>}</section>;
}

export function LessonBlocks({ blocks, theory }: { blocks: LessonContentBlock[]; theory: boolean }) {
  let section = 0;
  return <>{blocks.map((block, index) => {
    const sectionNumber = theory && block.type === 'HEADING' ? ++section : undefined;
    return <Fragment key={index}>{sectionNumber && sectionNumber > 1 && <div className="lesson-section-divider" />}{<LessonBlock block={block} theory={theory} sectionNumber={sectionNumber} />}</Fragment>;
  })}</>;
}

export function LessonBlock({ block, theory, sectionNumber }: { block: LessonContentBlock; theory: boolean; sectionNumber?: number }) {
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

export function InlineContent({ content, fallback }: { content?: LessonInlineContent[]; fallback?: string }) {
  if (!content?.length) return <>{fallback}</>;
  return <>{content.map((part, index) => {
    if (part.type === 'CODE') return <code key={index}>{part.text}</code>;
    if (part.type === 'EMPHASIS') return <em key={index}>{part.text}</em>;
    if (part.type === 'STRONG') return <strong key={index}>{part.text}</strong>;
    if (part.type === 'LINK') return <span key={index} className="lesson-inline-link">{part.text}</span>;
    return <span key={index}>{part.text}</span>;
  })}</>;
}
