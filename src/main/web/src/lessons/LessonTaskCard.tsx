import { useEffect, useState } from 'react';
import type { LessonInlineContent, LessonSession, PracticeVerificationResult } from './protocol';

type Props = { session: LessonSession; verification: PracticeVerificationResult | null; verifying: boolean; onVerify(): void };

export function LessonTaskCard({ session, verification, verifying, onVerify }: Props) {
  const [open, setOpen] = useState(true);
  const practice = session.practice;

  useEffect(() => setOpen(true), [session.sessionId, session.currentStep, practice?.id]);

  if (session.phase !== 'PRACTICE' || !practice) return null;
  if (!open) return <button type="button" className="lesson-task-reopen" onClick={() => setOpen(true)}>Tarefa</button>;

  return <aside className="lesson-task-card" aria-label="Sua tarefa">
    <header><span>Sua tarefa</span><button type="button" className="lesson-task-close" onClick={() => setOpen(false)} aria-label="Ocultar tarefa">×</button></header>
    <p className="lesson-task-instruction"><InlineContent content={practice.instruction} /></p>
    {verification && <section className="lesson-task-feedback"><strong>{verification.status === 'SUCCESS' ? 'Correto' : 'Revise sua resposta'}</strong><p>{verification.message}</p></section>}
    <footer><button type="button" className="primary-action" onClick={onVerify} disabled={verifying}>{verifying ? 'Verificando...' : 'Verificar'}</button></footer>
  </aside>;
}

function InlineContent({ content }: { content: LessonInlineContent[] }) {
  return <>{content.map((part, index) => {
    if (part.type === 'CODE') return <code key={index}>{part.text}</code>;
    if (part.type === 'EMPHASIS') return <em key={index}>{part.text}</em>;
    if (part.type === 'STRONG') return <strong key={index}>{part.text}</strong>;
    return <span key={index}>{part.text}</span>;
  })}</>;
}
