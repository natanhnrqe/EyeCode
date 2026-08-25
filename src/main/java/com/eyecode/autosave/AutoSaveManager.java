package com.eyecode.autosave;

import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.filesystem.FileFingerprint;

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
    private final Map<EditorDocument, FileFingerprint> expectedFingerprints = new ConcurrentHashMap<>();
    private final Map<EditorDocument, FileFingerprint> selfWrittenFingerprints = new ConcurrentHashMap<>();
    private final Map<EditorDocument, PendingWrite> pendingWrites = new ConcurrentHashMap<>();
    private final Map<EditorDocument, ExternalFileState> externalStates = new ConcurrentHashMap<>();
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
        try {
            expectedFingerprints.put(document, FileFingerprint.capture(fileSystemService, document.getSourceFile()));
            externalStates.put(document, ExternalFileState.SYNCED);
        } catch (IOException exception) {
            failures.put(document, exception);
        }
    }

    /**
     * Stops tracking a document and cancels any pending save.
     */
    public synchronized void unregister(EditorDocument document) {
        Binding binding = bindings.remove(document);
        if (binding == null) return;
        document.removeDocumentChangeListener(binding.listener);
        cancelPending(binding);
        expectedFingerprints.remove(document);
        selfWrittenFingerprints.remove(document);
        pendingWrites.remove(document);
        externalStates.remove(document);
        failures.remove(document);
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

    public ExternalFileState externalState(EditorDocument document) {
        return document == null ? ExternalFileState.IGNORED
                : externalStates.getOrDefault(document, ExternalFileState.IGNORED);
    }

    public boolean hasExternalConflict(EditorDocument document) {
        ExternalFileState state = externalState(document);
        return state == ExternalFileState.CONFLICT || state == ExternalFileState.DELETED;
    }

    public synchronized ExternalFileState synchronizeExternal(EditorDocument document) {
        if (document == null || document.getSourceFile() == null || !bindings.containsKey(document)) {
            return ExternalFileState.IGNORED;
        }
        try {
            FileFingerprint current = FileFingerprint.capture(fileSystemService, document.getSourceFile());
            FileFingerprint expected = expectedFingerprints.get(document);
            PendingWrite pending = pendingWrites.get(document);
            Path sourcePath = document.getSourceFile().toAbsolutePath().normalize();
            if (pending != null && pending.path.equals(sourcePath)
                    && current.sameContent(pending.content)) {
                pendingWrites.remove(document, pending);
                expectedFingerprints.put(document, current);
                externalStates.put(document, ExternalFileState.SYNCED);
                return ExternalFileState.SYNCED;
            }
            FileFingerprint selfWritten = selfWrittenFingerprints.get(document);
            if (selfWritten != null && current.equals(selfWritten)) {
                selfWrittenFingerprints.remove(document, selfWritten);
                expectedFingerprints.put(document, current);
                externalStates.put(document, ExternalFileState.SYNCED);
                return ExternalFileState.SYNCED;
            }
            if (current.equals(expected)) {
                return ExternalFileState.SYNCED;
            }
            if (!current.exists()) {
                externalStates.put(document, ExternalFileState.DELETED);
                return ExternalFileState.DELETED;
            }
            if (document.isDirty()) {
                externalStates.put(document, ExternalFileState.CONFLICT);
                return ExternalFileState.CONFLICT;
            }
            String content = fileSystemService.readFile(document.getSourceFile());
            stateDispatcher.accept(() -> {
                document.setText(content);
                document.markClean();
            });
            pendingWrites.remove(document);
            selfWrittenFingerprints.remove(document);
            expectedFingerprints.put(document, current);
            externalStates.put(document, ExternalFileState.RELOADED);
            return ExternalFileState.RELOADED;
        } catch (IOException exception) {
            failures.put(document, exception);
            return ExternalFileState.IGNORED;
        }
    }

    public boolean reloadFromDisk(EditorDocument document) {
        if (document == null || document.getSourceFile() == null) return false;
        try {
            FileFingerprint current = FileFingerprint.capture(fileSystemService, document.getSourceFile());
            if (!current.exists()) return false;
            String content = fileSystemService.readFile(document.getSourceFile());
            stateDispatcher.accept(() -> {
                document.setText(content);
                document.markClean();
            });
            expectedFingerprints.put(document, current);
            pendingWrites.remove(document);
            selfWrittenFingerprints.remove(document);
            externalStates.put(document, ExternalFileState.RELOADED);
            return true;
        } catch (IOException exception) {
            failures.put(document, exception);
            return false;
        }
    }

    public boolean keepLocalChanges(EditorDocument document) {
        if (document == null) return false;
        try {
            expectedFingerprints.put(document, FileFingerprint.capture(fileSystemService, document.getSourceFile()));
        } catch (IOException exception) {
            failures.put(document, exception);
            return false;
        }
        boolean result = saveNow(document);
        if (result) externalStates.put(document, ExternalFileState.SYNCED);
        return result;
    }

    public synchronized void rebind(EditorDocument document, Path newPath) {
        if (document == null) return;
        Binding binding = bindings.get(document);
        if (binding != null) {
            synchronized (binding) {
                cancelPending(binding);
            }
        }
        expectedFingerprints.remove(document);
        selfWrittenFingerprints.remove(document);
        pendingWrites.remove(document);
        externalStates.remove(document);
        failures.remove(document);
        document.setSourceFile(newPath);
        if (binding != null) {
            try {
                expectedFingerprints.put(document, FileFingerprint.capture(fileSystemService, newPath));
                externalStates.put(document, ExternalFileState.SYNCED);
            } catch (IOException exception) {
                failures.put(document, exception);
            }
        }
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
            FileFingerprint current = FileFingerprint.capture(fileSystemService, path);
            FileFingerprint expected = expectedFingerprints.get(document);
            if (expected != null && !current.equals(expected)) {
                externalStates.put(document, current.exists()
                        ? ExternalFileState.CONFLICT : ExternalFileState.DELETED);
                notifySaved(path, false, new IOException("File changed outside EyeCode"));
                return false;
            }
            while (document.isDirty()) {
                var snapshot = document.snapshot();
                pendingWrites.put(document, new PendingWrite(
                        path.toAbsolutePath().normalize(), snapshot.version(),
                        FileFingerprint.contentOnly(snapshot.getText())));
                fileSystemService.writeFile(path, snapshot.getText());
                if (document.currentVersion() == snapshot.version()) {
                    expectedFingerprints.put(document, FileFingerprint.capture(fileSystemService, path));
                    selfWrittenFingerprints.put(document, expectedFingerprints.get(document));
                    pendingWrites.remove(document);
                    externalStates.put(document, ExternalFileState.SYNCED);
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
            pendingWrites.remove(document);
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

    private record PendingWrite(Path path, long version, FileFingerprint content) {
    }
}
