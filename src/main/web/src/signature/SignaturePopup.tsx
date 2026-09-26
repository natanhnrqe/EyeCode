import { useLayoutEffect, useRef, useState } from 'react';
import type { SignatureInfo, SignaturePopupState } from './protocol';

type Segment = {
  text: string;
  active: boolean;
};

function decorateParameter(parameter: string, value: string): string {
  if (!value) return parameter;
  const space = parameter.lastIndexOf(' ');
  const type = space > 0 ? parameter.slice(0, space) : parameter;
  return `${type}: ${value}`;
}

function buildSegments(state: SignaturePopupState, signature: SignatureInfo): {
  name: string;
  segments: Segment[];
} {
  const label = signature.label;
  const open = label.indexOf('(');
  const name = open >= 0 ? label.slice(0, open) : label;
  const pairs = signature.parameters.map(parameter =>
    Array.isArray(parameter.label) ? parameter.label : null);

  if (!pairs.length || pairs.some(pair => pair === null)) {
    return {
      name,
      segments: [{ text: label.slice(open >= 0 ? open : 0), active: false }]
    };
  }

  const segments: Segment[] = [];
  let cursor = open >= 0 ? open : 0;
  pairs.forEach((pair, index) => {
    const [start, end] = pair as [number, number];
    if (start > cursor) segments.push({ text: label.slice(cursor, start), active: false });
    const value = state.argValues[index]?.trim() ?? '';
    segments.push({
      text: decorateParameter(label.slice(start, end), value),
      active: index === state.activeParameter
    });
    cursor = end;
  });
  if (cursor < label.length) segments.push({ text: label.slice(cursor), active: false });
  return { name, segments };
}

export function SignaturePopup({ state }: { state: SignaturePopupState }) {
  const popupRef = useRef<HTMLElement>(null);
  const [position, setPosition] = useState(state.anchor);
  const [placement, setPlacement] = useState<'above' | 'below' | null>(null);

  useLayoutEffect(() => {
    const popup = popupRef.current;
    if (!popup) return;
    const bounds = popup.getBoundingClientRect();
    const left = Math.min(
      Math.max(8, state.anchor.left),
      Math.max(8, window.innerWidth - bounds.width - 8)
    );
    if (placement === null) {
      setPlacement(
        state.anchor.top + bounds.height <= window.innerHeight - 8 ? 'below' : 'above'
      );
      return;
    }
    const top = placement === 'below'
      ? Math.min(state.anchor.top, Math.max(8, window.innerHeight - bounds.height - 8))
      : Math.max(8, state.anchor.top - bounds.height);
    setPosition({ left, top });
  }, [placement, state.anchor, state.signatures]);

  const signature = state.signatures[state.activeSignature] ?? state.signatures[0];
  if (!signature) return null;

  const { name, segments } = buildSegments(state, signature);

  return (
    <section
      ref={popupRef}
      className="signature-popup"
      style={{ left: position.left, top: position.top }}
      role="tooltip"
      aria-label="Signature help"
    >
      <div className="signature-line">
        <span className="signature-name">{name}</span>
        <span className="signature-params">
          {segments.map((segment, index) => (
            <span
              key={index}
              className={segment.active ? 'signature-segment active' : 'signature-segment'}
            >
              {segment.text}
            </span>
          ))}
        </span>
        {state.signatures.length > 1 && (
          <span className="signature-count">
            {state.activeSignature + 1} / {state.signatures.length}
          </span>
        )}
      </div>
    </section>
  );
}
