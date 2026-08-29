import { useLayoutEffect, useRef, useState } from 'react';
import type { CompletionPopupState, MonacoCompletionItem } from './protocol';

type Props = {
  state: CompletionPopupState;
  onSelect: (index: number) => void;
  onAccept: () => void;
};

function signature(item: MonacoCompletionItem): { name: string; suffix: string } {
  const value = item.signature || '';
  const index = value.indexOf(item.label);
  if (index >= 0) return { name: item.label, suffix: value.slice(index + item.label.length) };
  return { name: item.label, suffix: value && value !== item.label ? ` ${value}` : '' };
}

function iconName(kind: MonacoCompletionItem['kind']): string {
  if (kind === 'CONSTRUCTOR') return 'method';
  if (kind === 'RECORD' || kind === 'ENUM') return 'class';
  return kind.toLowerCase();
}

function iconUrl(kind: MonacoCompletionItem['kind']): string {
  const base = window.location.protocol === 'file:' ? './icons/completion' : '/icons/completion';
  return `${base}/${iconName(kind)}.svg`;
}

export function CompletionPopup({ state, onSelect, onAccept }: Props) {
  const popupRef = useRef<HTMLElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState(state.anchor);

  useLayoutEffect(() => {
    const popup = popupRef.current;
    if (!popup) return;
    const bounds = popup.getBoundingClientRect();
    const left = Math.min(Math.max(8, state.anchor.left), Math.max(8, window.innerWidth - bounds.width - 8));
    const below = state.anchor.top;
    const top = below + bounds.height <= window.innerHeight - 8
      ? below
      : Math.max(8, state.anchor.top - bounds.height);
    setPosition({ left, top });
  }, [state.anchor, state.items, state.selectedIndex]);

  useLayoutEffect(() => {
    const selected = listRef.current?.querySelector<HTMLElement>('[aria-selected="true"]');
    selected?.scrollIntoView({ block: 'nearest' });
  }, [state.selectedIndex]);

  const selected = state.items[state.selectedIndex];
  const selectedSignature = selected ? signature(selected) : null;

  return (
    <section ref={popupRef} className="completion-popup" style={{ left: position.left, top: position.top }}
      role="listbox" aria-label="Completion suggestions">
      <div ref={listRef} className="completion-list">
        {state.items.map((item, index) => {
          const itemSignature = signature(item);
          return (
            <button type="button" className={`completion-row ${index === state.selectedIndex ? 'selected' : ''}`}
              key={`${item.label}-${index}`} role="option" aria-selected={index === state.selectedIndex}
              onMouseEnter={() => onSelect(index)} onMouseDown={event => event.preventDefault()}
              onClick={onAccept}>
              <img src={iconUrl(item.kind)} alt="" />
              <span className="completion-name"><strong>{itemSignature.name}</strong><span>{itemSignature.suffix}</span></span>
              {item.returnType && <span className="completion-return">{item.returnType}</span>}
              {item.owner && <span className="completion-owner">{item.owner}</span>}
            </button>
          );
        })}
      </div>
      {selected && selectedSignature && (
        <footer className="completion-details">
          <div className="completion-detail-title">
            <span>{selected.returnType}</span><strong>{selectedSignature.name}{selectedSignature.suffix}</strong>
          </div>
          {selected.owner && <div className="completion-detail-owner">{selected.owner}</div>}
          {selected.documentation && <p>{selected.documentation}</p>}
          {selected.example && <div className="completion-example"><span>Example</span><pre>{selected.example}</pre></div>}
        </footer>
      )}
    </section>
  );
}
