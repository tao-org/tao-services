package ro.cs.tao.ogc.model.processes.core;

import java.util.Arrays;
import java.util.List;

public class InputValueNoObject<E> {
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

    public static InputValueNoObject<List<?>> of(List<?> value) {
        return new InputValueNoObject<>(value);
    }

    public static <E> InputValueNoObject<List<?>> of(E[] value) {
        return of(Arrays.asList(value));
    }

    public InputValueNoObject() {
    }

    public InputValueNoObject(E value) {
        this.value = value;
    }
}
