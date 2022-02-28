package ro.cs.tao.services.workspace.controllers;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.persistence.WorkspaceProvider;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.impl.TransferService;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.topology.Workspace;

import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/transfer")
public class TransferController extends BaseController {

    @Autowired
    private TransferService transferService;
    @Autowired
    private WorkspaceProvider workspaceProvider;

    @RequestMapping(value = "/move/", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> moveTo(@RequestParam("sourceWorkspace") int srcWksId,
                                                     @RequestParam("sourceObject") String jsonFileObject,
                                                     @RequestParam("destinationWorkspace") int dstWksId,
                                                     @RequestParam("destinationPath") String dstFolder) {
        try {
            final FileObject fileObject = new ObjectMapper().reader(FileObject.class).readValue(jsonFileObject);
            final Workspace srcWorkspace = workspaceProvider.get(srcWksId);
            final Workspace dstWorkspace = workspaceProvider.get(dstWksId);
            int count = 0;
            if (fileObject.isFolder()) {
                final String root = fileObject.getRelativePath();
                final StorageService service = transferService.getService(srcWorkspace);
                final List<FileObject> files = service.listFiles(Paths.get(fileObject.getRelativePath()), null, null);
                for (FileObject fObject : files) {
                    if (!fObject.isFolder()) {
                        final String path = dstFolder + fObject.getRelativePath().replace(root, "");
                        queueFile(fObject, srcWorkspace, dstWorkspace, path);
                        count++;
                    }
                }
            } else {
                queueFile(fileObject, srcWorkspace, dstWorkspace, dstFolder);
                count = 1;
            }
            return prepareResult(String.format("Transfer for '%s' queued (%d files)", fileObject.getRelativePath(), count),
                                 ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private void queueFile(FileObject file, Workspace source, Workspace destination, String folder) {
        final TransferRequest request = new TransferRequest(source, file, destination, folder);
        transferService.request(request);
    }
}
