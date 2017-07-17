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

package ro.cs.tao.eodata.serialization;

import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Cosmin Cara
 */
public class CRSAdapter extends XmlAdapter<CoordinateReferenceSystem, String> {
    private CRSFactory factory;

    public CRSAdapter() {
        this.factory = ReferencingFactoryFinder.getCRSFactory(null);
    }

    public String unmarshal(CoordinateReferenceSystem v) throws Exception {
        return v.getName().getCode();
    }

    public CoordinateReferenceSystem marshal(String v) throws Exception {
        return this.factory.createFromWKT(v);
    }
}
