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

package ro.cs.tao.datasource.remote.scihub;

import ro.cs.tao.datasource.common.converters.ConversionException;
import ro.cs.tao.datasource.common.converters.PolygonConverter;
import ro.cs.tao.datasource.common.parameter.QueryParameter;

/**
 * @author Cosmin Cara
 */
public class SciHubPolygonConverter extends PolygonConverter {

    public SciHubPolygonConverter(QueryParameter parameter) {
        super(parameter);
    }

    @Override
    public String stringValue() throws ConversionException {
        return "\"Intersects(" + super.stringValue() + ")\"";
    }
}
