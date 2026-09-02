package com.eyecode.runtime;

public final class ProcessTree {

    private ProcessTree() {
    }

    public static void destroy(Process process, boolean forcibly) {
        if (process == null) {
            return;
        }
        ProcessHandle handle = process.toHandle();
        handle.descendants().forEach(child -> destroy(child, forcibly));
        destroy(handle, forcibly);
    }

    private static void destroy(ProcessHandle handle, boolean forcibly) {
        if (forcibly) {
            handle.destroyForcibly();
        } else {
            handle.destroy();
        }
    }
}
