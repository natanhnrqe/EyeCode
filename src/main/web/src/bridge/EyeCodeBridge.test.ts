// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { WebShellBridge, type ElectronWindowControls } from './EyeCodeBridge';

function mockElectron() {
  const controls = {
    minimize: vi.fn(),
    toggleMaximize: vi.fn(),
    close: vi.fn(),
    isMaximized: vi.fn().mockResolvedValue(false),
    onMaximizeChange: vi.fn().mockReturnValue(() => {}),
  } satisfies ElectronWindowControls;
  window.electronAPI = controls;
  return controls;
}

describe('WebShellBridge electron window-control routing', () => {
  beforeEach(() => {
    delete window.cefQuery;
    delete window.electronAPI;
    delete (window as unknown as Record<string, unknown>).__EYECODE_LOCAL_TRANSPORT__;
  });

  afterEach(() => {
    delete window.electronAPI;
  });

  it('routes native windowMinimize to the electron minimize operation', async () => {
    const electron = mockElectron();
    const bridge = new WebShellBridge();
    await expect(bridge.request('native', 'windowMinimize', {})).resolves.toEqual({ accepted: true });
    expect(electron.minimize).toHaveBeenCalledTimes(1);
    expect(electron.toggleMaximize).not.toHaveBeenCalled();
    expect(electron.close).not.toHaveBeenCalled();
  });

  it('routes native windowToggleMaximize to the electron toggle operation', async () => {
    const electron = mockElectron();
    const bridge = new WebShellBridge();
    await expect(bridge.request('native', 'windowToggleMaximize', {})).resolves.toEqual({ accepted: true });
    expect(electron.toggleMaximize).toHaveBeenCalledTimes(1);
    expect(electron.minimize).not.toHaveBeenCalled();
    expect(electron.close).not.toHaveBeenCalled();
  });

  it('routes native windowClose to the electron close operation', async () => {
    const electron = mockElectron();
    const bridge = new WebShellBridge();
    await expect(bridge.request('native', 'windowClose', {})).resolves.toEqual({ accepted: true });
    expect(electron.close).toHaveBeenCalledTimes(1);
    expect(electron.minimize).not.toHaveBeenCalled();
    expect(electron.toggleMaximize).not.toHaveBeenCalled();
  });

  it('does not intercept non-window native requests when electron is present', async () => {
    const electron = mockElectron();
    const bridge = new WebShellBridge();
    await expect(bridge.request('native', 'someOtherNativeOp', {})).rejects.toThrow('CEFFX bridge is unavailable');
    expect(electron.minimize).not.toHaveBeenCalled();
    expect(electron.toggleMaximize).not.toHaveBeenCalled();
    expect(electron.close).not.toHaveBeenCalled();
  });

  it('does not intercept non-native channels when electron is present', async () => {
    const electron = mockElectron();
    const bridge = new WebShellBridge();
    await expect(bridge.request('editor', 'documentRead', {})).rejects.toThrow('CEFFX bridge is unavailable');
    expect(electron.minimize).not.toHaveBeenCalled();
  });

  it('keeps the existing web behavior without the electron api', async () => {
    const bridge = new WebShellBridge();
    await expect(bridge.request('native', 'windowMinimize', {})).rejects.toThrow('CEFFX bridge is unavailable');
  });
});
