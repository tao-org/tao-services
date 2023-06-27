package ro.cs.tao.services.jupyter.model;

import java.util.List;

public class Worksheet {
    private Object metadata;
    private List<Cell> cells;

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }
}
