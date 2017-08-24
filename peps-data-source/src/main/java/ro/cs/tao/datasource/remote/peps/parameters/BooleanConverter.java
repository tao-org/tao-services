package ro.cs.tao.datasource.remote.peps.parameters;

import ro.cs.tao.datasource.common.converters.ConversionException;
import ro.cs.tao.datasource.common.converters.DefaultConverter;
import ro.cs.tao.datasource.common.parameter.QueryParameter;

/**
 * @author Cosmin Cara
 */
public class BooleanConverter extends DefaultConverter {
    public BooleanConverter(QueryParameter parameter) {
        super(parameter);
        if (!Boolean.class.equals(parameter.getType())) {
            throw new IllegalArgumentException("Invalid parameter type");
        }
    }

    @Override
    public String stringValue() throws ConversionException {
        return ((Boolean) parameter.getValue()) ? "1" : "0";
    }
}