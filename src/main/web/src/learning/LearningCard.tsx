import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
  type MouseEvent,
} from 'react';

import type { LearningPopupState } from './protocol';

type Props = {
  state: LearningPopupState;
  onNavigate: (identifier: string) => void;
  onHover: (hovered: boolean) => void;
};

type PopupPosition = {
  left: number;
  top: number;
  maxHeight?: number;
};

type PlacementDiagnostics = {
  anchorLeft: number;
  anchorTop: number;
  editorBounds: DOMRect;
  cardWidth: number;
  cardHeight: number;
  cssMaxHeight: number;
  finalLeft: number;
  finalTop: number;
  finalMaxHeight: number;
  computedPopupMaxHeight?: number;
  renderedPopupHeight?: number;
  renderedPopupBottom?: number;
  bodyClientHeight?: number;
  bodyScrollHeight?: number;
  boundary: string;
  visibleMonaco: boolean;
};
const MARGIN = 8;
const ANCHOR_GAP = 5;

function describeElement(element: HTMLElement | null): object | null {
  if (!element) return null;
  return {
    tagName: element.tagName,
    id: element.id,
    className: element.className,
  };
}

function findVisibleEditorRegion(anchor: { left: number; top: number }): HTMLElement | null {
  const regions = Array.from(document.querySelectorAll<HTMLElement>('.editor-region'))
    .filter(region => {
      const bounds = region.getBoundingClientRect();
      const style = window.getComputedStyle(region);
      return bounds.width > 0 && bounds.height > 0
        && style.display !== 'none' && style.visibility !== 'hidden'
        && region.querySelector('.monaco-editor') !== null;
    });
  return regions.find(region => {
    const bounds = region.getBoundingClientRect();
    return anchor.left >= bounds.left && anchor.left <= bounds.right
      && anchor.top >= bounds.top && anchor.top <= bounds.bottom;
  }) ?? regions[0] ?? null;
}

