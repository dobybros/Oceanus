package script.core.runtime.java;

import chat.errors.CoreException;
import script.Runtime;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.handler.compile.CompileServiceHandler;


/**
 * Created by lick on 2020/12/18.
 * Description：
 */
public class JavaRuntime extends Runtime {
    private CompileServiceHandler compileServiceHandler;
    public JavaRuntime(AbstractRuntimeContext runtimeContext) throws Throwable {
        this.runtimeContext = runtimeContext;
        this.compileServiceHandler = (CompileServiceHandler) Class.forName("com.docker.script.executor.compile.DefaultCompileServiceHandler").getDeclaredConstructor(AbstractRuntimeContext.class).newInstance(runtimeContext);
    }
    @Override
    public void start() throws CoreException {
        this.compileServiceHandler.compile();
    }

    @Override
    public void close() {

    }
}
