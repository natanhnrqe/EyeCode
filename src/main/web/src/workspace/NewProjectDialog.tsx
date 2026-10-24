import { useEffect, useState } from 'react';

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

  useEffect(() => {
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape' && !creating) onCancel(); };
    window.addEventListener('keydown', close);
    return () => window.removeEventListener('keydown', close);
  }, [creating, onCancel]);

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
    <form className="new-project-dialog" onPointerDown={event => event.stopPropagation()} onSubmit={event => { event.preventDefault(); void submit(); }}>
      <header><strong>Criar novo projeto</strong><span>Projeto Java com Maven</span></header>
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
