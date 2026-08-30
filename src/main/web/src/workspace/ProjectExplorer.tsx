import { useEffect, useState } from 'react';
import type { ProjectNode, ProjectSnapshot } from './protocol';
import { EyeCodeIcon } from './EyeCodeIcon';

type Props = {
  project?: ProjectSnapshot;
  childrenByPath: Record<string, ProjectNode[]>;
  onLoadChildren(path: string): void;
  onOpenFile(path: string): void;
  onOpenProject(): void;
  onNewFile(): void;
};

export function ProjectExplorer({ project, childrenByPath, onLoadChildren, onOpenFile, onOpenProject, onNewFile }: Props) {
  if (!project) return <section className="project-explorer project-explorer-empty">
    <div className="empty-mark">EC</div>
    <strong>No project open</strong>
    <span>Open a Java workspace to browse and edit its source.</span>
    <button type="button" className="primary-action" onClick={onOpenProject}>Open Project</button>
    <button type="button" className="quiet-action" onClick={onNewFile}>New File</button>
  </section>;

  return <section className="project-explorer">
    <header className="panel-heading">
      <span>Project</span>
      <div className="panel-heading-actions">
        <button type="button" onClick={onNewFile} aria-label="New file"><EyeCodeIcon name="newFile" /></button>
        <button type="button" onClick={() => onLoadChildren(project.root.path)} aria-label="Refresh project"><EyeCodeIcon name="reload" /></button>
      </div>
    </header>
    <div className="project-tree" role="tree">
      <TreeNode node={project.root} depth={0} childrenByPath={childrenByPath}
        onLoadChildren={onLoadChildren} onOpenFile={onOpenFile} initiallyExpanded />
    </div>
  </section>;
}

type TreeNodeProps = {
  node: ProjectNode;
  depth: number;
  childrenByPath: Record<string, ProjectNode[]>;
  onLoadChildren(path: string): void;
  onOpenFile(path: string): void;
  initiallyExpanded?: boolean;
};

function TreeNode({ node, depth, childrenByPath, onLoadChildren, onOpenFile, initiallyExpanded = false }: TreeNodeProps) {
  const [expanded, setExpanded] = useState(initiallyExpanded);
  const directory = node.kind !== 'file';
  const children = childrenByPath[node.path];

  useEffect(() => {
    if (expanded && directory && children === undefined) onLoadChildren(node.path);
  }, [children, directory, expanded, node.path, onLoadChildren]);

  const toggle = () => {
    if (!directory) {
      onOpenFile(node.path);
      return;
    }
    setExpanded(value => !value);
  };

  return <div className="tree-node" role="treeitem" aria-expanded={directory ? expanded : undefined}>
    <button type="button" className={`tree-row tree-${node.kind}`} onClick={toggle}
      style={{ paddingLeft: `${8 + depth * 15}px` }}>
      <span className={`tree-chevron ${directory && expanded ? 'is-open' : ''}`}>{directory && node.hasChildren ? '›' : ''}</span>
      <EyeCodeIcon name={treeIcon(node, expanded)} className="tree-icon" />
      <span className="tree-label">{node.name}</span>
    </button>
    {directory && expanded && children?.map(child => <TreeNode key={child.path} node={child} depth={depth + 1}
      childrenByPath={childrenByPath} onLoadChildren={onLoadChildren} onOpenFile={onOpenFile} />)}
  </div>;
}

function treeIcon(node: ProjectNode, expanded: boolean): string {
  if (node.kind === 'file') return node.name.endsWith('.java') ? 'java' : 'file';
  if (node.kind === 'project') return 'project';
  return expanded ? 'folderOpen' : 'folder';
}
