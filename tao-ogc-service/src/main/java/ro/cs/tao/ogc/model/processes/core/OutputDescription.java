package ro.cs.tao.ogc.model.processes.core;

public class OutputDescription extends DescriptionType {
    private Descriptor<?> schema;

    public Descriptor<?> getSchema() {
        return schema;
    }

    public void setSchema(Descriptor<?> schema) {
        this.schema = schema;
    }
}
