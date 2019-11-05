package ro.cs.tao.services.query.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.ProductStatusListener;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;
import ro.cs.tao.quota.QuotaException;
import ro.cs.tao.quota.UserQuotaManager;
import ro.cs.tao.security.SessionStore;

import java.security.Principal;
import java.util.logging.Logger;

@Service("downloadListener")
public class DownloadListener implements ProductStatusListener {
    private Logger logger = Logger.getLogger(DownloadListener.class.getName());

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public boolean downloadStarted(EOProduct product) {
    	final Principal principal = SessionStore.currentContext().getPrincipal();
    	
        try {
        	// check input quota before download
        	if (product.getRefs() != null && !product.getRefs().contains(principal.getName()) && !UserQuotaManager.getInstance().checkUserInputQuota(principal, product.getApproximateSize())) {
        		// do not allow for the download to start
        		return false;
        	}
    	
        	// check if the product already exists
        	final EOProduct oldProd = persistenceManager.getEOProduct(product.getId());
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
            persistenceManager.saveEOProduct(product);
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
        	final EOProduct oldProd = persistenceManager.getEOProduct(product.getId());
        	if (oldProd != null) {
        		product.setRefs(oldProd.getRefs());
        	}
        	
            product.setProductStatus(ProductStatus.DOWNLOADED);
            // update the product's reference
            product.addReference(principal.getName());
            persistenceManager.saveEOProduct(product);
            
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
            persistenceManager.saveEOProduct(product);
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
}
