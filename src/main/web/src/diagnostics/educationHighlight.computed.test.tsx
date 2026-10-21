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
  sourceExcerpt: {
    lines: [{ lineNumber: 2, text: 'public static void main(String[] args) {' },
      { lineNumber: 3, text: '    int idade = 15; String nome = "Ana";' }],
    pointerLine: 3, pointerColumn: 19
  },
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

test('os três blocos do Entender compartilham fonte JetBrains Mono e cor base idêntica', () => {
  setup();
  try {
    act(() => {
      root!.render(createElement('div', null,
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
    // abre o bloco correctedPattern (é condicional)
    const exampleToggle = [...document.querySelectorAll<HTMLButtonElement>('button')]
      .find(button => button.textContent === 'Ver exemplo parecido');
    act(() => { exampleToggle!.click(); });

    const sourceBlock = document.querySelector<HTMLElement>('.learn-problem-source')!;
    const patternBlock = document.querySelector<HTMLElement>('.learn-problem-pattern')!;
    const exampleBlock = document.querySelector<HTMLElement>('.learn-problem-example')!;
    const understandBlock = document.querySelector<HTMLElement>('.learn-diagnostic-understanding pre')!;

    for (const block of [sourceBlock, patternBlock, exampleBlock, understandBlock]) {
      expect(block, 'todos os blocos devem existir').toBeTruthy();
      const fontFamily = getComputedStyle(block).fontFamily;
      expect(fontFamily.toLowerCase(), 'todo bloco de código usa JetBrains Mono com fallbacks')
        .toContain('jetbrains mono');
      expect(fontFamily.toLowerCase()).toContain('monospace');
    }

    // a cor base (texto sem token) deve ser idêntica nos blocos de código —
    // a regra legada color:#b6d7ff fazia o pattern parecer outro componente
    const baseColor = getComputedStyle(patternBlock).color;
    expect(getComputedStyle(exampleBlock).color, 'correctedPattern mesma cor base').toBe(baseColor);
    expect(getComputedStyle(understandBlock).color, 'Entender mesma cor base').toBe(baseColor);

    // SourceExcerpt: agora com tokens realçados (a causa do bloco "diferente")
    const sourceKeyword = sourceBlock.querySelector<HTMLElement>('.learning-token-keyword');
    expect(sourceKeyword, 'SourceExcerpt deve conter spans de token realçados').not.toBeNull();
    expect(getComputedStyle(sourceKeyword!).color,
        'SourceExcerpt keyword mesma cor do bloco funcional').toBe('rgb(240, 170, 130)');
    const sourceString = sourceBlock.querySelector<HTMLElement>('.learning-token-string');
    expect(sourceString, 'SourceExcerpt deve realçar strings').not.toBeNull();
    expect(getComputedStyle(sourceString!).color).toBe('rgb(155, 212, 159)');

    // Entender: os cinco tipos de token com as cores do tema
    const expectedTokens: Record<string, string> = {
      'learning-token-keyword': 'rgb(240, 170, 130)',
      'learning-token-string': 'rgb(155, 212, 159)',
      'learning-token-number': 'rgb(98, 211, 219)',
      'learning-token-type': 'rgb(180, 200, 255)',
      'learning-token-comment': 'rgb(143, 154, 167)'
    };
    for (const [className, color] of Object.entries(expectedTokens)) {
      const span = understandBlock.querySelector<HTMLElement>('code .' + className);
      expect(span, 'Entender deve conter ' + className).not.toBeNull();
      expect(getComputedStyle(span!).color).toBe(color);
    }
    // paridade com o bloco funcional de referência
    const keywordInUnderstand = getComputedStyle(
      understandBlock.querySelector<HTMLElement>('.learning-token-keyword')!).color;
    expect(keywordInUnderstand).toBe('rgb(240, 170, 130)');
  } finally {
    teardown();
  }
});
