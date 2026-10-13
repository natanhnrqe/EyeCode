import { describe, expect, it } from 'vitest';
import type { LessonDescriptor, LessonsCatalog } from './protocol';
import { availableLessons, buildLearnExplorer, filterLearnExplorer, findLessonLocation, isPracticeLesson, learnExplorerTotals, lessonStatus, nextAvailableLesson, withoutCompletedLessons } from './learnExplorerModel';

function lesson(id: string, overrides: Partial<LessonDescriptor> = {}): LessonDescriptor {
  return {
    id,
    title: id,
    description: '',
    categoryId: 'java',
    topicId: 'java.fundamentals',
    difficulty: 'BEGINNER',
    estimatedMinutes: 10,
    kind: 'THEORY',
    concepts: [],
    executable: true,
    ...overrides
  };
}

const catalog: LessonsCatalog = {
  categories: [
    {
      id: 'java',
      title: 'Java',
      description: '',
      topics: [
        {
          id: 'java.fundamentals',
          title: 'Fundamentos',
          description: '',
          categoryId: 'java',
          roadmapSections: [],
          lessons: [
            lesson('java.theory'),
            lesson('java.practice', { kind: 'PRACTICE', practiceFiles: [{ id: 'main', name: 'Main.java', language: 'java', starterCode: '', readOnly: false }] }),
            lesson('java.soon', { executable: false })
          ]
        },
        {
          id: 'java.empty',
          title: 'Sem aulas',
          description: '',
          categoryId: 'java',
          roadmapSections: [],
          lessons: []
        }
      ]
    },
    {
      id: 'spring',
      title: 'Spring',
      description: '',
      topics: [
        {
          id: 'spring.core',
          title: 'Core',
          description: '',
          categoryId: 'spring',
          roadmapSections: [],
          lessons: [lesson('spring.soon', { categoryId: 'spring', topicId: 'spring.core', executable: false })]
        }
      ]
    }
  ]
};

describe('lessonStatus', () => {
  it('prioriza concluída sobre selecionada', () => {
    expect(lessonStatus(lesson('a'), 'a', 'a')).toBe('completed');
  });

  it('marca a aula selecionada como atual', () => {
    expect(lessonStatus(lesson('a'), 'a', null)).toBe('current');
  });

  it('marca executável como disponível', () => {
    expect(lessonStatus(lesson('a'), null, null)).toBe('available');
  });

  it('marca não executável como em breve', () => {
    expect(lessonStatus(lesson('a', { executable: false }), null, null)).toBe('soon');
  });
});

describe('isPracticeLesson', () => {
  it('exige kind PRACTICE e arquivos de prática', () => {
    expect(isPracticeLesson(lesson('a', { kind: 'PRACTICE' }))).toBe(false);
    expect(isPracticeLesson(lesson('a', { kind: 'PRACTICE', practiceFiles: [] }))).toBe(true);
    expect(isPracticeLesson(lesson('a', { kind: 'THEORY', practiceFiles: [] }))).toBe(false);
  });
});

describe('buildLearnExplorer', () => {
  it('representa todas as categorias e tópicos, inclusive vazios', () => {
    const model = buildLearnExplorer(catalog);
    expect(model.map(entry => entry.category.id)).toEqual(['java', 'spring']);
    expect(model[0].topics.map(entry => entry.topic.id)).toEqual(['java.fundamentals', 'java.empty']);
    expect(model[1].topics.map(entry => entry.topic.id)).toEqual(['spring.core']);
  });

  it('calcula contagens reais de disponibilidade', () => {
    const model = buildLearnExplorer(catalog);
    expect(model[0].total).toBe(3);
    expect(model[0].available).toBe(2);
    expect(model[0].topics[0].total).toBe(3);
    expect(model[0].topics[0].available).toBe(2);
    expect(model[0].topics[1].available).toBe(0);
    expect(model[1].available).toBe(0);
  });

  it('conta apenas as aulas concluídas como progresso', () => {
    const model = buildLearnExplorer(catalog, { completedLessonId: 'java.practice' });
    expect(model[0].completed).toBe(1);
    expect(model[0].topics[0].completed).toBe(1);
    expect(model[0].topics[1].completed).toBe(0);
    expect(model[1].completed).toBe(0);
    expect(buildLearnExplorer(catalog)[0].completed).toBe(0);
  });

  it('deriva o status de cada aula a partir da sessão atual', () => {
    const model = buildLearnExplorer(catalog, { selectedLessonId: 'java.theory', completedLessonId: 'java.practice' });
    const lessons = model[0].topics[0].lessons;
    expect(lessons.find(entry => entry.lesson.id === 'java.theory')?.status).toBe('current');
    expect(lessons.find(entry => entry.lesson.id === 'java.practice')?.status).toBe('completed');
    expect(lessons.find(entry => entry.lesson.id === 'java.soon')?.status).toBe('soon');
  });

  it('sinaliza aulas de prática e monta o caminho de navegação', () => {
    const model = buildLearnExplorer(catalog);
    const practice = model[0].topics[0].lessons.find(entry => entry.lesson.id === 'java.practice');
    expect(practice?.practice).toBe(true);
    expect(practice?.path).toEqual(['Java', 'Fundamentos', 'java.practice']);
  });
});

