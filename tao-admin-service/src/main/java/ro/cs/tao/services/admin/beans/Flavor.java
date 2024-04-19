package ro.cs.tao.services.admin.beans;

import ro.cs.tao.topology.NodeFlavor;

public class Flavor {
    private NodeFlavor flavor;
    private int quantity;
    private Integer hdd;
    private Integer ssd;

    public NodeFlavor getFlavor() {
        return flavor;
    }

    public void setFlavor(NodeFlavor flavor) {
        this.flavor = flavor;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getHdd() {
        return hdd;
    }

    public void setHdd(Integer hdd) {
        this.hdd = hdd;
    }

    public Integer getSsd() {
        return ssd;
    }

    public void setSsd(Integer ssd) {
        this.ssd = ssd;
    }
}
