import path from 'node:path';
import fs from 'node:fs';

export const MAIN_CLASS = 'com.eyecode.ui.web.LocalWebShellLauncher';
export const DIST_JAR_NAME = 'eyecode-webshell.jar';
export const DIST_LIB_DIR_NAME = 'lib';
export const READINESS_URL_PATTERN = /\[EyeCode\] URL: (http:\/\/127\.0\.0\.1:\d+\/webshell\/index\.html)/;
export const STARTUP_TIMEOUT_MS = 60_000;
export const STAGED_JDK_RELATIVE = path.join('target', 'eyecode-desktop', 'runtime', 'jdk');

export function resolveDistDir(repoRoot, env = process.env) {
  const override = (env.EYECODE_WEBSHELL_DIST || '').trim();
  if (override) return path.resolve(override);
  return path.join(repoRoot, 'webshell-dist', 'target', 'webshell-dist');
}

export function isJdkHome(home, platform = process.platform, existsSync = fs.existsSync) {
  if (!home) return false;
  const javaBin = path.join(home, 'bin', platform === 'win32' ? 'java.exe' : 'java');
  const javacBin = path.join(home, 'bin', platform === 'win32' ? 'javac.exe' : 'javac');
  return existsSync(javaBin) && existsSync(javacBin);
}

export function resolveJdkHome({
  repoRoot,
  env = process.env,
  platform = process.platform,
  existsSync = fs.existsSync,
  resourcesPath,
} = {}) {
  const override = (env.EYECODE_JDK_HOME || '').trim();
  if (override && isJdkHome(override, platform, existsSync)) return override;

  const staged = path.join(repoRoot, STAGED_JDK_RELATIVE);
  if (isJdkHome(staged, platform, existsSync)) return staged;

  if (resourcesPath) {
    const packaged = path.join(resourcesPath, 'jdk');
    if (isJdkHome(packaged, platform, existsSync)) return packaged;
  }

  const javaHome = (env.JAVA_HOME || '').trim();
  if (javaHome && isJdkHome(javaHome, platform, existsSync)) return javaHome;

  return null;
}

export function resolveJavaExecutable({
  env = process.env,
  platform = process.platform,
  existsSync = fs.existsSync,
  repoRoot,
  resourcesPath,
} = {}) {
  const override = (env.EYECODE_JAVA || '').trim();
  if (override) return override;

  if (repoRoot) {
    const jdkHome = resolveJdkHome({ repoRoot, env, platform, existsSync, resourcesPath });
    if (jdkHome) return path.join(jdkHome, 'bin', platform === 'win32' ? 'java.exe' : 'java');
  }

  const javaHome = (env.JAVA_HOME || '').trim();
  if (javaHome) {
    const candidate = path.join(javaHome, 'bin', platform === 'win32' ? 'java.exe' : 'java');
    if (existsSync(candidate)) return candidate;
  }
  return 'java';
}

export function assertDistReady(distDir, existsSync = fs.existsSync) {
  const jar = path.join(distDir, DIST_JAR_NAME);
  const libDir = path.join(distDir, DIST_LIB_DIR_NAME);
  if (!existsSync(jar) || !existsSync(libDir)) {
    throw new Error(
      `WebShell distribution not found at:\n  ${distDir}\n\n` +
      'Build it from the repository root:\n' +
      '  mvn -f webshell-dist/pom.xml package\n\n' +
      'Or run "npm run electron:dev" from src/main/web, which builds the frontend and the distribution before starting Electron.'
    );
  }
  return { jar, libDir };
}

export function buildBackendLaunch({ repoRoot, env = process.env, platform = process.platform, existsSync = fs.existsSync, resourcesPath }) {
  const distDir = resolveDistDir(repoRoot, env);
  const { jar, libDir } = assertDistReady(distDir, existsSync);
  const jdkHome = resolveJdkHome({ repoRoot, env, platform, existsSync, resourcesPath });
  const java = resolveJavaExecutable({ env, platform, existsSync, repoRoot, resourcesPath });
  const classpath = `${jar}${path.delimiter}${path.join(libDir, '*')}`;
  const args = ['-Deyecode.web.openBrowser=false'];
  const jdtlsHome = (env.EYECODE_JDTLS_HOME || '').trim();
  if (jdtlsHome) {
    args.push(`-Deyecode.jdtls.home=${jdtlsHome}`);
  }
  if (jdkHome) {
    args.push(`-Deyecode.jdk.home=${jdkHome}`);
  }
  args.push('-cp', classpath, MAIN_CLASS);
  return { command: java, args, cwd: distDir, distDir, jdkHome: jdkHome || null };
}

export function parseReadinessUrl(buffer) {
  const match = buffer.match(READINESS_URL_PATTERN);
  return match ? match[1] : null;
}
