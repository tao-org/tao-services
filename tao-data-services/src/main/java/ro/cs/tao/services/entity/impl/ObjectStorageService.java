package ro.cs.tao.services.entity.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.cs.tao.persistence.AuxiliaryDataProvider;
import ro.cs.tao.persistence.EOProductProvider;
import ro.cs.tao.persistence.VectorDataProvider;
import ro.cs.tao.services.interfaces.StorageService;
import ro.cs.tao.services.model.FileObject;
import ro.cs.tao.topology.openstack.SwiftService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Service("objectStorageService")
public class ObjectStorageService implements StorageService<byte[]> {
    @Autowired
    private EOProductProvider productProvider;
    @Autowired
    private VectorDataProvider vectorDataProvider;
    @Autowired
    private AuxiliaryDataProvider auxiliaryDataProvider;

    private final SwiftService swiftService = new SwiftService();

    @Override
    public Path createFolder(String folderRelativePath, boolean userOnly) throws IOException {
        final Path path = Paths.get(folderRelativePath);
        return Paths.get(swiftService.createFolder(path.getName(0).toString(),
                                                   "/" + path.getName(0).relativize(path)));
    }

    @Override
    public void storeUserFile(byte[] object, String relativeFolder, String description) throws Exception {

    }

    @Override
    public void storePublicFile(byte[] object, String relativeFolder, String description) throws Exception {

    }

    @Override
    public void remove(String name) throws IOException {

    }

    @Override
    public void move(String source, String destination) throws IOException {

    }

    @Override
    public List<FileObject> listUploaded() throws IOException {
        return null;
    }

    @Override
    public List<FileObject> listPublicWorkspace() throws IOException {
        return null;
    }

    @Override
    public List<FileObject> listUploaded(String userName) throws IOException {
        return null;
    }

    @Override
    public List<FileObject> listUserWorkspace(String userName) throws IOException {
        return null;
    }

    @Override
    public List<FileObject> listFiles(Path fromPath, Set<Path> exclusions) throws IOException {
        return null;
    }

    @Override
    public List<FileObject> getWorkflowResults(long workflowId) throws IOException {
        return null;
    }

    @Override
    public List<FileObject> getJobResults(long jobId) throws IOException {
        return null;
    }
}
