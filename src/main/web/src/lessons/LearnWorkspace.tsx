import { useEffect, useMemo, useState } from 'react';
import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import type { LearningCategory, LearningTopic, LessonDescriptor, LessonFile, LessonsCatalog } from './protocol';
import { availableLessons, buildLearnExplorer, filterLearnExplorer, findLessonLocation, learnExplorerTotals, nextAvailableLesson, withoutCompletedLessons, type LearnExplorerCategory, type LearnExplorerLesson, type LearnExplorerTopic } from './learnExplorerModel';

export type LearnNavigationState =
  | { screen: 'HOME' }
  | { screen: 'ROADMAP'; categoryId: string }
  | { screen: 'TOPIC'; categoryId: string; topicId: string }
  | { screen: 'LESSON' };

type LearnWorkspaceProps = {
  navigation: LearnNavigationState;
  catalog: LessonsCatalog | null;
  error: string;
  onHome(): void;
  onOpenRoadmap(categoryId: string): void;
  onOpenTopic(categoryId: string, topicId: string): void;
  onOpenLesson(lesson: LessonDescriptor, path: string[]): void;
};

export function LearnWorkspace({ navigation, catalog, error, onHome, onOpenRoadmap, onOpenTopic, onOpenLesson }: LearnWorkspaceProps) {
  if (error) return <main className="learn-navigation-page"><p className="learn-navigation-error" role="alert">{error}</p></main>;
  if (!catalog) return <main className="learn-navigation-page"><p className="learn-navigation-loading">Carregando aulas...</p></main>;
  if (navigation.screen === 'LESSON') return null;
  const categoryId = navigation.screen === 'HOME' ? null : navigation.categoryId;
  const topicId = navigation.screen === 'TOPIC' ? navigation.topicId : null;
  const category = categoryId ? catalog.categories.find(item => item.id === categoryId) : undefined;
  const topic = category && topicId ? category.topics.find(item => item.id === topicId) : undefined;
  return <main className={`learn-navigation-page ${learnPageClass(navigation)}`}>
    {navigation.screen === 'HOME' && <LearnHome catalog={catalog} onOpenRoadmap={onOpenRoadmap} />}
    {navigation.screen === 'ROADMAP' && (category
      ? <Roadmap category={category} onHome={onHome} onOpenTopic={onOpenTopic} />
      : <MissingPage onHome={onHome} />)}
    {navigation.screen === 'TOPIC' && (category
      ? topic
        ? <TopicPage category={category} topic={topic} onBack={() => onOpenRoadmap(category.id)} onOpenLesson={onOpenLesson} />
        : <Roadmap category={category} onHome={onHome} onOpenTopic={onOpenTopic} />
      : <MissingPage onHome={onHome} />)}
  </main>;
}

export type LearnExplorerProps = {
  catalog: LessonsCatalog | null;
  activeTrackId?: string | null;
  selectedLessonId?: string | null;
  completedLessonId?: string | null;
  files?: LessonFile[];
  activeFileId?: string | null;
  onOpenFile?(fileId: string): void;
  onOpenLesson(lesson: LessonDescriptor, path: string[]): void;
};

