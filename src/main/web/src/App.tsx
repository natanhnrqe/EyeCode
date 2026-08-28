import { useEffect, useState } from 'react';
import { bridge } from './bridge/EyeCodeBridge';
import type { ShellBootstrap, WebShellEnvelope } from './bridge/protocol';
import './styles.css';

export function App() {
  const [connected, setConnected] = useState(false);
  const [bootstrap, setBootstrap] = useState<ShellBootstrap | null>(null);
  const [lastResponse, setLastResponse] = useState('No request sent');

  useEffect(() => {
    const unsubscribe = bridge.subscribe((message: WebShellEnvelope) => {
      if (message.channel === 'shell' && message.name === 'bootstrap') {
        setBootstrap(message.payload as ShellBootstrap);
        setConnected(true);
      }
    });
    bridge.emit('shell', 'ready', {});
    return unsubscribe;
  }, []);

  async function ping() {
    try {
      const response = await bridge.request<{ message: string }>('shell', 'ping', {});
      setLastResponse(response.message);
    } catch (error) {
      setLastResponse(error instanceof Error ? error.message : String(error));
    }
  }

  return (
    <main className="shell-diagnostic">
      <section className="diagnostic-card">
        <div className="eyebrow">EyeCode / Web Shell</div>
        <h1>Web Shell foundation</h1>
        <p className="lede">Phase 0 connectivity surface for the future workspace.</p>
        <dl className="status-grid">
          <div><dt>Status</dt><dd className={connected ? 'online' : 'pending'}>{connected ? 'connected' : 'connecting'}</dd></div>
          <div><dt>Protocol</dt><dd>eyecode.web/1</dd></div>
          <div><dt>Java bridge</dt><dd>{connected ? 'ready' : 'pending'}</dd></div>
          <div><dt>Mode</dt><dd>{bootstrap?.webShellMode ?? 'WEB_SHELL'}</dd></div>
        </dl>
        <button type="button" onClick={ping}>Ping Java</button>
        <p className="response-label">Last response</p>
        <output>{lastResponse}</output>
      </section>
    </main>
  );
}
