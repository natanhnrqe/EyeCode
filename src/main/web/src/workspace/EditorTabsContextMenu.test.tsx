// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act } from 'react';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { EditorTabs } from './EditorTabs';
import type { DocumentSnapshot } from '../document/protocol';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

const fileTab: DocumentTab = {
  uri: 'file:///project/src/CnpjValidator.java',
  displayName: 'CnpjValidator.java',
  language: 'java',
  version: 1,
  dirty: false,
  readOnly: false,
  kind: 'file'
};

function renderTabs(props: { onSplitRight?(uri: string): void; onSplitDown?(uri: string): void }): { root: Root; container: HTMLElement } {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  act(() => {
    root.render(createElement(EditorTabs, {
      documents: [fileTab],
      activeUri: fileTab.uri,
      onActivate: () => {},
      onClose: () => {},
      onSplitRight: props.onSplitRight,
      onSplitDown: props.onSplitDown
    }));
  });
  return { root, container };
}

function fire(element: Element | Window, type: string, init: Record<string, unknown> = {}) {
  act(() => {
    const eventClass = type === 'keydown' ? KeyboardEvent : MouseEvent;
    element.dispatchEvent(new eventClass(type, { bubbles: true, cancelable: true, ...init }));
  });
}

const roots: Array<() => void> = [];
afterEach(() => {
  while (roots.length) roots.pop()!();
});

function track(root: Root) {
  roots.push(() => {
    act(() => root.render(createElement('div')));
    root.unmount();
  });
}

test('context menu opens on right click and Split Right invokes the callback', () => {
  const onSplitRight = vi.fn();
  const { root, container } = renderTabs({ onSplitRight });
  track(root);

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'contextmenu', { button: 2, clientX: 40, clientY: 12 });
  const menu = document.querySelector('.editor-context-menu');
  expect(menu).not.toBeNull();

  const splitRight = [...menu!.querySelectorAll('button')].find(button => button.textContent === 'Split Right')!;
  fire(splitRight, 'mousedown', { button: 0, clientX: 50, clientY: 20 });
  fire(splitRight, 'click', { button: 0, clientX: 50, clientY: 20 });

  expect(onSplitRight).toHaveBeenCalledTimes(1);
  expect(onSplitRight).toHaveBeenCalledWith(fileTab.uri);
  expect(document.querySelector('.editor-context-menu')).toBeNull();
});

test('Split Down invokes the callback and closes the menu', () => {
  const onSplitDown = vi.fn();
  const { root, container } = renderTabs({ onSplitDown });
  track(root);

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'contextmenu', { button: 2, clientX: 40, clientY: 12 });
  const splitDown = [...document.querySelectorAll('.editor-context-menu button')].find(button => button.textContent === 'Split Down')!;
  fire(splitDown, 'mousedown', { button: 0 });
  fire(splitDown, 'click', { button: 0 });

  expect(onSplitDown).toHaveBeenCalledWith(fileTab.uri);
  expect(document.querySelector('.editor-context-menu')).toBeNull();
});

test('clicking outside the menu closes it without invoking split actions', () => {
  const onSplitRight = vi.fn();
  const { root, container } = renderTabs({ onSplitRight });
  track(root);

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'contextmenu', { button: 2, clientX: 40, clientY: 12 });
  expect(document.querySelector('.editor-context-menu')).not.toBeNull();

  fire(document.body, 'mousedown', { button: 0 });
  expect(document.querySelector('.editor-context-menu')).toBeNull();
  expect(onSplitRight).not.toHaveBeenCalled();
});

test('menu items stay clickable: mousedown inside the menu does not close it', () => {
  const onSplitRight = vi.fn();
  const { root, container } = renderTabs({ onSplitRight });
  track(root);

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'contextmenu', { button: 2, clientX: 40, clientY: 12 });
  const menu = document.querySelector('.editor-context-menu')!;

  fire(menu, 'mousedown', { button: 0 });
  expect(document.querySelector('.editor-context-menu')).not.toBeNull();
});

