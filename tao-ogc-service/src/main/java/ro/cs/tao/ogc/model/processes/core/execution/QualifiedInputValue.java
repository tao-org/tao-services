package ro.cs.tao.ogc.model.processes.core.execution;

import java.util.List;

public class QualifiedInputValue<E> extends InputValueNoObject<E> {
    private Format format;

    private List<InputValueNoObject<?>> additionalProperties;

    public QualifiedInputValue() {
        super();
    }

    public QualifiedInputValue(E value) {
        super(value);
    }

    public List<InputValueNoObject<?>> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(List<InputValueNoObject<?>> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }
}