export function LearnExplorer({ catalog, activeTrackId = null, selectedLessonId = null, completedLessonId = null, files = [], activeFileId = null, onOpenFile, onOpenLesson }: LearnExplorerProps) {
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(() => new Set());
  const [expandedTopics, setExpandedTopics] = useState<Set<string>>(() => new Set());
  const [query, setQuery] = useState('');
  const [hideCompleted, setHideCompleted] = useState(false);

  useEffect(() => {
    if (!activeTrackId) return;
    setExpandedCategories(current => current.has(activeTrackId) ? current : new Set(current).add(activeTrackId));
  }, [activeTrackId]);

  useEffect(() => {
    if (!catalog || !selectedLessonId) return;
    const location = findLessonLocation(catalog, selectedLessonId);
    if (!location) return;
    setExpandedCategories(current => current.has(location.categoryId) ? current : new Set(current).add(location.categoryId));
    setExpandedTopics(current => current.has(location.topicId) ? current : new Set(current).add(location.topicId));
  }, [catalog, selectedLessonId]);

  const model = useMemo(() => catalog ? buildLearnExplorer(catalog, { selectedLessonId, completedLessonId }) : [], [catalog, selectedLessonId, completedLessonId]);
  const filtered = useMemo(() => filterLearnExplorer(model, query), [model, query]);
  const visible = useMemo(() => hideCompleted ? withoutCompletedLessons(filtered) : filtered, [filtered, hideCompleted]);
  const totals = useMemo(() => learnExplorerTotals(model), [model]);
  const scope = useMemo(() => activeTrackId ? model.filter(entry => entry.category.id === activeTrackId) : model, [model, activeTrackId]);
  const scopeTotals = useMemo(() => learnExplorerTotals(scope), [scope]);
  const next = useMemo(() => nextAvailableLesson(model, selectedLessonId, completedLessonId), [model, selectedLessonId, completedLessonId]);
  const searching = query.trim().length > 0;
  const expandedIds = new Set([...expandedCategories, ...expandedTopics]);
  const trackIds = model.flatMap(entry => [entry.category.id, ...entry.topics.map(topic => topic.topic.id)]);
  const allExpanded = trackIds.length > 0 && trackIds.every(id => expandedIds.has(id));
  const progressPercent = scopeTotals.total ? Math.round((scopeTotals.completed / scopeTotals.total) * 100) : 0;
  const progressCaption = next ? `Próxima: ${next.lesson.title}` : scopeTotals.available > 0 ? 'Trilha concluída' : 'Nenhuma aula disponível';
  const emptyMessage = searching ? 'Nenhuma aula corresponde à busca.' : 'Nenhuma aula disponível.';
  const journeyTitle = activeTrackId ? (model.find(entry => entry.category.id === activeTrackId)?.category.title ?? 'Catálogo de aulas') : 'Catálogo de aulas';

  function toggle(setter: (update: (current: Set<string>) => Set<string>) => void, id: string) {
    setter(current => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleAll() {
    if (allExpanded) {
      setExpandedCategories(new Set());
      setExpandedTopics(new Set());
      return;
    }
    setExpandedCategories(new Set(model.map(entry => entry.category.id)));
    setExpandedTopics(new Set(model.flatMap(entry => entry.topics.map(topic => topic.topic.id))));
  }

  return <section className="project-explorer learn-explorer" data-pane-id="explorer" aria-label="Navegação das aulas">
    <header className="learn-explorer-heading" data-dock-handle>
      <div className="learn-explorer-title">
        <span className="learn-explorer-eyebrow">Sua jornada</span>
        <h2>Aulas</h2>
        <p>{activeTrackId ? 'Continue de onde você parou.' : 'Explore as trilhas disponíveis.'}</p>
      </div>
      <span className="learn-explorer-course" title="Linguagem da trilha">Java</span>
    </header>
    <div className="learn-explorer-progress" aria-label="Resumo da trilha">
      <div className="learn-explorer-progress-top"><strong>{journeyTitle}</strong><span>{progressPercent}% concluído</span></div>
      <div className="learn-explorer-progress-track" role="presentation"><span style={{ width: `${progressPercent}%` }} /></div>
      <div className="learn-explorer-progress-caption">
        <span>{scopeTotals.completed} de {scopeTotals.total} aulas</span>
        <span>{progressCaption}</span>
      </div>
    </div>
    <div className="learn-explorer-tools">
      <label className="learn-explorer-search">
        <span aria-hidden="true">⌕</span>
        <input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Buscar aula ou conceito…" aria-label="Buscar aula ou conceito" />
      </label>
      <button type="button" className={`learn-explorer-filter${hideCompleted ? ' is-active' : ''}`} onClick={() => setHideCompleted(value => !value)}
        aria-pressed={hideCompleted} title="Alternar aulas concluídas" aria-label="Alternar aulas concluídas"><span aria-hidden="true">☷</span></button>
    </div>
    <div className="learn-explorer-scroll">
      <div className="learn-explorer-section-label"><span>Trilha de aprendizado</span><span>{totals.total} aulas</span></div>
      {!catalog && <p className="learn-navigation-loading">Carregando aulas...</p>}
      {catalog && visible.length === 0 && <p className="learn-explorer-empty">{emptyMessage}</p>}
      <div className="learn-explorer-tree" role="tree" aria-label="Trilha de aprendizado">
        {visible.map((entry, index) => <CategoryNode key={entry.category.id} entry={entry} index={index}
          expanded={searching || expandedCategories.has(entry.category.id)} onToggle={() => toggle(setExpandedCategories, entry.category.id)}
          expandedTopics={expandedTopics} searching={searching} onToggleTopic={id => toggle(setExpandedTopics, id)}
          selectedLessonId={selectedLessonId} files={files} activeFileId={activeFileId}
          activeCategoryId={activeTrackId} onOpenFile={onOpenFile} onOpenLesson={onOpenLesson} />)}
      </div>
    </div>
    <div className="learn-explorer-footer">
      <span>Seu ritmo, seu progresso</span>
      <button type="button" onClick={toggleAll}>{allExpanded ? 'Recolher trilhas ↙' : 'Expandir trilhas ↗'}</button>
    </div>
  </section>;
}

type CategoryNodeProps = {
  entry: LearnExplorerCategory;
  index: number;
  expanded: boolean;
  onToggle(): void;
  expandedTopics: Set<string>;
  searching: boolean;
  onToggleTopic(topicId: string): void;
  selectedLessonId: string | null;
  files: LessonFile[];
  activeFileId: string | null;
  activeCategoryId: string | null;
  onOpenFile?(fileId: string): void;
  onOpenLesson(lesson: LessonDescriptor, path: string[]): void;
};

function CategoryNode({ entry, index, expanded, onToggle, expandedTopics, searching, onToggleTopic, selectedLessonId, files, activeFileId, activeCategoryId, onOpenFile, onOpenLesson }: CategoryNodeProps) {
  const active = entry.category.id === activeCategoryId;
  const complete = entry.total > 0 && entry.completed === entry.total;
  return <section className={`learn-module learn-explorer-category${active ? ' is-active' : ''}${expanded ? ' is-open' : ''}`} role="treeitem" aria-expanded={expanded} aria-level={1}>
    <button type="button" className="learn-module-toggle" onClick={onToggle} aria-expanded={expanded}>
      <span className="learn-module-chevron" aria-hidden="true">›</span>
      <span className="learn-module-icon" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
      <span className="learn-module-name"><strong>{entry.category.title}</strong><small>{entry.category.description}</small></span>
      <span className={`learn-module-status${complete ? ' is-complete' : ''}`}>{entry.completed}/{entry.total}</span>
    </button>
    {expanded && <div className="learn-module-body" role="group">
      {entry.topics.map(topicEntry => <TopicNode key={topicEntry.topic.id} entry={topicEntry}
        expanded={searching || expandedTopics.has(topicEntry.topic.id)} onToggle={() => onToggleTopic(topicEntry.topic.id)}
        selectedLessonId={selectedLessonId} files={files} activeFileId={activeFileId}
        onOpenFile={onOpenFile} onOpenLesson={onOpenLesson} />)}
    </div>}
  </section>;
}

type TopicNodeProps = {
  entry: LearnExplorerTopic;
  expanded: boolean;
  onToggle(): void;
  selectedLessonId: string | null;
  files: LessonFile[];
  activeFileId: string | null;
  onOpenFile?(fileId: string): void;
  onOpenLesson(lesson: LessonDescriptor, path: string[]): void;
};

function TopicNode({ entry, expanded, onToggle, selectedLessonId, files, activeFileId, onOpenFile, onOpenLesson }: TopicNodeProps) {
  return <div className={`learn-topic-group learn-explorer-topic${expanded ? ' is-open' : ''}`} role="treeitem" aria-expanded={expanded} aria-level={2}>
    <button type="button" className="learn-topic-toggle" onClick={onToggle} aria-expanded={expanded}>
      <span className="learn-topic-chevron" aria-hidden="true">›</span>
      <span className="learn-topic-title">{entry.topic.title}</span>
      <span className="learn-explorer-count">{entry.completed}/{entry.total}</span>
    </button>
    {expanded && <div className="learn-explorer-lessons" role="group">
      {entry.lessons.map((lessonEntry, index) => <LessonNode key={lessonEntry.lesson.id} entry={lessonEntry} index={index}
        selected={selectedLessonId === lessonEntry.lesson.id} files={files} activeFileId={activeFileId}
        onOpenFile={onOpenFile} onOpenLesson={onOpenLesson} />)}
    </div>}
  </div>;
}

type LessonNodeProps = {
  entry: LearnExplorerLesson;
  index: number;
  selected: boolean;
  files: LessonFile[];
  activeFileId: string | null;
  onOpenFile?(fileId: string): void;
  onOpenLesson(lesson: LessonDescriptor, path: string[]): void;
};

function LessonNode({ entry, index, selected, files, activeFileId, onOpenFile, onOpenLesson }: LessonNodeProps) {
  const { lesson, path, practice, status } = entry;
  const soon = status === 'soon';
  const done = status === 'completed';
  const meta = soon ? 'Em breve' : selected ? 'Atual' : done ? 'Concluída' : lesson.estimatedMinutes ? `${lesson.estimatedMinutes} min` : lesson.kind === 'PRACTICE' ? 'Prática' : 'Teoria';
  const number = done ? '✓' : soon ? '⌑' : `${index + 1}`;
  const className = `learn-explorer-lesson${selected ? ' is-active' : ''}${done ? ' is-done' : ''}${soon ? ' is-soon' : ''}`;
  return <>
    <button type="button" role="treeitem" aria-level={3} aria-selected={selected} className={className} disabled={!lesson.executable}
      onClick={() => onOpenLesson(lesson, path)}>
      <span className="learn-explorer-lesson-number" aria-hidden="true">{number}</span>
      <span className="learn-explorer-lesson-title">{lesson.title}</span>
      <span className="learn-explorer-lesson-meta">{meta}</span>
    </button>
    {practice && selected && files.length > 0 && <div className="learn-explorer-lesson-files">
      {files.map(file => <button key={file.id} type="button" className={`learn-explorer-lesson-file${activeFileId === file.id ? ' is-active' : ''}`}
        onClick={() => { if (onOpenFile) onOpenFile(file.id); }}>
        <EyeCodeIcon name="java" className="tree-icon" /><span>{file.name}</span>
      </button>)}
    </div>}
  </>;
}

function LearnHome({ catalog, onOpenRoadmap }: { catalog: LessonsCatalog; onOpenRoadmap(categoryId: string): void }) {
  return <div className="shell-page-content">
    <header className="learn-page-heading"><span>Aulas</span><h1>Aprenda no seu ritmo</h1><p>Escolha uma trilha para começar.</p></header>
    <section className="learn-roadmap-cards" aria-label="Trilhas disponíveis">
      {catalog.categories.map(category => <button key={category.id} type="button" className="learn-roadmap-card" onClick={() => onOpenRoadmap(category.id)}>
        <strong>{category.title}</strong><span>{category.description}</span><small>{category.topics.length} tópicos · {availableLessons(category)} aulas disponíveis</small>
      </button>)}
    </section>
  </div>;
}

function Roadmap({ category, onHome, onOpenTopic }: { category: LearningCategory; onHome(): void; onOpenTopic(categoryId: string, topicId: string): void }) {
  return <div className="shell-page-content">
    <button type="button" className="learn-back" onClick={onHome}>Aulas</button>
    <header className="learn-page-heading"><span>Trilha</span><h1>{category.title}</h1><p>{category.description}</p></header>
    <section className="learn-topic-trail" aria-label={`Tópicos de ${category.title}`}>
      {category.topics.map((topic, index) => <button key={topic.id} type="button" className="learn-topic-card" onClick={() => onOpenTopic(category.id, topic.id)}>
        <span className="learn-topic-marker">{index + 1}</span><span><strong>{topic.title}</strong><small>{topic.description}</small><em>{topic.lessons.length} aulas · {availableLessons(topic)} disponíveis</em></span>
      </button>)}
    </section>
  </div>;
}

function TopicPage({ category, topic, onBack, onOpenLesson }: { category: LearningCategory; topic: LearningTopic; onBack(): void; onOpenLesson(lesson: LessonDescriptor, path: string[]): void }) {
  return <div className="shell-page-content">
    <button type="button" className="learn-back" onClick={onBack}>Voltar para {category.title}</button>
    <header className="learn-page-heading"><span>{category.title}</span><h1>{topic.title}</h1><p>{topic.description}</p></header>
    <section className="learn-lesson-list" aria-label={`Aulas de ${topic.title}`}>
      {roadmapLessons(topic).map((lesson, index) => <LessonItem key={lesson.id} lesson={lesson} index={index} onOpen={() => onOpenLesson(lesson, [category.title, topic.title, lesson.title])} />)}
    </section>
  </div>;
}

function MissingPage({ onHome }: { onHome(): void }) {
  return <div className="shell-page-content"><button type="button" className="learn-back" onClick={onHome}>Voltar para Aulas</button></div>;
}

function learnPageClass(navigation: LearnNavigationState): string {
  if (navigation.screen === 'HOME') return 'learn-home-page';
  if (navigation.screen === 'ROADMAP') return 'learn-roadmap-page';
  return 'learn-topic-page';
}

export function topicsForTrack(catalog: LessonsCatalog, activeTrackId: string | null): LearningTopic[] {
  return catalog.categories.find(category => category.id === activeTrackId)?.topics ?? [];
}

function LessonItem({ lesson, index, onOpen }: { lesson: LessonDescriptor; index: number; onOpen(): void }) {
  const label = lesson.executable ? lesson.kind === 'THEORY' ? 'Teoria' : 'Prática' : 'Em breve';
  return <button type="button" className={`learn-lesson-item${lesson.executable ? '' : ' is-unavailable'}`} onClick={onOpen} disabled={!lesson.executable}>
    <span className="learn-lesson-order">{index + 1}</span><span className="learn-lesson-copy"><strong>{lesson.title}</strong><small>{lesson.description}</small></span><span className={`learn-lesson-kind${lesson.kind === 'PRACTICE' ? ' is-practice' : ''}`}>{label}</span>
  </button>;
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
