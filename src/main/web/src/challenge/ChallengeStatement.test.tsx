// @vitest-environment jsdom
import { afterEach, expect, test } from 'vitest';
import { act, createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { challengeFor, CNPJ_VALIDATOR_CHALLENGE } from './challengeData';
import { ChallengeStatement } from './ChallengeStatement';
import { CHALLENGE_CATALOG, CHALLENGE_TRACKS } from './catalog';
import type { Challenge } from './types';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

let root: Root | null = null;
let container: HTMLElement | null = null;

afterEach(() => {
  if (root && container) {
    act(() => root!.unmount());
    container.remove();
  }
  root = null;
  container = null;
});

function render(challenge: Challenge, width?: number, onOpenDocumentation = (_id: string) => {}): HTMLElement {
  container = document.createElement('div');
  if (width) container.style.width = `${width}px`;
  document.body.appendChild(container);
  root = createRoot(container);
  act(() => root!.render(createElement(ChallengeStatement, { challenge, onOpenDocumentation })));
  return container;
}

function challenge(overrides: Partial<Challenge> = {}): Challenge {
  return {
    id: 'custom', title: 'Desafio', descriptionMarkdown: '', starterCode: '', entryClassName: 'Main', ...overrides
  };
}

test('renders standard populated sections and omits empty optional sections', () => {
  const view = render(challenge({
    statement: {
      objective: 'Validar entradas.', instructions: 'Implemente a validação.', businessRules: ['Aceite valores válidos.'],
      examples: [{ input: 'entrada', output: 'saída' }]
    }
  }));
  expect(view.textContent).toContain('Objetivo');
  expect(view.textContent).toContain('O que implementar');
  expect(view.textContent).toContain('Regras de negócio');
  expect(view.textContent).toContain('Exemplo');
  expect(view.textContent).not.toContain('Restrições e casos-limite');
  expect(view.textContent).not.toContain('Dicas');
});

test('renders the existing legacy CNPJ statement and pairs its input with expected output', () => {
  const view = render(CNPJ_VALIDATOR_CHALLENGE);
  expect(view.textContent).toContain('Implemente a validação de um CNPJ brasileiro.');
  expect(view.textContent).toContain('Um CNPJ possui 14 dígitos.');
  const example = view.querySelector('.challenge-example');
  expect(example?.children[0].textContent).toContain('isValid("04.252.011/0001-10")');
  expect(example?.children[1].textContent).toContain('true');
});

test('renders multiple examples as distinct input/output pairs', () => {
  const view = render(challenge({ statement: { examples: [
    { input: 'input one', output: 'output one' }, { input: 'input two', output: 'output two' }
  ] } }));
  const examples = view.querySelectorAll('.challenge-example');
  expect(examples).toHaveLength(2);
  expect(examples[0].textContent).toContain('input one');
  expect(examples[0].textContent).toContain('output one');
  expect(examples[1].textContent).toContain('input two');
  expect(examples[1].textContent).toContain('output two');
});

test('CNPJ references EyeCode String and regex articles', () => {
  const view = render(CNPJ_VALIDATOR_CHALLENGE);
  expect(view.textContent).toContain('String');
  expect(view.textContent).toContain('Regex (Pattern e Matcher)');
});

test('every catalog challenge resolves to its own statement and existing internal documentation references', () => {
  for (const item of CHALLENGE_CATALOG) {
    const resolved = challengeFor(item.id);
    expect(resolved.id).toBe(item.id);
    expect(resolved.title).toBe(item.title);
    expect(item.documentationReferences.length).toBeGreaterThan(0);
    for (const reference of item.documentationReferences) {
      expect(reference.id).toMatch(/^java\//);
      expect(reference.title.trim()).not.toBe('');
      expect(reference.relevance.trim()).not.toBe('');
    }
  }
});

test('Java fundamentals topics are covered by challenges with instructions, examples, and documentation', () => {
  const fundamentals = CHALLENGE_CATALOG.filter(item => item.trackId === 'java-fundamentals');
  const track = CHALLENGE_TRACKS.find(item => item.id === 'java-fundamentals');
  expect(track).toBeDefined();
  for (const topic of track!.topics) {
    expect(fundamentals.some(item => item.topicId === topic.id), `${topic.title} has a challenge`).toBe(true);
  }
  for (const item of fundamentals) {
    const challengeData = challengeFor(item.id);
    const view = render(challengeData);
    expect(view.textContent).toContain('O que implementar');
    expect(view.querySelectorAll('.challenge-example').length).toBeGreaterThan(0);
    expect(item.documentationReferences.length).toBeGreaterThan(0);
    act(() => root!.unmount());
    container!.remove();
    root = null;
    container = null;
  }
});

test('multiple documentation references open their internal article IDs', () => {
  const opened: string[] = [];
  const view = render(challenge({ metadata: { documentationReferences: [
    { id: 'java/jdk/java.lang/string', title: 'String' },
    { id: 'java/jdk/java.util/regex', title: 'Regex' },
    { id: 'javascript:alert(1)', title: 'Unsafe' },
    { id: '../outside', title: 'Traversal' }
  ] } }), undefined, id => opened.push(id));
  expect(view.querySelector('.challenge-documentation-link')).toBeNull();
  const links = view.querySelectorAll<HTMLButtonElement>('.challenge-documentation-list button');
  expect(links).toHaveLength(2);
  act(() => links[0].click());
  act(() => links[1].click());
  expect(opened).toEqual(['java/jdk/java.lang/string', 'java/jdk/java.util/regex']);
  expect(view.textContent).not.toContain('Unsafe');
});

test('single documentation action opens its article in the existing editor documentation flow', () => {
  const opened: string[] = [];
  const view = render(challenge({ metadata: { documentationReferences: [{ id: 'java/jdk/java.lang/string', title: 'String' }] } }), undefined, id => opened.push(id));
  const action = view.querySelector<HTMLButtonElement>('.challenge-documentation-link');
  expect(action?.textContent).toContain('Abrir documentação no editor');
  act(() => action?.click());
  expect(opened).toEqual(['java/jdk/java.lang/string']);
});

test('no documentation action is rendered without a valid internal reference', () => {
  const view = render(challenge({ metadata: { documentationReferences: [{ id: 'https://example.org', title: 'External' }] } }));
  expect(view.querySelector('.challenge-documentation-link')).toBeNull();
  expect(view.querySelector('.challenge-documentation-list')).toBeNull();
});

test('statement remains stacked and scrollable inside a narrow panel', () => {
  const view = render(CNPJ_VALIDATOR_CHALLENGE, 280);
  expect(view.style.width).toBe('280px');
  expect(view.querySelectorAll('.challenge-example > div')).toHaveLength(2);
  expect(view.textContent).toContain('Regras de negócio');
});
