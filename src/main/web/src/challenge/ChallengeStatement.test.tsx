// @vitest-environment jsdom
import { afterEach, expect, test } from 'vitest';
import { act, createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { CNPJ_VALIDATOR_CHALLENGE } from './challengeData';
import { ChallengeStatement } from './ChallengeStatement';
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

function render(challenge: Challenge, width?: number): HTMLElement {
  container = document.createElement('div');
  if (width) container.style.width = `${width}px`;
  document.body.appendChild(container);
  root = createRoot(container);
  act(() => root!.render(createElement(ChallengeStatement, { challenge })));
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

test('single verified documentation reference opens a new tab with safe rel attributes', () => {
  const view = render(CNPJ_VALIDATOR_CHALLENGE);
  const link = view.querySelector<HTMLAnchorElement>('.challenge-documentation-link');
  expect(link?.href).toBe('https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html');
  expect(link?.target).toBe('_blank');
  expect(link?.rel).toContain('noopener');
  expect(link?.rel).toContain('noreferrer');
});

test('multiple documentation references are listed and invalid references are omitted', () => {
  const view = render(challenge({ metadata: { documentationReferences: [
    { title: 'API A', url: 'https://example.org/api', section: 'types' },
    { title: 'API B', url: 'https://example.org/other' },
    { title: 'Unsafe', url: 'javascript:alert(1)' },
    { title: 'Credential URL', url: 'https://user:pass@example.org' }
  ] } }));
  expect(view.querySelector('.challenge-documentation-link')).toBeNull();
  expect(view.querySelectorAll('.challenge-documentation-list a')).toHaveLength(2);
  expect(view.querySelector('.challenge-documentation-list a')?.getAttribute('href')).toBe('https://example.org/api#types');
  expect(view.textContent).not.toContain('Unsafe');
});

test('no documentation button is rendered without a valid reference', () => {
  const view = render(challenge({ metadata: { documentationReferences: [{ title: 'Inseguro', url: 'http://example.org' }] } }));
  expect(view.querySelector('.challenge-documentation-link')).toBeNull();
  expect(view.querySelector('.challenge-documentation-list')).toBeNull();
});

test('statement remains stacked and scrollable inside a narrow panel', () => {
  const view = render(CNPJ_VALIDATOR_CHALLENGE, 280);
  expect(view.style.width).toBe('280px');
  expect(view.querySelectorAll('.challenge-example > div')).toHaveLength(2);
  expect(view.textContent).toContain('Regras de negócio');
});
