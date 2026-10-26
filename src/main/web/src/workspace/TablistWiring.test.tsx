// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act, createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { BottomPanel } from './BottomPanel';
import { DockPane } from './DockPane';
import { LearnContextPanel } from '../lessons/LearnContextPanel';
import { LearningPanel } from '../lessons/LearningPanel';
import { ChallengeRightPanel } from '../challenge/ChallengeRightPanel';
import { ChallengeProvider } from '../challenge/useChallenge';
import type { LessonSession } from '../lessons/protocol';
import type { DiagnosticsViewState } from '../diagnostics/protocol';
import type { RunState, TerminalState } from './protocol';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

vi.mock('../bridge/EyeCodeBridge', () => ({ bridge: { request: vi.fn(async () => ({ exists: true })) } }));

const roots: Array<() => void> = [];
afterEach(() => {
  while (roots.length) roots.pop()!();
  vi.clearAllMocks();
});

function render(element: ReturnType<typeof createElement>): HTMLElement {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root: Root = createRoot(container);
  roots.push(() => {
    act(() => root.render(createElement('div')));
    root.unmount();
    container.remove();
  });
  act(() => root.render(element));
  return container;
}

function fire(element: Element, key: string) {
  act(() => {
    element.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
  });
}

const runState: RunState = { running: false, phase: 'IDLE', finished: false, exitCode: null, stopped: false, rerunAvailable: false, configurations: [], selectedConfigurationId: '' };
const terminalState: TerminalState = { requested: false, running: false, workingDirectory: '' };

function assertTablistSemantics(list: Element, expectedPanelId: string) {
  expect(list.getAttribute('role')).toBe('tablist');
  const tabs = [...list.querySelectorAll<HTMLElement>('[role="tab"]')];
  expect(tabs.length).toBeGreaterThan(1);
  const selected = tabs.filter(tab => tab.getAttribute('aria-selected') === 'true');
  expect(selected.length).toBe(1);
  for (const tab of tabs) {
    expect(tab.getAttribute('aria-selected')).toBe(tab === selected[0] ? 'true' : 'false');
    expect(tab.tabIndex).toBe(tab === selected[0] ? 0 : -1);
    const controls = tab.getAttribute('aria-controls')!;
    expect(controls).toBe(expectedPanelId);
    expect(document.getElementById(controls)).not.toBeNull();
  }
  const panel = document.getElementById(expectedPanelId)!;
  expect(panel.getAttribute('role')).toBe('tabpanel');
  expect(panel.getAttribute('aria-labelledby')).toBe(selected[0].id);
}

test('BottomPanel tabs behave as a tablist wired to the shared panel with arrow-key navigation', () => {
  const onSelect = vi.fn();
  const view = render(createElement(BottomPanel, {
    active: 'problems', output: [], outputDropped: 0, runState, terminalState,
    diagnostics: null satisfies DiagnosticsViewState | null, documents: [],
    onSelect, onNavigateProblem: () => {}, onNewTerminal: () => {}, onRestartTerminal: () => {},
    onStopTerminal: () => {}, onClearRunOutput: () => {}
  }));

  const list = view.querySelector('[role="tablist"]')!;
  assertTablistSemantics(list, 'bottom-tools-panel');

  const tabs = [...list.querySelectorAll<HTMLElement>('[role="tab"]')];
  expect(tabs.map(tab => tab.textContent)).toEqual(['Problemas', 'Run', 'Terminal', 'Git']);
  tabs[0].focus();
  fire(tabs[0], 'ArrowRight');
  expect(onSelect).toHaveBeenCalledWith('run');
  expect(document.activeElement).toBe(tabs[1]);
});

test('LearnContextPanel tabs behave as a tablist wired to the shared panel', () => {
  const onSelect = vi.fn();
  const view = render(createElement(LearnContextPanel, {
    active: 'problems', output: [], outputDropped: 0, runState,
    diagnostics: null, documents: [], trace: null, capturing: false,
    onSelect, onNavigateProblem: () => {}
  }));

  const list = view.querySelector('[role="tablist"]')!;
  assertTablistSemantics(list, 'learn-context-panel');

  const tabs = [...list.querySelectorAll<HTMLElement>('[role="tab"]')];
  expect(tabs.map(tab => tab.textContent)).toEqual(['Problemas', 'Saída', 'Entender', 'Variáveis', 'Visualização']);
  tabs[0].focus();
  fire(tabs[0], 'End');
  expect(onSelect).toHaveBeenCalledWith('visualization');
  expect(document.activeElement).toBe(tabs[4]);
});

