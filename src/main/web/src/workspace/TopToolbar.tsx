import { useState } from 'react';
import type { RunState } from './protocol';
import { EyeCodeIcon } from './EyeCodeIcon';

type Props = {
  projectName?: string;
  runState: RunState;
  onOpenProject(): void;
  onNewFile(): void;
  onRun(): void;
  onRerun(): void;
  onStop(): void;
  onSelectConfiguration(id: string): void;
  onOpenSearch(): void;
  onOpenSettings(): void;
  onWindowAction(action: 'windowMinimize' | 'windowToggleMaximize' | 'windowClose'): void;
};

export function TopToolbar({ projectName, runState, onOpenProject, onNewFile, onRun, onRerun, onStop, onSelectConfiguration, onOpenSearch, onOpenSettings, onWindowAction }: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  return <header className="app-toolbar">
    <div className="toolbar-brand">
      <div className="toolbar-menu">
        <button type="button" className="toolbar-icon" aria-label="Main menu" onClick={() => setMenuOpen(value => !value)}>
          <EyeCodeIcon name="hamburger" />
        </button>
        {menuOpen && <div className="toolbar-menu-popover">
          <button type="button" onClick={() => { setMenuOpen(false); onOpenProject(); }}>Open Project</button>
          <button type="button" onClick={() => { setMenuOpen(false); onNewFile(); }}>New Java File</button>
        </div>}
      </div>
      <span className="brand-sign">EC</span>
      <button type="button" className="project-switcher" onClick={onOpenProject}>
        <strong>{projectName || 'EyeCode Workspace'}</strong><span>⌄</span>
      </button>
    </div>
    <div className="toolbar-run-group">
      <select value={runState.selectedConfigurationId} onChange={event => onSelectConfiguration(event.target.value)}
        aria-label="Run configuration" disabled={!runState.configurations.length}>
        {runState.configurations.length === 0 && <option value="">No run configuration</option>}
        {runState.configurations.map(configuration => <option key={configuration.id} value={configuration.id}>
          {configuration.name}
        </option>)}
      </select>
      <button type="button" className="toolbar-run" onClick={onRun} disabled={runState.running || !runState.configurations.length}>
        <EyeCodeIcon name="run" /> Run
      </button>
      <button type="button" className="toolbar-icon" onClick={onRerun} disabled={!runState.rerunAvailable} aria-label="Rerun"><EyeCodeIcon name="reload" /></button>
      <button type="button" className="toolbar-icon stop" onClick={onStop} disabled={!runState.running} aria-label="Stop"><EyeCodeIcon name="stop" /></button>
    </div>
    <div className="toolbar-actions">
      <button type="button" className="toolbar-icon" onClick={onOpenSearch} aria-label="Search"><EyeCodeIcon name="search" /></button>
      <button type="button" className="toolbar-icon" onClick={onOpenSettings} aria-label="Settings"><EyeCodeIcon name="settings" /></button>
      <span className="toolbar-separator" />
      <div className="toolbar-window-controls">
        <button type="button" className="toolbar-icon" onClick={() => onWindowAction('windowMinimize')} aria-label="Minimize"><EyeCodeIcon name="minimize" /></button>
        <button type="button" className="toolbar-icon" onClick={() => onWindowAction('windowToggleMaximize')} aria-label="Maximize or restore"><EyeCodeIcon name="maximize" /></button>
        <button type="button" className="toolbar-icon window-close" onClick={() => onWindowAction('windowClose')} aria-label="Close"><EyeCodeIcon name="close" /></button>
      </div>
    </div>
  </header>;
}
