import { useEffect, useMemo, useState } from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import type { DocumentationCatalogResponse, DocumentationEntry } from './protocol';
import { filterEntries, groupBranches, type DocumentationBranch, type DocumentationSubgroup } from './tree';

type Props = {
  activeId: string | null;
  onOpen(id: string): void;
};

export function DocumentationExplorer({ activeId, onOpen }: Props) {
  const [entries, setEntries] = useState<DocumentationEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});

  useEffect(() => {
    let cancelled = false;
    bridge.request<DocumentationCatalogResponse>('docs', 'catalog', {})
      .then(response => {
        if (cancelled) return;
        setEntries(response.entries ?? []);
        setLoading(false);
      })
      .catch(cause => {
        if (cancelled) return;
        setError(formatError(cause));
        setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  const visible = useMemo(() => filterEntries(entries, query), [entries, query]);
  const branches = useMemo(() => groupBranches(visible), [visible]);

  const toggle = (key: string) => setCollapsed(current => ({ ...current, [key]: !current[key] }));

  const renderPage = (entry: DocumentationEntry) => (
    <li key={entry.id}>
      <button
        type="button"
        className={`docs-page-item${activeId === entry.id ? ' is-active' : ''}`}
        title={entry.summary ?? entry.title}
        onClick={() => onOpen(entry.id)}
      >
        <span className="docs-page-title">{entry.title}</span>
        {entry.summary && <span className="docs-page-summary">{entry.summary}</span>}
      </button>
    </li>
  );

  const renderSubgroup = (group: DocumentationBranch, subgroup: DocumentationSubgroup) => {
    const key = `${group.branch}/${subgroup.key}`;
    const isCollapsed = collapsed[key] === true;
    return (
      <div key={subgroup.key} className="docs-subgroup">
        <button
          type="button"
          className="docs-branch-toggle docs-subgroup-toggle"
          aria-expanded={!isCollapsed}
          onClick={() => toggle(key)}
        >
          <span className={`docs-branch-caret${isCollapsed ? ' is-collapsed' : ''}`}>▸</span>
          <span className="docs-branch-label docs-subgroup-label">{subgroup.label}</span>
          <span className="docs-branch-count">{subgroup.entries.length}</span>
        </button>
        {!isCollapsed && <ul className="docs-page-list">{subgroup.entries.map(renderPage)}</ul>}
      </div>
    );
  };

  return (
    <section className="auxiliary-panel docs-explorer">
      <header className="panel-heading"><span>Documentation</span></header>
      <div className="docs-explorer-search">
        <input
          type="search"
          value={query}
          onChange={event => setQuery(event.target.value)}
          placeholder="Buscar tópico..."
          aria-label="Buscar na documentação"
        />
      </div>
      <div className="docs-explorer-tree">
        {loading && <p className="docs-explorer-hint">Carregando catálogo...</p>}
        {error && <p className="docs-explorer-error">{error}</p>}
        {!loading && !error && branches.length === 0 && (
          <p className="docs-explorer-hint">Nenhum tópico encontrado.</p>
        )}
        {branches.map(group => {
          const isCollapsed = collapsed[group.branch] === true;
          return (
            <div key={group.branch} className="docs-branch">
              <button
                type="button"
                className="docs-branch-toggle"
                aria-expanded={!isCollapsed}
                onClick={() => toggle(group.branch)}
              >
                <span className={`docs-branch-caret${isCollapsed ? ' is-collapsed' : ''}`}>▸</span>
                <span className="docs-branch-label">{group.label}</span>
                <span className="docs-branch-count">{group.total}</span>
              </button>
              {!isCollapsed && (
                <>
                  {group.entries.length > 0 && <ul className="docs-page-list">{group.entries.map(renderPage)}</ul>}
                  {group.subgroups.map(subgroup => renderSubgroup(group, subgroup))}
                </>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function formatError(error: unknown): string {
  if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
    const value = error as { code: string; message: string };
    return `${value.code}: ${value.message}`;
  }
  return error instanceof Error ? error.message : String(error);
}
