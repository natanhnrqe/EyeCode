// @vitest-environment jsdom
import { afterEach, expect, test, vi } from 'vitest';
import { act } from 'react';
import { createElement, type ReactElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { dialogFocusables, useModalDialog } from './modalDialog';
import { NewJavaClassDialog } from './NewJavaClassDialog';
import { NewProjectDialog } from './NewProjectDialog';

(globalThis as Record<string, unknown>).IS_REACT_ACT_ENVIRONMENT = true;

const disposers: Array<() => void> = [];
afterEach(() => {
  while (disposers.length) disposers.pop()!();
});

function renderElement(element: ReactElement): { container: HTMLElement; root: Root } {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  disposers.push(() => {
    act(() => root.render(createElement('div')));
    root.unmount();
    container.remove();
  });
  act(() => root.render(element));
  return { container, root };
}

function fireKey(key: string, options: { shiftKey?: boolean } = {}): KeyboardEvent {
  const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...options });
  act(() => {
    window.dispatchEvent(event);
  });
  return event;
}

function Probe({ escapeEnabled, onClose }: { escapeEnabled: boolean; onClose(): void }): ReactElement {
  const dialogRef = useModalDialog<HTMLFormElement>({ escapeEnabled, onClose });
  return createElement('form', { ref: dialogRef },
    createElement('input', { type: 'text' }),
    createElement('button', { type: 'button' }, 'Meio'),
    createElement('button', { type: 'button', disabled: true }, 'Desativado'),
    createElement('button', { type: 'submit' }, 'Enviar'));
}

test('dialogFocusables follows DOM order and skips disabled controls', () => {
  const { container } = renderElement(createElement(Probe, { escapeEnabled: true, onClose: () => {} }));
  const focusables = dialogFocusables(container.querySelector('form')!);
  expect(focusables.map(element => element.tagName)).toEqual(['INPUT', 'BUTTON', 'BUTTON']);
  expect(focusables.every(element => element.textContent !== 'Desativado')).toBe(true);
});

test('Tab wraps from the last control to the first and Shift+Tab from the first to the last', () => {
  const { container } = renderElement(createElement(Probe, { escapeEnabled: true, onClose: () => {} }));
  const [first, , last] = dialogFocusables(container.querySelector('form')!);

  last.focus();
  const wrapForward = fireKey('Tab');
  expect(wrapForward.defaultPrevented).toBe(true);
  expect(document.activeElement).toBe(first);

  first.focus();
  const wrapBackward = fireKey('Tab', { shiftKey: true });
  expect(wrapBackward.defaultPrevented).toBe(true);
  expect(document.activeElement).toBe(last);
});

test('Tab pulls focus back into the dialog when it escaped to the page', () => {
  const { container } = renderElement(createElement(Probe, { escapeEnabled: true, onClose: () => {} }));
  const [first, , last] = dialogFocusables(container.querySelector('form')!);

  expect(document.activeElement).toBe(document.body);
  fireKey('Tab');
  expect(document.activeElement).toBe(first);

  fireKey('Tab', { shiftKey: true });
  expect(document.activeElement).toBe(last);
});

test('Tab between inner controls is left to the browser', () => {
  const { container } = renderElement(createElement(Probe, { escapeEnabled: true, onClose: () => {} }));
  const [, middle] = dialogFocusables(container.querySelector('form')!);
  middle.focus();

  const event = fireKey('Tab');
  expect(event.defaultPrevented).toBe(false);
  expect(document.activeElement).toBe(middle);
});

test('Escape closes the dialog while it allows closing', () => {
  const onClose = vi.fn();
  renderElement(createElement(Probe, { escapeEnabled: true, onClose }));
  fireKey('Escape');
  expect(onClose).toHaveBeenCalledTimes(1);
});

test('Escape is ignored while the dialog disallows closing', () => {
  const onClose = vi.fn();
  renderElement(createElement(Probe, { escapeEnabled: false, onClose }));
  fireKey('Escape');
  expect(onClose).not.toHaveBeenCalled();
});

test('focus returns to the invoker when the dialog unmounts', () => {
  const invoker = document.createElement('button');
  document.body.appendChild(invoker);
  invoker.focus();
  const { root } = renderElement(createElement(Probe, { escapeEnabled: true, onClose: () => {} }));
  const dialogInput = document.querySelector<HTMLInputElement>('form input')!;
  dialogInput.focus();
  expect(document.activeElement).toBe(dialogInput);

  act(() => root.render(createElement('div')));
  expect(document.activeElement).toBe(invoker);
  invoker.remove();
});

test('the new project dialog exposes dialog semantics and traps Tab across its fields', () => {
  const onCancel = vi.fn();
  const { container } = renderElement(createElement(NewProjectDialog, {
    onCancel,
    onBrowse: () => Promise.resolve(undefined),
    onCreate: () => Promise.resolve()
  }));
  const form = container.querySelector('form')!;
  expect(form.getAttribute('role')).toBe('dialog');
  expect(form.getAttribute('aria-modal')).toBe('true');
  const labelledBy = form.getAttribute('aria-labelledby')!;
  expect(container.querySelector(`#${labelledBy}`)!.textContent).toBe('Criar novo projeto');

  const focusables = dialogFocusables(form);
  expect(focusables).toHaveLength(6);
  focusables[focusables.length - 1].focus();
  fireKey('Tab');
  expect(document.activeElement).toBe(focusables[0]);

  fireKey('Escape');
  expect(onCancel).toHaveBeenCalledTimes(1);
});

test('the new java class dialog exposes dialog semantics, keeps autoFocus, and closes on Escape', () => {
  const onCancel = vi.fn();
  const { container } = renderElement(createElement(NewJavaClassDialog, {
    onCancel,
    onCreate: () => Promise.resolve()
  }));
  const form = container.querySelector('form')!;
  expect(form.getAttribute('role')).toBe('dialog');
  expect(form.getAttribute('aria-modal')).toBe('true');
  const labelledBy = form.getAttribute('aria-labelledby')!;
  expect(container.querySelector(`#${labelledBy}`)!.textContent).toBe('Nova Classe Java');
  expect(document.activeElement).toBe(container.querySelector('form input'));

  fireKey('Escape');
  expect(onCancel).toHaveBeenCalledTimes(1);
});
