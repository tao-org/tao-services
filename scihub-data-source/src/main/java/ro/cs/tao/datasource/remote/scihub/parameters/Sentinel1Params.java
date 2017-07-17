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

package ro.cs.tao.datasource.remote.scihub.parameters;

import ro.cs.tao.datasource.common.QueryParameter;

/**
 * @author Cosmin Cara
 */
public class Sentinel1Params {

    public static QueryParameter POLARISATION = new QueryParameter(String.class, "polarisationMode");
    public static QueryParameter SENSOR_MODE = new QueryParameter(String.class, "sensorOperationalMode");
    public static QueryParameter RELATIVE_ORBIT = new QueryParameter(String.class, "relativeOrbitNumber");
}
