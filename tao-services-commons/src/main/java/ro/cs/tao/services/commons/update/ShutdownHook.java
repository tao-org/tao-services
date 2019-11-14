package ro.cs.tao.services.commons.update;

import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.nio.file.Path;

public class ShutdownHook implements Runnable {
    private static Thread shutdownHookThread;
    private final Path applicationPath;

    private ShutdownHook(Path applicationPath) {
        this.applicationPath = applicationPath;
    }

    @Override
    public void run() {
        final String extension = SystemUtils.IS_OS_WINDOWS ? ".bat" : ".sh";
        try {
            final String args = (SystemUtils.IS_OS_WINDOWS ? "cmd /c " : "") + applicationPath.resolve("bin").resolve("start" + extension).toString();
            final ProcessBuilder processBuilder = new ProcessBuilder().command(args.split(" "));
            processBuilder.inheritIO();
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized void register(Path applicationPath) {
        if (shutdownHookThread == null) {
            ShutdownHook hook = new ShutdownHook(applicationPath);
            shutdownHookThread = new Thread(hook);
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        } else {
            System.err.println("ShutdownHook.register() should be called only once. Subsequent calls are ignored.");
        }
    }

    static synchronized void unregister() {
        if (shutdownHookThread != null) {
            if (Runtime.getRuntime().removeShutdownHook(shutdownHookThread)) {
                shutdownHookThread = null;
            } else {
                System.err.println("ShutdownHook was not unregistered");
            }
        }
    }
}
