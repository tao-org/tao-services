package ro.cs.tao.services.startup;

import ro.cs.tao.lifecycle.ComponentLifeCycle;
import ro.cs.tao.persistence.PersistenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Base class for component activators.
 *
 * @author Cosmin Cara
 */
public abstract class BaseLifeCycle implements ComponentLifeCycle {
    protected PersistenceManager persistenceManager;
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public final void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    protected void asyncExecute(Runnable runnable) {
        executorService.execute(runnable);
    }

}
