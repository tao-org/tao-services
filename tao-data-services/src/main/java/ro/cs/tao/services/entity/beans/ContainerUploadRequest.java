package ro.cs.tao.services.entity.beans;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ContainerUploadRequest {
    private String name;
    private String description;
    private boolean system;
    private String jsonDescriptor;
    private List<MultipartFile> dockerFiles;
    private List<MultipartFile> auxiliaryFiles;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public String getJsonDescriptor() { return jsonDescriptor; }
    public void setJsonDescriptor(String jsonDescriptor) { this.jsonDescriptor = jsonDescriptor; }

    public List<MultipartFile> getDockerFiles() { return dockerFiles; }
    public void setDockerFiles(List<MultipartFile> dockerFiles) { this.dockerFiles = dockerFiles; }

    public List<MultipartFile> getAuxiliaryFiles() { return auxiliaryFiles; }
    public void setAuxiliaryFiles(List<MultipartFile> auxiliaryFiles) { this.auxiliaryFiles = auxiliaryFiles; }
}
