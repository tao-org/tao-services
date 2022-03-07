package ro.cs.tao.services.workspace.controllers;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.persistence.WorkspaceProvider;
import ro.cs.tao.serialization.BaseSerializer;
import ro.cs.tao.serialization.MediaType;
import ro.cs.tao.serialization.SerializerFactory;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.services.workspace.impl.TransferService;
import ro.cs.tao.services.workspace.model.TransferRequest;
import ro.cs.tao.topology.Workspace;

import java.nio.file.Paths;
import java.util.ArrayList;
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
            List<FileObject> requestedFiles;
            try {
                final FileObject fileObject = new ObjectMapper().reader(FileObject.class).readValue(jsonFileObject);
                requestedFiles = new ArrayList<>();
                requestedFiles.add(fileObject);
            } catch (Exception e) {
                BaseSerializer<FileObject> serializer = SerializerFactory.create(FileObject.class, MediaType.JSON);
                requestedFiles = serializer.deserialize(FileObject.class, jsonFileObject);
            }
            final Workspace srcWorkspace = workspaceProvider.get(srcWksId);
            final Workspace dstWorkspace = workspaceProvider.get(dstWksId);
            int count = 0;
            long size = 0;
            if (!dstFolder.endsWith("/")) {
                dstFolder += "/";
            }
            String targetFolder;
            for (FileObject fileObject : requestedFiles) {
                String root = fileObject.getRelativePath();
                size = fileObject.getSize();
                targetFolder = dstFolder;
                if (fileObject.isFolder()) {
                    final int idx = root.indexOf('/');
                    if (idx > 0 && idx != root.length() - 1) {
                        targetFolder += root.substring(idx + 1);
                    }
                    final StorageService service = transferService.getService(srcWorkspace);
                    final List<FileObject> files = service.listTree(Paths.get(fileObject.getRelativePath()));
                    for (FileObject fObject : files) {
                        if (!fObject.isFolder()) {
                            final String path = dstWorkspace.getParameters().get(dstWorkspace.getType().rootKey())
                                    + "/" + currentUser() + "/" + targetFolder + fObject.getRelativePath().replace(root, "");
                            queueFile(root, fObject, srcWorkspace, dstWorkspace, path);
                            size += fObject.getSize();
                            count++;
                        }
                    }
                } else {
                    queueFile(root, fileObject, srcWorkspace, dstWorkspace,
                              dstWorkspace.getParameters().get(dstWorkspace.getType().rootKey())
                                      + "/" + currentUser() + "/" + targetFolder);
                    count = 1;
                }
            }
            return prepareResult(String.format("Transfer queued (%d files, %d bytes)", count, size),
                                 ResponseStatus.SUCCEEDED);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private void queueFile(String batchId, FileObject file, Workspace source, Workspace destination, String folder) {
        final TransferRequest request = new TransferRequest(batchId, source, file, destination, folder);
        transferService.request(request);
    }
}
