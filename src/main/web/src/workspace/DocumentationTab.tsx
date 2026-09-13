import { useEffect, useState } from 'react';
import type { DocumentSnapshot } from '../document/protocol';

type Props = {
  document: Omit<DocumentSnapshot, 'content'>;
};

export function DocumentationTab({ document }: Props) {
  const [failed, setFailed] = useState(false);
  const url = document.documentationUrl ?? '';

  useEffect(() => setFailed(false), [url]);

  if (!url || failed) {
    return <section className="documentation-viewer documentation-viewer-fallback">
      <strong>Não foi possível carregar esta documentação dentro da IDE.</strong>
      {url && <a href={url} target="_blank" rel="noreferrer">Abrir no navegador</a>}
    </section>;
  }

  return <section className="documentation-viewer">
    <iframe title={document.displayName} src={url} onError={() => setFailed(true)} />
  </section>;
}
