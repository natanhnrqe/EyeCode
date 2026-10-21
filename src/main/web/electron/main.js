import { app, BrowserWindow, dialog, shell, Menu, ipcMain } from 'electron';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { buildBackendLaunch, parseReadinessUrl, STARTUP_TIMEOUT_MS } from './backendLaunch.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '..', '..', '..', '..');

let mainWindow = null;
let backendProcess = null;
let backendUrl = null;

function startBackend() {
  return new Promise((resolve, reject) => {
    let launch;
    try {
      launch = buildBackendLaunch({ repoRoot, resourcesPath: process.resourcesPath });
    } catch (error) {
      reject(error);
      return;
    }

    console.log(`[Electron] Starting Java backend from ${launch.distDir}`);

    backendProcess = spawn(launch.command, launch.args, {
      cwd: launch.cwd,
      env: process.env,
      detached: process.platform !== 'win32',
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    let stdoutBuffer = '';
    let stderrBuffer = '';
    let resolved = false;

    const timeout = setTimeout(() => {
      if (!resolved) {
        resolved = true;
        killBackend();
        reject(new Error(`Backend startup timed out after ${STARTUP_TIMEOUT_MS / 1000} seconds.`));
      }
    }, STARTUP_TIMEOUT_MS);

    backendProcess.stdout.on('data', (data) => {
      const text = data.toString();
      stdoutBuffer += text;
      process.stdout.write(text);

      const url = parseReadinessUrl(stdoutBuffer);
      if (url && !resolved) {
        resolved = true;
        clearTimeout(timeout);
        backendUrl = url;
        console.log(`[Electron] Backend ready at ${backendUrl}`);
        resolve(backendUrl);
      }
    });

    backendProcess.stderr.on('data', (data) => {
      const text = data.toString();
      stderrBuffer += text;
      process.stderr.write(text);
    });

    backendProcess.on('error', (error) => {
      if (!resolved) {
        resolved = true;
        clearTimeout(timeout);
        reject(new Error(
          `Failed to start Java backend: ${error.message}\n\n` +
          `Command:\n  ${launch.command} ${launch.args.join(' ')}`
        ));
      }
    });

    backendProcess.on('exit', (code) => {
      if (!resolved) {
        resolved = true;
        clearTimeout(timeout);
        reject(new Error(
          `Backend exited with code ${code}.\n\n` +
          `stdout:\n${stdoutBuffer.slice(-2000)}\n\n` +
          `stderr:\n${stderrBuffer.slice(-2000)}`
        ));
      } else {
        console.log(`[Electron] Backend process exited with code ${code}`);
        if (mainWindow && !mainWindow.isDestroyed()) {
          mainWindow.close();
        }
      }
    });
  });
}

function killBackend() {
  if (backendProcess && !backendProcess.killed) {
    console.log('[Electron] Stopping backend...');
    if (process.platform === 'win32') {
      spawn('taskkill', ['/pid', String(backendProcess.pid), '/T', '/F'], {
        stdio: 'ignore',
        windowsHide: true,
      });
    } else {
      try {
        process.kill(-backendProcess.pid, 'SIGTERM');
      } catch {
        backendProcess.kill('SIGTERM');
      }
    }
    backendProcess = null;
  }
}

function registerWindowIpc(win) {
  const trustedSender = (event) => event.sender === win.webContents;

  ipcMain.on('window:minimize', (event) => {
    if (!trustedSender(event)) return;
    win.minimize();
  });

  ipcMain.on('window:toggle-maximize', (event) => {
    if (!trustedSender(event)) return;
    if (win.isMaximized()) win.unmaximize();
    else win.maximize();
  });

  ipcMain.on('window:close', (event) => {
    if (!trustedSender(event)) return;
    win.close();
  });

  ipcMain.handle('window:is-maximized', (event) => {
    if (!trustedSender(event)) return false;
    return win.isMaximized();
  });

  const notifyMaximizeState = () => {
    if (!win.isDestroyed()) {
      win.webContents.send('window:maximized-changed', win.isMaximized());
    }
  };
  win.on('maximize', notifyMaximizeState);
  win.on('unmaximize', notifyMaximizeState);
}

function createWindow(url) {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    title: 'EyeCode',
    show: false,
    frame: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webSecurity: true,
      preload: path.join(__dirname, 'preload.cjs'),
    },
  });

  registerWindowIpc(mainWindow);

  mainWindow.webContents.on('preload-error', (_event, preloadPath, error) => {
    console.error(`[Electron] Preload failed: ${preloadPath}`, error);
  });

  const showTimeout = setTimeout(() => {
    if (mainWindow && !mainWindow.isVisible()) {
      mainWindow.show();
    }
  }, 10000);

  mainWindow.once('ready-to-show', () => {
    clearTimeout(showTimeout);
    mainWindow.show();
  });

  mainWindow.webContents.setWindowOpenHandler(({ url: targetUrl }) => {
    if (targetUrl.startsWith('https://')) {
      shell.openExternal(targetUrl);
    }
    return { action: 'deny' };
  });

  mainWindow.webContents.on('will-navigate', (event, targetUrl) => {
    const backendOrigin = new URL(url).origin;
    const targetOrigin = new URL(targetUrl).origin;

    if (targetOrigin !== backendOrigin) {
      event.preventDefault();
      if (targetUrl.startsWith('https://') || targetUrl.startsWith('http://')) {
        shell.openExternal(targetUrl);
      }
    }
  });

  mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
    if (errorCode !== -3) {
      clearTimeout(showTimeout);
      mainWindow.show();
      dialog.showErrorBox(
        'EyeCode - Load Failed',
        `Failed to load the application:\n${errorDescription} (code ${errorCode})\n\nURL: ${url}`
      );
    }
  });

  mainWindow.loadURL(url);

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(async () => {
  Menu.setApplicationMenu(null);
  console.log(`[Electron] EyeCode Desktop (${app.isPackaged ? 'packaged' : 'development'} mode)`);
  try {
    const url = await startBackend();
    createWindow(url);
  } catch (error) {
    console.error('[Electron] Startup failed:', error.message);
    dialog.showErrorBox('EyeCode - Startup Failed', error.message);
    app.quit();
  }
});

app.on('window-all-closed', () => {
  killBackend();
  app.quit();
});

app.on('before-quit', () => {
  killBackend();
});
