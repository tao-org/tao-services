package ro.cs.tao.services.zeppelin.service;

import org.springframework.stereotype.Service;
import ro.cs.tao.services.zeppelin.model.Notebook;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParagraph;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParams;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

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
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
