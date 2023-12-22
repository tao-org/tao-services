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
package ro.cs.tao.services.cleanup.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.interfaces.CleanupService;
import ro.cs.tao.utils.FileUtilities;

/**
 * Lucian Barbulescu
 */
@Service("cleanupService")
public class CleanupServiceImpl implements CleanupService {

	/**	Logger. */
	private static final Logger logger = Logger.getLogger(CleanupServiceImpl.class.getName());;
	
	/**	Configuration key for deleting invalid products. */
	private static final String DELETE_INVALID_PRODUCTS = "tao.remove.invalid.products";
	
	/**	Database access. */
	@Autowired
	private PersistenceManager persistenceManager;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long cleanupDatabase() {
		return cleanupDatabaseAction();
	}
	
	/**
	 * Periodic action to be executed once a day.
	 * 
	 * @return the number of deleted products
	 */
	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
	private long cleanupDatabaseAction() {
		if (!ConfigurationManager.getInstance().getBooleanValue(DELETE_INVALID_PRODUCTS)) {
			// return without executing
			return 0;
		}
		logger.fine("Starting Database cleanup task!");
		final long startTime = System.currentTimeMillis();
		// get all products from the database
		final List<EOProduct> products = persistenceManager.rasterData().list();
		final List<EOProduct> invalidProducts = new ArrayList<>();
		
		for (EOProduct product : products) {
			
			try {
				
				if (FileUtilities.isURI(product.getLocation())) {
					// check if the URI points to a local file
					final URI locationURI = new URI(product.getLocation());
					if (!"file".equalsIgnoreCase(locationURI.getScheme())) {
						// ignore the current product as it points to an external URI
						continue;
					}
				}
				
				// get the declared location as a Path object
				final Path path = FileUtilities.toPath(product.getLocation());
				if (Files.notExists(path)) {
					//the declared path is invalid
					logger.fine("The declared location (" + product.getLocation() + ") for product " + product.getName() + " is not valid.");
					invalidProducts.add(product);
				}
			} catch (URISyntaxException e) {
				//the declared path is invalid
				logger.fine("The declared location (" + product.getLocation() + ") for product " + product.getName() + " follows the general URI sintax, but is not a valid URI. Exception message: " + e.getMessage());
				
			} catch(InvalidPathException e) {
				//the declared path is invalid
				logger.fine("The declared location (" + product.getLocation() + ") for product " + product.getName() + " is not a Path. Exception message: " + e.getMessage());
			} catch (Exception e) {
				// general exception
				logger.fine("The declared location (" + product.getLocation() + ") for product " + product.getName() + " is not valid. Exception message: " + e.getMessage());
			}
		}
		
		// delete invalid products
		invalidProducts.stream().forEach(p -> {
			try {
				logger.fine("Deleting product " + p.getName());
				persistenceManager.rasterData().delete(p);
			} catch (PersistenceException e) {
				logger.severe("Cannot delete product from the database: " + e.getMessage());
			}
		});
		
		final long endTime = System.currentTimeMillis();
		logger.info("Database cleanup finished in " + ((endTime - startTime) / 1000) + " seconds. Prducts deleted: " + invalidProducts.size());
		
		return invalidProducts.size();
	}

}
