/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.commons;

/**
 * A generic interface designed to facilitate the conversion of objects between
 * two distinct types. The {@code Converter} interface provides methods for
 * transforming objects of type {@code V} into objects of type {@code T}, and
 * vice versa.
 *
 * This interface is particularly useful in scenarios that require mapping
 * between different data transfer object models, domain models, or any other
 * distinct object representations, ensuring consistency and reusability.
 *
 * @param <T> the target type of the conversion
 * @param <V> the source type of the conversion
 */
public interface Converter<T, V> {
    T from(V value);
    V to(T value);
}
