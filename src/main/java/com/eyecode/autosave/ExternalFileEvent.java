package com.eyecode.autosave;

import java.nio.file.Path;

public record ExternalFileEvent(Path path, ExternalFileState state) {
}
