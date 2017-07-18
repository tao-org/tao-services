/*
 *
 *  * Copyright (C) 2017 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *  *
 *
 */

package ro.cs.tao.component.constraints;

import ro.cs.tao.eodata.EOProduct;

import java.util.Arrays;

/**
 * @author Cosmin Cara
 */
public class SensorConstraint implements Constraint<EOProduct> {
    @Override
    public boolean check(EOProduct... args) {
        return args != null && args.length > 0 &&
                Arrays.stream(args)
                        .allMatch(a -> args[0].getSensorType().equals(a.getSensorType()));
    }
}
