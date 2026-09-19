package com.eyecode.ui.web;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Compatibility facade implemented by native desktop adapters.
 *
 * <p>Controllers depend on one of its segregated parent capabilities instead.
 * The unavailable implementation reports no file-selection capability and
 * treats all window commands as no-ops, so the local Web runtime can preserve
 * normal protocol error handling without a toolkit check.</p>
 */
public interface WebShellNativeUi extends WebShellNativeFileSelection, WebShellWindowControls {

    static WebShellNativeUi unavailable() {
        return new WebShellNativeUi() {
            @Override public boolean isAvailable() { return false; }
            @Override public CompletableFuture<Path> chooseDirectoryAsync(String title) {
                return CompletableFuture.completedFuture(null);
            }
            @Override public Path chooseJavaSaveTarget(String suggestedName) { return null; }
        };
    }
}
