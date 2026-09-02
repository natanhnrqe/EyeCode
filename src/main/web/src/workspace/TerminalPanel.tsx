import { useLayoutEffect, useRef } from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import type { TerminalOutput, TerminalState } from './protocol';

type Props = { output: TerminalOutput[]; state: TerminalState; onStart(): void; onRestart(): void; onStop(): void; onInput(data: string): void; };

export function TerminalPanel(_: Props) {
  const host = useRef<HTMLElement>(null);
  useLayoutEffect(() => {
    const publish = () => {
      const bounds = host.current?.getBoundingClientRect();
      if (!bounds) return;
      bridge.emit('terminal', 'layout', { x: bounds.left, y: bounds.top, width: bounds.width, height: bounds.height, viewportWidth: window.innerWidth });
    };
    void bridge.emit('terminal', 'show', {});
    publish();
    const observer = new ResizeObserver(publish);
    if (host.current) observer.observe(host.current);
    window.addEventListener('resize', publish);
    return () => { observer.disconnect(); window.removeEventListener('resize', publish); void bridge.emit('terminal', 'hide', {}); };
  }, []);
  return <section ref={host} className="terminal-native-host" aria-label="Terminal" />;
}
