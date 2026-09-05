import {
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type MouseEvent,
} from 'react';

import { highlightLearningJavaHtml } from './highlightJava';
import type { LearningPopupState } from './protocol';

type Props = {
  state: LearningPopupState;
  onNavigate: (identifier: string) => void;
  onAction: (action: 'openDocumentation' | 'openJdkSource') => void;
  onHover: (hovered: boolean) => void;
};

type PopupPosition = {
  left: number;
  top: number;
  maxHeight?: number;
};

const MARGIN = 8;
const ANCHOR_GAP = 5;

export function LearningCard({
  state,
  onNavigate,
  onAction,
  onHover,
}: Props) {
  const cardRef = useRef<HTMLElement>(null);

  const [position, setPosition] = useState<PopupPosition>({
    left: 0,
    top: 0,
  });
  const card = state.card;
  useLayoutEffect(() => {
    const cardElement = cardRef.current;
    if (!cardElement) return;

    const updatePosition = () => {
      const appliedMaxHeight = cardElement.style.maxHeight;
      cardElement.style.maxHeight = '';
      const cardBounds = cardElement.getBoundingClientRect();
      const cssMaxHeight = Number.parseFloat(window.getComputedStyle(cardElement).maxHeight);
      cardElement.style.maxHeight = appliedMaxHeight;

      const top = Math.max(MARGIN, state.anchor.top + ANCHOR_GAP);
      const maxLeft = Math.max(MARGIN, window.innerWidth - cardBounds.width - MARGIN);
      const left = Math.min(Math.max(MARGIN, state.anchor.left), maxLeft);
      const availableHeight = Math.max(0, window.innerHeight - MARGIN - top);
      const preferredMaxHeight = Number.isFinite(cssMaxHeight) ? cssMaxHeight : cardBounds.height;
      const maxHeight = Math.min(preferredMaxHeight, availableHeight);

      setPosition(current => current.left === left && current.top === top
        && current.maxHeight === maxHeight ? current : { left, top, maxHeight });
    };

    updatePosition();
    const observer = new ResizeObserver(updatePosition);
    observer.observe(cardElement);
    window.addEventListener('resize', updatePosition);
    return () => {
      observer.disconnect();
      window.removeEventListener('resize', updatePosition);
    };
  }, [state.anchor.left, state.anchor.top, card]);
  const highlightedBodyHtml = useMemo(() => highlightLearningJavaHtml(card.renderedBodyHtml), [card.renderedBodyHtml]);

  function navigate(
    event: MouseEvent<HTMLElement>,
    identifier: string,
  ) {
    event.preventDefault();
    onNavigate(identifier);
  }

  function handleBodyClick(
    event: MouseEvent<HTMLElement>,
  ) {
    const anchor = (
      event.target as HTMLElement
    ).closest(
      'a[href]',
    ) as HTMLAnchorElement | null;

    if (
      !anchor?.href.startsWith(
        'eyecode://learn/',
      )
    ) {
      return;
    }

    const identifier = decodeURIComponent(
      anchor.href.slice(
        'eyecode://learn/'.length,
      ),
    );

    if (identifier) {
      navigate(event, identifier);
    }
  }

  const style: CSSProperties = {
    left: position.left,
    top: position.top,
    maxHeight: position.maxHeight,
  };

  return (
    <section
      ref={cardRef}
      className={`learning-popup learning-size-${card.sizeClass}`}
      style={style}
      aria-label="Learning card"
      onMouseEnter={() => onHover(true)}
      onMouseLeave={() => onHover(false)}
    >
<header className="learning-header">
  <div className="learning-header-main">
    <div className="learning-heading">
      <div className="learning-title-row">
        {card.iconUrl ? (
          <img
            className="learning-icon"
            src={card.iconUrl}
            alt=""
          />
        ) : (
          <span
            className="learning-icon"
            aria-hidden="true"
          >
            {card.iconKind.slice(0, 1)}
          </span>
        )}

        <div className="learning-title">
          {card.title}
        </div>
      </div>

      <div className="learning-subtitle">
        {card.subtitle}
      </div>
    </div>

    {(card.docsAvailable || card.sourceAvailable) && (
      <div className="learning-header-actions">
        {card.docsAvailable && (
          <button
            type="button"
            onClick={() => onAction('openDocumentation')}
          >
            Abrir documentação
          </button>
        )}

        {card.sourceAvailable && (
          <button
            type="button"
            onClick={() => onAction('openJdkSource')}
          >
            Abrir fonte do JDK
          </button>
        )}
      </div>
    )}
  </div>

  {card.breadcrumb.length > 0 && (
    <nav
      className="learning-breadcrumb"
      aria-label="Learning path"
    >
      {card.breadcrumb.map(
        (item, index) =>
          index === card.breadcrumb.length - 1 ? (
            <span key={item.id}>
              {item.title}
            </span>
          ) : (
            <button
              key={item.id}
              type="button"
              onClick={event =>
                navigate(event, item.id)
              }
            >
              {item.title}
            </button>
          ),
      )}
    </nav>
  )}
</header>

      <section
        className="learning-body"
        onClick={handleBodyClick}
        dangerouslySetInnerHTML={{
          __html: highlightedBodyHtml,
        }}
      />

      {card.commonMethods.length > 0 && (
        <section className="learning-common-methods">
          <span>Métodos comuns</span>

          <div>
            {card.commonMethods.map(item => (
              <button
                key={item.id}
                type="button"
                onClick={event =>
                  navigate(
                    event,
                    item.id,
                  )
                }
              >
                {item.title}
              </button>
            ))}
          </div>
        </section>
      )}

          {card.relatedItems.length > 0 && (
            <footer className="learning-footer">
              <span className="learning-footer-label">
                Relacionados
              </span>

              <div className="learning-related-items">
                {card.relatedItems.map(item => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={event =>
                      navigate(event, item.id)
                    }
                  >
                    {item.title}
                  </button>
                ))}
              </div>
            </footer>
          )}
        </section>
      );
    }
