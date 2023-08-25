package ro.cs.tao.services.jupyter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Notebook {
    private NotebookMetadata metadata = new NotebookMetadata();
    private int nbformat = 4;
    private int nbformat_minor = 5;
    private List<Cell> cells = Collections.singletonList(new Cell());

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

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
}
