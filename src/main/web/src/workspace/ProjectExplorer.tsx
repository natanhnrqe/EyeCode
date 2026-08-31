import { useCallback, useEffect, useRef, useState, type MutableRefObject } from 'react';
import type { ProjectNode, ProjectSnapshot, TreeReveal } from './protocol';
import { EyeCodeIcon } from './EyeCodeIcon';

type Props = {
  project?: ProjectSnapshot;
  childrenByPath: Record<string, ProjectNode[]>;
  reveal?: TreeReveal;
  treeChangedPath?: string;
  treeRefreshRevision: number;
  onLoadChildren(path: string, force?: boolean): Promise<void>;
  onOpenFile(path: string): Promise<void>;
  onRefresh(paths: string[]): Promise<string[]>;
  onOpenProject(): void;
  onNewFile(): void;
};

export function ProjectExplorer({ project, childrenByPath, reveal, treeChangedPath, treeRefreshRevision, onLoadChildren, onOpenFile, onRefresh, onOpenProject, onNewFile }: Props) {
  const [expandedPaths, setExpandedPaths] = useState<Set<string>>(() => new Set());
  const [loadingPaths, setLoadingPaths] = useState<Set<string>>(() => new Set());
  const [failedPaths, setFailedPaths] = useState<Set<string>>(() => new Set());
  const [selectedPath, setSelectedPath] = useState<string>();
  const loading = useRef(new Map<string, Promise<void>>());
  const nodeRefs = useRef(new Map<string, HTMLDivElement>());
  const handledReveal = useRef<string | undefined>(undefined);
  const children = useRef(childrenByPath);
  const openFile = useRef(onOpenFile);

  useEffect(() => { children.current = childrenByPath; }, [childrenByPath]);
  useEffect(() => { openFile.current = onOpenFile; }, [onOpenFile]);

  useEffect(() => {
    loading.current.clear();
    handledReveal.current = undefined;
    setExpandedPaths(project ? new Set([project.root.path]) : new Set());
    setLoadingPaths(new Set());
    setFailedPaths(new Set());
    setSelectedPath(undefined);
  }, [project?.root.path]);

  const ensureChildren = useCallback((path: string, force = false): Promise<void> => {
    if (!force && children.current[path] !== undefined) return Promise.resolve();
    const current = loading.current.get(path);
    if (current) return current;
    setLoadingPaths(paths => new Set(paths).add(path));
    setFailedPaths(paths => { const next = new Set(paths); next.delete(path); return next; });
    const request = onLoadChildren(path, force).catch(() => {
      setFailedPaths(paths => new Set(paths).add(path));
    }).finally(() => {
      loading.current.delete(path);
      setLoadingPaths(paths => { const next = new Set(paths); next.delete(path); return next; });
    });
    loading.current.set(path, request);
    return request;
  }, [onLoadChildren]);

  useEffect(() => {
    if (project) void ensureChildren(project.root.path);
  }, [ensureChildren, project]);

  useEffect(() => {
    if (!treeChangedPath || !expandedPaths.has(treeChangedPath)) return;
    void ensureChildren(treeChangedPath, true);
  }, [ensureChildren, expandedPaths, treeChangedPath, treeRefreshRevision]);

  useEffect(() => {
    if (!project || !reveal) return;
    const key = `${project.root.path}:${reveal.targetPath}`;
    if (handledReveal.current === key) return;
    handledReveal.current = key;
    let cancelled = false;
    void (async () => {
      for (const path of [project.root.path, ...reveal.ancestors]) {
        if (cancelled) return;
        await ensureChildren(path);
        if (cancelled) return;
        setExpandedPaths(paths => new Set(paths).add(path));
      }
      if (cancelled) return;
      setSelectedPath(reveal.targetPath);
      await openFile.current(reveal.targetPath);
    })();
    return () => { cancelled = true; };
  }, [ensureChildren, project?.root.path, reveal?.targetPath, reveal?.ancestors.join('|')]);

  useEffect(() => {
    if (!selectedPath) return;
    nodeRefs.current.get(selectedPath)?.scrollIntoView({ block: 'nearest' });
  }, [childrenByPath, expandedPaths, selectedPath]);

  if (!project) return <section className="project-explorer project-explorer-empty">
    <div className="empty-mark">EC</div>
    <strong>No project open</strong>
    <span>Open a Java workspace to browse and edit its source.</span>
    <button type="button" className="primary-action" onClick={onOpenProject}>Open Project</button>
    <button type="button" className="quiet-action" onClick={onNewFile}>New File</button>
  </section>;

  const toggle = (node: ProjectNode) => {
    if (node.kind === 'file') {
      setSelectedPath(node.path);
      void openFile.current(node.path);
      return;
    }
    if (!expandedPaths.has(node.path)) {
      void ensureChildren(node.path);
      setExpandedPaths(paths => new Set(paths).add(node.path));
      return;
    }
    setExpandedPaths(paths => { const next = new Set(paths); next.delete(node.path); return next; });
  };

  return <section className="project-explorer">
    <header className="panel-heading">
      <span>Project</span>
      <div className="panel-heading-actions">
        <button type="button" onClick={onNewFile} aria-label="New file"><EyeCodeIcon name="newFile" /></button>
        <button type="button" onClick={() => void refreshExpandedPaths()} aria-label="Refresh project"><EyeCodeIcon name="reload" /></button>
      </div>
    </header>
    <div className="project-tree" role="tree">
      <TreeNode node={project.root} depth={0} childrenByPath={childrenByPath} expandedPaths={expandedPaths}
        loadingPaths={loadingPaths} failedPaths={failedPaths} selectedPath={selectedPath} onToggle={toggle} nodeRefs={nodeRefs} />
    </div>
  </section>;

  async function refreshExpandedPaths() {
    const paths = [...new Set([...Object.keys(childrenByPath), ...expandedPaths])];
    const validPaths = await onRefresh(paths);
    const valid = new Set(validPaths);
    setExpandedPaths(current => new Set([...current].filter(path => valid.has(path))));
    for (const path of paths.filter(path => valid.has(path)).sort((left, right) => left.length - right.length)) {
      await ensureChildren(path, true);
    }
  }
}

