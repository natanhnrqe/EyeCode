import { useEffect, useRef, type RefObject } from 'react';

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export function dialogFocusables(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
}

export function trapDialogTab(container: HTMLElement, event: KeyboardEvent): void {
  const focusables = dialogFocusables(container);
  if (focusables.length === 0) return;
  const active =
    document.activeElement instanceof HTMLElement && container.contains(document.activeElement)
      ? document.activeElement
      : null;
  const first = focusables[0];
  const last = focusables[focusables.length - 1];
  if (event.shiftKey) {
    if (!active || active === first) {
      event.preventDefault();
      last.focus();
    }
    return;
  }
  if (!active || active === last) {
    event.preventDefault();
    first.focus();
  }
}

export function useModalDialog<T extends HTMLElement>(options: {
  escapeEnabled: boolean;
  onClose(): void;
}): RefObject<T | null> {
  const { escapeEnabled, onClose } = options;
  const dialogRef = useRef<T | null>(null);

  useEffect(() => {
    const invoker = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    return () => {
      if (invoker?.isConnected) invoker.focus();
    };
  }, []);

  useEffect(() => {
    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (escapeEnabled) onClose();
        return;
      }
      const container = dialogRef.current;
      if (event.key === 'Tab' && container) trapDialogTab(container, event);
    };
    window.addEventListener('keydown', handleKeydown);
    return () => window.removeEventListener('keydown', handleKeydown);
  }, [dialogRef, escapeEnabled, onClose]);

  return dialogRef;
}
