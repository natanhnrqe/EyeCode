package com.eyecode.autosave;

import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.filesystem.FileSystemService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Schedules automatic persistence of editor documents.
 * <p>
 * Every text change in a registered document restarts a debounce timer.
 * Once the document stays unchanged for {@value #DEFAULT_DELAY_MILLIS} ms the
 * content is written through {@link FileSystemService#writeFile(Path, String)}
 * and the document is marked clean.
 * <p>
 * Immediate saves can be triggered with {@link #saveNow(EditorDocument)} and
 * {@link #saveAll()} for focus loss, project execution, tab close and IDE
 * shutdown scenarios.
 */
public final class AutoSaveManager {

    /** Default debounce delay in milliseconds. */
    public static final long DEFAULT_DELAY_MILLIS = 700L;

    private final FileSystemService fileSystemService;
    private final long delayMillis;
    private final ScheduledExecutorService executor;
    private final boolean ownsExecutor;
    private final Consumer<Runnable> stateDispatcher;

    private final Map<EditorDocument, Binding> bindings = new ConcurrentHashMap<>();
    private final Map<EditorDocument, IOException> failures = new ConcurrentHashMap<>();
    private final List<Consumer<SavedEvent>> saveListeners = new CopyOnWriteArrayList<>();
    private volatile boolean shutdown;

    public AutoSaveManager(FileSystemService fileSystemService) {
        this(fileSystemService, DEFAULT_DELAY_MILLIS, newDaemonExecutor(), true, Runnable::run);
    }

    public AutoSaveManager(FileSystemService fileSystemService,
                           long delayMillis,
                           ScheduledExecutorService executor) {
        this(fileSystemService, delayMillis, executor, false, Runnable::run);
    }

    public AutoSaveManager(FileSystemService fileSystemService,
                           long delayMillis,
                           ScheduledExecutorService executor,
                           Consumer<Runnable> stateDispatcher) {
        this(fileSystemService, delayMillis, executor, false, stateDispatcher);
    }

    public AutoSaveManager(FileSystemService fileSystemService,
                           Consumer<Runnable> stateDispatcher) {
        this(fileSystemService, DEFAULT_DELAY_MILLIS, newDaemonExecutor(), true, stateDispatcher);
    }

    private AutoSaveManager(FileSystemService fileSystemService,
                            long delayMillis,
                            ScheduledExecutorService executor,
                            boolean ownsExecutor,
                            Consumer<Runnable> stateDispatcher) {
        if (fileSystemService == null) {
            throw new IllegalArgumentException("fileSystemService must not be null");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must not be negative");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        this.fileSystemService = fileSystemService;
        this.delayMillis = delayMillis;
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.stateDispatcher = stateDispatcher == null ? Runnable::run : stateDispatcher;
    }

    /**
     * Starts tracking a document. Subsequent text changes restart the debounce
     * timer. Registering an already tracked document is a no-op.
     */
    public synchronized void register(EditorDocument document) {
        if (document == null || shutdown) return;
        if (bindings.containsKey(document)) return;
        DocumentChangeListener listener = event -> scheduleSave(document);
        document.addDocumentChangeListener(listener);
        bindings.put(document, new Binding(listener));
    }

    /**
     * Stops tracking a document and cancels any pending save.
     */
    public synchronized void unregister(EditorDocument document) {
        Binding binding = bindings.remove(document);
        if (binding == null) return;
        document.removeDocumentChangeListener(binding.listener);
        cancelPending(binding);
    }

    /**
     * Restarts the debounce timer for the given document.
     */
    public void scheduleSave(EditorDocument document) {
        if (shutdown) return;
        Binding binding = bindings.get(document);
        if (binding == null) return;
        synchronized (binding) {
            cancelPending(binding);
            binding.pending = executor.schedule(
                    () -> performSave(document),
                    delayMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Immediately persists the given document, cancelling any pending
     * debounced save.
     */
    public boolean saveNow(EditorDocument document) {
        Binding binding = bindings.get(document);
        if (binding != null) {
            synchronized (binding) {
                cancelPending(binding);
                binding.pending = null;
                return performSave(document);
            }
        }
        return performSave(document);
    }

    /**
     * Immediately persists every registered dirty document.
     */
    public boolean saveAll() {
        boolean success = true;
        for (EditorDocument document : List.copyOf(bindings.keySet())) {
            success &= saveNow(document);
        }
        return success;
    }

    public boolean hasSaveFailure(EditorDocument document) {
        return document != null && failures.containsKey(document);
    }

    public void addSaveListener(Consumer<SavedEvent> listener) {
        if (listener != null) saveListeners.add(listener);
    }

    public void removeSaveListener(Consumer<SavedEvent> listener) {
        saveListeners.remove(listener);
    }

    /**
     * Cancels all pending saves and releases the owned executor. Documents are
     * not flushed automatically; call {@link #saveAll()} beforehand when
     * required.
     */
    public synchronized void shutdown() {
        shutdown = true;
        for (Binding binding : bindings.values()) {
            synchronized (binding) {
                cancelPending(binding);
            }
        }
        if (ownsExecutor) {
            executor.shutdownNow();
        }
    }

    private boolean performSave(EditorDocument document) {
        Path path = document.getSourceFile();
        if (path == null || !document.isDirty()) return true;

        IOException error = null;
        try {
            while (document.isDirty()) {
                var snapshot = document.snapshot();
                fileSystemService.writeFile(path, snapshot.getText());
                if (document.currentVersion() == snapshot.version()) {
                    failures.remove(document);
                    stateDispatcher.accept(() -> {
                        if (document.currentVersion() == snapshot.version()) {
                            document.markClean();
                        }
                    });
                    break;
                }
            }
        } catch (IOException ex) {
            error = ex;
            failures.put(document, ex);
        }
        notifySaved(path, error == null, error);
        return error == null;
    }

    private void notifySaved(Path path, boolean success, IOException error) {
        if (saveListeners.isEmpty()) return;
        SavedEvent event = new SavedEvent(path, success, error);
        for (Consumer<SavedEvent> listener : saveListeners) {
            listener.accept(event);
        }
    }

    private void cancelPending(Binding binding) {
        ScheduledFuture<?> pending = binding.pending;
        if (pending != null) {
            pending.cancel(false);
            binding.pending = null;
        }
    }

    private static ScheduledExecutorService newDaemonExecutor() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AutoSaveManager");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static final class Binding {
        final DocumentChangeListener listener;
        volatile ScheduledFuture<?> pending;

        Binding(DocumentChangeListener listener) {
            this.listener = listener;
        }
    }
}
