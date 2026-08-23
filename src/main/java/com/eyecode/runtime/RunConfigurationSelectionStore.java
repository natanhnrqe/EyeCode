package com.eyecode.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class RunConfigurationSelectionStore {
    private final Path path;

    public RunConfigurationSelectionStore() {
        this(Path.of(System.getProperty("user.home"), ".eyecode", "run-configurations.properties"));
    }

    public RunConfigurationSelectionStore(Path path) {
        this.path = path;
    }

    public synchronized String selectedId(Path projectRoot) {
        Properties properties = load();
        return properties.getProperty(key(projectRoot));
    }

    public synchronized void select(Path projectRoot, String id) {
        Properties properties = load();
        String key = key(projectRoot);
        if (id == null || id.isBlank()) {
            properties.remove(key);
        } else {
            properties.setProperty(key, id);
        }
        save(properties);
    }

    private Properties load() {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private void save(Properties properties) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "EyeCode run configuration selections");
            }
        } catch (IOException ignored) {
        }
    }

    private String key(Path projectRoot) {
        return projectRoot.toAbsolutePath().normalize().toString();
    }
}
