// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act, useState } from 'react';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { nextTabIndex, panelIdFor, tabIdFor, useAriaTabs } from './ariaTabs';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

const roots: Array<() => void> = [];
afterEach(() => {
  while (roots.length) roots.pop()!();
});

function track(root: Root, container: HTMLElement) {
  roots.push(() => {
    act(() => root.render(createElement('div')));
    root.unmount();
    container.remove();
  });
}

function fire(element: Element, key: string) {
  act(() => {
    element.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
  });
}

type ProbeTab = 'alpha' | 'beta' | 'gamma';

function Probe({ sharedPanelId }: { sharedPanelId?: string }): ReturnType<typeof createElement> {
  const [active, setActive] = useState<ProbeTab>('alpha');
  const tabs = useAriaTabs<ProbeTab>({
    listId: 'probe',
    label: 'Sondas',
    tabIds: ['alpha', 'beta', 'gamma'],
    activeId: active,
    onSelect: setActive,
    sharedPanelId,
  });
  return createElement('div', null,
    createElement('div', { ...tabs.tabListProps, 'data-testid': 'list' },
      createElement('button', { type: 'button', ...tabs.tabProps('alpha'), onClick: () => setActive('alpha') }, 'Alpha'),
      createElement('button', { type: 'button', ...tabs.tabProps('beta'), onClick: () => setActive('beta') }, 'Beta'),
      createElement('button', { type: 'button', ...tabs.tabProps('gamma'), onClick: () => setActive('gamma') }, 'Gamma')),
    sharedPanelId
      ? createElement('div', { role: 'tabpanel', id: sharedPanelId })
      : createElement('div', { role: 'tabpanel', id: panelIdFor('probe', active) }));
}

function renderProbe(sharedPanelId?: string) {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  track(root, container);
  act(() => root.render(createElement(Probe, { sharedPanelId })));
  return { container, root };
}

test('nextTabIndex moves and wraps through the tab order', () => {
  expect(nextTabIndex('ArrowRight', 0, 3)).toBe(1);
  expect(nextTabIndex('ArrowRight', 2, 3)).toBe(0);
  expect(nextTabIndex('ArrowLeft', 0, 3)).toBe(2);
  expect(nextTabIndex('ArrowLeft', 1, 3)).toBe(0);
  expect(nextTabIndex('Home', 2, 3)).toBe(0);
  expect(nextTabIndex('End', 0, 3)).toBe(2);
  expect(nextTabIndex('Enter', 1, 3)).toBeNull();
  expect(nextTabIndex('ArrowRight', 0, 0)).toBeNull();
});

test('id helpers build stable tab and panel identities', () => {
  expect(tabIdFor('probe', 'alpha')).toBe('probe-tab-alpha');
  expect(panelIdFor('probe', 'alpha')).toBe('probe-panel-alpha');
});

test('arrow keys move focus and select the target tab with roving tabindex', () => {
  const { container } = renderProbe();
  const list = container.querySelector('[data-testid="list"]')!;
  const tabs = [...list.querySelectorAll('button')];

  expect(tabs.map(tab => tab.tabIndex)).toEqual([0, -1, -1]);
  tabs[0].focus();
  expect(document.activeElement).toBe(tabs[0]);

  fire(tabs[0], 'ArrowRight');
  expect(document.activeElement).toBe(tabs[1]);
  expect(tabs[1].getAttribute('aria-selected')).toBe('true');
  expect(tabs[0].getAttribute('aria-selected')).toBe('false');
  expect(tabs.map(tab => tab.tabIndex)).toEqual([-1, 0, -1]);

  fire(tabs[1], 'ArrowRight');
  fire(tabs[2], 'ArrowRight');
  expect(document.activeElement).toBe(tabs[0]);
  expect(tabs[0].getAttribute('aria-selected')).toBe('true');

  fire(tabs[0], 'ArrowLeft');
  expect(document.activeElement).toBe(tabs[2]);
  expect(tabs[2].getAttribute('aria-selected')).toBe('true');
});

test('Home and End jump to the first and last tab', () => {
  const { container } = renderProbe();
  const list = container.querySelector('[data-testid="list"]')!;
  const tabs = [...list.querySelectorAll('button')];

  tabs[1].focus();
  fire(tabs[1], 'Home');
  expect(document.activeElement).toBe(tabs[0]);
  expect(tabs[0].getAttribute('aria-selected')).toBe('true');

  fire(tabs[0], 'End');
  expect(document.activeElement).toBe(tabs[2]);
  expect(tabs[2].getAttribute('aria-selected')).toBe('true');
});

test('unhandled keys leave focus and selection untouched', () => {
  const { container } = renderProbe();
  const list = container.querySelector('[data-testid="list"]')!;
  const tabs = [...list.querySelectorAll('button')];
  tabs[0].focus();

  fire(tabs[0], 'Enter');
  expect(document.activeElement).toBe(tabs[0]);
  expect(tabs[0].getAttribute('aria-selected')).toBe('true');
  expect(tabs[1].getAttribute('aria-selected')).toBe('false');
});

test('tabs wire aria-controls to per-tab panels by default and to a shared panel when provided', () => {
  const { container } = renderProbe();
  const list = container.querySelector('[data-testid="list"]')!;
  const tabs = [...list.querySelectorAll('button')];
  expect(tabs[0].getAttribute('aria-controls')).toBe('probe-panel-alpha');
  expect(tabs[2].getAttribute('aria-controls')).toBe('probe-panel-gamma');

  const shared = renderProbe('probe-shared-panel');
  const sharedTabs = [...shared.container.querySelectorAll('[data-testid="list"] button')];
  expect(sharedTabs.every(tab => tab.getAttribute('aria-controls') === 'probe-shared-panel')).toBe(true);
  expect(shared.container.querySelector('#probe-shared-panel')).not.toBeNull();
});

test('the tablist exposes its role and label', () => {
  const { container } = renderProbe();
  const list = container.querySelector('[data-testid="list"]')!;
  expect(list.getAttribute('role')).toBe('tablist');
  expect(list.getAttribute('aria-label')).toBe('Sondas');
});
