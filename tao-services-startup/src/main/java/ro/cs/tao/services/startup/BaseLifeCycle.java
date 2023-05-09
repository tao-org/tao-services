package ro.cs.tao.services.startup;

import ro.cs.tao.lifecycle.ComponentLifeCycle;
import ro.cs.tao.persistence.PersistenceManager;

import java.util.logging.Logger;

/**
 * Base class for component activators.
 *
 * @author Cosmin Cara
 */
public abstract class BaseLifeCycle implements ComponentLifeCycle {
    protected PersistenceManager persistenceManager;
    protected Logger logger = Logger.getLogger(getClass().getName());

    public final void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
}
