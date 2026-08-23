package com.eyecode.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record FileFingerprint(boolean exists, long size, long lastModifiedMillis, String contentHash) {

    public static FileFingerprint capture(FileSystemService fileSystemService, Path path) throws IOException {
        if (path == null || !fileSystemService.exists(path)) {
            return absent();
        }
        String content = fileSystemService.readFile(path);
        long modified = -1L;
        try {
            modified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
        }
        return new FileFingerprint(true, content.getBytes(StandardCharsets.UTF_8).length,
                modified, hash(content));
    }

    public static FileFingerprint absent() {
        return new FileFingerprint(false, 0L, -1L, "");
    }

    private static String hash(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
