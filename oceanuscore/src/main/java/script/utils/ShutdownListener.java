package script.utils;

import oceanus.apis.CoreException;

public interface ShutdownListener {
    void shutdown() throws CoreException;
}
