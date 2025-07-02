package ro.cs.tao.services.zeppelin.model.paragraph;

public class ParagraphResultsMessage {
    private String type = "TEXT";
    private String data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
