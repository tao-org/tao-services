package ro.cs.tao.services.entity.beans;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.services.entity.conversion.ContainerRequestDeserializer;

import java.io.Serializable;
import java.util.List;

@JsonDeserialize(using = ContainerRequestDeserializer.class)
public class ContainerRequest implements Serializable {
    private String name;
    private String description;
    private boolean system;
    private MultipartFile containerLogo;
    private String containerDescriptor;
    private String componentDescriptors;
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

    public String getContainerDescriptor() { return containerDescriptor; }
    public void setContainerDescriptor(String containerDescriptor) { this.containerDescriptor = containerDescriptor; }

    public String getComponentDescriptors() { return componentDescriptors; }
    public void setComponentDescriptors(String componentDescriptors) { this.componentDescriptors = componentDescriptors; }

    public List<MultipartFile> getDockerFiles() { return dockerFiles; }
    public void setDockerFiles(List<MultipartFile> dockerFiles) { this.dockerFiles = dockerFiles; }

    public List<MultipartFile> getAuxiliaryFiles() { return auxiliaryFiles; }
    public void setAuxiliaryFiles(List<MultipartFile> auxiliaryFiles) { this.auxiliaryFiles = auxiliaryFiles; }
}