export function LearningCard({
  state,
  onNavigate,
  onHover,
}: Props) {
  const cardRef = useRef<HTMLElement>(null);

  const [position, setPosition] = useState<PopupPosition>({
    left: 0,
    top: 0,
  });
  const [placement, setPlacement] = useState<PlacementDiagnostics | null>(null);

  const card = state.card;

  useEffect(() => {
    console.log('LEARNING_PLACEMENT version=editor-region-v2');
  }, []);

  useLayoutEffect(() => {
    const cardElement = cardRef.current;
    if (!cardElement) return;

    let layoutFrame: number | null = null;

    const updatePosition = () => {
      const editorRegion = findVisibleEditorRegion(state.anchor);
      if (!editorRegion) return;

      const editorBounds = editorRegion.getBoundingClientRect();
      const appliedMaxHeight = cardElement.style.maxHeight;
      cardElement.style.maxHeight = '';
      const cardBounds = cardElement.getBoundingClientRect();
      const cssMaxHeight = Number.parseFloat(window.getComputedStyle(cardElement).maxHeight);
      cardElement.style.maxHeight = appliedMaxHeight;
      const visibleLeft = Math.max(editorBounds.left, 0) + MARGIN;
      const visibleRight = Math.min(editorBounds.right, window.innerWidth) - MARGIN;
      const visibleBottom = Math.min(editorBounds.bottom, window.innerHeight) - MARGIN;
      const top = state.anchor.top + ANCHOR_GAP;
      const maxLeft = Math.max(visibleLeft, visibleRight - cardBounds.width);
      const left = Math.min(Math.max(visibleLeft, state.anchor.left), maxLeft);
      const availableHeight = Math.max(0, visibleBottom - top);
      const maxHeight = Number.isFinite(cssMaxHeight)
        ? Math.min(cssMaxHeight, availableHeight)
        : availableHeight;

      const monacoElement = editorRegion.querySelector<HTMLElement>('.monaco-editor');
      const monacoBounds = monacoElement?.getBoundingClientRect();
      const monacoStyle = monacoElement ? window.getComputedStyle(monacoElement) : null;
      const containsVisibleMonaco = !!monacoBounds && monacoBounds.width > 0 && monacoBounds.height > 0
        && monacoStyle?.display !== 'none' && monacoStyle?.visibility !== 'hidden';
      const boundary = `${editorRegion.tagName.toLowerCase()}${editorRegion.id ? `#${editorRegion.id}` : ''}${editorRegion.className ? `.${editorRegion.className.split(/\s+/).join('.')}` : ''}`;
      const diagnostic: PlacementDiagnostics = {
        anchorLeft: state.anchor.left,
        anchorTop: state.anchor.top,
        editorBounds,
        cardWidth: cardBounds.width,
        cardHeight: cardBounds.height,
        cssMaxHeight,
        finalLeft: left,
        finalTop: top,
        finalMaxHeight: maxHeight,
        boundary,
        visibleMonaco: containsVisibleMonaco,
      };
      console.log('LEARNING_PLACEMENT', {
        ...diagnostic,
        editorRegionRect: {
          left: editorBounds.left,
          top: editorBounds.top,
          right: editorBounds.right,
          bottom: editorBounds.bottom,
          width: editorBounds.width,
          height: editorBounds.height,
        },
        viewport: { width: window.innerWidth, height: window.innerHeight },
        cardRect: { width: cardBounds.width, height: cardBounds.height },
        boundary: {
          ...describeElement(editorRegion),
          parents: [
            describeElement(editorRegion.closest<HTMLElement>('.editor-stack')),
            describeElement(editorRegion.closest<HTMLElement>('.main-workspace')),
            describeElement(editorRegion.closest<HTMLElement>('.shell-workspace')),
            describeElement(editorRegion.closest<HTMLElement>('.app-shell')),
          ].filter(Boolean),
          containsVisibleMonaco,
        },
      });
      setPlacement(diagnostic);
      setPosition({ left, top, maxHeight });
      if (layoutFrame !== null) window.cancelAnimationFrame(layoutFrame);
      layoutFrame = window.requestAnimationFrame(() => {
        const renderedBounds = cardElement.getBoundingClientRect();
        const body = cardElement.querySelector<HTMLElement>('.learning-body');
        const layout = {
          computedPopupMaxHeight: Number.parseFloat(window.getComputedStyle(cardElement).maxHeight),
          renderedPopupHeight: renderedBounds.height,
          renderedPopupBottom: renderedBounds.bottom,
          bodyClientHeight: body?.clientHeight,
          bodyScrollHeight: body?.scrollHeight,
        };
        console.log('LEARNING_PLACEMENT_LAYOUT', {
          requestedMaxHeight: maxHeight,
          ...layout,
          editorBottom: editorBounds.bottom,
        });
        setPlacement(current => current ? { ...current, ...layout } : current);
      });
    };

    updatePosition();

    const resizeObserver = new ResizeObserver(updatePosition);
    const editorRegion = findVisibleEditorRegion(state.anchor);
    if (editorRegion) resizeObserver.observe(editorRegion);

    window.addEventListener('resize', updatePosition);

    return () => {
      resizeObserver.disconnect();
      if (layoutFrame !== null) window.cancelAnimationFrame(layoutFrame);
      window.removeEventListener('resize', updatePosition);
    };
  }, [
    state.anchor.left,
    state.anchor.top,
    card.sizeClass,
    card.renderedBodyHtml,
  ]);
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
      data-placement-version="editor-region-v2"
      aria-label="Learning card"
      onMouseEnter={() => onHover(true)}
      onMouseLeave={() => onHover(false)}
    >
      {placement && (
        <output
          aria-label="Learning placement diagnostics"
          style={{
            position: 'absolute',
            top: 6,
            right: 6,
            zIndex: 1,
            maxWidth: 310,
            padding: '5px 7px',
            border: '1px solid rgba(255, 196, 82, .62)',
            borderRadius: 4,
            color: '#ffe0a3',
            background: 'rgba(8, 10, 14, .88)',
            font: '10px/1.35 "JetBrains Mono", monospace',
            pointerEvents: 'none',
            whiteSpace: 'pre-line',
          }}
        >
          {[
            'version: editor-region-v2',
            `anchor: ${placement.anchorLeft.toFixed(1)}, ${placement.anchorTop.toFixed(1)}`,
            `editor: ${placement.editorBounds.left.toFixed(1)}, ${placement.editorBounds.top.toFixed(1)}, ${placement.editorBounds.right.toFixed(1)}, ${placement.editorBounds.bottom.toFixed(1)}`,
            `viewport: ${window.innerWidth} x ${window.innerHeight}`,
            `card: ${placement.cardWidth.toFixed(1)} x ${placement.cardHeight.toFixed(1)}`,
            `final: ${placement.finalLeft.toFixed(1)}, ${placement.finalTop.toFixed(1)}, max ${placement.finalMaxHeight.toFixed(1)}`,
            `computed max: ${placement.computedPopupMaxHeight?.toFixed(1) ?? 'pending'}`,
            `rendered: ${placement.renderedPopupHeight?.toFixed(1) ?? 'pending'}, bottom ${placement.renderedPopupBottom?.toFixed(1) ?? 'pending'}`,
            `body: ${placement.bodyClientHeight ?? 'pending'} / ${placement.bodyScrollHeight ?? 'pending'}`,
            `boundary: ${placement.boundary}`,
            `visibleMonaco: ${placement.visibleMonaco}`,
          ].join('\n')}
        </output>
      )}
      <header className="learning-header">
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

        {card.breadcrumb.length > 0 && (
          <nav
            className="learning-breadcrumb"
            aria-label="Learning path"
          >
            {card.breadcrumb.map(
              (item, index) =>
                index ===
                card.breadcrumb.length - 1 ? (
                  <span key={item.id}>
                    {item.title}
                  </span>
                ) : (
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
                ),
            )}
          </nav>
        )}
      </header>

      <section
        className="learning-body"
        onClick={handleBodyClick}
        dangerouslySetInnerHTML={{
          __html:
            card.renderedBodyHtml,
        }}
      />

      {card.commonMethods.length > 0 && (
        <section className="learning-common-methods">
          <span>Common methods</span>

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
          <div className="learning-related">
            <span>Related</span>

            <div>
              {card.relatedItems.map(item => (
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
          </div>
        </footer>
      )}
    </section>
  );
}