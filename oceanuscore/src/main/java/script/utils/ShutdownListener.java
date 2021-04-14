package script.utils;

import chat.errors.CoreException;

public interface ShutdownListener {
    void shutdown() throws CoreException;
}
