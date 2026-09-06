import { memo, useLayoutEffect, useRef, useState } from 'react';
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

function highlightedLabel(item: MonacoCompletionItem) {
  const matches = new Set(item.matchIndices ?? []);
  return item.label.split('').map((character, index) => matches.has(index)
    ? <mark key={index}>{character}</mark>
    : <span key={index}>{character}</span>);
}

type RowProps = {
  item: MonacoCompletionItem;
  index: number;
  selected: boolean;
  onSelect: (index: number) => void;
  onAccept: () => void;
};

const CompletionRow = memo(function CompletionRow({ item, index, selected, onSelect, onAccept }: RowProps) {
  const itemSignature = signature(item);
  return (
    <button type="button" className={`completion-row ${selected ? 'selected' : ''}`}
      role="option" aria-selected={selected}
      onMouseEnter={() => onSelect(index)} onMouseDown={event => event.preventDefault()}
      onClick={onAccept}>
      <img src={iconUrl(item.kind)} alt="" />
      <span className="completion-name"><strong>{highlightedLabel(item)}</strong><span>{itemSignature.suffix}</span></span>
      {item.returnType && <span className="completion-return">{item.returnType}</span>}
      {item.owner && <span className="completion-owner">{item.owner}</span>}
    </button>
  );
});

export function CompletionPopup({ state, onSelect, onAccept }: Props) {
  const popupRef = useRef<HTMLElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const sessionRef = useRef(state.requestId);
  const [position, setPosition] = useState(state.anchor);
  const [placement, setPlacement] = useState<'above' | 'below' | null>(null);

  useLayoutEffect(() => {
    if (sessionRef.current === state.requestId) return;
    sessionRef.current = state.requestId;
    setPlacement(null);
  }, [state.requestId]);

  useLayoutEffect(() => {
    const popup = popupRef.current;
    if (!popup) return;
    const bounds = popup.getBoundingClientRect();
    const left = Math.min(Math.max(8, state.anchor.left), Math.max(8, window.innerWidth - bounds.width - 8));
    if (placement === null) {
      setPlacement(state.anchor.top + bounds.height <= window.innerHeight - 8 ? 'below' : 'above');
      return;
    }
    const top = placement === 'below'
      ? Math.min(state.anchor.top, Math.max(8, window.innerHeight - bounds.height - 8))
      : Math.max(8, state.anchor.top - bounds.height);
    setPosition({ left, top });
  }, [placement, state.anchor, state.items]);

  useLayoutEffect(() => {
    const list = listRef.current;
    const selected = list?.children[state.selectedIndex] as HTMLElement | undefined;
    if (!list || !selected) return;
    const visibleTop = list.scrollTop;
    const visibleBottom = visibleTop + list.clientHeight;
    const selectedTop = selected.offsetTop;
    const selectedBottom = selectedTop + selected.offsetHeight;
    if (selectedTop < visibleTop) {
      list.scrollTop = selectedTop;
    } else if (selectedBottom > visibleBottom) {
      list.scrollTop = selectedBottom - list.clientHeight;
    }
  }, [state.selectedIndex]);

  const selected = state.items[state.selectedIndex];
  const selectedSignature = selected ? signature(selected) : null;

  return (
    <section ref={popupRef} className="completion-popup" style={{ left: position.left, top: position.top }}
      role="listbox" aria-label="Completion suggestions">
      <div ref={listRef} className="completion-list">
        {state.items.map((item, index) => (
          <CompletionRow key={`${item.label}-${index}`} item={item} index={index}
            selected={index === state.selectedIndex} onSelect={onSelect} onAccept={onAccept} />
        ))}
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
