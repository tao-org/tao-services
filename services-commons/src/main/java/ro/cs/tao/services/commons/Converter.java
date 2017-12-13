package ro.cs.tao.services.commons;

/**
 * @author Cosmin Cara
 */
public interface Converter<T, V> {
    T from(V value);
    V to(T value);
}
