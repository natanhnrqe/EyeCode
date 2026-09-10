import { createRoot } from 'react-dom/client';
import { App } from './App';
import { installWebMouseDiagnostic } from './diagnostics/WebMouseDiagnostic';

installWebMouseDiagnostic();
createRoot(document.getElementById('root')!).render(<App />);
