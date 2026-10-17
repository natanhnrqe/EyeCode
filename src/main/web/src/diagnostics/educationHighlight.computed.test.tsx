// @vitest-environment jsdom
import { expect, test } from 'vitest';
import { act } from 'react';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ProblemsPanel } from './ProblemsPanel';
import { LearnContextPanel } from '../lessons/LearnContextPanel';
import type { DiagnosticsViewState, WebDiagnostic } from './protocol';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

const styles = readFileSync(join(__dirname, '..', 'styles.css'), 'utf8');

const diagnostic: WebDiagnostic = {
  severity: 'ERROR', code: 'compiler.err.expected', message: "';' expected",
  startLine: 3, startColumn: 5, endLine: 3, endColumn: 18,
  education: {
    title: 'Falta o ponto e vírgula',
    summary: 'Toda instrução termina com ;',
    explanation: 'O compilador esperava um ; fechando a instrução.',
    pattern: 'int idade = 15; // registro\nString nome = "Ana";',
    correctedPattern: 'int idade = 15;\nString nome = "Ana";',
    tip: 'Confira o fim da linha.',
    relatedContent: []
  }
};

const state: DiagnosticsViewState = {
  activeUri: null, active: null,
  results: [{
    uri: 'lesson://fundamentals/s1/main', requestId: '1', modelVersion: 1,
    diagnostics: [diagnostic]
  }]
};

let styleElement: HTMLStyleElement | null = null;
let root: Root | null = null;
let container: HTMLElement | null = null;

function setup() {
  styleElement = document.createElement('style');
  styleElement.textContent = styles;
  document.head.appendChild(styleElement);
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
}

function teardown() {
  act(() => { root?.render(createElement('div')); });
  root?.unmount();
  container?.remove();
  styleElement?.remove();
  styleElement = null;
  root = null;
  container = null;
}

test('a Cor de um token keyword é idêntica no bloco funcional e nos blocos educacionais', () => {
  setup();
  try {
    act(() => {
      root!.render(createElement('div', null,
        createElement('div', { className: 'learning-body' },
          createElement('pre', null,
            createElement('code', { className: 'language-java' },
              createElement('span', { className: 'learning-token-keyword' }, 'int')))),
        createElement(ProblemsPanel, {
          state, documents: [{ uri: 'lesson://fundamentals/s1/main', displayName: 'Main.java' }],
          onNavigate: () => {}, learnMode: true
        }),
        createElement(LearnContextPanel, {
          active: 'understand', output: [], outputDropped: 0, runState: {
            running: false, finished: false, stopped: false, exitCode: null,
            rerunAvailable: false, configurations: [], selectedConfigurationId: '', phase: 'IDLE'
          },
          diagnostics: state, documents: [{ uri: 'lesson://fundamentals/s1/main', displayName: 'Main.java' }],
          trace: null, capturing: false, onSelect: () => {}, onNavigateProblem: () => {}
        })));
    });

    const functionalKeyword = document.querySelector<HTMLElement>('.learning-body .learning-token-keyword');
    const patternBlock = document.querySelector<HTMLElement>('.learn-problem-pattern');
    const understandBlock = document.querySelector<HTMLElement>('.learn-diagnostic-understanding pre');

    expect(functionalKeyword, 'o bloco funcional de referência deve existir').not.toBeNull();
    expect(patternBlock, 'o bloco pattern deve existir').not.toBeNull();
    expect(understandBlock, 'o bloco Entender deve existir').not.toBeNull();

    // o bloco correctedPattern é condicional: abrir "Ver exemplo parecido"
    const exampleToggle = [...document.querySelectorAll<HTMLButtonElement>('button')]
      .find(button => button.textContent === 'Ver exemplo parecido');
    expect(exampleToggle, 'o botão de exemplo deve existir').toBeDefined();
    act(() => { exampleToggle!.click(); });
    const exampleBlock = document.querySelector<HTMLElement>('.learn-problem-example');
    expect(exampleBlock, 'o bloco correctedPattern deve existir').not.toBeNull();

    // os spans do highlighter estão presentes nos três blocos educacionais
    const patternKeyword = patternBlock!.querySelector<HTMLElement>('.learning-token-keyword');
    const exampleKeyword = exampleBlock!.querySelector<HTMLElement>('.learning-token-keyword');
    expect(patternKeyword, 'pattern deve conter spans de token').not.toBeNull();
    expect(exampleKeyword, 'correctedPattern deve conter spans de token').not.toBeNull();
    expect(understandBlock!.querySelector('.learning-token-keyword'), 'Entender deve conter spans de token').not.toBeNull();

    const functionalColor = getComputedStyle(functionalKeyword!).color;
    const patternColor = getComputedStyle(patternKeyword!).color;
    const exampleColor = getComputedStyle(exampleKeyword!).color;

    // se o CSS do repositório não aplicar a cor ao bloco education, este teste
    // falha — é exatamente a regressão vista manualmente.
    expect(patternColor, 'pattern precisa da mesma cor do bloco funcional').toBe(functionalColor);
    expect(exampleColor, 'correctedPattern precisa da mesma cor do bloco funcional').toBe(functionalColor);
    expect(functionalColor, 'a cor computada deve ser a do tema, não herdada do texto base').toBe('rgb(240, 170, 130)');
  } finally {
    teardown();
  }
});

test('os cinco tipos de token recebem cores distintas no bloco pattern', () => {
  setup();
  try {
    act(() => {
      root!.render(createElement('div', null,
        createElement('div', { className: 'learning-body' },
          createElement('pre', null,
            createElement('code', { className: 'language-java' }, 'x')),
        createElement('div', null)),
        createElement(ProblemsPanel, {
          state, documents: [{ uri: 'lesson://fundamentals/s1/main', displayName: 'Main.java' }],
          onNavigate: () => {}, learnMode: true
        }),
        createElement(LearnContextPanel, {
          active: 'understand', output: [], outputDropped: 0, runState: {
            running: false, finished: false, stopped: false, exitCode: null,
            rerunAvailable: false, configurations: [], selectedConfigurationId: '', phase: 'IDLE'
          },
          diagnostics: state, documents: [{ uri: 'lesson://fundamentals/s1/main', displayName: 'Main.java' }],
          trace: null, capturing: false, onSelect: () => {}, onNavigateProblem: () => {}
        })));
    });
    const block = document.querySelector<HTMLElement>('.learn-problem-pattern')!;
    const expected: Record<string, string> = {
      'learning-token-keyword': 'rgb(240, 170, 130)',
      'learning-token-string': 'rgb(155, 212, 159)',
      'learning-token-number': 'rgb(98, 211, 219)',
      'learning-token-type': 'rgb(180, 200, 255)',
      'learning-token-comment': 'rgb(143, 154, 167)'
    };
    for (const [className, color] of Object.entries(expected)) {
      const span = block.querySelector<HTMLElement>('.' + className);
      expect(span, 'falta o token ' + className + ' no pattern realçado').not.toBeNull();
      expect(getComputedStyle(span!).color, className + ' deve ter cor própria').toBe(color);
    }
  } finally {
    teardown();
  }
});
