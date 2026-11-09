package ro.cs.tao.services.zeppelin.model;

import ro.cs.tao.services.zeppelin.model.notebook.NotebookConfig;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookForms;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParagraph;
import ro.cs.tao.services.zeppelin.model.notebook.NotebookParams;

import java.util.ArrayList;
import java.util.List;

public class Notebook {
    private List<NotebookParagraph> paragraphs = new ArrayList<>();
    private String name = "TAO Python Notebook";
    private String id;
    private String defaultInterpreterGroup = "python";
    private String version = "0.12.0";
    private NotebookConfig config = new NotebookConfig();
    private NotebookParams noteParams = new NotebookParams();
    private NotebookForms noteForms = new NotebookForms();

    public Notebook() {
        paragraphs.add(new NotebookParagraph());
    }

    public List<NotebookParagraph> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<NotebookParagraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDefaultInterpreterGroup() {
        return defaultInterpreterGroup;
    }

    public void setDefaultInterpreterGroup(String defaultInterpreterGroup) {
        this.defaultInterpreterGroup = defaultInterpreterGroup;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public NotebookConfig getConfig() {
        return config;
    }

    public void setConfig(NotebookConfig config) {
        this.config = config;
    }

    public NotebookParams getNoteParams() {
        return noteParams;
    }

    public void setNoteParams(NotebookParams noteParams) {
        this.noteParams = noteParams;
    }

    public NotebookForms getNoteForms() {
        return noteForms;
    }

    public void setNoteForms(NotebookForms noteForms) {
        this.noteForms = noteForms;
    }
}

