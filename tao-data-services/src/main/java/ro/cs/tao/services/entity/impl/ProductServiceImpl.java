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
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.ProgressNotifier;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.interfaces.ProductService;
import ro.cs.tao.spi.OutputDataHandlerManager;
import ro.cs.tao.spi.ServiceRegistryManager;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.async.Parallel;
import ro.cs.tao.utils.executors.monitoring.ProgressListener;
import ro.cs.tao.workspaces.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("productService")
public class ProductServiceImpl extends EntityService<EOProduct> implements ProductService {
	private static final Set<String> inspectionExclusions = new HashSet<String>() {{
		add(".aux.xml"); add(".png"); add(".jpg"); add(".log");
	}};

	@Autowired
	private EOProductProvider productProvider;

	@Autowired
	private UserProvider userProvider;

	@Override
	protected void validateFields(EOProduct entity, List<String> errors) {

	}

	@Override
	public EOProduct findById(String id) {
		return productProvider.get(id);
	}

	@Override
	public List<EOProduct> list() {
		List<EOProduct> products = null;
		try {
			products = productProvider.list();
		} catch (Exception e) {
			logger.warning(e.getMessage());
		}
		return products;
	}

	@Override
	public List<EOProduct> list(Iterable<String> ids) {
		return productProvider.list(ids);
	}

	@Override
	public List<EOProduct> getByNames(String... names) {
		return productProvider.getProductsByNames(names);
	}

	@Override
	public EOProduct save(EOProduct object) {
		try {
			return productProvider.save(object);
		} catch (PersistenceException e) {
			logger.severe(e.getMessage());
			return null;
		}
	}

	@Override
	public EOProduct update(EOProduct object) throws PersistenceException {
		return productProvider.save(object);
	}

	@Override
	public void delete(String id) {
		try {
			productProvider.delete(id);
		} catch (PersistenceException e) {
			logger.severe(e.getMessage());
		}
	}

	@Override
	public void delete(EOProduct product) {
		try {
			productProvider.delete(product);
		} catch (PersistenceException e) {
			logger.severe(e.getMessage());
		}
	}

	@Override
	public int countAdditionalProductReferences(String componentId, String name) {
		return productProvider.countOtherProductReferences(componentId, name);
	}

	@Override
	public void deleteIfNotReferenced(String refererComponentId, String productName) {
		productProvider.deleteIfNotReferenced(refererComponentId, productName);
	}

