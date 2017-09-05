package ro.cs.tao.services.commons;

/**
 * @author Cosmin Cara
 */
public class ServiceError {
    private String message;

    public ServiceError(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
