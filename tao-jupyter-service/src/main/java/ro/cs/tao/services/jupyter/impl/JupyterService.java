package ro.cs.tao.services.jupyter.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.jupyter.model.Notebook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service("jupyterService")
public class JupyterService
{
    public int generateNotebook(Notebook ws){

        var mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            var json = mapper.writeValueAsString(ws);
            // path to be updated in order to write to mapped drive
            Files.write(Paths.get("templateNotebook.ipynb").toFile().toPath(), json.getBytes());
            // mapper.writeValue(Paths.get("test_file.ipynb").toFile(), json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
