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
    expect(pkg.scripts['electron:package:win']).toBe('node scripts/package-win.mjs');
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

  it('targets the maker-zip maker for win32 only', () => {
    expect(source).toContain('@electron-forge/maker-zip');
    expect(source).toContain("platforms: ['win32']");
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

  it('keeps Electron 33.2.1 as the packaged runtime', () => {
    const pkg = JSON.parse(read('package.json'));
    expect(pkg.devDependencies.electron).toBe('33.2.1');
    expect(pkg.devDependencies['@electron-forge/cli']).toBe('7.9.0');
    expect(pkg.devDependencies['@electron-forge/maker-zip']).toBe('7.9.0');
  });
});

describe('scripts/package-win.mjs', () => {
  const source = read(path.join('scripts', 'package-win.mjs'));

  it('runs the pipeline in the required order', () => {
    const build = source.indexOf("run('npm run build')");
    const dist = source.indexOf("run('npm run webshell:dist')");
    const stage = source.indexOf("run(`powershell");
    const make = source.indexOf("run('npx electron-forge make");
    expect(build).toBeGreaterThan(-1);
    expect(dist).toBeGreaterThan(build);
    expect(stage).toBeGreaterThan(dist);
    expect(make).toBeGreaterThan(stage);
  });

  it('invokes forge for win32 x64 only and does not launch the app', () => {
    expect(source).toContain('--platform=win32 --arch=x64');
    expect(source).not.toMatch(/electron electron\/main\.js/);
    expect(source).not.toMatch(/app\.launch/);
  });

  it('fails when required artifacts are missing', () => {
    expect(source).toContain('PACKAGE FAILED');
    expect(source).toContain('process.exit(1)');
    expect(source).toContain('eyecode-webshell.jar');
    expect(source).toContain('jdk-manifest.json');
  });

  it('writes a package manifest with electron, jdk, jars and zip size', () => {
    expect(source).toContain('package-manifest.json');
    expect(source).toContain('archiveSha256');
    expect(source).toContain('sizeBytes');
    expect(source).toContain('runtimeJars');
  });
});

describe('electron main.js packaged mode', () => {
  const source = read(path.join('electron', 'main.js'));

  it('passes resourcesPath into buildBackendLaunch', () => {
    expect(source).toContain('resourcesPath: process.resourcesPath');
  });

  it('reports packaged versus development mode', () => {
    expect(source).toContain('app.isPackaged');
  });
});
