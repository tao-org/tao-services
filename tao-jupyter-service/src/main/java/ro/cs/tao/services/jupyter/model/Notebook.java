package ro.cs.tao.services.jupyter.model;

import java.util.List;

public class Notebook {
    private NotebookMetadata metadata;
    private int nbformat;
    private int nbformat_minor;
    private List<Worksheet> worksheets;

    public NotebookMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(NotebookMetadata metadata) {
        this.metadata = metadata;
    }

    public int getNbformat() {
        return nbformat;
    }

    public void setNbformat(int nbformat) {
        this.nbformat = nbformat;
    }

    public int getNbformat_minor() {
        return nbformat_minor;
    }

    public void setNbformat_minor(int nbformat_minor) {
        this.nbformat_minor = nbformat_minor;
    }

    public List<Worksheet> getWorksheets() {
        return worksheets;
    }

    public void setWorksheets(List<Worksheet> worksheets) {
        this.worksheets = worksheets;
    }
}
