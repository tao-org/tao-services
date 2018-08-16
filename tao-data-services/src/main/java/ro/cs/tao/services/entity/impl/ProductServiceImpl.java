/*
 * Copyright (C) 2017 CS ROMANIA
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

package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.services.interfaces.ProductService;

import java.util.List;

@Service("productService")
public class ProductServiceImpl extends EntityService<EOProduct> implements ProductService {

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    protected void validateFields(EOProduct entity, List<String> errors) {

    }

    @Override
    public EOProduct findById(String id) throws PersistenceException {
        return null;
    }

    @Override
    public List<EOProduct> list() {
        List<EOProduct> products = null;
        try {
            products = persistenceManager.getEOProducts();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    @Override
    public EOProduct save(EOProduct object) {
        try {
            return persistenceManager.saveEOProduct(object);
        } catch (PersistenceException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public EOProduct update(EOProduct object) throws PersistenceException {
        return null;
    }

    @Override
    public void delete(String id) throws PersistenceException {

    }
}
