package ro.cs.tao.services.entity.beans;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ContainerUploadRequest {
    private String name;
    private String description;
    private boolean system;
    private MultipartFile containerLogo;
    private MultipartFile jsonContainerDescriptor;
    private MultipartFile jsonComponentsDescriptor;
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

    public MultipartFile getJsonContainerDescriptor() { return jsonContainerDescriptor; }
    public void setJsonContainerDescriptor(MultipartFile jsonContainerDescriptor) { this.jsonContainerDescriptor = jsonContainerDescriptor; }

    public MultipartFile getJsonComponentsDescriptor() { return jsonComponentsDescriptor; }
    public void setJsonComponentsDescriptor(MultipartFile jsonComponentsDescriptor) { this.jsonComponentsDescriptor = jsonComponentsDescriptor; }

    public List<MultipartFile> getDockerFiles() { return dockerFiles; }
    public void setDockerFiles(List<MultipartFile> dockerFiles) { this.dockerFiles = dockerFiles; }

    public List<MultipartFile> getAuxiliaryFiles() { return auxiliaryFiles; }
    public void setAuxiliaryFiles(List<MultipartFile> auxiliaryFiles) { this.auxiliaryFiles = auxiliaryFiles; }
}
