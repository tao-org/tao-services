package ro.cs.tao.services.zeppelin.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import ro.cs.tao.services.zeppelin.model.Notebook;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParagraph;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service("zeppelinService")
public class ZeppelinService {

    public int generateNotebook(Path path, List<NotebookParams> ws) {
        Notebook notebook = new Notebook();
        ws.removeIf(NotebookParams::isNull);
        insertParam(notebook, ws);
        // deserialize
        writeNotebook(path, notebook);
        return 0;
    }

    public void insertParam(Notebook notebook, List<NotebookParams> params) {
        NotebookParagraph paragraph = notebook.getParagraphs().getFirst();
        StringBuilder paragraphContent = new StringBuilder();
//        for (NotebookParams param : params) {
//            paragraphContent.append(String.format("%s = %s\n", param.getName(), param.getDefaultValue()));
//        }
        paragraph.setText("%python print(\"Welcome To TAO\")");
    }

    public void writeNotebook(Path path, Notebook notebook) {
        var mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        try {
            var json = mapper.writeValueAsString(notebook);
            Files.write(path.resolve("templateNotebook.zpln"), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readNotebook(Path path)
    {
        Path notebookPath = path.resolve("templateNotebook.zpln");
        try {
            ObjectMapper mapper = new ObjectMapper();
            Notebook notebook = mapper.readValue(notebookPath.toFile(), Notebook.class);
            List<String> sourceCode = notebook.getParagraphs().stream().map(NotebookParagraph::getText).toList();
            return sourceCode.getFirst();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
