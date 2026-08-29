import { useEffect, useRef } from 'react';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';

type Props = { service: MonacoWorkspaceService };

export function MonacoHost({ service }: Props) {
  const host = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!host.current) return;
    void service.mount(host.current);
    return () => service.dispose();
  }, [service]);

  return <div ref={host} className="monaco-host" />;
}
