package ro.cs.tao.services.jupyter.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.services.jupyter.model.Cell;
import ro.cs.tao.services.jupyter.model.Notebook;
import ro.cs.tao.services.jupyter.model.NotebookParams;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        var mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        try {
            var json = mapper.writeValueAsString(notebook);
            Files.write(path.resolve("templateNotebook.ipynb"), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readNotebook(Path path)
    {
        Path notebookPath = path.resolve("templateNotebook.ipynb");
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
