import { describe, expect, it } from 'vitest';
import { branchLabel, extractToc, filterEntries, groupBranches, subgroupLabel } from './tree';
import type { DocumentationEntry } from './protocol';

const entries: DocumentationEntry[] = [
  { id: 'java/jdk/variables', title: 'Variáveis em Java', branch: 'jdk', summary: 'Declaração e escopo.' },
  { id: 'java/jdk/string', title: 'String', branch: 'jdk' },
  { id: 'java/spring/boot-basics', title: 'Spring Boot: Primeiros passos', branch: 'spring' },
  { id: 'java/javafx/stage-scene', title: 'JavaFX: Stage e Scene', branch: 'javafx' },
  { id: 'java/junit/first-test', title: 'Seu primeiro teste com JUnit 5', branch: 'junit' },
];

describe('branchLabel', () => {
  it('maps known branches to friendly names', () => {
    expect(branchLabel('jdk')).toBe('JDK');
    expect(branchLabel('spring')).toBe('Spring Boot');
    expect(branchLabel('javafx')).toBe('JavaFX');
    expect(branchLabel('junit')).toBe('JUnit');
  });

  it('falls back to title case for unknown branches', () => {
    expect(branchLabel('meu-topico')).toBe('Meu Topico');
    expect(branchLabel('quarkus')).toBe('Quarkus');
  });
});

describe('subgroupLabel', () => {
  it('keeps package-like labels literal', () => {
    expect(subgroupLabel('java.lang')).toBe('java.lang');
    expect(subgroupLabel('java.util')).toBe('java.util');
  });

  it('title cases plain word subgroups', () => {
    expect(subgroupLabel('fundamentos')).toBe('Fundamentos');
    expect(subgroupLabel('meu-topico')).toBe('Meu Topico');
  });
});

describe('groupBranches', () => {
  it('groups entries preserving catalog order', () => {
    const groups = groupBranches(entries);
    expect(groups.map(group => group.branch)).toEqual(['jdk', 'spring', 'javafx', 'junit']);
    expect(groups[0].label).toBe('JDK');
    expect(groups[0].entries.map(entry => entry.id)).toEqual(['java/jdk/variables', 'java/jdk/string']);
  });

  it('returns an empty list for an empty catalog', () => {
    expect(groupBranches([])).toEqual([]);
  });
});

describe('groupBranches subgroups', () => {
  const catalog: DocumentationEntry[] = [
    { id: 'java/jdk/fundamentos/variables', title: 'Variáveis em Java', branch: 'jdk', subgroup: 'fundamentos' },
    { id: 'java/jdk/fundamentos/classes', title: 'Classes e Objetos', branch: 'jdk', subgroup: 'fundamentos' },
    { id: 'java/jdk/java.lang/string', title: 'String', branch: 'jdk', subgroup: 'java.lang', type: 'api' },
    { id: 'java/jdk/java.util/list', title: 'List', branch: 'jdk', subgroup: 'java.util', type: 'api' },
    { id: 'java/jdk/java.util/map', title: 'Map', branch: 'jdk', subgroup: 'java.util', type: 'api' },
    { id: 'java/spring/boot-basics', title: 'Spring Boot: Primeiros passos', branch: 'spring' },
  ];

  it('splits entries into subgroups keeping direct entries separate', () => {
    const [jdk, spring] = groupBranches(catalog);

    expect(jdk.entries).toEqual([]);
    expect(jdk.total).toBe(5);
    expect(jdk.subgroups.map(subgroup => subgroup.key)).toEqual(['fundamentos', 'java.lang', 'java.util']);
    expect(jdk.subgroups[0].entries.map(entry => entry.id)).toEqual([
      'java/jdk/fundamentos/variables',
      'java/jdk/fundamentos/classes',
    ]);
    expect(spring.entries.map(entry => entry.id)).toEqual(['java/spring/boot-basics']);
    expect(spring.subgroups).toEqual([]);
    expect(spring.total).toBe(1);
  });

  it('labels subgroups by package literal or title case', () => {
    const [jdk] = groupBranches(catalog);
    expect(jdk.subgroups.map(subgroup => subgroup.label)).toEqual(['Fundamentos', 'java.lang', 'java.util']);
  });

  it('drops subgroups whose entries are filtered out', () => {
    const filtered = filterEntries(catalog, 'string');
    const [jdk] = groupBranches(filtered);

    expect(jdk.subgroups.map(subgroup => subgroup.key)).toEqual(['java.lang']);
    expect(jdk.total).toBe(1);
  });
});

describe('filterEntries', () => {
  it('returns everything for a blank query', () => {
    expect(filterEntries(entries, '   ')).toHaveLength(5);
  });

  it('matches by title case-insensitively', () => {
    const result = filterEntries(entries, 'STRING');
    expect(result.map(entry => entry.id)).toEqual(['java/jdk/string']);
  });

  it('matches by summary text', () => {
    const result = filterEntries(entries, 'escopo');
    expect(result.map(entry => entry.id)).toEqual(['java/jdk/variables']);
  });

  it('matches by identifier fragment', () => {
    const result = filterEntries(entries, 'junit/');
    expect(result.map(entry => entry.id)).toEqual(['java/junit/first-test']);
  });

  it('returns an empty list when nothing matches', () => {
    expect(filterEntries(entries, 'zzz-nada')).toEqual([]);
  });
});

describe('extractToc', () => {
  it('collects level two and level three headings with ids', () => {
    const html = '<h2 id="por-que-existe">Por que existe</h2><p>x</p>'
      + '<h3 id="anatomia">Anatomia da <code>sintaxe</code></h3>';
    expect(extractToc(html)).toEqual([
      { level: 2, id: 'por-que-existe', text: 'Por que existe' },
      { level: 3, id: 'anatomia', text: 'Anatomia da sintaxe' },
    ]);
  });

  it('decodes html entities in heading text', () => {
    expect(extractToc('<h2 id="a-b">A &amp; B</h2>')).toEqual([
      { level: 2, id: 'a-b', text: 'A & B' },
    ]);
  });

  it('returns an empty list when there are no anchored headings', () => {
    expect(extractToc('<p>Sem títulos</p>')).toEqual([]);
  });
});
