package ro.cs.tao.services.workspace.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.entity.impl.FileStorageService;
import ro.cs.tao.services.workspace.model.TransferableItem;

import java.util.HashMap;
import java.util.Map;

@Service("transferService")
public class TransferService {
    private final Map<String, UserTransferService> serviceMap;

    @Autowired
    private FileStorageService fileStorageService;

    private TransferService() {
        this.serviceMap = new HashMap<>();
    }

    /**
     * Submits a transfer request to the service queue.
     * @param requests   The requests
     */
    public synchronized void request(TransferableItem... requests) {
        if (requests != null && requests.length > 0) {
            final String user = requests[0].getUser();
            if (!this.serviceMap.containsKey(user)) {
                this.serviceMap.put(user, new UserTransferService(user, fileStorageService));
            }
            this.serviceMap.get(user).request(requests);
        }
    }

}