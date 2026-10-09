// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act, createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { ChallengeLobby } from './ChallengeLobby';
import { CHALLENGE_TRACKS } from './catalog';

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

function render(onBack = vi.fn(), onOpenChallenge = vi.fn()): HTMLElement {
  container = document.createElement('div');
  document.body.appendChild(container);
  root = createRoot(container);
  act(() => root!.render(createElement(ChallengeLobby, { onBack, onOpenChallenge })));
  return container;
}

test('shows a matching icon for each learning track', () => {
  const view = render();
  const icons = view.querySelectorAll('.challenge-track-icon img');
  expect(icons).toHaveLength(CHALLENGE_TRACKS.length);
  CHALLENGE_TRACKS.forEach((track, index) => {
    expect(icons[index].getAttribute('src')).toBe(`icons/${track.icon}.svg`);
  });
});

test('keeps the back action and opens the selected challenge', () => {
  const onBack = vi.fn();
  const onOpenChallenge = vi.fn();
  const view = render(onBack, onOpenChallenge);
  const back = view.querySelector<HTMLButtonElement>('.challenge-lobby-back');
  const challenge = view.querySelector<HTMLButtonElement>('.challenge-card');
  act(() => back?.click());
  act(() => challenge?.click());
  expect(onBack).toHaveBeenCalledOnce();
  expect(onOpenChallenge).toHaveBeenCalledOnce();
});
