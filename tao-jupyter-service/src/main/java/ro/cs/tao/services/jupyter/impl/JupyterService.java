package ro.cs.tao.services.jupyter.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import ro.cs.tao.docker.DockerVolumeMap;
import ro.cs.tao.docker.ExecutionConfiguration;
import ro.cs.tao.services.jupyter.model.Cell;
import ro.cs.tao.services.jupyter.model.Notebook;
import ro.cs.tao.services.jupyter.model.NotebookParams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service("jupyterService")
public class JupyterService {
    public int USER_CELL = 0; // editable cell where user can modify content.

    public int generateNotebook(NotebookParams ws) {
        // generate default ipynb template
        Notebook notebook = new Notebook();
        // inject params if any
        if (!ws.isNull()) {
            insertParam(notebook, ws);
        }
        // deserialize
        var mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            var json = mapper.writeValueAsString(notebook);
            final DockerVolumeMap map = ExecutionConfiguration.getMasterContainerVolumeMap();
            final String userMappedVolume = map.getHostWorkspaceFolder().endsWith("/")
                    ? map.getHostWorkspaceFolder()
                    : (map.getHostWorkspaceFolder() + "/");
            Files.write(Paths.get(userMappedVolume + "templateNotebook.ipynb").toFile().toPath(), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public void insertParam(Notebook notebook, NotebookParams params) {
        Cell targetCell = notebook.getCellById(USER_CELL);
        // Script to be added / edited here
        String cellContent = String.format("%s = %s", params.getName(), params.getDefaultValue());
        targetCell.setSource(Collections.singletonList(cellContent));

        notebook.setCells(Collections.singletonList(targetCell));
    }
}