import type { KeyboardEvent } from 'react';

export type AriaTabListProps = {
  role: 'tablist';
  'aria-label': string;
  onKeyDown(event: KeyboardEvent<HTMLElement>): void;
};

export type AriaTabProps = {
  role: 'tab';
  id: string;
  'aria-selected': boolean;
  tabIndex: number;
  'aria-controls': string;
};

export function nextTabIndex(key: string, currentIndex: number, count: number): number | null {
  if (count <= 0) return null;
  if (key === 'ArrowRight') return (currentIndex + 1) % count;
  if (key === 'ArrowLeft') return (currentIndex - 1 + count) % count;
  if (key === 'Home') return 0;
  if (key === 'End') return count - 1;
  return null;
}

export function tabIdFor(listId: string, tabId: string): string {
  return `${listId}-tab-${tabId}`;
}

export function panelIdFor(listId: string, tabId: string): string {
  return `${listId}-panel-${tabId}`;
}

export function useAriaTabs<Id extends string>(options: {
  listId: string;
  label: string;
  tabIds: readonly Id[];
  activeId: Id;
  onSelect(id: Id): void;
  sharedPanelId?: string;
}): { tabListProps: AriaTabListProps; tabProps(id: Id): AriaTabProps } {
  const { listId, label, tabIds, activeId, onSelect, sharedPanelId } = options;
  const handleKeyDown = (event: KeyboardEvent<HTMLElement>): void => {
    const next = nextTabIndex(event.key, tabIds.indexOf(activeId), tabIds.length);
    if (next === null) return;
    event.preventDefault();
    const nextId = tabIds[next];
    if (nextId !== activeId) onSelect(nextId);
    const tabs = event.currentTarget.querySelectorAll<HTMLElement>('[role="tab"]');
    tabs[next]?.focus();
  };
  const tabProps = (id: Id): AriaTabProps => ({
    role: 'tab',
    id: tabIdFor(listId, id),
    'aria-selected': activeId === id,
    tabIndex: activeId === id ? 0 : -1,
    'aria-controls': sharedPanelId ?? panelIdFor(listId, id),
  });
  return {
    tabListProps: { role: 'tablist', 'aria-label': label, onKeyDown: handleKeyDown },
    tabProps,
  };
}
