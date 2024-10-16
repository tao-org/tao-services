package ro.cs.tao.ogc.model.processes.core;

public class QualifiedInputValue<E> extends InputValueNoObject<E> {
    private Format format;

    public QualifiedInputValue() {
        super();
    }

    public QualifiedInputValue(E value) {
        super(value);
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }
}
