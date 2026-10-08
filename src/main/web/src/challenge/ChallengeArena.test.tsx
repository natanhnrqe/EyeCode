// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act } from 'react';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

type WorkspaceProps = { challenge?: { id: string; path: string; onExit(): void; onLeave(): void } | null; onOpenChallenges?: () => void };

const harness = vi.hoisted(() => ({
  workspaceProps: [] as WorkspaceProps[],
  pendingRequests: [] as Array<{ args: unknown[]; resolve: (value: unknown) => void; reject: (reason: unknown) => void }>
}));

vi.mock('../bridge/EyeCodeBridge', () => ({
  bridge: {
    request: (...args: unknown[]) => new Promise((resolve, reject) => {
      harness.pendingRequests.push({ args, resolve, reject });
    })
  }
}));

vi.mock('../workspace/Workspace', () => ({
  Workspace: (props: WorkspaceProps) => {
    harness.workspaceProps.push(props);
    return null;
  }
}));

import { ChallengeArena } from './ChallengeArena';

let root: Root | null = null;
let container: HTMLElement | null = null;

afterEach(() => {
  if (root && container) {
    act(() => root!.unmount());
    container.remove();
  }
  root = null;
  container = null;
  harness.workspaceProps.length = 0;
  harness.pendingRequests.length = 0;
  vi.clearAllMocks();
});

function renderArena(onBack: () => void): void {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  act(() => {
    root!.render(createElement(ChallengeArena, { challengeId: 'cnpj-validator', onBack }));
  });
}

test('gate is shown while the challenge environment is being ensured', () => {
  renderArena(() => {});
  expect(container?.querySelector('.challenge-arena-gate')).not.toBeNull();
  expect(container?.querySelector('.challenge-arena-gate')?.textContent).toContain('Preparando o ambiente do desafio');
  expect(harness.workspaceProps).toHaveLength(0);
  expect(harness.pendingRequests).toHaveLength(1);
  expect(harness.pendingRequests[0].args.slice(0, 3)).toEqual(['challenges', 'ensure', { id: 'cnpj-validator' }]);
});

test('workspace receives the challenge context once the environment is ready', async () => {
  const onBack = vi.fn();
  renderArena(onBack);
  await act(async () => {
    harness.pendingRequests[0].resolve({ path: 'C:\\Users\\dev\\.eyecode\\challenges\\cnpj-validator', fresh: false });
  });
  expect(harness.workspaceProps).toHaveLength(1);
  const props = harness.workspaceProps[0];
  expect(props.challenge?.id).toBe('cnpj-validator');
  expect(props.challenge?.path).toBe('C:\\Users\\dev\\.eyecode\\challenges\\cnpj-validator');
  props.challenge?.onExit();
  expect(onBack).toHaveBeenCalledTimes(1);
  expect(props.onOpenChallenges).toBe(onBack);
});

test('onLeave keeps the workspace mounted with a null challenge context', async () => {
  renderArena(() => {});
  await act(async () => {
    harness.pendingRequests[0].resolve({ path: 'C:\\Users\\dev\\.eyecode\\challenges\\cnpj-validator', fresh: false });
  });
  await act(async () => {
    harness.workspaceProps[harness.workspaceProps.length - 1].challenge?.onLeave();
  });
  expect(container?.querySelector('.challenge-arena-gate')).toBeNull();
  expect(harness.workspaceProps).toHaveLength(2);
  expect(harness.workspaceProps[1].challenge).toBeNull();
  expect(harness.workspaceProps[1].onOpenChallenges).toBeDefined();
});

test('failed ensure shows the error gate instead of the workspace', async () => {
  renderArena(() => {});
  await act(async () => {
    harness.pendingRequests[0].reject(new Error('sem disco'));
  });
  expect(container?.querySelector('.challenge-arena-gate')?.textContent).toContain('sem disco');
  expect(harness.workspaceProps).toHaveLength(0);
});
