import { useLayoutEffect, useRef, useState, type MouseEvent } from 'react';
import type { LearningPopupState } from './protocol';

type Props = {
  state: LearningPopupState;
  onNavigate: (identifier: string) => void;
  onHover: (hovered: boolean) => void;
};

export function LearningCard({ state, onNavigate, onHover }: Props) {
  const cardRef = useRef<HTMLElement>(null);
  const [position, setPosition] = useState(state.anchor);
  const card = state.card;

  useLayoutEffect(() => {
    const element = cardRef.current;
    if (!element) return;
    const bounds = element.getBoundingClientRect();
    const left = Math.min(Math.max(8, state.anchor.left), Math.max(8, window.innerWidth - bounds.width - 8));
    const below = state.anchor.top;
    const top = below + bounds.height <= window.innerHeight - 8
      ? below
      : Math.max(8, state.anchor.top - bounds.height);
    setPosition({ left, top });
  }, [state.anchor]);

  function navigate(event: MouseEvent<HTMLElement>, identifier: string) {
    event.preventDefault();
    onNavigate(identifier);
  }

  function handleBodyClick(event: MouseEvent<HTMLElement>) {
    const anchor = (event.target as HTMLElement).closest('a[href]') as HTMLAnchorElement | null;
    if (!anchor?.href.startsWith('eyecode://learn/')) return;
    const identifier = decodeURIComponent(anchor.href.slice('eyecode://learn/'.length));
    if (identifier) navigate(event, identifier);
  }

  return (
    <section ref={cardRef} className={`learning-popup learning-size-${card.sizeClass}`} style={position}
      aria-label="Learning card" onMouseEnter={() => onHover(true)} onMouseLeave={() => onHover(false)}>
      <header className="learning-header">
        <div className="learning-title-row">
          {card.iconUrl ? <img className="learning-icon" src={card.iconUrl} alt="" />
            : <span className="learning-icon" aria-hidden="true">{card.iconKind.slice(0, 1)}</span>}
          <div className="learning-title">{card.title}</div>
        </div>
        <div className="learning-subtitle">{card.subtitle}</div>
        {card.breadcrumb.length > 0 && <nav className="learning-breadcrumb" aria-label="Learning path">
          {card.breadcrumb.map((item, index) => index === card.breadcrumb.length - 1
            ? <span key={item.id}>{item.title}</span>
            : <button key={item.id} type="button" onClick={event => navigate(event, item.id)}>{item.title}</button>)}
        </nav>}
      </header>
      <section className="learning-body" onClick={handleBodyClick}
        dangerouslySetInnerHTML={{ __html: card.renderedBodyHtml }} />
      {card.commonMethods.length > 0 && <section className="learning-common-methods">
        <span>Common methods</span>
        <div>{card.commonMethods.map(item => <button key={item.id} type="button"
          onClick={event => navigate(event, item.id)}>{item.title}</button>)}</div>
      </section>}
      {card.relatedItems.length > 0 && <footer className="learning-footer">
        <div className="learning-related"><span>Related</span><div>
          {card.relatedItems.map(item => <button key={item.id} type="button"
            onClick={event => navigate(event, item.id)}>{item.title}</button>)}
        </div></div>
      </footer>}
    </section>
  );
}
