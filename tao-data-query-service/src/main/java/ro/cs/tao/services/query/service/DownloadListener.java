package ro.cs.tao.services.query.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.EnumUtils;
import ro.cs.tao.datasource.ProductStatusListener;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.UserPrincipal;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of the {@link ProductStatusListener} interface to handle various events during the
 * lifecycle of a product download, such as start, completion, failure, abortion, and queuing.
 * This class is annotated as a service and orchestrates actions based on the state of the product
 * and user-related quotas.
 *
 * Responsibilities include:
 * - Validating user input quotas before initiating a download.
 * - Persisting product updates and maintaining consistency across references and statuses.
 * - Handling quota rolls and updates in case of failures or completions.
 * - Managing lifecycle states such as downloading, downloaded, queued, or failed.
 *
 * This implementation also logs all significant events and errors for monitoring.
 *
 * It integrates with the {@link EOProductProvider} for product-related persistence and the
 * {@link UserQuotaManager} for quota validations and updates.
 */
@Service("downloadListener")
public class DownloadListener implements ProductStatusListener {
    private final Logger logger = Logger.getLogger(DownloadListener.class.getName());

    @Autowired
    private EOProductProvider productProvider;

    @Override
    public boolean downloadStarted(EOProduct product) {
    	final Principal principal;
        if (product.getAttributeValue("principal") != null) {
            principal = new UserPrincipal(product.getAttributeValue("principal"));
        } else {
            principal = SessionStore.currentContext().getPrincipal();
        }
    	
        try {
        	// check input quota before download
            Set<String> refs = null;
        	if ((refs = product.getRefs()) != null && !refs.contains(principal.getName())) {
        		// do not allow for the download to start if quota exceeded
                if (!UserQuotaManager.getInstance().checkUserInputQuota(principal, product.getApproximateSize())) {
                    // For symlinks, the quota is not affected and hence should continue
                    final String fetchMode = product.getAttributeValue("fetch");
                    if (fetchMode != null && EnumUtils.getEnumConstantByName(FetchMode.class, fetchMode).value() < 4) {
                        Messaging.send(principal, Topic.WARNING.getCategory(), "Quota exceeded");
                        return false;
                    } else {
                        product.removeAttribute("fetch");
                    }
                }
        	}
    	
        	// check if the product already exists
        	final EOProduct oldProd = productProvider.get(product.getId());
        	if (oldProd != null) {
        		// if the product is failed or downloading, copy its references but continue with the download
        		if (oldProd.getProductStatus() == ProductStatus.FAILED || oldProd.getProductStatus() == ProductStatus.QUERIED) {
        			product.setRefs(oldProd.getRefs());
        		} else {
        			// add the current user as reference
        			product.setRefs(oldProd.getRefs());
        			// copy the status of the previous product
        			product.setProductStatus(oldProd.getProductStatus());

        			// update the user's input quota
                    UserQuotaManager.getInstance().updateUserInputQuota(principal);
            		// do not allow for the download to start
        			return false;
        		}
        	}
        	
        	product.addReference(principal.getName());
        	product.setProductStatus(ProductStatus.DOWNLOADING);
            productProvider.save(product);
            // update the user's input quota
            UserQuotaManager.getInstance().updateUserInputQuota(principal);
            return true;
        } catch (PersistenceException e) {
            logger.severe(String.format("Updating product %s failed. Reason: %s", product.getName(), e.getMessage()));
            return false;
        } catch (QuotaException  e) {
        	logger.severe(String.format("Cannot update the input quota for user %s. Reason: %s", principal.getName(), e.getMessage()));
        	return false;
        }
    }

    @Override
    public void downloadCompleted(EOProduct product) {
        final Principal principal;
        if (product.getAttributeValue("principal") != null) {
            principal = new UserPrincipal(product.getAttributeValue("principal"));
            product.removeAttribute("principal");
        } else {
            principal = SessionStore.currentContext().getPrincipal();
        }
        try {
        	// re-update the references, in case some other user tried to download this product after 
        	// the current user started
        	final EOProduct oldProd = productProvider.get(product.getId());
        	if (oldProd != null) {
        		product.setRefs(oldProd.getRefs());
        	}
        	
            product.setProductStatus(ProductStatus.DOWNLOADED);
            // update the product's reference
            product.addReference(principal.getName());
            productProvider.save(product);
            
            // update the user's input quota
            UserQuotaManager.getInstance().updateUserInputQuota(principal);
        } catch (PersistenceException e) {
            logger.severe(String.format("Updating product %s failed. Reason: %s", product.getName(), e.getMessage()));
        } catch (QuotaException  e) {
        	logger.severe(String.format("Cannot update the input quota for user %s. Reason: %s", principal.getName(), e.getMessage()));
        }
    }

    @Override
    public void downloadFailed(EOProduct product, String reason) {
        final Principal principal;
        if (product.getAttributeValue("principal") != null) {
            principal = new UserPrincipal(product.getAttributeValue("principal"));
            product.removeAttribute("principal");
        } else {
            principal = SessionStore.currentContext().getPrincipal();
        }
    	
        try {
        	product.removeReference(principal.getName());
            product.setProductStatus(ProductStatus.FAILED);
            productProvider.save(product);
            // roll back the user's quota
            UserQuotaManager.getInstance().updateUserInputQuota(principal);
            logger.warning(String.format("Product %s not downloaded. Reason: %s", product.getName(), reason));
        } catch (PersistenceException e) {
            logger.severe(String.format("Updating product %s failed. Reason: %s", product.getName(), e.getMessage()));
        } catch (QuotaException  e) {
        	logger.severe(String.format("Cannot update the input quota for user %s. Reason: %s", principal.getName(), e.getMessage()));
        }
    }

    @Override
    public void downloadAborted(EOProduct product, String reason) {
        downloadFailed(product, reason);
    }

    @Override
    public void downloadIgnored(EOProduct product, String reason) {
        //No-op
    }

    @Override
    public void downloadQueued(EOProduct product, String reason) {
        product.setProductStatus(ProductStatus.QUEUED);
        logger.warning(String.format("Product %s not downloaded. Reason: %s", product.getName(), reason));
    }
}
