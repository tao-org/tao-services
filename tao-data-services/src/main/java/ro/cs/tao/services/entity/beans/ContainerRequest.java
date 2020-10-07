package ro.cs.tao.services.entity.beans;

import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.component.ProcessingComponent;
import ro.cs.tao.docker.Container;

import java.util.List;

public class ContainerRequest {
    private String name;
    private String description;
    private boolean system;
    private MultipartFile containerLogo;
    private Container containerDescriptor;
    private ProcessingComponent[] componentDescriptors;
    private List<MultipartFile> dockerFiles;
    private List<MultipartFile> auxiliaryFiles;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public MultipartFile getContainerLogo() { return containerLogo; }
    public void setContainerLogo(MultipartFile containerLogo) { this.containerLogo = containerLogo; }

    public Container getContainerDescriptor() { return containerDescriptor; }
    public void setContainerDescriptor(Container containerDescriptor) { this.containerDescriptor = containerDescriptor; }

    public ProcessingComponent[] getComponentDescriptors() { return componentDescriptors; }
    public void setComponentDescriptors(ProcessingComponent[] componentDescriptors) { this.componentDescriptors = componentDescriptors; }

    public List<MultipartFile> getDockerFiles() { return dockerFiles; }
    public void setDockerFiles(List<MultipartFile> dockerFiles) { this.dockerFiles = dockerFiles; }

    public List<MultipartFile> getAuxiliaryFiles() { return auxiliaryFiles; }
    public void setAuxiliaryFiles(List<MultipartFile> auxiliaryFiles) { this.auxiliaryFiles = auxiliaryFiles; }
}
