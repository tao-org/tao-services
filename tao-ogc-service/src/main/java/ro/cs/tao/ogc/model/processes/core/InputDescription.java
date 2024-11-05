package ro.cs.tao.ogc.model.processes.core;

import java.util.Arrays;
import java.util.List;

public abstract class InputDescription<D extends Descriptor<?>> extends DescriptionType {
    private D schema;
    private int minOccurs;
    private List<ValuePassing> valuePassing;

    public static <N extends Number> NumericDescriptor<N> of(N value) {
        return new NumericDescriptor<>();
    }

    public static StringDescriptor of(String value) {
        return new StringDescriptor();
    }

    public static ListDescriptor of(List<?> value) {
        return new ListDescriptor();
    }

    public static <E> ListDescriptor of(E[] value) {
        return of(Arrays.asList(value));
    }

    public static InputDescription of(Type type) {
        final InputDescription result;
        switch (type) {
            case NUMBER:
            case INTEGER:
                result = new NumericInputDescriptor();
                break;
            case BOOLEAN:
            case STRING:
                result = new StringInputDescriptor();
                break;
            case ARRAY:
                result = new ListInputDescriptor();
                break;
            case OBJECT:
            default:
                result = new StringInputDescriptor();
                break;
        }
        return result;
    }

    public InputDescription() {
    }

    public D getSchema() {
        return schema;
    }

    public void setSchema(D schema) {
        this.schema = schema;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public List<ValuePassing> getValuePassing() {
        return valuePassing;
    }

    public void setValuePassing(List<ValuePassing> valuePassing) {
        this.valuePassing = valuePassing;
    }

    public static class NumericInputDescriptor<E extends Number> extends InputDescription<NumericDescriptor<E>> {
        public NumericInputDescriptor() {
            super();
        }
    }

    public static class StringInputDescriptor extends InputDescription<StringDescriptor> {
        public StringInputDescriptor() {
            super();
        }
    }

    public static class ListInputDescriptor extends InputDescription<ListDescriptor> {
        public ListInputDescriptor() {
            super();
        }
    }
}
