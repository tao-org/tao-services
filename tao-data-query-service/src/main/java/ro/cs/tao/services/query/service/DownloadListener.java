package ro.cs.tao.services.query.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.ProductStatusListener;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Topic;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;

import java.security.Principal;
import java.util.logging.Logger;

@Service("downloadListener")
public class DownloadListener implements ProductStatusListener {
    private final Logger logger = Logger.getLogger(DownloadListener.class.getName());

    @Autowired
    private EOProductProvider productProvider;

    @Override
    public boolean downloadStarted(EOProduct product) {
    	final Principal principal = SessionStore.currentContext().getPrincipal();
    	
        try {
        	// check input quota before download
        	if (product.getRefs() != null && !product.getRefs().contains(principal.getName())) {
        		// do not allow for the download to start if quota exceeded
                if (!UserQuotaManager.getInstance().checkUserInputQuota(principal, product.getApproximateSize())) {
                    Messaging.send(principal, Topic.WARNING.getCategory(), "Quota exceeded");
                    return false;
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
    	
    	final Principal principal = SessionStore.currentContext().getPrincipal();
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
    	final Principal principal = SessionStore.currentContext().getPrincipal();
    	
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
