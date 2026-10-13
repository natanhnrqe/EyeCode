import { useLayoutEffect, useState } from 'react';
import type { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import type { LessonAnnotation as AnnotationData, LessonEditorRange } from './protocol';
import type { LessonDiagnosticEducation } from './lessonEducationModel';

type Props = {
  service: MonacoWorkspaceService;
  lessonUri: string | null;
  annotation?: AnnotationData;
  diagnostic?: LessonDiagnosticEducation | null;
};

export function LessonAnnotation({ service, lessonUri, annotation, diagnostic }: Props) {
  const [position, setPosition] = useState<{ left: number; top: number } | null>(null);
  const range: LessonEditorRange | undefined = diagnostic?.range ?? annotation?.range;
  const title = diagnostic?.title ?? annotation?.title ?? '';
  const message = diagnostic ? (diagnostic.tip || diagnostic.summary) : annotation?.message;

  useLayoutEffect(() => {
    if (!lessonUri || !range) {
      setPosition(null);
      return;
    }
    const update = () => setPosition(service.lessonAnnotationAnchor(lessonUri, range));
    update();
    const unsubscribe = service.subscribeViewport(update);
    window.addEventListener('resize', update);
    return () => {
      unsubscribe();
      window.removeEventListener('resize', update);
    };
  }, [range, lessonUri, service]);

  if (!range || !position || !message) return null;
  return <aside className={`lesson-annotation${diagnostic ? ' is-diagnostic' : ''}`} style={position}
    aria-label={diagnostic ? `Erro no código: ${title}` : title}>
    {diagnostic && <span className="lesson-annotation-kicker">Erro no seu código</span>}
    <strong>{title}</strong><p>{message}</p>
  </aside>;
}
