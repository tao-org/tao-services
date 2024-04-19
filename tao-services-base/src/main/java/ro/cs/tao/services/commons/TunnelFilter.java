package ro.cs.tao.services.commons;

import javax.servlet.Filter;
import java.util.regex.Pattern;

public interface TunnelFilter extends Filter {

    Pattern filterExpression();

}
