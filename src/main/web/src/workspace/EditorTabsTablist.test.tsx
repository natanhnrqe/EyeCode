// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act, createElement, useState } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { EditorTabs, EDITOR_TABS_LIST_ID, EDITOR_TABS_PANEL_ID } from './EditorTabs';
import { tabIdFor } from './ariaTabs';
import type { DocumentSnapshot } from '../document/protocol';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

function tab(uri: string, name: string): DocumentTab {
  return { uri, displayName: name, language: 'java', version: 1, dirty: false, readOnly: false, kind: 'file' };
}

const tabsFixture: DocumentTab[] = [
  tab('file:///project/src/Main.java', 'Main.java'),
  tab('file:///project/src/Util.java', 'Util.java'),
  tab('file:///project/src/App.java', 'App.java')
];

const roots: Array<() => void> = [];
afterEach(() => {
  while (roots.length) roots.pop()!();
});

type HarnessProps = {
  initialActive: string | null;
  onActivate(uri: string): void;
  onClose(uri: string): void;
  onSplitRight?(uri: string): void;
};

function Harness({ initialActive, onActivate, onClose, onSplitRight }: HarnessProps): ReturnType<typeof createElement> {
  const [activeUri, setActiveUri] = useState<string | null>(initialActive);
  return createElement('div', null,
    createElement(EditorTabs, {
      documents: tabsFixture,
      activeUri,
      onActivate: (uri: string) => { onActivate(uri); setActiveUri(uri); },
      onClose,
      onSplitRight
    }),
    createElement('div', { id: EDITOR_TABS_PANEL_ID, role: 'tabpanel' }));
}

function renderHarness(props: Partial<HarnessProps> = {}): HTMLElement {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root: Root = createRoot(container);
  roots.push(() => {
    act(() => root.render(createElement('div')));
    root.unmount();
    container.remove();
  });
  act(() => {
    root.render(createElement(Harness, {
      initialActive: props.initialActive === undefined ? tabsFixture[0].uri : props.initialActive,
      onActivate: props.onActivate ?? (() => {}),
      onClose: props.onClose ?? (() => {}),
      onSplitRight: props.onSplitRight
    }));
  });
  return container;
}

function fireKey(element: Element, key: string): KeyboardEvent {
  const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true });
  act(() => { element.dispatchEvent(event); });
  return event;
}

function fireClick(element: Element): void {
  act(() => { element.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, button: 0 })); });
}

function tabElements(container: HTMLElement): Array<HTMLElement> {
  return [...container.querySelectorAll<HTMLElement>('[role="tab"]')];
}

test('editor tabs form a roving tablist wired to the editor region', () => {
  const container = renderHarness();

  const list = container.querySelector('.editor-tabs')!;
  expect(list.getAttribute('role')).toBe('tablist');
  expect(list.getAttribute('aria-label')).toBe('Documentos abertos');

  const tabs = tabElements(container);
  expect(tabs.map(item => item.tabIndex)).toEqual([0, -1, -1]);
  expect(tabs.map(item => item.getAttribute('aria-selected'))).toEqual(['true', 'false', 'false']);
  expect(tabs[0].id).toBe(tabIdFor(EDITOR_TABS_LIST_ID, tabsFixture[0].uri));

  const panel = document.getElementById(EDITOR_TABS_PANEL_ID)!;
  expect(panel).not.toBeNull();
  expect(panel.getAttribute('role')).toBe('tabpanel');
  for (const item of tabs) expect(item.getAttribute('aria-controls')).toBe(EDITOR_TABS_PANEL_ID);
});

test('ArrowRight moves focus to the next tab and activates it', () => {
  const onActivate = vi.fn();
  const container = renderHarness({ onActivate });
  const tabs = tabElements(container);

  act(() => { tabs[0].focus(); });
  fireKey(tabs[0], 'ArrowRight');

  expect(onActivate).toHaveBeenCalledWith(tabsFixture[1].uri);
  expect(document.activeElement).toBe(tabs[1]);
  expect(tabs[1].getAttribute('aria-selected')).toBe('true');
  expect(tabs.map(item => item.tabIndex)).toEqual([-1, 0, -1]);
});

test('Home, End and ArrowLeft roam the strip with wrap-around', () => {
  const onActivate = vi.fn();
  const container = renderHarness({ onActivate });
  const tabs = tabElements(container);

  act(() => { tabs[0].focus(); });
  fireKey(tabs[0], 'End');
  expect(document.activeElement).toBe(tabs[2]);
  expect(onActivate).toHaveBeenCalledWith(tabsFixture[2].uri);

  fireKey(tabs[2], 'ArrowLeft');
  expect(document.activeElement).toBe(tabs[1]);
  expect(onActivate).toHaveBeenLastCalledWith(tabsFixture[1].uri);

  fireKey(tabs[1], 'Home');
  expect(document.activeElement).toBe(tabs[0]);
  expect(onActivate).toHaveBeenLastCalledWith(tabsFixture[0].uri);

  fireKey(tabs[0], 'ArrowLeft');
  expect(document.activeElement).toBe(tabs[2]);
  expect(onActivate).toHaveBeenLastCalledWith(tabsFixture[2].uri);
});

test('Enter and Space activate the focused tab and suppress the native default', () => {
  const onActivate = vi.fn();
  const container = renderHarness({ onActivate });
  const tabs = tabElements(container);

  const enter = fireKey(tabs[1], 'Enter');
  expect(onActivate).toHaveBeenCalledWith(tabsFixture[1].uri);
  expect(enter.defaultPrevented).toBe(true);

  const space = fireKey(tabs[1], ' ');
  expect(onActivate).toHaveBeenCalledTimes(2);
  expect(space.defaultPrevented).toBe(true);
});

test('the close control is a real button outside the tab order and closes without activating', () => {
  const onActivate = vi.fn();
  const onClose = vi.fn();
  const container = renderHarness({ onActivate, onClose });
  const tabs = tabElements(container);

  const close = tabs[0].querySelector<HTMLButtonElement>('.editor-tab-close')!;
  expect(close).not.toBeNull();
  expect(close.tagName).toBe('BUTTON');
  expect(close.tabIndex).toBe(-1);
  expect(close.getAttribute('aria-label')).toBe(`Fechar ${tabsFixture[0].displayName}`);

  fireClick(close);
  expect(onClose).toHaveBeenCalledWith(tabsFixture[0].uri);
  expect(onActivate).not.toHaveBeenCalled();
});

test('arrow keys do not roam the tablist from inside the context menu', () => {
  const onActivate = vi.fn();
  const container = renderHarness({ onActivate, onSplitRight: () => {} });
  const tabs = tabElements(container);

  fireKey(tabs[0], 'ContextMenu');
  const menu = document.querySelector('.editor-context-menu')!;
  expect(menu).not.toBeNull();
  const item = menu.querySelector<HTMLButtonElement>('button')!;
  act(() => { item.focus(); });

  fireKey(item, 'ArrowRight');
  expect(onActivate).not.toHaveBeenCalled();
});

test('the strip keeps a single tab stop when no document is active', () => {
  const container = renderHarness({ initialActive: null });
  const tabs = tabElements(container);

  expect(tabs.map(item => item.tabIndex)).toEqual([0, -1, -1]);
  expect(tabs.every(item => item.getAttribute('aria-selected') === 'false')).toBe(true);
});
