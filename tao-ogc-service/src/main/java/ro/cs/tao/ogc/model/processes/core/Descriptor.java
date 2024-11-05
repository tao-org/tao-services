package ro.cs.tao.ogc.model.processes.core;

public abstract class Descriptor<E> {
    private String title;
    private Type type;
    private String description;
    private String format;
    private E defaultValue;
    private boolean nullable;
    private boolean readOnly;
    private boolean writeOnly;
    private boolean deprecated;
    private String contentMediaType;
    private String contentEncoding;
    private String contentSchema;

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getContentMediaType() {
        return contentMediaType;
    }

    public void setContentMediaType(String contentMediaType) {
        this.contentMediaType = contentMediaType;
    }

    public String getContentSchema() {
        return contentSchema;
    }

    public void setContentSchema(String contentSchema) {
        this.contentSchema = contentSchema;
    }

    public E getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(E defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isWriteOnly() {
        return writeOnly;
    }

    public void setWriteOnly(boolean writeOnly) {
        this.writeOnly = writeOnly;
    }
}