test('dragging a tab past the threshold shows the ghost and suppresses the activation click', () => {
  const onActivate = vi.fn();
  const onTabSplitDrop = vi.fn();
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  track(root);
  act(() => {
    root.render(createElement(EditorTabs, {
      documents: [fileTab],
      activeUri: null,
      onActivate,
      onClose: () => {},
      onTabSplitDrop
    }));
  });

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'pointerdown', { button: 0, clientX: 20, clientY: 10 });
  fire(window, 'pointermove', { clientX: 24, clientY: 12 });
  expect(document.querySelector('.editor-tab-drag-ghost')).toBeNull();

  fire(window, 'pointermove', { clientX: 60, clientY: 40 });
  expect(document.querySelector('.editor-tab-drag-ghost')).not.toBeNull();
  expect(document.body.classList.contains('is-editor-tab-dragging')).toBe(true);
  expect(tab.className).toContain('is-drag-source');

  fire(window, 'pointerup', { clientX: 60, clientY: 40 });
  expect(document.querySelector('.editor-tab-drag-ghost')).toBeNull();
  expect(document.body.classList.contains('is-editor-tab-dragging')).toBe(false);

  fire(tab, 'click', { button: 0 });
  expect(onActivate).not.toHaveBeenCalled();
  fire(tab, 'click', { button: 0 });
  expect(onActivate).toHaveBeenCalledWith(fileTab.uri);
  expect(onTabSplitDrop).not.toHaveBeenCalled();
});

test('Escape cancels an in-flight tab drag', () => {
  const onTabSplitDrop = vi.fn();
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  track(root);
  act(() => {
    root.render(createElement(EditorTabs, {
      documents: [fileTab],
      activeUri: null,
      onActivate: () => {},
      onClose: () => {},
      onTabSplitDrop
    }));
  });

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'pointerdown', { button: 0, clientX: 20, clientY: 10 });
  fire(window, 'pointermove', { clientX: 60, clientY: 40 });
  expect(document.querySelector('.editor-tab-drag-ghost')).not.toBeNull();

  fire(window, 'keydown', { key: 'Escape' });
  expect(document.querySelector('.editor-tab-drag-ghost')).toBeNull();
  expect(document.body.classList.contains('is-editor-tab-dragging')).toBe(false);
  expect(onTabSplitDrop).not.toHaveBeenCalled();
});

test('dropping a tab over an editor group resolves the zone and invokes onTabSplitDrop', () => {
  const onTabSplitDrop = vi.fn();
  const container = document.createElement('div');
  document.body.appendChild(container);
  const group = document.createElement('section');
  group.setAttribute('data-editor-group-id', '0');
  group.getBoundingClientRect = () => DOMRect.fromRect({ x: 500, y: 200, width: 400, height: 300 });
  document.body.appendChild(group);
  const root = createRoot(container);
  track(root);
  roots.push(() => group.remove());
  act(() => {
    root.render(createElement(EditorTabs, {
      documents: [fileTab],
      activeUri: null,
      onActivate: () => {},
      onClose: () => {},
      onTabSplitDrop
    }));
  });

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'pointerdown', { button: 0, clientX: 20, clientY: 10 });
  fire(window, 'pointermove', { clientX: 700, clientY: 350 });
  fire(window, 'pointerup', { clientX: 700, clientY: 350 });

  expect(onTabSplitDrop).toHaveBeenCalledTimes(1);
  expect(onTabSplitDrop).toHaveBeenCalledWith(fileTab.uri, { groupId: 0, zone: 'CENTER' });
});

test('dropping near the right edge of a group resolves the RIGHT split zone', () => {
  const onTabSplitDrop = vi.fn();
  const container = document.createElement('div');
  document.body.appendChild(container);
  const group = document.createElement('section');
  group.setAttribute('data-editor-group-id', '0');
  group.getBoundingClientRect = () => DOMRect.fromRect({ x: 500, y: 200, width: 400, height: 300 });
  document.body.appendChild(group);
  const root = createRoot(container);
  track(root);
  roots.push(() => group.remove());
  act(() => {
    root.render(createElement(EditorTabs, {
      documents: [fileTab],
      activeUri: null,
      onActivate: () => {},
      onClose: () => {},
      onTabSplitDrop
    }));
  });

  const tab = container.querySelector('.editor-tab')!;
  fire(tab, 'pointerdown', { button: 0, clientX: 20, clientY: 10 });
  fire(window, 'pointermove', { clientX: 880, clientY: 350 });
  fire(window, 'pointerup', { clientX: 880, clientY: 350 });

  expect(onTabSplitDrop).toHaveBeenCalledWith(fileTab.uri, { groupId: 0, zone: 'RIGHT' });
});
