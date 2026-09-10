import { useEffect, useState } from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import type { LearningCategory, LearningTopic, LessonDescriptor, LessonsCatalog } from './protocol';

export type LearnNavigationState =
  | { screen: 'HOME' }
  | { screen: 'ROADMAP'; categoryId: string }
  | { screen: 'TOPIC'; categoryId: string; topicId: string }
  | { screen: 'LESSON' };

type Props = {
  navigation: LearnNavigationState;
  onHome(): void;
  onOpenRoadmap(categoryId: string): void;
  onOpenTopic(categoryId: string, topicId: string): void;
  onOpenLesson(lesson: LessonDescriptor, path: string[]): void;
};

export function LearnWorkspace({ navigation, onHome, onOpenRoadmap, onOpenTopic, onOpenLesson }: Props) {
  const [catalog, setCatalog] = useState<LessonsCatalog | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    void bridge.request<LessonsCatalog>('lessons', 'catalog', {})
      .then(setCatalog)
      .catch(reason => setError(reason instanceof Error ? reason.message : 'Não foi possível carregar as aulas.'));
  }, []);

  if (error) return <main className="learn-navigation-page"><p className="learn-navigation-error" role="alert">{error}</p></main>;
  if (!catalog) return <main className="learn-navigation-page"><p className="learn-navigation-loading">Carregando aulas...</p></main>;
  if (navigation.screen === 'HOME') return <LearnHome catalog={catalog} onOpenRoadmap={onOpenRoadmap} />;
  if (navigation.screen === 'LESSON') return null;
  const category = catalog.categories.find(item => item.id === navigation.categoryId);
  if (!category) return <main className="learn-navigation-page"><button type="button" className="learn-back" onClick={onHome}>Voltar para Aulas</button></main>;
  if (navigation.screen === 'ROADMAP') return <Roadmap category={category} onHome={onHome} onOpenTopic={onOpenTopic} />;
  const topic = category.topics.find(item => item.id === navigation.topicId);
  if (!topic) return <Roadmap category={category} onHome={onHome} onOpenTopic={onOpenTopic} />;
  return <TopicPage category={category} topic={topic} onBack={() => onOpenRoadmap(category.id)} onOpenLesson={onOpenLesson} />;
}

function LearnHome({ catalog, onOpenRoadmap }: { catalog: LessonsCatalog; onOpenRoadmap(categoryId: string): void }) {
  return <main className="learn-navigation-page learn-home-page">
    <header className="learn-page-heading"><span>Aulas</span><h1>Aprenda no seu ritmo</h1><p>Escolha uma trilha para começar.</p></header>
    <section className="learn-roadmap-cards" aria-label="Trilhas disponíveis">
      {catalog.categories.map(category => <button key={category.id} type="button" className="learn-roadmap-card" onClick={() => onOpenRoadmap(category.id)}>
        <strong>{category.title}</strong><span>{category.description}</span><small>{category.topics.length} tópicos · {availableLessons(category)} aulas disponíveis</small>
      </button>)}
    </section>
  </main>;
}

function Roadmap({ category, onHome, onOpenTopic }: { category: LearningCategory; onHome(): void; onOpenTopic(categoryId: string, topicId: string): void }) {
  return <main className="learn-navigation-page learn-roadmap-page">
    <button type="button" className="learn-back" onClick={onHome}>Aulas</button>
    <header className="learn-page-heading"><span>Trilha</span><h1>{category.title}</h1><p>{category.description}</p></header>
    <section className="learn-topic-trail" aria-label={`Tópicos de ${category.title}`}>
      {category.topics.map((topic, index) => <button key={topic.id} type="button" className="learn-topic-card" onClick={() => onOpenTopic(category.id, topic.id)}>
        <span className="learn-topic-marker">{index + 1}</span><span><strong>{topic.title}</strong><small>{topic.description}</small><em>{topic.lessons.length} aulas · {availableLessons(topic)} disponíveis</em></span>
      </button>)}
    </section>
  </main>;
}

function TopicPage({ category, topic, onBack, onOpenLesson }: { category: LearningCategory; topic: LearningTopic; onBack(): void; onOpenLesson(lesson: LessonDescriptor, path: string[]): void }) {
  return <main className="learn-navigation-page learn-topic-page">
    <button type="button" className="learn-back" onClick={onBack}>Voltar para {category.title}</button>
    <header className="learn-page-heading"><span>{category.title}</span><h1>{topic.title}</h1><p>{topic.description}</p></header>
    <section className="learn-lesson-list" aria-label={`Aulas de ${topic.title}`}>
      {roadmapLessons(topic).map((lesson, index) => <LessonItem key={lesson.id} lesson={lesson} index={index} onOpen={() => onOpenLesson(lesson, [category.title, topic.title, lesson.title])} />)}
    </section>
  </main>;
}

function LessonItem({ lesson, index, onOpen }: { lesson: LessonDescriptor; index: number; onOpen(): void }) {
  const label = lesson.executable ? lesson.kind === 'THEORY' ? 'Teoria' : 'Prática' : 'Em breve';
  return <button type="button" className={`learn-lesson-item${lesson.executable ? '' : ' is-unavailable'}`} onClick={onOpen} disabled={!lesson.executable}>
    <span className="learn-lesson-order">{index + 1}</span><span className="learn-lesson-copy"><strong>{lesson.title}</strong><small>{lesson.description}</small></span><span className={`learn-lesson-kind${lesson.kind === 'PRACTICE' ? ' is-practice' : ''}`}>{label}</span>
  </button>;
}

function availableLessons(value: LearningCategory | LearningTopic): number {
  const topics = 'topics' in value ? value.topics : [value];
  return topics.flatMap(topic => topic.lessons).filter(lesson => lesson.executable).length;
}

function roadmapLessons(topic: LearningTopic): LessonDescriptor[] {
  const lessonsByTitle = new Map(topic.lessons.map(lesson => [lesson.title, lesson]));
  const roadmapLessons = topic.roadmapSections.flatMap(section => section.items).map(item => lessonsByTitle.get(item.title)
    ?? { id: `${topic.id}.${item.id}`, title: item.title, description: item.description, categoryId: topic.categoryId,
      topicId: topic.id, difficulty: 'BEGINNER' as const, estimatedMinutes: 0, kind: 'THEORY' as const,
      concepts: [], executable: false });
  const listed = new Set(roadmapLessons.map(lesson => lesson.id));
  return [...roadmapLessons, ...topic.lessons.filter(lesson => !listed.has(lesson.id))];
}
