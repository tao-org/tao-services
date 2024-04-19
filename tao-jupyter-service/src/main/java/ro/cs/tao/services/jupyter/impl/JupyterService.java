package ro.cs.tao.services.jupyter.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.jupyter.model.Cell;
import ro.cs.tao.services.jupyter.model.Notebook;
import ro.cs.tao.services.jupyter.model.NotebookParams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service("jupyterService")
public class JupyterService {
    public int USER_CELL = 0; // editable cell where user can modify content.

    public int generateNotebook(Path path, List<NotebookParams> ws) {
        // generate default ipynb template
        Notebook notebook = new Notebook();
        // filter and remove unsuitable parameters
        ws.removeIf(NotebookParams::isNull);
        // inject params if any
        insertParam(notebook, ws);
        // deserialize
        writeNotebook(path, notebook);
        return 0;
    }

    public void insertParam(Notebook notebook, List<NotebookParams> params) {
        Cell targetCell = notebook.getCellById(USER_CELL);
        // Script to be added / edited here
        StringBuilder cellContent = new StringBuilder();
        // inject every variable into source code section
        for (NotebookParams param : params) {
            cellContent.append(String.format("%s = %s\n", param.getName(), param.getDefaultValue()));
        }
        // Update cell source code
        targetCell.setSource(Collections.singletonList(cellContent.toString()));
        // Update notebook with new cell
        notebook.setCells(Collections.singletonList(targetCell));
    }

    public void writeNotebook(Path path, Notebook notebook) {
        var mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            var json = mapper.writeValueAsString(notebook);
            Files.write(path.resolve("templateNotebook.ipynb"), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readNotebook(Path path)
    {
        Path notebookPath = Paths.get(path.toString() + path.getRoot().toString() + "templateNotebook.ipynb");
        try {
            ObjectMapper mapper = new ObjectMapper();
            Notebook notebook = mapper.readValue(notebookPath.toFile(), Notebook.class);
            List<String> sourceCode = notebook.getCellById(0).getSource();
            return sourceCode.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}