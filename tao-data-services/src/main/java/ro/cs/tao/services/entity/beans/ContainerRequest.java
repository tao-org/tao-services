package ro.cs.tao.services.entity.beans;

import org.springframework.web.multipart.MultipartFile;
import ro.cs.tao.services.entity.conversion.ContainerRequestDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a data transfer object for creating or modifying a container configuration.
 * The class contains properties for storing metadata, descriptors, files, and system-specific attributes
 * related to a container request.
 *
 * This class is designed to be deserialized from JSON using a custom deserializer,
 * {@link ContainerRequestDeserializer}, which handles mapping JSON fields into this object's properties.
 *
 * Implements {@link Serializable} to allow instances of this class to be serialized.
 */
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
