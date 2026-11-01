import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const electronDir = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(electronDir, '..');

function read(relative) {
  return fs.readFileSync(path.join(webRoot, relative), 'utf8');
}

describe('package.json packaging contract', () => {
  const pkg = JSON.parse(read('package.json'));

  it('pins electron:package:win to the packaging script', () => {
    expect(pkg.scripts['electron:package:win']).toContain('scripts/package-win.mjs');
  });

  it('offers linux and mac packaging scripts', () => {
    expect(pkg.scripts['electron:package:linux']).toContain('package-desktop.mjs');
    expect(pkg.scripts['electron:package:linux']).toContain('--platform linux');
    expect(pkg.scripts['electron:package:linux']).toContain('--arch x64');
    expect(pkg.scripts['electron:package:mac']).toContain('electron:package:mac:arm64');
    expect(pkg.scripts['electron:package:mac']).toContain('electron:package:mac:x64');
    expect(pkg.scripts['electron:package:mac:arm64']).toContain('--platform darwin');
    expect(pkg.scripts['electron:package:mac:arm64']).toContain('--arch arm64');
    expect(pkg.scripts['electron:package:mac:x64']).toContain('--platform darwin');
    expect(pkg.scripts['electron:package:mac:x64']).toContain('--arch x64');
  });

  it('keeps electron:dev and webshell:dist unchanged', () => {
    expect(pkg.scripts['electron:dev']).toBe(
      'npm run build && npm run webshell:dist && electron electron/main.js'
    );
    expect(pkg.scripts['webshell:dist']).toBe('mvn -f ../../../webshell-dist/pom.xml package');
  });

  it('declares the Electron main entry and productName', () => {
    expect(pkg.main).toBe('electron/main.js');
    expect(pkg.productName).toBe('EyeCode');
    expect(pkg.version).toMatch(/^\d+\.\d+\.\d+/);
  });
});

describe('forge.config.cjs', () => {
  const source = read('forge.config.cjs');

  it('targets the maker-zip maker for every desktop platform', () => {
    expect(source).toContain('@electron-forge/maker-zip');
    expect(source).toContain("platforms: ['win32', 'linux', 'darwin']");
  });

  it('bundles the WebShell distribution and staged JDK as extraResource', () => {
    expect(source).toContain('extraResource');
    expect(source).toContain("'webshell-dist', 'target', 'webshell-dist'");
    expect(source).toContain("'target', 'eyecode-desktop', 'runtime', 'jdk'");
  });

  it('packages only package.json and the electron directory into asar', () => {
    expect(source).toContain('asar: true');
    expect(source).toContain("/package.json");
    expect(source).toContain("/electron");
  });

  it('gates darwin electron zip reuse and overwrite behind explicit options', () => {
    expect(source).toContain('EYECODE_ELECTRON_ZIP_DIR');
    expect(source).toContain('electronZipDir');
    expect(source).toContain('overwrite: true');
    expect(source).not.toContain('tmpdir: false');
  });

  it('bypasses the darwin host symlink gate through a node require shim', () => {
    const script = read('scripts/package-desktop.mjs');
    expect(script).toContain('forge-symlink-gate-patch.cjs');
    expect(script).toContain('NODE_OPTIONS');
    const patch = read('scripts/forge-symlink-gate-patch.cjs');
    expect(patch).toContain('testSymlink');
    expect(patch).toContain('checkOverwrite');
    expect(patch).toContain('canCreateSymlinks = true');
  });

  it('keeps Electron 33.2.1 as the packaged runtime', () => {
    const pkg = JSON.parse(read('package.json'));
    expect(pkg.devDependencies.electron).toBe('33.2.1');
    expect(pkg.devDependencies['@electron-forge/cli']).toBe('7.9.0');
    expect(pkg.devDependencies['@electron-forge/maker-zip']).toBe('7.9.0');
  });
});

describe('scripts/package-desktop.mjs', () => {
  const source = read('scripts/package-desktop.mjs');

  it('runs the pipeline in the required order', () => {
    const build = source.indexOf("run('npm run build')");
    const dist = source.indexOf("run('npm run webshell:dist')");
    const stage = source.indexOf('run(`powershell');
    const make = source.indexOf('run(`npx electron-forge make');
    expect(build).toBeGreaterThan(-1);
    expect(dist).toBeGreaterThan(build);
    expect(stage).toBeGreaterThan(dist);
    expect(make).toBeGreaterThan(stage);
  });

  it('invokes forge for the requested platform and does not launch the app', () => {
    expect(source).toContain('--platform=${platform} --arch=${arch}');
    expect(source).not.toMatch(/electron electron\/main\.js/);
    expect(source).not.toMatch(/app\.launch/);
  });

  it('fails when required artifacts are missing', () => {
    expect(source).toContain('PACKAGE FAILED');
    expect(source).toContain('process.exit(1)');
    expect(source).toContain('eyecode-webshell.jar');
    expect(source).toContain('jdk-manifest.json');
  });

  it('writes a package manifest with electron, jdk, maven, jars and zip size', () => {
    expect(source).toContain('package-manifest.json');
    expect(source).toContain('archiveSha256');
    expect(source).toContain('archiveSha512');
    expect(source).toContain('sizeBytes');
    expect(source).toContain('runtimeJars');
  });

  it('applies POSIX exec bits for linux and darwin zips', () => {
    expect(source).toContain('applyPosixExecBits');
    expect(source).toContain('unixPermissions');
    expect(source).toContain('chrome-sandbox');
    expect(source).toContain('jspawnhelper');
    expect(source).toContain('chrome_crashpad_handler');
  });

  it('prepares a symlink-stripped electron zip for darwin builds', () => {
    expect(source).toContain('downloadArtifact');
    expect(source).toContain('stripElectronSymlinks');
    expect(source).toContain('EYECODE_ELECTRON_ZIP_DIR');
    expect(source).toContain('0o120000');
  });

  it('restores the darwin app prefix, modes and symlinks during finalize', () => {
    expect(source).toContain('finalizeDarwinZip');
    expect(source).toContain('EyeCode.app/');
    expect(source).toContain('darwinOriginalKey');
    expect(source).toContain('Electron Login Helper');
  });

  it('selects the maker zip by the exact app version', () => {
    expect(source).toContain('EyeCode-${platform}-${arch}-${webPkg.version}.zip');
  });
});

describe('scripts/package-win.mjs', () => {
  const source = read('scripts/package-win.mjs');

  it('delegates to the shared desktop pipeline for win32/x64', () => {
    expect(source).toContain("import { runPackage } from './package-desktop.mjs'");
    expect(source).toContain("platform: 'win32'");
    expect(source).toContain("arch: 'x64'");
  });
});

describe('electron main.js packaged mode', () => {
  const source = read('electron/main.js');

  it('passes resourcesPath into buildBackendLaunch', () => {
    expect(source).toContain('resourcesPath: process.resourcesPath');
  });

  it('reports packaged versus development mode', () => {
    expect(source).toContain('app.isPackaged');
  });

  it('keeps the frameless window usable on macOS via hiddenInset', () => {
    expect(source).toContain("titleBarStyle: 'hiddenInset'");
  });
});
