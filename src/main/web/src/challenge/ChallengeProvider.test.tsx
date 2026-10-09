// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act, createElement } from 'react';
import type React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { bridge } from '../bridge/EyeCodeBridge';
import { ChallengeProvider, useChallenge } from './useChallenge';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

let root: Root | null = null;
let container: HTMLElement | null = null;

vi.mock('../bridge/EyeCodeBridge', () => ({ bridge: { request: vi.fn() } }));

afterEach(() => {
  if (root && container) {
    act(() => root!.unmount());
    container.remove();
  }
  root = null;
  container = null;
  vi.clearAllMocks();
});

function Consumer(): React.ReactElement {
  const challenge = useChallenge();
  return <div>
    <span data-testid="title">{challenge.challenge.title}</span>
    <button type="button" onClick={() => void challenge.submitChallenge()}>Executar</button>
    <output data-testid="tests">{challenge.tests.map(test => `${test.name}:${test.status}`).join(',')}</output>
  </div>;
}

function render(beforeRun?: () => Promise<boolean>): HTMLElement {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  act(() => root!.render(createElement(ChallengeProvider, {
    challengeId: 'ano-bissexto', beforeRun, children: createElement(Consumer)
  })));
  return container;
}

test('selected foundation challenge runs its Maven tests after saving the workspace', async () => {
  const beforeRun = vi.fn(async () => true);
  vi.mocked(bridge.request).mockImplementation(async (channel, operation) => {
    if (operation === 'state') return { exists: true } as never;
    if (channel === 'challenges' && operation === 'run') return {
      message: 'Testes executados.',
      tests: [{ id: 'leap#acceptsDivisibleByFour', name: 'acceptsDivisibleByFour', status: 'success' }]
    } as never;
    throw new Error('unexpected request');
  });
  const view = render(beforeRun);

  expect(view.querySelector('[data-testid="title"]')?.textContent).toBe('Verificador de Ano Bissexto');
  await act(async () => { view.querySelector('button')?.click(); });

  expect(beforeRun).toHaveBeenCalledOnce();
  expect(bridge.request).toHaveBeenCalledWith('challenges', 'run', { id: 'ano-bissexto' }, { timeoutMs: 190000 });
  expect(view.querySelector('[data-testid="tests"]')?.textContent).toContain('acceptsDivisibleByFour:success');
});

test('does not run tests when saving the challenge workspace fails', async () => {
  const beforeRun = vi.fn(async () => false);
  vi.mocked(bridge.request).mockResolvedValue({ exists: true } as never);
  const view = render(beforeRun);

  await act(async () => { view.querySelector('button')?.click(); });

  expect(bridge.request).not.toHaveBeenCalledWith('challenges', 'run', expect.anything(), expect.anything());
  expect(view.querySelector('[data-testid="tests"]')?.textContent).toContain('Falha ao executar os testes:failure');
});
