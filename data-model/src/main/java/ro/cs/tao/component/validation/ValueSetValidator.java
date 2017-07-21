package ro.cs.tao.component.validation;

import ro.cs.tao.component.ParameterDescriptor;

import java.util.Arrays;

/**
 * @author Cosmin Cara
 */
public class ValueSetValidator implements Validator {

    ValueSetValidator() { }

    @Override
    public void validate(ParameterDescriptor parameter, Object value) throws ValidationException {
        if (parameter == null) {
            throw new ValidationException("Cannot validate a null reference");
        }
        if (value != null && parameter.isNotNull()) {
            final String[] valueSet = parameter.getValueSet();
            if (valueSet != null && Arrays.stream(valueSet).noneMatch(v -> v.equals(value))) {
                throw new ValidationException(String.format("Value for [%s] is invalid.", parameter.getName()));
            }
        }
    }
}
