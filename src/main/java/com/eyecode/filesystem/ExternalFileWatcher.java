package com.eyecode.filesystem;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class ExternalFileWatcher implements AutoCloseable {

    private final WatchService watchService;
    private final ExecutorService executor;
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    private final Set<Path> roots = ConcurrentHashMap.newKeySet();
    private final Set<Path> watchedFiles = ConcurrentHashMap.newKeySet();
    private final Set<Path> pending = ConcurrentHashMap.newKeySet();
    private final Map<Path, Long> lastDelivered = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Path>> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean closed;

    public ExternalFileWatcher() {
        try {
            watchService = Path.of(".").getFileSystem().newWatchService();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create file watcher", exception);
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "EyeCode-ExternalFileWatcher");
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(this::watchLoop);
    }

    public void addListener(Consumer<Path> listener) {
        if (listener != null) listeners.addIfAbsent(listener);
    }

    public void removeListener(Consumer<Path> listener) {
        listeners.remove(listener);
    }

    public synchronized void watchRoot(Path root) throws IOException {
        if (closed || root == null || !Files.isDirectory(root)) return;
        Path normalized = root.toAbsolutePath().normalize();
        roots.add(normalized);
        Files.walkFileTree(normalized, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                register(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void watchFile(Path file) throws IOException {
        if (file == null) return;
        Path normalized = file.toAbsolutePath().normalize();
        watchedFiles.add(normalized);
        Path parent = normalized.getParent();
        if (parent != null) watchRoot(parent);
    }

    public synchronized void clearRoots() {
        roots.clear();
        watchedFiles.clear();
        for (WatchKey key : Set.copyOf(keys.keySet())) {
            key.cancel();
        }
        keys.clear();
    }

    private void register(Path directory) throws IOException {
        if (!keys.containsValue(directory)) {
            WatchKey key = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            keys.put(key, directory);
        }
    }

    private void watchLoop() {
        while (!closed) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException exception) {
                return;
            }
            Path directory = keys.get(key);
            if (directory != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    Path path = directory.resolve((Path) event.context()).toAbsolutePath().normalize();
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
                        try {
                            watchRoot(path);
                        } catch (IOException ignored) {
                        }
                    }
                    if (watchedFiles.contains(path) || roots.stream().anyMatch(path::startsWith)) {
                        pending.add(path);
                    }
                }
            }
            if (!key.reset()) keys.remove(key);
            flushPending();
        }
    }

    private void flushPending() {
        long now = System.currentTimeMillis();
        for (Path path : Set.copyOf(pending)) {
            if (pending.remove(path)) {
                Long previous = lastDelivered.get(path);
                if (previous != null && now - previous < 100L) continue;
                lastDelivered.put(path, now);
                for (Consumer<Path> listener : listeners) listener.accept(path);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            watchService.close();
        } catch (IOException ignored) {
        }
        executor.shutdownNow();
        keys.clear();
        roots.clear();
        watchedFiles.clear();
        pending.clear();
        lastDelivered.clear();
    }
}
