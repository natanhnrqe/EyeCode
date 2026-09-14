import { useLayoutEffect, useState } from 'react';
import type { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import type { LessonAnnotation as AnnotationData } from './protocol';

type Props = { service: MonacoWorkspaceService; lessonUri: string | null; annotation?: AnnotationData };

export function LessonAnnotation({ service, lessonUri, annotation }: Props) {
  const [position, setPosition] = useState<{ left: number; top: number } | null>(null);

  useLayoutEffect(() => {
    if (!lessonUri || !annotation) {
      setPosition(null);
      return;
    }
    const update = () => setPosition(service.lessonAnnotationAnchor(lessonUri, annotation.range));
    update();
    const unsubscribe = service.subscribeViewport(update);
    window.addEventListener('resize', update);
    return () => {
      unsubscribe();
      window.removeEventListener('resize', update);
    };
  }, [annotation, lessonUri, service]);

  if (!annotation || !position) return null;
  return <aside className="lesson-annotation" style={position} aria-label={annotation.title}>
    <strong>{annotation.title}</strong><p>{annotation.message}</p>
  </aside>;
}
