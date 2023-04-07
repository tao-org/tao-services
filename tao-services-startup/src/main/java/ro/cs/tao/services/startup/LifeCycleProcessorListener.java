package ro.cs.tao.services.startup;

/**
 * Listener to be called when all the life cycle processors have completed.
 */
public interface LifeCycleProcessorListener {
    void activationCompleted();
}
