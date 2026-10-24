import type { ReactNode } from 'react';
import { EyeCodeIcon } from './EyeCodeIcon';

type PanelEmptyStateAction = { label: string; run(): void };

type Props = {
  icon: string;
  title: string;
  description: string;
  meantime?: ReactNode[];
  action?: PanelEmptyStateAction;
  layout?: 'column' | 'row';
};

export function PanelEmptyState({ icon, title, description, meantime, action, layout = 'column' }: Props) {
  return <section className={`panel-empty-state is-${layout}`}>
    <span className="panel-empty-state-icon" aria-hidden="true"><EyeCodeIcon name={icon} /></span>
    <div className="panel-empty-state-copy">
      <strong>{title}</strong>
      <p>{description}</p>
      {meantime && meantime.length > 0 && <div className="panel-empty-state-meantime">
        <span className="panel-empty-state-meantime-label">Enquanto isso</span>
        <ul>{meantime.map((item, index) => <li key={index}>{item}</li>)}</ul>
      </div>}
    </div>
    {action && <button type="button" className="primary-action" onClick={action.run}>{action.label}</button>}
  </section>;
}
