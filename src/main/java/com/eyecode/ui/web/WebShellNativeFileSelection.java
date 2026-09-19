package com.eyecode.ui.web;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

/**
 * Native file-selection capability required by workspace and document flows.
 *
 * <p>Selection completes asynchronously so a transport handler never needs to
 * know which toolkit thread owns a native dialog. {@link #isAvailable()} is
 * {@code true} only when both selection operations can be invoked. A
 * completed {@code null} result represents user cancellation; an exceptional
 * completion represents a native failure.</p>
 */
public interface WebShellNativeFileSelection {
    boolean isAvailable();

    CompletionStage<Path> chooseDirectoryAsync(String title);

    Path chooseJavaSaveTarget(String suggestedName);
}
