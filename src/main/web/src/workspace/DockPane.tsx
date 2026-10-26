import type { ReactNode } from 'react';
import type { WorkspacePaneId } from './WorkspacePane';

type Props = {
  paneId: WorkspacePaneId;
  children: ReactNode;
  className?: string;
  label?: string;
  header?: ReactNode;
  headerClassName?: string;
  headerLabel?: string;
  bodyClassName?: string;
  bodyRole?: 'tabpanel';
  bodyId?: string;
  bodyLabelledBy?: string;
  footer?: ReactNode;
  footerClassName?: string;
  dragHandleOnly?: boolean;
};

export function DockPane({ paneId, children, className, label, header, headerClassName, headerLabel, bodyClassName, bodyRole, bodyId, bodyLabelledBy, footer, footerClassName, dragHandleOnly = false }: Props) {
  return <section className={joinClasses('dock-pane', className)} data-pane-id={paneId} aria-label={label}>
    {header && <header className={joinClasses('dock-pane-header', headerClassName)} data-dock-handle={dragHandleOnly ? undefined : ''} aria-label={headerLabel}>{header}</header>}
    <div className={joinClasses('dock-pane-body', bodyClassName)} role={bodyRole} id={bodyId} aria-labelledby={bodyLabelledBy}>{children}</div>
    {footer && <footer className={joinClasses('dock-pane-footer', footerClassName)}>{footer}</footer>}
  </section>;
}

function joinClasses(...classes: Array<string | undefined>) {
  return classes.filter(Boolean).join(' ');
}