describe('findLessonLocation', () => {
  it('localiza a categoria e o tópico da aula', () => {
    expect(findLessonLocation(catalog, 'java.practice')).toEqual({ categoryId: 'java', topicId: 'java.fundamentals' });
    expect(findLessonLocation(catalog, 'spring.soon')).toEqual({ categoryId: 'spring', topicId: 'spring.core' });
  });

  it('retorna null quando a aula não existe', () => {
    expect(findLessonLocation(catalog, 'missing')).toBeNull();
  });
});

describe('availableLessons', () => {
  it('conta aulas executáveis da categoria e do tópico', () => {
    expect(availableLessons(catalog.categories[0])).toBe(2);
    expect(availableLessons(catalog.categories[0].topics[0])).toBe(2);
  });
});

describe('learnExplorerTotals', () => {
  it('soma aulas, disponíveis, concluídas e tópicos de toda a árvore', () => {
    expect(learnExplorerTotals(buildLearnExplorer(catalog))).toEqual({ total: 4, available: 2, completed: 0, topics: 3 });
    expect(learnExplorerTotals(buildLearnExplorer(catalog, { completedLessonId: 'java.practice' })))
      .toEqual({ total: 4, available: 2, completed: 1, topics: 3 });
  });
});

describe('nextAvailableLesson', () => {
  it('encontra a próxima aula executável após a selecionada', () => {
    const model = buildLearnExplorer(catalog);
    expect(nextAvailableLesson(model, 'java.theory')?.lesson.id).toBe('java.practice');
  });

  it('retorna null quando não há aulas executáveis', () => {
    const empty = buildLearnExplorer({ categories: [{ id: 'x', title: 'X', description: '', topics: [] }] });
    expect(nextAvailableLesson(empty, null)).toBeNull();
  });
});

describe('filterLearnExplorer', () => {
  it('retorna a árvore intacta quando a busca é vazia', () => {
    const model = buildLearnExplorer(catalog);
    expect(filterLearnExplorer(model, '   ')).toBe(model);
  });

  it('filtra por título de aula mantendo a hierarquia', () => {
    const filtered = filterLearnExplorer(buildLearnExplorer(catalog), 'java.theory');
    expect(filtered).toHaveLength(1);
    expect(filtered[0].topics).toHaveLength(1);
    expect(filtered[0].topics[0].lessons.map(entry => entry.lesson.id)).toEqual(['java.theory']);
  });

  it('filtra por conceito e descrição', () => {
    const byConcept = filterLearnExplorer(buildLearnExplorer(catalog), 'zzz');
    expect(byConcept).toHaveLength(0);
  });

  it('mantém as aulas da categoria correspondente e descarta tópicos vazios', () => {
    const filtered = filterLearnExplorer(buildLearnExplorer(catalog), 'java');
    expect(filtered).toHaveLength(1);
    expect(filtered[0].topics[0].lessons).toHaveLength(3);
  });

  it('recalcula as contagens de concluídas após filtrar', () => {
    const filtered = filterLearnExplorer(
      buildLearnExplorer(catalog, { completedLessonId: 'java.theory' }), 'java.theory');
    expect(filtered[0].completed).toBe(1);
    expect(filtered[0].topics[0].completed).toBe(1);
  });
});

describe('withoutCompletedLessons', () => {
  it('oculta as aulas concluídas preservando hierarquia e contagens', () => {
    const visible = withoutCompletedLessons(buildLearnExplorer(catalog, { completedLessonId: 'java.practice' }));
    expect(visible[0].topics[0].lessons.map(entry => entry.lesson.id)).toEqual(['java.theory', 'java.soon']);
    expect(visible[0].topics[0]).toMatchObject({ total: 3, available: 2, completed: 1 });
    expect(visible[0]).toMatchObject({ total: 3, available: 2, completed: 1 });
  });

  it('mantém tópicos e categorias mesmo quando todas as aulas estão concluídas', () => {
    const single = buildLearnExplorer({
      categories: [{
        id: 'x', title: 'X', description: '', topics: [{
          id: 'x.t', title: 'T', description: '', categoryId: 'x', roadmapSections: [], lessons: [lesson('x.l')]
        }]
      }]
    }, { completedLessonId: 'x.l' });
    const visible = withoutCompletedLessons(single);
    expect(visible).toHaveLength(1);
    expect(visible[0].topics).toHaveLength(1);
    expect(visible[0].topics[0].lessons).toHaveLength(0);
    expect(visible[0].topics[0]).toMatchObject({ total: 1, completed: 1 });
  });

  it('mantém a árvore intacta quando nada foi concluído', () => {
    const model = buildLearnExplorer(catalog);
    expect(withoutCompletedLessons(model)).toEqual(model);
  });
});
