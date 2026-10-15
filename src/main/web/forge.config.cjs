const path = require('node:path');

const webRoot = __dirname;
const repoRoot = path.resolve(webRoot, '..', '..', '..');
const webshellDist = path.join(repoRoot, 'webshell-dist', 'target', 'webshell-dist');
const stagedJdk = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');

module.exports = {
  packagerConfig: {
    name: 'EyeCode',
    asar: true,
    appVersion: require('./package.json').version,
    ignore: (file) => {
      if (file === '') return false;
      if (file === '/package.json') return false;
      if (file === '/electron' || file.startsWith('/electron/')) return false;
      return true;
    },
    extraResource: [webshellDist, stagedJdk],
  },
  makers: [
    {
      name: '@electron-forge/maker-zip',
      config: {},
      platforms: ['win32'],
    },
  ],
};
