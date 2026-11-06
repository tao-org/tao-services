package ro.cs.tao.services.startup;

/**
 * Listener to be called when all the life cycle processors have completed.
 */
public interface LifeCycleProcessorListener {
    /**
     * Invoked when all life cycle processors have completed their activation process.
     * This method should be implemented to handle any post-activation tasks
     * or notifications required when the activation phase of the system is fully completed.
     */
    void activationCompleted();
}
