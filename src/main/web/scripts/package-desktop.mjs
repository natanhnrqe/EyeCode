import { execSync } from 'node:child_process';
import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import JSZip from 'jszip';

const require = createRequire(import.meta.url);
const { downloadArtifact } = require('@electron/get');

const webRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = path.resolve(webRoot, '..', '..', '..');

const DIST_DIR = path.join(repoRoot, 'webshell-dist', 'target', 'webshell-dist');
const STAGED_JDK = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');
const JDK_MANIFEST = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk-manifest.json');
const STAGE_SCRIPT = path.join(repoRoot, 'scripts', 'stage-eyecode-jdk.ps1');
const STAGED_MAVEN = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'maven');
const MAVEN_MANIFEST = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'maven-manifest.json');
const STAGE_MAVEN_SCRIPT = path.join(repoRoot, 'scripts', 'stage-eyecode-maven.ps1');
const THIRD_PARTY_NOTICES = path.join(repoRoot, 'third-party-notices', 'THIRD-PARTY-NOTICES.md');
const EYE_CODE_LICENSE = path.join(repoRoot, 'LICENSE');
const OUT_DIR = path.join(webRoot, 'out');
const PACKAGE_MANIFEST = path.join(repoRoot, 'target', 'eyecode-desktop', 'package-manifest.json');
const ELECTRON_ZIP_DIR = path.join(repoRoot, 'target', 'eyecode-desktop', 'electron-zips');

const POSIX_EXEC_PATTERNS = [
  /^EyeCode$/,
  /^EyeCode\.app\/Contents\/MacOS\/EyeCode$/,
  /^chrome-sandbox$/,
  /^chrome_crashpad_handler$/,
  /^libEGL\.so$/,
  /^libGLESv2\.so$/,
  /^libffmpeg\.so$/,
  /^libvulkan\.so\.1$/,
  /^libvk_swiftshader\.so$/,
  /^resources\/jdk\/bin\/[^/]+$/,
  /^resources\/jdk\/lib\/jspawnhelper$/,
  /^resources\/maven\/bin\/[^/]+$/,
  /^EyeCode\.app\/Contents\/Resources\/jdk\/bin\/[^/]+$/,
  /^EyeCode\.app\/Contents\/Resources\/jdk\/lib\/jspawnhelper$/,
  /^EyeCode\.app\/Contents\/Resources\/jdk\/lib\/.*\.dylib$/,
  /^EyeCode\.app\/Contents\/Resources\/maven\/bin\/[^/]+$/,
];

function parseTarget() {
  const argv = process.argv.slice(2);
  let platform = 'win32';
  let arch = 'x64';
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === '--platform') platform = argv[++i];
    else if (argv[i] === '--arch') arch = argv[++i];
    else if (argv[i].startsWith('--platform=')) platform = argv[i].slice('--platform='.length);
    else if (argv[i].startsWith('--arch=')) arch = argv[i].slice('--arch='.length);
  }
  const supported = {
    win32: ['x64'],
    linux: ['x64'],
    darwin: ['x64', 'arm64'],
  };
  if (!supported[platform] || !supported[platform].includes(arch)) {
    throw new Error(`Unsupported target ${platform}/${arch}. Supported: ${Object.entries(supported).map(([p, as]) => as.map((a) => `${p}/${a}`).join(', ')).join('; ')}`);
  }
  return { platform, arch };
}

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

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
}

function javaBinaryName(platform) {
  return platform === 'win32' ? 'java.exe' : 'java';
}

function javacBinaryName(platform) {
  return platform === 'win32' ? 'javac.exe' : 'javac';
}

function isUsableJdkHome(home, platform) {
  return (
    fs.existsSync(path.join(home, 'bin', javaBinaryName(platform))) &&
    fs.existsSync(path.join(home, 'bin', javacBinaryName(platform)))
  );
}

function stagedJdkMatches(platform, arch) {
  if (!isUsableJdkHome(STAGED_JDK, platform) || !fs.existsSync(JDK_MANIFEST)) return false;
  try {
    const manifest = readJson(JDK_MANIFEST);
    return manifest.platform === platform && manifest.architecture === arch;
  } catch {
    return false;
  }
}

