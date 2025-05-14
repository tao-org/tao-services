package ro.cs.tao.services.commons;

import javax.servlet.Filter;
import java.util.regex.Pattern;

/**
 * Represents a filter that is used to intercept and process communication
 * in a tunneling context within an application. This interface extends the
 * generic {@link Filter}, providing additional functionality specific to
 * tunneling mechanisms.
 *
 * Implementations of this interface are expected to define a pattern-based
 * filtering mechanism through the {@code filterExpression} method and provide
 * the logic to handle tunneling-related filtering processes.
 */
public interface TunnelFilter extends Filter {

    Pattern filterExpression();

}
