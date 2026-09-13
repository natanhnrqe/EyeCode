package com.eyecode.javafx.web;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class LocalWebShellNativeUi implements WebShellNativeUi {
    @Override
    public Path chooseDirectory(String title) {
        return chooseDirectoryAsync(title).join();
    }

    public CompletableFuture<Path> chooseDirectoryAsync(String title) {
        if (Platform.isWindows()) {
            return CompletableFuture.supplyAsync(() -> WindowsFolderPicker.choose(title));
        }
        CompletableFuture<Path> result = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            JFrame owner = chooserOwner();
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(title);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                owner.toFront();
                owner.requestFocus();
                int selection = chooser.showOpenDialog(owner);
                Path selected = selection == JFileChooser.APPROVE_OPTION
                        ? chooser.getSelectedFile().toPath().toAbsolutePath().normalize() : null;
                result.complete(selected);
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            } finally {
                owner.dispose();
            }
        });
        return result;
    }

    @Override
    public Path chooseJavaSaveTarget(String suggestedName) {
        return callOnEdt(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Java File");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Files", "java"));
            if (suggestedName != null && !suggestedName.isBlank()) chooser.setSelectedFile(new File(suggestedName));
            return chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION
                    ? chooser.getSelectedFile().toPath().toAbsolutePath().normalize() : null;
        });
    }

    @Override
    public void minimizeWindow() {
    }

    @Override
    public void toggleMaximizeWindow() {
    }

    @Override
    public void closeWindow() {
    }

    private static <T> T callOnEdt(Callable<T> task) {
        if (SwingUtilities.isEventDispatchThread()) return call(task);
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(call(task)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to open the native file chooser", exception);
        }
        return result.get();
    }

    private static <T> T call(Callable<T> task) {
        try {
            return task.call();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to open the native file chooser", exception);
        }
    }

    private static JFrame chooserOwner() {
        JFrame owner = new JFrame("EyeCode");
        owner.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        owner.setType(Window.Type.UTILITY);
        owner.setUndecorated(true);
        owner.setSize(1, 1);
        owner.setLocationRelativeTo(null);
        owner.setAlwaysOnTop(true);
        owner.setVisible(true);
        return owner;
    }

    private static final class WindowsFolderPicker {
        private static final Guid.CLSID CLSID_FILE_OPEN_DIALOG = new Guid.CLSID("{DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7}");
        private static final Guid.IID IID_FILE_OPEN_DIALOG = new Guid.IID("{D57C7288-D4AD-4768-BE02-9D969532D960}");
        private static final int CLSCTX_INPROC_SERVER = 0x1;
        private static final int FOS_PICKFOLDERS = 0x20;
        private static final int FOS_FORCEFILESYSTEM = 0x40;
        private static final int FOS_PATHMUSTEXIST = 0x800;
        private static final int SIGDN_FILESYSPATH = 0x80058000;
        private static final int ERROR_CANCELLED = 0x800704C7;

        private static Path choose(String title) {
            WinNT.HRESULT initialization = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_APARTMENTTHREADED);
            if (COMUtils.FAILED(initialization)) {
                throw new IllegalStateException("Unable to initialize Windows folder picker");
            }
            try {
                PointerByReference pointer = new PointerByReference();
                COMUtils.checkRC(Ole32.INSTANCE.CoCreateInstance(CLSID_FILE_OPEN_DIALOG, Pointer.NULL,
                        CLSCTX_INPROC_SERVER, IID_FILE_OPEN_DIALOG, pointer));
                FileOpenDialog dialog = new FileOpenDialog(pointer.getValue());
                try {
                    COMUtils.checkRC(dialog.setOptions(FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM | FOS_PATHMUSTEXIST));
                    COMUtils.checkRC(dialog.setTitle(new WString(title)));
                    WinNT.HRESULT shown = dialog.show(foregroundOwner());
                    if (shown.intValue() == ERROR_CANCELLED) return null;
                    COMUtils.checkRC(shown);
                    return dialog.selectedPath();
                } finally {
                    dialog.Release();
                }
            } finally {
                Ole32.INSTANCE.CoUninitialize();
            }
        }

        private static WinDef.HWND foregroundOwner() {
            WinDef.HWND owner = User32.INSTANCE.GetForegroundWindow();
            if (owner != null && Pointer.nativeValue(owner.getPointer()) != 0) {
                User32.INSTANCE.BringWindowToTop(owner);
                User32.INSTANCE.SetForegroundWindow(owner);
            }
            return owner;
        }

        private static final class FileOpenDialog extends Unknown {
            private FileOpenDialog(Pointer pointer) {
                super(pointer);
            }

            private WinNT.HRESULT setOptions(int options) {
                return invoke(9, options);
            }

            private WinNT.HRESULT setTitle(WString title) {
                return invoke(17, title);
            }

            private WinNT.HRESULT show(WinDef.HWND owner) {
                return invoke(3, owner == null ? Pointer.NULL : owner);
            }

            private Path selectedPath() {
                PointerByReference itemPointer = new PointerByReference();
                COMUtils.checkRC(invoke(20, itemPointer));
                ShellItem item = new ShellItem(itemPointer.getValue());
                try {
                    return item.path();
                } finally {
                    item.Release();
                }
            }

            private WinNT.HRESULT invoke(int index, Object... arguments) {
                Object[] values = new Object[arguments.length + 1];
                values[0] = getPointer();
                System.arraycopy(arguments, 0, values, 1, arguments.length);
                return (WinNT.HRESULT) _invokeNativeObject(index, values, WinNT.HRESULT.class);
            }
        }

        private static final class ShellItem extends Unknown {
            private ShellItem(Pointer pointer) {
                super(pointer);
            }

            private Path path() {
                PointerByReference value = new PointerByReference();
                Object[] arguments = {getPointer(), SIGDN_FILESYSPATH, value};
                COMUtils.checkRC((WinNT.HRESULT) _invokeNativeObject(5, arguments, WinNT.HRESULT.class));
                Pointer memory = value.getValue();
                try {
                    return Path.of(memory.getWideString(0)).toAbsolutePath().normalize();
                } finally {
                    Ole32.INSTANCE.CoTaskMemFree(memory);
                }
            }
        }
    }
}