function isUsableMavenHome(home) {
  return (
    fs.existsSync(path.join(home, 'bin', 'mvn.cmd')) &&
    fs.existsSync(path.join(home, 'bin', 'mvn')) &&
    fs.existsSync(path.join(home, 'conf', 'settings.xml'))
  );
}

export async function ensureElectronSourceZip(version, platform, arch) {
  const zipPath = await downloadArtifact({ artifactName: 'electron', version, platform, arch });
  if (!zipPath || !fs.existsSync(zipPath)) {
    fail(`Electron ${version} ${platform}/${arch} ZIP not available at ${zipPath}`);
  }
  console.log(`Electron source ZIP: ${zipPath}`);
  return zipPath;
}

export async function readElectronZipMetadata(sourceZipPath) {
  const zip = await JSZip.loadAsync(fs.readFileSync(sourceZipPath));
  const modes = new Map();
  const symlinks = [];
  for (const entry of Object.values(zip.files)) {
    if (entry.dir) continue;
    const name = entry.name.replace(/\\/g, '/');
    const mode = entry.unixPermissions == null ? null : entry.unixPermissions & 0xFFFF;
    if (mode != null) modes.set(name, mode);
    if (mode != null && (mode & 0o170000) === 0o120000) {
      symlinks.push({ name, target: await entry.async('string'), mode });
    }
  }
  console.log(`Electron ZIP metadata: ${modes.size} file modes, ${symlinks.length} symlinks`);
  return { modes, symlinks };
}

export async function stripElectronSymlinks(sourceZipPath, outPath, expectedSymlinkCount) {
  const zip = await JSZip.loadAsync(fs.readFileSync(sourceZipPath));
  let removed = 0;
  for (const entry of Object.values(zip.files)) {
    if (entry.dir) continue;
    const mode = entry.unixPermissions == null ? 0 : entry.unixPermissions & 0xFFFF;
    if ((mode & 0o170000) === 0o120000) {
      zip.remove(entry.name);
      removed += 1;
    }
  }
  if (removed === 0 || removed !== expectedSymlinkCount) {
    fail(`Symlink strip mismatch for ${sourceZipPath}: removed ${removed}, expected ${expectedSymlinkCount}.`);
  }
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  const tmpPath = `${outPath}.tmp`;
  const output = await zip.generateAsync({
    type: 'nodebuffer',
    platform: 'UNIX',
    compression: 'DEFLATE',
  });
  fs.writeFileSync(tmpPath, output);
  fs.renameSync(tmpPath, outPath);
  console.log(`Stripped ${removed} symlinks into ${outPath} (${output.length} bytes)`);
}

function darwinOriginalKey(name) {
  const base = name.startsWith('EyeCode.app/') ? name.slice('EyeCode.app/'.length) : name;
  const originalBase =
    base === 'Contents/MacOS/EyeCode'
      ? 'Contents/MacOS/Electron'
      : base
          .split('EyeCode Login Helper')
          .join('Electron Login Helper')
          .split('EyeCode Helper')
          .join('Electron Helper');
  return `Electron.app/${originalBase}`;
}

function resolveEntryMode(name, metadata, originalKeyOf) {
  if (POSIX_EXEC_PATTERNS.some((pattern) => pattern.test(name))) return 0o755;
  const key = originalKeyOf ? originalKeyOf(name) : name;
  const original =
    metadata.modes.get(key) ?? (name === 'EyeCode' ? metadata.modes.get('electron') : undefined);
  if (original != null) return (original & 0o111) !== 0 ? 0o755 : original & 0o777;
  return 0o644;
}

async function writeZip(zip, zipPath) {
  const tmpPath = `${zipPath}.tmp`;
  const output = await zip.generateAsync({
    type: 'nodebuffer',
    platform: 'UNIX',
    compression: 'DEFLATE',
  });
  fs.writeFileSync(tmpPath, output);
  fs.rmSync(zipPath, { force: true });
  fs.renameSync(tmpPath, zipPath);
  return output.length;
}