	@Override
	public List<EOProduct> inspect(Repository repository, String sourcePath) throws IOException {
		if (StringUtilities.isNullOrEmpty(sourcePath)) {
			sourcePath = "/";
		}
		Set<MetadataInspector> services = ServiceRegistryManager.getInstance()
																.getServiceRegistry(MetadataInspector.class).getServices();
		MetadataInspector inspector;
		if (services == null) {
			throw new IOException("No product inspector found");
		}
		logger.fine("Inspection of " + sourcePath + " starting");
		List<EOProduct> results = new ArrayList<>();
		Path source = Paths.get(repository.resolve(sourcePath));
		final String userId = repository.getUserId();
		Principal principal = new UserPrincipal(userId);
		final ProgressListener progressListener = new ProgressNotifier(principal,
																	   sourcePath,
																	   Topic.TRANSFER_PROGRESS,
																	   new HashMap<String, String>() {{
																		   put("Repository", repository.getId());
																	   }});
		try {
			progressListener.started(sourcePath);
			int count = 1;
			inspector = services.stream()
					.filter(i -> DecodeStatus.INTENDED == i.decodeQualification(source)).findFirst()
					.orElse(services.stream()
							.filter(i -> DecodeStatus.SUITABLE == i.decodeQualification(source))
							.findFirst().orElse(null));
			if (inspector == null) {
				logger.warning("No inspector suitable found for " + source);
			} else {
				final EOProduct eoProduct = inspectProduct(inspector, principal, source);
				if (eoProduct != null) {
					results.add(eoProduct);
				}
				progressListener.notifyProgress(count);
			}
			if (results.isEmpty()) {
				try (Stream<Path> stream = Files.walk(Paths.get(repository.resolve(sourcePath)), 1)) {
					List<Path> files = stream.collect(Collectors.toList());
					files.removeIf(f -> f.equals(source));
					Path userFolder = Paths.get(repository.root());
					final double size = files.size();
					for (Path file : files) {
						try {
							if (inspectionExclusions.stream().anyMatch(file::endsWith)) {
								continue;
							}
							Path targetPath;
							if (!file.toString().startsWith(userFolder.toString())) {
								targetPath = userFolder.resolve(file.getFileName());
								if (!Files.exists(targetPath)) {
									Logger.getLogger(ProductService.class.getName())
											.fine(String.format("Copying %s to %s", file, userFolder.toFile()));
									if (Files.isDirectory(file)) {
										FileUtils.copyDirectoryToDirectory(file.toFile(), userFolder.toFile());
									} else {
										FileUtils.copyFile(file.toFile(), userFolder.toFile());
									}
									FileUtilities.ensurePermissions(targetPath);
								}
							} else {
								targetPath = file;
							}
							inspector = services.stream()
									.filter(i -> DecodeStatus.INTENDED == i.decodeQualification(targetPath)).findFirst()
									.orElse(services.stream()
											.filter(i -> DecodeStatus.SUITABLE == i.decodeQualification(targetPath))
											.findFirst().orElse(null));
							if (inspector == null) {
								logger.warning("No inspector suitable found for " + file);
								continue;
							}
							final EOProduct eoProduct = inspectProduct(inspector, principal, targetPath);
							if (eoProduct != null) {
								results.add(eoProduct);
							}
						} catch (Exception e1) {
							logger.warning(String.format("Import for %s failed. Reason: %s", file, e1.getMessage()));
						}
						progressListener.notifyProgress((double) count++ / size);
					}
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			progressListener.ended();
			logger.fine("Inspection of " + sourcePath + " completed");
			Messaging.send(principal, Topic.INFORMATION.getCategory(), "Inspection of " + sourcePath + " found " + results.size() + " products");
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
				productProvider.save(product);
				UserQuotaManager.getInstance().updateUserInputQuota(principal);
				count++;
			} catch (PersistenceException | QuotaException e) {
				logger.severe(e.getMessage());
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
		try (Stream<Path> stream = Files.walk(srcPath, 1)) {
			List<Path> folders = stream.collect(Collectors.toList());
			Path publicFolder = Paths.get(SystemVariable.USER_WORKSPACE.value());
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
							EOProduct existing = productProvider.get(product.getId());
							if (existing == null) {
								product.setEntryPoint(metadata.getEntryPoint());
								product.setVisibility(Visibility.PUBLIC);
								product.setAcquisitionDate(metadata.getAquisitionDate());
								if (metadata.getSize() != null) {
									product.setApproximateSize(metadata.getSize());
								}
							} else {
								product = existing;
							}
							product.setProductStatus(ProductStatus.DOWNLOADED);
							product.addReference(SessionStore.currentContext().getPrincipal().getName());
							productProvider.save(product);
							count.getAndIncrement();

						} else {
							logger.info(String.format("Skipping %s. Reason: unable to read metadata", targetPath));
						}
					}
				} catch (Exception e1) {
					logger.warning(String.format("Import for %s failed. Reason: %s", folder, e1.getMessage()));
				}
			});
		} catch (Exception e) {
			throw new IOException(e);
		}
		// update user quota
		UserQuotaManager.getInstance().updateUserInputQuota(principal);

		if (skipped.get() != 0) {
			// throw am exception to force an error response sent to the user
			throw new QuotaException(String.format("Not all products found were imported because you have reached your input quota! "
														   + "Imported products: %d, Skipped products: %d", count.get(), skipped.get()));
		}

		return count.get();
	}

	@Override
	public List<String> checkExisting(String... names) {
		return productProvider.getExistingProductNames(names);
	}

	private EOProduct inspectProduct (MetadataInspector inspector, Principal principal, Path targetPath) throws Exception {
		List<EOProduct> list = productProvider.getByLocation(targetPath.toAbsolutePath().toUri().toString());
		EOProduct product = null;
		if (list.isEmpty()) {
			MetadataInspector.Metadata metadata = inspector.getMetadata(targetPath);
			if (metadata != null) {
				product = metadata.toProductDescriptor(targetPath);
				product.setEntryPoint(metadata.getEntryPoint());
				product.addReference(principal.getName());
				product.setVisibility(Visibility.PUBLIC);
				product.setAcquisitionDate(metadata.getAquisitionDate());
				if (metadata.getSize() != null) {
					product.setApproximateSize(metadata.getSize());
				}
				if (metadata.getProductId() != null) {
					product.setId(metadata.getProductId());
				}
				product.setProductStatus(ProductStatus.PRODUCED);
			}
		} else {
			product = list.get(0);
		}
		if (product != null) {
			try {
				Path quicklook = OutputDataHandlerManager.getInstance().applyHandlers(targetPath);
				if (quicklook != null) {
					product.setQuicklookLocation(quicklook.toString());
				}
			} catch (Exception e) {
				logger.warning(String.format("Unable to create quicklook for %s. Reason: %s", targetPath, e.getMessage()));
			}
			product = productProvider.save(product);
			logger.finest("Inspection of " + targetPath + " completed");
			if (Files.isRegularFile(targetPath)) {
				Files.deleteIfExists(Paths.get(targetPath + ".aux.xml"));
			}
		}
		return product;
	}
}
