package ro.cs.tao.services.admin.beans;

import ro.cs.tao.topology.NodeFlavor;

/**
 * Represents a flavor configuration with additional details about storage and quantity.
 *
 * This class is primarily used to define the specifications of a resource flavor,
 * including its type, quantity, and storage details (both HDD and SSD).
 */
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
