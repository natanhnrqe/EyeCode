import { runPackage } from './package-desktop.mjs';

await runPackage({ platform: 'win32', arch: 'x64' });