test('LearningPanel tabs wire each tab to its own panel and keep inactive panels hidden', () => {
  const view = render(createElement(LearningPanel, {
    session: sessionFixture(), verification: null, verifying: false,
    onVerify: () => {}, onPrevious: () => {}, onNext: () => {}, onExit: () => {},
    hasDiagnostics: false, onShowProblems: () => {}
  }));

  const list = view.querySelector('[role="tablist"]')!;
  expect(list.getAttribute('aria-label')).toBe('Recursos da aula');
  const tabs = [...list.querySelectorAll<HTMLElement>('[role="tab"]')];
  expect(tabs.map(tab => tab.textContent)).toEqual(['Exercício', 'Conceitos', 'Dúvidas', 'Exemplos']);

  const active = tabs.find(tab => tab.getAttribute('aria-selected') === 'true')!;
  expect(active.textContent).toBe('Exercício');
  const activePanel = document.getElementById(active.getAttribute('aria-controls')!)!;
  expect(activePanel.getAttribute('role')).toBe('tabpanel');
  expect(activePanel.getAttribute('aria-labelledby')).toBe(active.id);
  expect(activePanel.hasAttribute('hidden')).toBe(false);
  for (const tab of tabs) {
    expect(tab.getAttribute('aria-controls')).toBe(`learning-panel-panel-${tab.textContent === 'Exercício' ? 'exercise' : tab.textContent === 'Conceitos' ? 'concepts' : tab.textContent === 'Dúvidas' ? 'faq' : 'examples'}`);
    expect(tab.getAttribute('id')).toBe(`learning-panel-tab-${tab.textContent === 'Exercício' ? 'exercise' : tab.textContent === 'Conceitos' ? 'concepts' : tab.textContent === 'Dúvidas' ? 'faq' : 'examples'}`);
  }

  tabs[0].focus();
  fire(tabs[0], 'ArrowRight');
  expect(tabs[1].getAttribute('aria-selected')).toBe('true');
  expect(document.activeElement).toBe(tabs[1]);
  expect(activePanel.hasAttribute('hidden')).toBe(true);
  const conceptsPanel = document.getElementById(tabs[1].getAttribute('aria-controls')!)!;
  expect(conceptsPanel.getAttribute('role')).toBe('tabpanel');
  expect(conceptsPanel.getAttribute('aria-labelledby')).toBe(tabs[1].id);
  expect(conceptsPanel.hasAttribute('hidden')).toBe(false);
});

test('ChallengeRightPanel tabs behave as a tablist with per-tab panels over the hidden attribute', () => {
  const view = render(createElement(ChallengeProvider, {
    challengeId: 'ano-bissexto',
    children: createElement(ChallengeRightPanel, {
      onExit: () => {}, onCollapse: () => {}, onOpenDocumentation: () => {}
    })
  }));

  const list = view.querySelector('.challenge-tabs')!;
  expect(list.getAttribute('role')).toBe('tablist');
  expect(list.getAttribute('aria-label')).toBe('Seções do desafio');
  const tabs = [...list.querySelectorAll<HTMLElement>('[role="tab"]')];
  expect(tabs.map(tab => tab.textContent)).toEqual(['Enunciado', 'Testes']);

  const statementTab = tabs[0];
  const testsTab = tabs[1];
  const statementPanel = document.getElementById(statementTab.getAttribute('aria-controls')!)!;
  const testsPanel = document.getElementById(testsTab.getAttribute('aria-controls')!)!;
  expect(statementPanel.className).toBe('challenge-statement');
  expect(testsPanel.className).toBe('challenge-tests');
  expect(statementPanel.hasAttribute('hidden')).toBe(false);
  expect(testsPanel.hasAttribute('hidden')).toBe(true);
  expect(statementTab.tabIndex).toBe(0);
  expect(testsTab.tabIndex).toBe(-1);

  statementTab.focus();
  fire(statementTab, 'ArrowRight');
  expect(testsTab.getAttribute('aria-selected')).toBe('true');
  expect(document.activeElement).toBe(testsTab);
  expect(statementPanel.hasAttribute('hidden')).toBe(true);
  expect(testsPanel.hasAttribute('hidden')).toBe(false);
});

test('DockPane applies optional tabpanel semantics to its body', () => {
  const view = render(createElement(DockPane, {
    paneId: 'bottom', bodyRole: 'tabpanel', bodyId: 'probe-body', bodyLabelledBy: 'probe-tab', children: null
  }));
  const body = view.querySelector('.dock-pane-body')!;
  expect(body.getAttribute('role')).toBe('tabpanel');
  expect(body.id).toBe('probe-body');
  expect(body.getAttribute('aria-labelledby')).toBe('probe-tab');
});

function sessionFixture(): LessonSession {
  return {
    sessionId: 'session-1', lessonId: 'lesson-1', kind: 'PRACTICE', currentStep: 0, currentPresentation: 0,
    presentationId: 'presentation-1', navigationDirection: 'NONE', totalSteps: 1, state: 'ACTIVE',
    phase: 'PRESENTATION', practiceCompleted: false, title: 'Aula de teste', message: '', contentBlocks: [],
    canPrevious: false, canNext: true, commands: []
  };
}
