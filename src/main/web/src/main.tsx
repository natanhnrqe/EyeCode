import { createRoot } from 'react-dom/client';

async function bootstrapLocalDevelopmentTransport(): Promise<void> {
  const backend = new URLSearchParams(window.location.search).get('backend');
  if (!backend) return;

  const response = await fetch(`${backend}/webshell/bootstrap`);
  if (!response.ok) throw new Error(`EyeCode development bootstrap failed: ${response.status}`);
  window.__EYECODE_LOCAL_TRANSPORT__ = await response.json();
}

async function start(): Promise<void> {
  await bootstrapLocalDevelopmentTransport();
  const { App } = await import('./App');
  createRoot(document.getElementById('root')!).render(<App />);
}

void start();
