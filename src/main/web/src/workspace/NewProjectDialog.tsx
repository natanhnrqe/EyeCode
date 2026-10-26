import { useState } from 'react';
import { useModalDialog } from './modalDialog';

type Props = {
  onCancel(): void;
  onBrowse(): Promise<string | undefined>;
  onCreate(request: { name: string; location: string; groupId: string }): Promise<void>;
};

export function NewProjectDialog({ onCancel, onBrowse, onCreate }: Props) {
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [groupId, setGroupId] = useState('com.example');
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const dialogRef = useModalDialog<HTMLFormElement>({ escapeEnabled: !creating, onClose: onCancel });

  async function browse() {
    try {
      const selected = await onBrowse();
      if (selected) setLocation(selected);
    } catch (reason) {
      setError(messageFor(reason));
    }
  }

  async function submit() {
    setCreating(true);
    setError('');
    try {
      await onCreate({ name, location, groupId });
    } catch (reason) {
      setError(messageFor(reason));
      setCreating(false);
    }
  }

  return <div className="new-project-backdrop" onPointerDown={() => { if (!creating) onCancel(); }}>
    <form ref={dialogRef} className="new-project-dialog" role="dialog" aria-modal="true" aria-labelledby="new-project-dialog-title" onPointerDown={event => event.stopPropagation()} onSubmit={event => { event.preventDefault(); void submit(); }}>
      <header><strong id="new-project-dialog-title">Criar novo projeto</strong><span>Projeto Java com Maven</span></header>
      <label>Nome do projeto<input autoFocus value={name} onChange={event => setName(event.target.value)} placeholder="my-project" disabled={creating} /></label>
      <label>Local<span className="new-project-location"><input value={location} onChange={event => setLocation(event.target.value)} placeholder="Escolha uma pasta" disabled={creating} /><button type="button" onClick={() => void browse()} disabled={creating}>Escolher pasta</button></span></label>
      <label>Group ID<input value={groupId} onChange={event => setGroupId(event.target.value)} placeholder="com.example" disabled={creating} /></label>
      <p className="new-project-build">Maven · Java 21</p>
      {error && <p className="new-project-error" role="alert">{error}</p>}
      <footer><button type="button" onClick={onCancel} disabled={creating}>Cancelar</button><button type="submit" disabled={creating}>{creating ? 'Criando…' : 'Criar projeto'}</button></footer>
    </form>
  </div>;
}

function messageFor(reason: unknown): string {
  if (reason && typeof reason === 'object' && 'message' in reason) return String(reason.message);
  return reason instanceof Error ? reason.message : String(reason);
}
