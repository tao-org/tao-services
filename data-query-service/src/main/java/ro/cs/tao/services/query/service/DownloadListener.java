package ro.cs.tao.services.query.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.datasource.ProductStatusListener;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.persistence.exception.PersistenceException;

import java.util.logging.Logger;

@Service("downloadListener")
public class DownloadListener implements ProductStatusListener {
    private Logger logger = Logger.getLogger(DownloadListener.class.getName());

    @Autowired
    private PersistenceManager persistenceManager;

    @Override
    public boolean downloadStarted(EOProduct product) {
        product.setProductStatus(ProductStatus.DOWNLOADING);
        try {
            persistenceManager.saveEOProduct(product);
            return true;
        } catch (PersistenceException e) {
            logger.severe(String.format("Updating product %s failed. Reason: %s", product.getName(), e.getMessage()));
            return false;
        }
    }

    @Override
    public void downloadCompleted(EOProduct product) {
        product.setProductStatus(ProductStatus.DOWNLOADED);
        try {
            persistenceManager.saveEOProduct(product);
        } catch (PersistenceException e) {
            logger.severe(String.format("Updating product %s failed. Reason: %s", product.getName(), e.getMessage()));
        }
    }

    @Override
    public void downloadFailed(EOProduct product, String reason) {
        product.setProductStatus(ProductStatus.FAILED);
        try {
            persistenceManager.saveEOProduct(product);
            logger.warning(String.format("Product %s not downloaded. Reason: %s", product.getName(), reason));
        } catch (PersistenceException e) {
            logger.severe(String.format("Updating product %s failed. Reason: %s", product.getName(), e.getMessage()));
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
