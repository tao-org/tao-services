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
import java.util.logging.Logger;

@Service("zeppelinService")
public class ZeppelinService {

    private static final Logger logger = Logger.getLogger(ZeppelinService.class.getName());

    public Notebook generateNotebook(Path path, List<NotebookParams> ws) {
        Notebook notebook = new Notebook();
        ws.removeIf(NotebookParams::isNull);
        insertParam(notebook, ws);
        // deserialize
        writeNotebook(path, notebook);
        return notebook;
    }

    public void insertParam(Notebook notebook, List<NotebookParams> params) {
        NotebookParagraph paragraph = notebook.getParagraphs().getFirst();
        paragraph.setText("%python print(\"Welcome To TAO\")");
    }

    public void writeNotebook(Path path, Notebook notebook) {
        ObjectMapper mapper = getNotebookMapper();
        try {
            String json = mapper.writeValueAsString(notebook);
            Files.write(path.resolve("templateNotebook.zpln"), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readNotebook(Path path)
    {
        Path notebookPath = path.resolve(path);
        try {
            ObjectMapper mapper = new ObjectMapper();
            Notebook notebook = mapper.readValue(notebookPath.toFile(), Notebook.class);
            List<String> sourceCode = notebook.getParagraphs().stream().map(NotebookParagraph::getText).toList();
            return sourceCode.getFirst();
        } catch (Exception ex) {
            logger.finest("Exception in reading Apache Zeppelin notebook: " + ex.getMessage());
        }
        return null;
    }


    private ObjectMapper getNotebookMapper() {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
