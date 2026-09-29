import { useEffect, useMemo, useRef, useState } from 'react';
import { highlightLearningJavaHtml } from '../learning/highlightJava';
import type { DocumentationPage } from './protocol';
import { extractToc } from './tree';

type Props = {
  page: DocumentationPage;
  fullscreen: boolean;
  onOpenRelated(id: string): void;
  onToggleFullscreen(): void;
};

const LEVEL_LABELS: Record<string, string> = {
  beginner: 'Iniciante',
  intermediate: 'Intermediário',
  advanced: 'Avançado',
};

export function DocumentationArticleTab({ page, fullscreen, onOpenRelated, onToggleFullscreen }: Props) {
  const toc = useMemo(() => extractToc(page.html), [page.html]);
  const html = useMemo(() => highlightLearningJavaHtml(page.html), [page.html]);
  const bodyRef = useRef<HTMLDivElement>(null);
  const [activeHeading, setActiveHeading] = useState('');

  useEffect(() => {
    setActiveHeading(toc[0]?.id ?? '');
  }, [page.id, toc]);

  useEffect(() => {
    if (!fullscreen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onToggleFullscreen();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [fullscreen, onToggleFullscreen]);

  useEffect(() => {
    const root = bodyRef.current;
    if (!root || toc.length === 0 || typeof IntersectionObserver === 'undefined') return;
    const headings = toc
      .map(item => document.getElementById(item.id))
      .filter((element): element is HTMLElement => element !== null);
    if (headings.length === 0) return;
    const observer = new IntersectionObserver(observed => {
      const visible = observed
        .filter(entry => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
      if (visible.length > 0) setActiveHeading(visible[0].target.id);
    }, { root, rootMargin: '0px 0px -70% 0px', threshold: 0 });
    headings.forEach(heading => observer.observe(heading));
    return () => observer.disconnect();
  }, [toc]);

  return (
    <section className="docs-article">
      <header className="docs-article-header">
        <nav className="docs-breadcrumb" aria-label="Trilha da página">
          {page.breadcrumb.map((crumb, index) => {
            const current = index === page.breadcrumb.length - 1;
            return (
              <span key={`${crumb}-${index}`} className="docs-breadcrumb-part">
                {index > 0 && <span className="docs-breadcrumb-sep">/</span>}
                <span className={current ? 'is-current' : undefined}>{crumb}</span>
              </span>
            );
          })}
        </nav>
        <h1 className="docs-article-title">{page.title}</h1>
        <div className="docs-article-meta">
          {page.level && (
            <span className="docs-chip">{LEVEL_LABELS[page.level] ?? page.level}</span>
          )}
          {page.duration != null && (
            <span className="docs-chip">{page.duration} min de leitura</span>
          )}
          <button
            type="button"
            className="docs-fullscreen-toggle"
            aria-pressed={fullscreen}
            onClick={onToggleFullscreen}
          >
            {fullscreen ? 'Sair da tela cheia' : 'Tela cheia'}
          </button>
        </div>
      </header>
      <div className="docs-article-body" ref={bodyRef}>
        <div className="docs-article-main">
          <div className="docs-article-content" dangerouslySetInnerHTML={{ __html: html }} />
          <footer className="docs-article-footer">
            {page.related.length > 0 && (
              <div className="docs-related">
                <span className="docs-related-label">Continue lendo</span>
                <div className="docs-related-items">
                  {page.related.map(item => (
                    <button
                      key={item.id}
                      type="button"
                      className="docs-related-item"
                      onClick={() => onOpenRelated(item.id)}
                    >
                      {item.title}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {page.officialDocs && (
              <a
                className="docs-official-link"
                href={page.officialDocs.url}
                target="_blank"
                rel="noreferrer"
              >
                Ver fonte oficial: {page.officialDocs.label}
              </a>
            )}
          </footer>
        </div>
        {toc.length > 0 && (
          <aside className="docs-toc" aria-label="Nesta página">
            <span className="docs-toc-label">Nesta página</span>
            <ul>
              {toc.map(item => (
                <li key={item.id} className={item.level === 3 ? 'is-level-3' : undefined}>
                  <button
                    type="button"
                    className={activeHeading === item.id ? 'is-active' : undefined}
                    onClick={() => document.getElementById(item.id)?.scrollIntoView({ block: 'start' })}
                  >
                    {item.text}
                  </button>
                </li>
              ))}
            </ul>
          </aside>
        )}
      </div>
    </section>
  );
}
