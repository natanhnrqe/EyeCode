type Props = { connected: boolean; activeDocument?: string; message?: string };

export function StatusBar({ connected, activeDocument, message }: Props) {
  return <footer className="status-bar">
    <span className={connected ? 'status-connection is-online' : 'status-connection'}>
      <i /> {connected ? 'Connected' : 'Connecting'}
    </span>
    <span className="status-path">{message || activeDocument || 'No active document'}</span>
    <span>Java 21</span><span>UTF-8</span><span>LF</span><span>Spaces: 4</span>
  </footer>;
}
