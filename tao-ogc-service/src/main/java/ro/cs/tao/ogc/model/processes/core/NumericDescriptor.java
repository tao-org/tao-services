package ro.cs.tao.ogc.model.processes.core;

public class NumericDescriptor<E extends Number> extends Descriptor<E>{
    private E minimum;
    private E maximum;
    private boolean exclusiveMinimum;
    private boolean exclusiveMaximum;

    public boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public E getMaximum() {
        return maximum;
    }

    public void setMaximum(E maximum) {
        this.maximum = maximum;
    }

    public E getMinimum() {
        return minimum;
    }

    public void setMinimum(E minimum) {
        this.minimum = minimum;
    }
}
