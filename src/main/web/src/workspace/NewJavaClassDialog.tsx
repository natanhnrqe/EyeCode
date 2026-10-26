import { useState } from 'react';
import { useModalDialog } from './modalDialog';

type Props = { onCancel(): void; onCreate(name: string): Promise<void> };

export function NewJavaClassDialog({ onCancel, onCreate }: Props) {
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const dialogRef = useModalDialog<HTMLFormElement>({ escapeEnabled: !creating, onClose: onCancel });

  async function submit() {
    setCreating(true);
    setError('');
    try {
      await onCreate(name);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : String(reason));
      setCreating(false);
    }
  }

  return <div className="new-project-backdrop" onPointerDown={() => { if (!creating) onCancel(); }}>
    <form ref={dialogRef} className="new-project-dialog" role="dialog" aria-modal="true" aria-labelledby="new-java-class-dialog-title" onPointerDown={event => event.stopPropagation()} onSubmit={event => { event.preventDefault(); void submit(); }}>
      <header><strong id="new-java-class-dialog-title">Nova Classe Java</strong><span>Crie uma classe na raiz do projeto</span></header>
      <label>Nome da classe<input autoFocus value={name} onChange={event => setName(event.target.value)} placeholder="Main" disabled={creating} /></label>
      {error && <p className="new-project-error" role="alert">{error}</p>}
      <footer><button type="button" onClick={onCancel} disabled={creating}>Cancelar</button><button type="submit" disabled={creating}>{creating ? 'Criando…' : 'Criar classe'}</button></footer>
    </form>
  </div>;
}
