import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

import { bridge } from '../bridge/EyeCodeBridge';
import type { TerminalState } from './protocol';

type Props = {
  state: TerminalState;
};

export function TerminalPanel({ state }: Props) {
  const host = useRef<HTMLElement>(null);
  const terminalRef = useRef<Terminal | null>(null);
  const fitAddonRef = useRef<FitAddon | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const [endpoint, setEndpoint] = useState('');

  useEffect(() => {
    void bridge.request<TerminalState>('terminal', 'show', {})
      .then(status => setEndpoint(status.endpoint ?? ''));
    return () => { void bridge.emit('terminal', 'hide', {}); };
  }, []);

  useEffect(() => {
    setEndpoint(state.endpoint ?? '');
  }, [state.endpoint]);

  useLayoutEffect(() => {
    const container = host.current;
    if (!container) return;

    const terminalSurface = window.getComputedStyle(container).getPropertyValue('--surface-editor').trim() || '#1a1b1d';
    const terminal = new Terminal({
      cursorBlink: true,
      convertEol: true,
      fontFamily: 'JetBrains Mono, monospace',
      fontSize: 13,
      theme: {
        background: terminalSurface,
        foreground: '#bbbfc8',
        cursor: '#e8e9ee',
        selectionBackground: '#6352b855',
      },
    });
    const fitAddon = new FitAddon();
    terminal.loadAddon(fitAddon);
    terminal.open(container);
    terminalRef.current = terminal;
    fitAddonRef.current = fitAddon;

    let resizeTimer: number | null = null;
    const resize = () => {
      fitAddon.fit();
      if (resizeTimer !== null) window.clearTimeout(resizeTimer);
      resizeTimer = window.setTimeout(() => {
        void bridge.emit('terminal', 'resize', { cols: terminal.cols, rows: terminal.rows });
      }, 40);
    };
    const observer = new ResizeObserver(resize);
    observer.observe(container);
    const input = terminal.onData(data => {
      const socket = socketRef.current;
      if (socket?.readyState === WebSocket.OPEN) socket.send(new TextEncoder().encode(data));
    });

    requestAnimationFrame(resize);
    return () => {
      observer.disconnect();
      input.dispose();
      if (resizeTimer !== null) window.clearTimeout(resizeTimer);
      socketRef.current?.close();
      socketRef.current = null;
      fitAddonRef.current = null;
      terminalRef.current = null;
      terminal.dispose();
    };
  }, []);

  useEffect(() => {
    const terminal = terminalRef.current;
    if (!terminal || !endpoint) return;

    socketRef.current?.close();
    const socket = new WebSocket(endpoint);
    socket.binaryType = 'arraybuffer';
    socketRef.current = socket;
    socket.onopen = () => {
      fitAddonRef.current?.fit();
      void bridge.emit('terminal', 'resize', { cols: terminal.cols, rows: terminal.rows });
      terminal.focus();
    };
    socket.onmessage = event => {
      if (event.data instanceof ArrayBuffer) terminal.write(new Uint8Array(event.data));
    };
    return () => {
      if (socketRef.current === socket) socketRef.current = null;
      socket.close();
    };
  }, [endpoint]);

  return <section ref={host} className="terminal-native-host" aria-label="Terminal" />;
}