import { useEffect, useRef, useState } from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import type { RecentProject, RunState } from './protocol';
import { EyeCodeIcon } from './EyeCodeIcon';

type Props = {
  learnMode?: boolean;
  projectName?: string;
  projectPath?: string;
  branch?: string;
  recentProjects: RecentProject[];
  runState: RunState;
  runAvailable: boolean;
  onNewProject(): void;
  onOpenProject(): void;
  onNewFile(): void;
  onOpenRecentProject(path: string): void;
  onRemoveRecentProject(path: string): void;
  onWelcome(): void;
  onRun(): void;
  onRerun(): void;
  onStop(): void;
  onSelectConfiguration(id: string): void;
  onOpenSearch(): void;
  onOpenSettings(): void;
  onWindowAction(action: 'windowMinimize' | 'windowToggleMaximize' | 'windowClose'): void;
};

export function TopToolbar({ learnMode = false, projectName, projectPath, branch, recentProjects, runState, runAvailable, onNewProject, onOpenProject, onNewFile, onOpenRecentProject, onRemoveRecentProject, onWelcome, onRun, onRerun, onStop, onSelectConfiguration, onOpenSearch, onOpenSettings, onWindowAction }: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [switcherOpen, setSwitcherOpen] = useState(false);
  const [maximized, setMaximized] = useState(false);
  const dragPointer = useRef<number | null>(null);
  useEffect(() => {
    const electron = window.electronAPI;
    if (!electron) return;
    void electron.isMaximized().then(setMaximized);
    return electron.onMaximizeChange(setMaximized);
  }, []);
  const otherRecentProjects = recentProjects.filter(project => project.path !== projectPath);
  const startDrag = (event: React.PointerEvent<HTMLElement>) => {
    if ((event.target as HTMLElement).closest('button,a,input,select,textarea,[role="button"],[role="tab"]')) return;
    dragPointer.current = event.pointerId;
    event.currentTarget.setPointerCapture(event.pointerId);
    bridge.emit('native', 'windowDragStart', { screenX: event.screenX, screenY: event.screenY });
  };
  const moveDrag = (event: React.PointerEvent<HTMLElement>) => {
    if (dragPointer.current === event.pointerId) bridge.emit('native', 'windowDragMove', { screenX: event.screenX, screenY: event.screenY });
  };
  const endDrag = (event: React.PointerEvent<HTMLElement>) => {
    if (dragPointer.current !== event.pointerId) return;
    dragPointer.current = null;
    bridge.emit('native', 'windowDragEnd', {});
  };
  return <header className="app-toolbar" onPointerDown={startDrag} onPointerMove={moveDrag} onPointerUp={endDrag} onPointerCancel={endDrag}>
    <div className="toolbar-brand">
      <div className="toolbar-menu">
        <button type="button" className="toolbar-icon" aria-label="Main menu" onClick={() => setMenuOpen(value => !value)}>
          <EyeCodeIcon name="hamburger" />
        </button>
        {menuOpen && <div className="toolbar-menu-popover">
          <button type="button" onClick={() => { setMenuOpen(false); onNewProject(); }}>New Project</button>
          <button type="button" onClick={() => { setMenuOpen(false); onOpenProject(); }}>Open Project</button>
          <button type="button" onClick={() => { setMenuOpen(false); onNewFile(); }}>New Java File</button>
          {projectName && <button type="button" onClick={() => { setMenuOpen(false); onWelcome(); }}>Back to Welcome</button>}
        </div>}
      </div>
      <span className="brand-sign">EC</span>
      <div className="project-switcher-wrap">
        <button type="button" className="project-switcher" onClick={() => setSwitcherOpen(value => !value)} aria-expanded={switcherOpen}>
          <strong>{learnMode ? 'EyeCode Learn' : projectName || 'EyeCode Workspace'}</strong><span>⌄</span>
        </button>
        {switcherOpen && <div className="project-switcher-popover">
          {projectName && <div className="project-switcher-current"><strong>{projectName}</strong><span className="project-switcher-path">{projectPath}</span></div>}
          {otherRecentProjects.length > 0 && <div className="project-switcher-recent">
            <span>Recent Projects</span>
            {otherRecentProjects.map(project => <div className="project-switcher-recent-row" key={project.path}>
              <button type="button" onClick={() => { setSwitcherOpen(false); onOpenRecentProject(project.path); }}>
                <strong>{project.name}</strong><small className="project-switcher-path">{project.path}</small>
              </button>
              <button type="button" aria-label={`Remove ${project.name} from recent projects`} onClick={() => onRemoveRecentProject(project.path)}>Remove</button>
            </div>)}
          </div>}
          <div className="project-switcher-actions">
            <button type="button" onClick={() => { setSwitcherOpen(false); onNewProject(); }}>New Project</button>
            <button type="button" onClick={() => { setSwitcherOpen(false); onOpenProject(); }}>Open Project</button>
            {projectName && <button type="button" onClick={() => { setSwitcherOpen(false); onWelcome(); }}>Back to Welcome</button>}
          </div>
        </div>}
      </div>
      {!learnMode && <span className="toolbar-branch" title="Git branch (visual indicator)">
        <EyeCodeIcon name="branch" />
        <span>{branch ?? 'main'}</span>
      </span>}
    </div>
    <div className="toolbar-run-group">
      {learnMode && <span className="toolbar-mode-indicator" aria-label="Modo Aprender">Aprender <span aria-hidden="true">⌄</span></span>}
      {!learnMode && <EyeCodeIcon name="completion/class" className="toolbar-config-icon" />}
      {!learnMode && <select value={runState.selectedConfigurationId} onChange={event => onSelectConfiguration(event.target.value)}
        aria-label="Run configuration" disabled={!runState.configurations.length}>
        {runState.configurations.length === 0 && <option value="">No run configuration</option>}
        {runState.configurations.map(configuration => <option key={configuration.id} value={configuration.id}>
          {configuration.name}
        </option>)}
      </select>}
      <button type="button" className="toolbar-run" onClick={runState.running ? onRerun : onRun}
        disabled={runState.running ? !runState.rerunAvailable : !runAvailable}
        aria-label={runState.running ? learnMode ? 'Reexecutando...' : 'Rerun' : learnMode ? 'Executar' : 'Run'}
        title={runState.running ? 'Rerun' : 'Run'}>
        {runState.running ? <EyeCodeIcon name="reload" /> : <EyeCodeIcon name="run" />}
      </button>
      {!learnMode && <button type="button" className="toolbar-icon" disabled aria-label="Debug" title="Debug (em breve)"><EyeCodeIcon name="debug" /></button>}
      <button type="button" className="toolbar-icon stop" onClick={onStop} disabled={!runState.running} aria-label="Stop" title="Stop"><EyeCodeIcon name="stop" /></button>
      {!learnMode && <button type="button" className="toolbar-icon" disabled aria-label="Run options" title="Main class / JVM options (em breve)"><EyeCodeIcon name="kebab" /></button>}
    </div>
    <div className="toolbar-actions">
      <button type="button" className="toolbar-icon" onClick={onOpenSearch} aria-label="Search"><EyeCodeIcon name="search" /></button>
      <button type="button" className="toolbar-icon" disabled aria-label="Account" title="Account (em breve)"><EyeCodeIcon name="account" /></button>
      <button type="button" className="toolbar-icon" onClick={onOpenSettings} aria-label="Settings"><EyeCodeIcon name="settings" /></button>
      <span className="toolbar-separator" />
      <div className="toolbar-window-controls">
        <button type="button" className="toolbar-icon" onClick={() => onWindowAction('windowMinimize')} aria-label="Minimize"><EyeCodeIcon name="minimize" /></button>
        <button type="button" className="toolbar-icon" onClick={() => onWindowAction('windowToggleMaximize')} aria-label={maximized ? 'Restore' : 'Maximize'}><EyeCodeIcon name={maximized ? 'restore' : 'maximize'} /></button>
        <button type="button" className="toolbar-icon window-close" onClick={() => onWindowAction('windowClose')} aria-label="Close"><EyeCodeIcon name="close" /></button>
      </div>
    </div>
  </header>;
}
