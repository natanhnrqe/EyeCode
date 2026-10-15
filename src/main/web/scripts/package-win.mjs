import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const webRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = path.resolve(webRoot, '..', '..', '..');

const DIST_DIR = path.join(repoRoot, 'webshell-dist', 'target', 'webshell-dist');
const STAGED_JDK = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');
const JDK_MANIFEST = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk-manifest.json');
const STAGE_SCRIPT = path.join(repoRoot, 'scripts', 'stage-eyecode-jdk.ps1');
const OUT_DIR = path.join(webRoot, 'out');
const PACKAGE_MANIFEST = path.join(repoRoot, 'target', 'eyecode-desktop', 'package-manifest.json');

function step(label) {
  console.log(`\n=== ${label} ===`);
}

function run(command, options = {}) {
  console.log(`> ${command}`);
  execSync(command, { stdio: 'inherit', cwd: webRoot, shell: true, ...options });
}

function fail(message) {
  console.error(`\nPACKAGE FAILED: ${message}`);
  process.exit(1);
}

function isUsableJdkHome(home) {
  return (
    fs.existsSync(path.join(home, 'bin', 'java.exe')) &&
    fs.existsSync(path.join(home, 'bin', 'javac.exe'))
  );
}

step('1/6 Build production frontend');
run('npm run build');
if (!fs.existsSync(path.join(repoRoot, 'src', 'main', 'resources', 'webshell', 'index.html'))) {
  fail('Frontend build did not produce src/main/resources/webshell/index.html');
}

step('2/6 Build isolated WebShell distribution');
run('npm run webshell:dist');
if (!fs.existsSync(path.join(DIST_DIR, 'eyecode-webshell.jar'))) {
  fail(`WebShell distribution jar missing at ${DIST_DIR}\\eyecode-webshell.jar`);
}
const libJars = fs.readdirSync(path.join(DIST_DIR, 'lib')).filter((n) => n.endsWith('.jar'));
if (libJars.length === 0) {
  fail(`WebShell distribution lib directory has no jars at ${path.join(DIST_DIR, 'lib')}`);
}
console.log(`WebShell runtime jars: ${libJars.length}`);

step('3/6 Stage and verify pinned JDK');
if (isUsableJdkHome(STAGED_JDK) && fs.existsSync(JDK_MANIFEST)) {
  console.log(`Staged JDK already valid: ${STAGED_JDK}`);
} else {
  run(`powershell -NoProfile -ExecutionPolicy Bypass -File "${STAGE_SCRIPT}"`);
}
if (!isUsableJdkHome(STAGED_JDK)) {
  fail(`Staged JDK is not usable at ${STAGED_JDK}. Run scripts/stage-eyecode-jdk.ps1 manually.`);
}
if (!fs.existsSync(JDK_MANIFEST)) {
  fail(`JDK manifest missing at ${JDK_MANIFEST}.`);
}

step('4/6 Verify package resources (extraResource sources)');
console.log(`webshell-dist: ${DIST_DIR}`);
console.log(`jdk:           ${STAGED_JDK}`);

step('5/6 Electron Forge make (win32 x64 ZIP)');
run('npx electron-forge make --platform=win32 --arch=x64');

step('6/6 Report ZIP and write package manifest');
const makeZipRoot = path.join(OUT_DIR, 'make', 'zip', 'win32', 'x64');
if (!fs.existsSync(makeZipRoot)) {
  fail(`Maker-zip output directory missing: ${makeZipRoot}`);
}
const zips = fs
  .readdirSync(makeZipRoot)
  .filter((n) => n.endsWith('.zip'))
  .map((n) => path.join(makeZipRoot, n));
if (zips.length === 0) {
  fail(`No ZIP produced under ${makeZipRoot}`);
}
zips.sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs);
const zipPath = zips[0];
const zipSize = fs.statSync(zipPath).size;

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8').replace(/^﻿/, ''));
}

const jdkManifest = readJson(JDK_MANIFEST);
const electronPkg = readJson(
  path.join(webRoot, 'node_modules', 'electron', 'package.json')
);
const webPkg = readJson(path.join(webRoot, 'package.json'));

const manifest = {
  schema: 'eyecode-desktop-package-manifest/1',
  createdAtUtc: new Date().toISOString(),
  electron: {
    version: electronPkg.version,
    platform: 'win32',
    arch: 'x64',
  },
  application: {
    name: webPkg.productName || webPkg.name,
    version: webPkg.version,
  },
  jdk: {
    vendor: jdkManifest.vendor,
    version: jdkManifest.version,
    architecture: jdkManifest.architecture,
    archiveSha256: jdkManifest.archiveSha256,
    downloadUrl: jdkManifest.downloadUrl,
    licenseNotice: jdkManifest.licenseNotice,
  },
  webshell: {
    jar: 'webshell-dist/eyecode-webshell.jar',
    runtimeJars: libJars.slice().sort(),
  },
  zip: {
    path: zipPath,
    sizeBytes: zipSize,
  },
};

fs.mkdirSync(path.dirname(PACKAGE_MANIFEST), { recursive: true });
fs.writeFileSync(PACKAGE_MANIFEST, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');

console.log(`\nZIP:  ${zipPath}`);
console.log(`Size: ${zipSize} bytes (${(zipSize / (1024 * 1024)).toFixed(1)} MiB)`);
console.log(`Manifest: ${PACKAGE_MANIFEST}`);