export async function applyPosixExecBits(zipPath, metadata) {
  const zip = await JSZip.loadAsync(fs.readFileSync(zipPath));
  let count = 0;
  for (const entry of Object.values(zip.files)) {
    const normalized = entry.name.replace(/\\/g, '/');
    if (entry.dir) {
      entry.unixPermissions = 0o40755;
      continue;
    }
    const mode = resolveEntryMode(normalized, metadata, null);
    entry.unixPermissions = mode;
    if ((mode & 0o111) !== 0) count += 1;
  }
  if (count === 0) {
    fail(`No POSIX executables found in ${zipPath}; refusing to ship a ZIP without exec bits.`);
  }
  await writeZip(zip, zipPath);
  console.log(`POSIX exec bits applied to ${count} entries in ${path.basename(zipPath)}`);
  return count;
}

export async function finalizeDarwinZip(zipPath, metadata) {
  if (!metadata || metadata.symlinks.length === 0) {
    fail('Darwin finalize requires Electron ZIP symlink metadata; refusing to ship a broken app bundle.');
  }
  const zip = await JSZip.loadAsync(fs.readFileSync(zipPath));
  const renames = [];
  for (const [key] of Object.entries(zip.files)) {
    const normalized = key.replace(/\\/g, '/');
    if (normalized.startsWith('EyeCode.app/')) continue;
    renames.push([key, `EyeCode.app/${normalized}`]);
  }
  for (const [oldKey, newKey] of renames) {
    const entry = zip.files[oldKey];
    delete zip.files[oldKey];
    entry.name = newKey;
    zip.files[newKey] = entry;
  }
  let execCount = 0;
  for (const entry of Object.values(zip.files)) {
    if (entry.dir) {
      entry.unixPermissions = 0o40755;
      continue;
    }
    const mode = resolveEntryMode(entry.name, metadata, darwinOriginalKey);
    entry.unixPermissions = mode;
    if ((mode & 0o111) !== 0) execCount += 1;
  }
  for (const link of metadata.symlinks) {
    const name = link.name.replace(/^Electron\.app\//, 'EyeCode.app/');
    zip.file(name, link.target, { unixPermissions: link.mode });
  }
  let symlinkCount = 0;
  let prefixedCount = 0;
  for (const entry of Object.values(zip.files)) {
    if (entry.dir) {
      if (entry.unixPermissions == null) entry.unixPermissions = 0o40755;
      continue;
    }
    if (!entry.name.startsWith('EyeCode.app/')) {
      fail(`Darwin ZIP entry missing EyeCode.app/ prefix: ${entry.name}`);
    }
    prefixedCount += 1;
    if ((entry.unixPermissions & 0o170000) === 0o120000) symlinkCount += 1;
  }
  if (symlinkCount !== metadata.symlinks.length) {
    fail(`Darwin ZIP restored ${symlinkCount} symlinks, expected ${metadata.symlinks.length}.`);
  }
  if (execCount === 0) {
    fail(`Darwin ZIP has no POSIX executables; refusing to ship ${zipPath}.`);
  }
  await writeZip(zip, zipPath);
  console.log(
    `Darwin ZIP finalized: ${prefixedCount} files prefixed, ${execCount} exec entries, ${symlinkCount} symlinks restored`
  );
}

export async function runPackage(target = parseTarget()) {
  const { platform, arch } = target;
  const targetLabel = `${platform}/${arch}`;

  delete process.env.NODE_INSTALLER;
  delete process.env.npm_config_user_agent;

  step(`1/8 Build production frontend [${targetLabel}]`);
  run('npm run build');
  if (!fs.existsSync(path.join(repoRoot, 'src', 'main', 'resources', 'webshell', 'index.html'))) {
    fail('Frontend build did not produce src/main/resources/webshell/index.html');
  }

  step('2/8 Build isolated WebShell distribution');
  run('npm run webshell:dist');
  if (!fs.existsSync(path.join(DIST_DIR, 'eyecode-webshell.jar'))) {
    fail(`WebShell distribution jar missing at ${DIST_DIR}\\eyecode-webshell.jar`);
  }
  const libJars = fs.readdirSync(path.join(DIST_DIR, 'lib')).filter((n) => n.endsWith('.jar'));
  if (libJars.length === 0) {
    fail(`WebShell distribution lib directory has no jars at ${path.join(DIST_DIR, 'lib')}`);
  }
  console.log(`WebShell runtime jars: ${libJars.length}`);

  step(`3/8 Stage and verify pinned JDK (${targetLabel})`);
  if (stagedJdkMatches(platform, arch)) {
    console.log(`Staged JDK already valid for ${targetLabel}: ${STAGED_JDK}`);
  } else {
    run(`powershell -NoProfile -ExecutionPolicy Bypass -File "${STAGE_SCRIPT}" -Platform ${platform} -Arch ${arch}`);
  }
  if (!isUsableJdkHome(STAGED_JDK, platform)) {
    fail(`Staged JDK is not usable for ${targetLabel} at ${STAGED_JDK}. Run scripts/stage-eyecode-jdk.ps1 -Platform ${platform} -Arch ${arch} manually.`);
  }
  if (!fs.existsSync(JDK_MANIFEST)) {
    fail(`JDK manifest missing at ${JDK_MANIFEST}.`);
  }
  {
    const manifest = readJson(JDK_MANIFEST);
    if (manifest.platform !== platform || manifest.architecture !== arch) {
      fail(`Staged JDK manifest targets ${manifest.platform}/${manifest.architecture} but this package needs ${targetLabel}.`);
    }
  }

  step('4/8 Stage and verify pinned Maven');
  if (isUsableMavenHome(STAGED_MAVEN) && fs.existsSync(MAVEN_MANIFEST)) {
    console.log(`Staged Maven already valid: ${STAGED_MAVEN}`);
  } else {
    run(`powershell -NoProfile -ExecutionPolicy Bypass -File "${STAGE_MAVEN_SCRIPT}"`);
  }
  if (!isUsableMavenHome(STAGED_MAVEN)) {
    fail(`Staged Maven is not usable at ${STAGED_MAVEN}. Run scripts/stage-eyecode-maven.ps1 manually.`);
  }
  if (!fs.existsSync(MAVEN_MANIFEST)) {
    fail(`Maven manifest missing at ${MAVEN_MANIFEST}.`);
  }

  step('5/8 Verify package resources (extraResource sources)');
  console.log(`webshell-dist: ${DIST_DIR}`);
  console.log(`jdk:           ${STAGED_JDK}`);
  console.log(`maven:         ${STAGED_MAVEN}`);
  if (!fs.existsSync(THIRD_PARTY_NOTICES)) {
    fail(`Third-party notices file missing at ${THIRD_PARTY_NOTICES}.`);
  }
  console.log(`notices:       ${THIRD_PARTY_NOTICES}`);
  if (!fs.existsSync(EYE_CODE_LICENSE)) {
    fail(`EyeCode LICENSE file missing at ${EYE_CODE_LICENSE}.`);
  }
  console.log(`license:       ${EYE_CODE_LICENSE}`);

  const electronPkg = readJson(path.join(webRoot, 'node_modules', 'electron', 'package.json'));
  const webPkg = readJson(path.join(webRoot, 'package.json'));

  step(`6/8 Electron Forge make (${targetLabel} ZIP)`);
  let electronMetadata = null;
  let makeEnv = null;
  if (platform === 'darwin') {
    const sourceZip = await ensureElectronSourceZip(electronPkg.version, platform, arch);
    electronMetadata = await readElectronZipMetadata(sourceZip);
    if (electronMetadata.symlinks.length === 0) {
      fail(`Electron ${electronPkg.version} ${platform}/${arch} ZIP has no symlinks; refusing to package a broken darwin app.`);
    }
    const strippedZip = path.join(
      ELECTRON_ZIP_DIR,
      `electron-v${electronPkg.version}-${platform}-${arch}.zip`
    );
    const needsStrip =
      !fs.existsSync(strippedZip) ||
      fs.statSync(strippedZip).mtimeMs < fs.statSync(sourceZip).mtimeMs;
    if (needsStrip) {
      await stripElectronSymlinks(sourceZip, strippedZip, electronMetadata.symlinks.length);
    } else {
      console.log(`Reusing stripped Electron ZIP: ${strippedZip}`);
    }
    const packagedApp = path.join(OUT_DIR, `EyeCode-${platform}-${arch}`);
    if (fs.existsSync(packagedApp)) {
      fs.rmSync(packagedApp, { recursive: true, force: true });
    }
    if (fs.existsSync(OUT_DIR)) {
      for (const name of fs.readdirSync(OUT_DIR)) {
        if (name.startsWith(`${platform}-${arch}-template-`)) {
          fs.rmSync(path.join(OUT_DIR, name), { recursive: true, force: true });
        }
      }
    }
    const gatePatch = path.join(webRoot, 'scripts', 'forge-symlink-gate-patch.cjs');
    const existingNodeOptions = process.env.NODE_OPTIONS ? `${process.env.NODE_OPTIONS} ` : '';
    makeEnv = {
      ...process.env,
      EYECODE_ELECTRON_ZIP_DIR: ELECTRON_ZIP_DIR,
      NODE_OPTIONS: `${existingNodeOptions}--require ${gatePatch}`,
    };
  }
  run(`npx electron-forge make --platform=${platform} --arch=${arch}`, makeEnv ? { env: makeEnv } : {});

  const makeZipRoot = path.join(OUT_DIR, 'make', 'zip', platform, arch);
  if (!fs.existsSync(makeZipRoot)) {
    fail(`Maker-zip output directory missing: ${makeZipRoot}`);
  }
  const expectedZipPath = path.join(
    makeZipRoot,
    `EyeCode-${platform}-${arch}-${webPkg.version}.zip`
  );
  if (!fs.existsSync(expectedZipPath)) {
    const available = fs.readdirSync(makeZipRoot).filter((n) => n.endsWith('.zip'));
    fail(
      `Expected maker ZIP missing: ${expectedZipPath}. Available: ${available.join(', ') || '(none)'}`
    );
  }
  const zipPath = expectedZipPath;

  step(`7/8 Finalize ZIP (${targetLabel})`);
  if (platform === 'win32') {
    console.log('Windows ZIP needs no POSIX exec bits.');
  } else if (platform === 'darwin') {
    await finalizeDarwinZip(zipPath, electronMetadata);
  } else {
    const sourceZip = await ensureElectronSourceZip(electronPkg.version, platform, arch);
    electronMetadata = await readElectronZipMetadata(sourceZip);
    await applyPosixExecBits(zipPath, electronMetadata);
  }

  step('8/8 Report ZIP and write package manifest');
  const zipSize = fs.statSync(zipPath).size;
  const jdkManifest = readJson(JDK_MANIFEST);
  const mavenManifest = readJson(MAVEN_MANIFEST);

  const manifest = {
    schema: 'eyecode-desktop-package-manifest/1',
    createdAtUtc: new Date().toISOString(),
    electron: {
      version: electronPkg.version,
      platform,
      arch,
    },
    application: {
      name: webPkg.productName || webPkg.name,
      version: webPkg.version,
    },
    jdk: {
      vendor: jdkManifest.vendor,
      version: jdkManifest.version,
      platform: jdkManifest.platform,
      architecture: jdkManifest.architecture,
      archiveSha256: jdkManifest.archiveSha256,
      downloadUrl: jdkManifest.downloadUrl,
      licenseNotice: jdkManifest.licenseNotice,
    },
    maven: {
      vendor: mavenManifest.vendor,
      name: mavenManifest.name,
      version: mavenManifest.version,
      archiveSha512: mavenManifest.archiveSha512,
      downloadUrl: mavenManifest.downloadUrl,
      licenseNotice: mavenManifest.licenseNotice,
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
}

const invokedDirectly =
  process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (invokedDirectly) {
  await runPackage();
}
