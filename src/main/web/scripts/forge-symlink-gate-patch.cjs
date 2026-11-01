try {
  const { Packager } = require('@electron/packager/dist/packager');
  if (Packager && typeof Packager.prototype.testSymlink === 'function') {
    Packager.prototype.testSymlink = async function (comboOpts, zipPath) {
      this.canCreateSymlinks = true;
      return this.checkOverwrite(comboOpts, zipPath);
    };
    console.log('[eyecode] darwin host symlink gate bypassed (stripped Electron ZIP)');
  }
} catch {
}
