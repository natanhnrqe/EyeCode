import type { LearningCategory, LearningTopic, LessonDescriptor, LessonsCatalog } from './protocol';

export type LearnLessonStatus = 'completed' | 'current' | 'available' | 'soon';

export type LearnExplorerLesson = {
  lesson: LessonDescriptor;
  path: string[];
  practice: boolean;
  status: LearnLessonStatus;
};

export type LearnExplorerTopic = {
  topic: LearningTopic;
  lessons: LearnExplorerLesson[];
  total: number;
  available: number;
};

export type LearnExplorerCategory = {
  category: LearningCategory;
  topics: LearnExplorerTopic[];
  total: number;
  available: number;
};

export type LearnExplorerModelOptions = {
  selectedLessonId?: string | null;
  completedLessonId?: string | null;
};

export function lessonStatus(
  lesson: LessonDescriptor,
  selectedLessonId: string | null = null,
  completedLessonId: string | null = null
): LearnLessonStatus {
  if (completedLessonId !== null && lesson.id === completedLessonId) return 'completed';
  if (selectedLessonId !== null && lesson.id === selectedLessonId) return 'current';
  return lesson.executable ? 'available' : 'soon';
}

export function isPracticeLesson(lesson: LessonDescriptor): boolean {
  return lesson.kind === 'PRACTICE' && lesson.practiceFiles !== undefined;
}

export function learnLessonPath(category: LearningCategory, topic: LearningTopic, lesson: LessonDescriptor): string[] {
  return [category.title, topic.title, lesson.title];
}

export function buildLearnExplorer(catalog: LessonsCatalog, options: LearnExplorerModelOptions = {}): LearnExplorerCategory[] {
  const selectedLessonId = options.selectedLessonId ?? null;
  const completedLessonId = options.completedLessonId ?? null;
  return catalog.categories.map(category => {
    const topics = category.topics.map(topic => {
      const lessons = topic.lessons.map(lesson => ({
        lesson,
        path: learnLessonPath(category, topic, lesson),
        practice: isPracticeLesson(lesson),
        status: lessonStatus(lesson, selectedLessonId, completedLessonId)
      }));
      return {
        topic,
        lessons,
        total: lessons.length,
        available: lessons.filter(entry => entry.lesson.executable).length
      };
    });
    return {
      category,
      topics,
      total: topics.reduce((sum, topic) => sum + topic.total, 0),
      available: topics.reduce((sum, topic) => sum + topic.available, 0)
    };
  });
}

export function findLessonLocation(catalog: LessonsCatalog, lessonId: string): { categoryId: string; topicId: string } | null {
  for (const category of catalog.categories) {
    for (const topic of category.topics) {
      if (topic.lessons.some(lesson => lesson.id === lessonId)) {
        return { categoryId: category.id, topicId: topic.id };
      }
    }
  }
  return null;
}

export function availableLessons(value: LearningCategory | LearningTopic): number {
  const topics = 'topics' in value ? value.topics : [value];
  return topics.flatMap(topic => topic.lessons).filter(lesson => lesson.executable).length;
}

export type LearnExplorerTotals = { total: number; available: number; topics: number };

export function learnExplorerTotals(model: LearnExplorerCategory[]): LearnExplorerTotals {
  return model.reduce<LearnExplorerTotals>((totals, category) => ({
    total: totals.total + category.total,
    available: totals.available + category.available,
    topics: totals.topics + category.topics.length
  }), { total: 0, available: 0, topics: 0 });
}

export function nextAvailableLesson(
  model: LearnExplorerCategory[],
  selectedLessonId: string | null = null,
  completedLessonId: string | null = null
): LearnExplorerLesson | null {
  const lessons = model.flatMap(category => category.topics.flatMap(topic => topic.lessons));
  const anchor = selectedLessonId ?? completedLessonId;
  const start = anchor ? lessons.findIndex(entry => entry.lesson.id === anchor) : -1;
  const ordered = start >= 0 ? [...lessons.slice(start + 1), ...lessons.slice(0, start + 1)] : lessons;
  return ordered.find(entry => entry.lesson.executable && entry.status !== 'completed') ?? null;
}

export function filterLearnExplorer(model: LearnExplorerCategory[], query: string): LearnExplorerCategory[] {
  const needle = query.trim().toLocaleLowerCase('pt-BR');
  if (!needle) return model;
  const categories: LearnExplorerCategory[] = [];
  for (const category of model) {
    const categoryMatches = category.category.title.toLocaleLowerCase('pt-BR').includes(needle);
    const topics: LearnExplorerTopic[] = [];
    for (const topic of category.topics) {
      const topicMatches = categoryMatches || topic.topic.title.toLocaleLowerCase('pt-BR').includes(needle);
      const lessons = topicMatches ? topic.lessons : topic.lessons.filter(entry =>
        entry.lesson.title.toLocaleLowerCase('pt-BR').includes(needle)
        || entry.lesson.description.toLocaleLowerCase('pt-BR').includes(needle)
        || entry.lesson.concepts.some(concept => concept.toLocaleLowerCase('pt-BR').includes(needle)));
      if (!lessons.length) continue;
      topics.push({ topic: topic.topic, lessons, total: lessons.length, available: lessons.filter(entry => entry.lesson.executable).length });
    }
    if (!topics.length) continue;
    categories.push({
      category: category.category,
      topics,
      total: topics.reduce((sum, topic) => sum + topic.total, 0),
      available: topics.reduce((sum, topic) => sum + topic.available, 0)
    });
  }
  return categories;
}
