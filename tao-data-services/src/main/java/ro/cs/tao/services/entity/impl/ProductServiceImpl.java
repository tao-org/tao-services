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

package ro.cs.tao.services.entity.impl;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.component.SystemVariable;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.eodata.enums.Visibility;
import ro.cs.tao.eodata.metadata.DecodeStatus;
import ro.cs.tao.eodata.metadata.MetadataInspector;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.async.Parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.Date;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	public List<EOProduct> list(Iterable<String> ids) {
		return persistenceManager.getEOProducts(ids);
	}

	@Override
	public List<EOProduct> getByNames(String... names) {
		return persistenceManager.getProductsByNames(names);
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

	@Override
	public List<EOProduct> inspect(Path sourcePath) throws IOException {
		if (sourcePath == null || !Files.exists(sourcePath)) {
			throw new IOException("Source directory not found");
		}
		Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
				.getServiceRegistry(MetadataInspector.class).getServices();
		MetadataInspector inspector;
		if (services == null) {
			throw new IOException("No product inspector found");
		}
		List<EOProduct> results = new ArrayList<>();
		try {
			List<Path> folders = Files.walk(sourcePath, 1).collect(Collectors.toList());
			Path publicFolder = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
			for (Path folder : folders) {
				try {
					if (Files.isDirectory(folder) && !folder.equals(sourcePath)) {
						Path targetPath;
						if (!folder.toString().startsWith(publicFolder.toString())) {
							targetPath = publicFolder.resolve(folder.getFileName());
							if (!Files.exists(targetPath)) {
								Logger.getLogger(ProductService.class.getName())
										.fine(String.format("Copying %s to %s", folder, publicFolder.toFile()));
								FileUtils.copyDirectoryToDirectory(folder.toFile(), publicFolder.toFile());
								FileUtilities.ensurePermissions(targetPath);
							}
						} else {
							targetPath = folder;
						}
						inspector = services.stream()
								.filter(i -> DecodeStatus.INTENDED == i.decodeQualification(targetPath)).findFirst()
								.orElse(services.stream()
										.filter(i -> DecodeStatus.SUITABLE == i.decodeQualification(targetPath))
										.findFirst().orElse(null));
						if (inspector == null) {
							continue;
						}
						MetadataInspector.Metadata metadata = inspector.getMetadata(targetPath);
						if (metadata != null) {
							EOProduct product = metadata.toProductDescriptor(targetPath);
							product.setEntryPoint(metadata.getEntryPoint());
							product.addReference(SessionStore.currentContext().getPrincipal().getName());
							product.setVisibility(Visibility.PUBLIC);
							if (metadata.getAquisitionDate() != null) {
								product.setAcquisitionDate(Date
										.from(metadata.getAquisitionDate().atZone(ZoneId.systemDefault()).toInstant()));
							}
							if (metadata.getSize() != null) {
								product.setApproximateSize(metadata.getSize());
							}
							if (metadata.getProductId() != null) {
								product.setId(metadata.getProductId());
							}
							results.add(product);
						}
					}
				} catch (Exception e1) {
					Logger.getLogger(ProductService.class.getName())
							.warning(String.format("Import for %s failed. Reason: %s", folder, e1.getMessage()));
				}
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
		return results;
	}

	@Override
	public int importProducts(List<EOProduct> products) {
		if (products == null) {
			return 0;
		}
		int count = 0;
		final Principal principal = SessionStore.currentContext().getPrincipal();
		for (EOProduct product : products) {
			try {
				product.setProductStatus(ProductStatus.DOWNLOADED);
				product.addReference(principal.getName());
				persistenceManager.saveEOProduct(product);
				UserQuotaManager.getInstance().updateUserInputQuota(principal);
				count++;
			} catch (PersistenceException e) {
				e.printStackTrace();
			} catch (QuotaException e) {
				e.printStackTrace();
			}
		}
		return count;
	}

	@Override
	public int importProducts(String sourcePath, boolean linkOnly) throws IOException, QuotaException {
		Path srcPath;
		if (sourcePath == null || !Files.exists((srcPath = Paths.get(sourcePath)))) {
			throw new IOException("Source directory not found");
		}
		Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
				.getServiceRegistry(MetadataInspector.class).getServices();
		if (services == null) {
			throw new IOException("No product inspector found");
		}
		final AtomicInteger count = new AtomicInteger(0);
		final AtomicInteger skipped = new AtomicInteger(0);
		final AtomicLong addedSize = new AtomicLong(0);
		final Object lock = new Object();
		final AtomicBoolean firstFail = new AtomicBoolean(true);
		final Principal principal = SessionStore.currentContext().getPrincipal();
		final List<String> skippedProducts = new ArrayList<String>();
		try {
			List<Path> folders = Files.walk(srcPath, 1).collect(Collectors.toList());
			Path publicFolder = Paths.get(SystemVariable.SHARED_WORKSPACE.value());
			Parallel.For(0, folders.size(), (idx) -> {
				Path folder = folders.get(idx);
				try {
					if (Files.isDirectory(folder) && !folder.equals(srcPath)) {
						// estimate the size of the product
						final long size = FileUtilities.folderSize(folder);
						
						synchronized (lock) {
							// check user quota. Return if no more quota available
							if (!UserQuotaManager.getInstance().checkUserInputQuota(principal, addedSize.get() + size)) {
								if (firstFail.get()) {
									// allow first fail.
									firstFail.set(false);
								} else {
									// increase the skipped products number
									skipped.getAndIncrement();
									return;
								}
							}
							addedSize.addAndGet(size);
						}

						Path targetPath;
						if (!folder.toString().startsWith(publicFolder.toString())) {
							targetPath = publicFolder.resolve(folder.getFileName());
							if (!Files.exists(targetPath)) {
								Logger.getLogger(ProductService.class.getName()).fine(String.format("%s %s to %s",
										linkOnly ? "Linking" : "Copying", folder, publicFolder.toFile()));
								if (linkOnly) {
									FileUtilities.link(folder, targetPath);
								} else {
									FileUtils.copyDirectoryToDirectory(folder.toFile(), publicFolder.toFile());
									FileUtilities.ensurePermissions(targetPath);
								}
							}
						} else {
							targetPath = folder;
						}
						MetadataInspector inspector = services.stream()
								.filter(i -> DecodeStatus.INTENDED == i.decodeQualification(targetPath)).findFirst()
								.orElse(services.stream()
										.filter(i -> DecodeStatus.SUITABLE == i.decodeQualification(targetPath))
										.findFirst().orElse(null));
						if (inspector == null) {
							return;
						}
						MetadataInspector.Metadata metadata = inspector.getMetadata(targetPath);
						if (metadata != null) {
							EOProduct product = metadata.toProductDescriptor(targetPath);
							if (metadata.getProductId() != null) {
								product.setId(metadata.getProductId());
							}
							EOProduct existing = persistenceManager.getEOProduct(product.getId());
							if (existing == null) {
								product.setEntryPoint(metadata.getEntryPoint());
								product.setVisibility(Visibility.PUBLIC);
								if (metadata.getAquisitionDate() != null) {
									product.setAcquisitionDate(Date.from(
											metadata.getAquisitionDate().atZone(ZoneId.systemDefault()).toInstant()));
								}
								if (metadata.getSize() != null) {
									product.setApproximateSize(metadata.getSize());
								}
							} else {
								product = existing;
							}
							product.setProductStatus(ProductStatus.DOWNLOADED);
							product.addReference(SessionStore.currentContext().getPrincipal().getName());
							persistenceManager.saveEOProduct(product);
							count.getAndIncrement();

						} else {
							Logger.getLogger(ProductService.class.getName())
									.info(String.format("Skipping %s. Reason: unable to read metadata", targetPath));
						}
					}
				} catch (Exception e1) {
					Logger.getLogger(ProductService.class.getName())
							.warning(String.format("Import for %s failed. Reason: %s", folder, e1.getMessage()));
				}
			});
		} catch (Exception e) {
			throw new IOException(e);
		}
		// update user quota
		UserQuotaManager.getInstance().updateUserInputQuota(principal);

		if (skipped.get() != 0) {
			// throw am exception to force an error response sent to the user
			QuotaException ex = new QuotaException(String.format(
					"Not all products found were imported because you have reached your input quota! "
					+ "Imported products: %d, Skipped products: %d", count.get(), skipped.get()));
			throw ex;
		}

		return count.get();
	}

	@Override
	public List<String> checkExisting(String... names) {
		return persistenceManager.getExistingProductNames(names);
	}
}
