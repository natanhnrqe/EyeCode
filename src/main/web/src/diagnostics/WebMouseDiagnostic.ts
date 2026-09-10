let movesToLog = 0;

export function installWebMouseDiagnostic(): void {
  const targetName = (target: EventTarget | null): string => {
    if (!(target instanceof Element)) return 'unknown';
    return target.className ? `${target.tagName.toLowerCase()}.${String(target.className)}` : target.tagName.toLowerCase();
  };

  const report = (event: MouseEvent | PointerEvent): void => {
    const pointer = 'pointerId' in event ? ` pointerId=${event.pointerId}` : '';
    console.log(`[WEB-MOUSE] ${event.type} button=${event.button} buttons=${event.buttons}${pointer} target=${targetName(event.target)} x=${event.clientX} y=${event.clientY}`);
  };

  const onPointerEvent = (event: PointerEvent): void => {
    if (event.type === 'pointerdown' || event.type === 'pointerup' || event.type === 'pointercancel') {
      movesToLog = 3;
      report(event);
      return;
    }
    if (movesToLog-- > 0) report(event);
  };

  const onMouseEvent = (event: MouseEvent): void => {
    if (event.type === 'mousedown' || event.type === 'mouseup') {
      movesToLog = 3;
      report(event);
      return;
    }
    if (movesToLog-- > 0) report(event);
  };

  document.addEventListener('pointerdown', onPointerEvent, true);
  document.addEventListener('pointerup', onPointerEvent, true);
  document.addEventListener('pointercancel', onPointerEvent, true);
  document.addEventListener('pointermove', onPointerEvent, true);
  document.addEventListener('mousedown', onMouseEvent, true);
  document.addEventListener('mouseup', onMouseEvent, true);
  document.addEventListener('mousemove', onMouseEvent, true);
}
