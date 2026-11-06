package ro.cs.tao.ogc.model.processes.core.execution;

import ro.cs.tao.ogc.model.common.Link;

import java.util.Arrays;
import java.util.List;

public class InputValueNoObject<E> extends InputBase {
    private E value;

    public static InputValueNoObject<Integer> of(int value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<Double> of(double value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<Number> of(Number value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<Boolean> of(boolean value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<String> of(String value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<BBox> of(BBox value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<Link> of(Link value) {
        return new InputValueNoObject<>(value);
    }

    public static InputValueNoObject<List<?>> of(List<?> value) {
        return new InputValueNoObject<>(value);
    }

    public static <E> InputValueNoObject<List<?>> of(E[] value) {
        return of(Arrays.asList(value));
    }

    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        this.value = value;
    }

    public InputValueNoObject() {
    }

    public InputValueNoObject(E value) {
        this.value = value;
    }
}
