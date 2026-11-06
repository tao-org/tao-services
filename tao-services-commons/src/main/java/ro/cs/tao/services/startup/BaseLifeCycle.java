package ro.cs.tao.services.startup;

import org.springframework.context.ApplicationContext;
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
    protected ApplicationContext applicationContext;
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public final void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public final void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected void asyncExecute(Runnable runnable) {
        executorService.execute(runnable);
    }

}
