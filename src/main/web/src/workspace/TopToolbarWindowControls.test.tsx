// @vitest-environment jsdom
import { afterEach, expect, test } from 'vitest';
import { act, createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { TopToolbar } from './TopToolbar';
import type { ElectronWindowControls } from '../bridge/EyeCodeBridge';

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
  delete window.electronAPI;
});

function renderTopToolbar(): { container: HTMLElement; maximizeButton: HTMLButtonElement } {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  act(() => root!.render(createElement(TopToolbar, {
    recentProjects: [],
    runState: { running: false, phase: 'IDLE', finished: false, exitCode: null, stopped: false, rerunAvailable: false, selectedConfigurationId: '', configurations: [] },
    runAvailable: false,
    onNewProject: () => {},
    onOpenProject: () => {},
    onNewFile: () => {},
    onOpenRecentProject: () => {},
    onRemoveRecentProject: () => {},
    onWelcome: () => {},
    onRun: () => {},
    onRerun: () => {},
    onStop: () => {},
    onSelectConfiguration: () => {},
    onOpenSearch: () => {},
    onOpenSettings: () => {},
    onWindowAction: () => {},
  })));
  const maximizeButton = container.querySelector<HTMLButtonElement>('[aria-label="Maximizar"], [aria-label="Restaurar"]');
  if (!maximizeButton) throw new Error('Maximize/Restore button not found');
  return { container, maximizeButton };
}

test('shows the maximize icon and label when the window is not maximized', async () => {
  const controls: ElectronWindowControls = {
    minimize: () => {},
    toggleMaximize: () => {},
    close: () => {},
    isMaximized: () => Promise.resolve(false),
    onMaximizeChange: () => () => {},
  };
  window.electronAPI = controls;
  const { maximizeButton } = renderTopToolbar();
  await act(async () => {});
  expect(maximizeButton.getAttribute('aria-label')).toBe('Maximizar');
  expect(maximizeButton.querySelector('img')?.getAttribute('src')).toContain('maximize');
});

test('switches to the restore icon and label when the window reports maximized', async () => {
  let listener: ((maximized: boolean) => void) | null = null;
  const controls: ElectronWindowControls = {
    minimize: () => {},
    toggleMaximize: () => {},
    close: () => {},
    isMaximized: () => Promise.resolve(false),
    onMaximizeChange: (callback) => {
      listener = callback;
      return () => { listener = null; };
    },
  };
  window.electronAPI = controls;
  const { maximizeButton } = renderTopToolbar();
  await act(async () => {});
  expect(maximizeButton.getAttribute('aria-label')).toBe('Maximizar');
  await act(async () => { listener!(true); });
  expect(maximizeButton.getAttribute('aria-label')).toBe('Restaurar');
  expect(maximizeButton.querySelector('img')?.getAttribute('src')).toContain('restore');
});

test('renders without the electron api in web mode', async () => {
  delete window.electronAPI;
  const { maximizeButton } = renderTopToolbar();
  await act(async () => {});
  expect(maximizeButton.getAttribute('aria-label')).toBe('Maximizar');
});
