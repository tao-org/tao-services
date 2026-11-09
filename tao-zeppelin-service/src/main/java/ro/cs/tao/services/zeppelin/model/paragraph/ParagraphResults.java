package ro.cs.tao.services.zeppelin.model.paragraph;

import java.util.ArrayList;
import java.util.List;

public class ParagraphResults {
    private String code;
    private List<ParagraphResultsMessage> msg = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<ParagraphResultsMessage> getMsg() {
        return msg;
    }
    public void addMsg(ParagraphResultsMessage msg){
        if(this.msg == null)
            this.msg = new ArrayList<>();
        this.msg.add(msg);
    }

    public void setMsg(List<ParagraphResultsMessage> msg) {
        this.msg = msg;
    }
}