type TreeNodeProps = {
  node: ProjectNode;
  depth: number;
  childrenByPath: Record<string, ProjectNode[]>;
  expandedPaths: Set<string>;
  loadingPaths: Set<string>;
  failedPaths: Set<string>;
  selectedPath?: string;
  onToggle(node: ProjectNode): void;
  nodeRefs: MutableRefObject<Map<string, HTMLDivElement>>;
};

function TreeNode({ node, depth, childrenByPath, expandedPaths, loadingPaths, failedPaths, selectedPath, onToggle, nodeRefs }: TreeNodeProps) {
  const directory = node.kind !== 'file';
  const expanded = expandedPaths.has(node.path);
  const children = childrenByPath[node.path];
  return <div className="tree-node" role="treeitem" aria-expanded={directory ? expanded : undefined}
    ref={element => { if (element) nodeRefs.current.set(node.path, element); else nodeRefs.current.delete(node.path); }}>
    <button type="button" className={`tree-row tree-${node.kind}${selectedPath === node.path ? ' is-selected' : ''}`}
      onClick={() => onToggle(node)} style={{ paddingLeft: `${8 + depth * 15}px` }}
      aria-busy={loadingPaths.has(node.path)} aria-invalid={failedPaths.has(node.path)}>
      <span className={`tree-chevron ${directory && expanded ? 'is-open' : ''}`}>{directory && node.hasChildren ? '›' : ''}</span>
      <EyeCodeIcon name={treeIcon(node, expanded)} className="tree-icon" />
      <span className="tree-label">{node.name}</span>
    </button>
    {directory && expanded && children?.map(child => <TreeNode key={child.path} node={child} depth={depth + 1}
      childrenByPath={childrenByPath} expandedPaths={expandedPaths} loadingPaths={loadingPaths} failedPaths={failedPaths}
      selectedPath={selectedPath} onToggle={onToggle} nodeRefs={nodeRefs} />)}
  </div>;
}

function treeIcon(node: ProjectNode, expanded: boolean): string {
  if (node.kind === 'file') return node.name.endsWith('.java') ? 'java' : 'file';
  if (node.kind === 'project') return 'project';
  return expanded ? 'folderOpen' : 'folder';
}
